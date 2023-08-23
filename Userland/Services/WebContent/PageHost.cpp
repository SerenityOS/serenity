/*
 * Copyright (c) 2020-2023, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "PageHost.h"
#include "ConnectionFromClient.h"
#include <LibGfx/Painter.h>
#include <LibGfx/ShareableBitmap.h>
#include <LibGfx/SystemTheme.h>
#include <LibWeb/CSS/SystemColor.h>
#include <LibWeb/Cookie/ParsedCookie.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/Layout/Viewport.h>
#include <LibWeb/Painting/PaintableBox.h>
#include <LibWeb/Painting/ViewportPaintable.h>
#include <LibWeb/Platform/Timer.h>
#include <WebContent/WebContentClientEndpoint.h>
#include <WebContent/WebDriverConnection.h>

namespace WebContent {

PageHost::PageHost(ConnectionFromClient& client)
    : m_client(client)
    , m_page(make<Web::Page>(*this))
{
    setup_palette();
    m_invalidation_coalescing_timer = Web::Platform::Timer::create_single_shot(0, [this] {
        m_client.async_did_invalidate_content_rect({ m_invalidation_rect.x().value(), m_invalidation_rect.y().value(), m_invalidation_rect.width().value(), m_invalidation_rect.height().value() });
        m_invalidation_rect = {};
    });
}

PageHost::~PageHost() = default;

void PageHost::set_has_focus(bool has_focus)
{
    m_has_focus = has_focus;
}

void PageHost::setup_palette()
{
    // FIXME: Get the proper palette from our peer somehow
    auto buffer_or_error = Core::AnonymousBuffer::create_with_size(sizeof(Gfx::SystemTheme));
    VERIFY(!buffer_or_error.is_error());
    auto buffer = buffer_or_error.release_value();
    auto* theme = buffer.data<Gfx::SystemTheme>();
    theme->color[(int)Gfx::ColorRole::Window] = Color::Magenta;
    theme->color[(int)Gfx::ColorRole::WindowText] = Color::Cyan;
    m_palette_impl = Gfx::PaletteImpl::create_with_anonymous_buffer(buffer);
}

bool PageHost::is_connection_open() const
{
    return m_client.is_open();
}

Gfx::Palette PageHost::palette() const
{
    return Gfx::Palette(*m_palette_impl);
}

void PageHost::set_palette_impl(Gfx::PaletteImpl& impl)
{
    m_palette_impl = impl;
    if (auto* document = page().top_level_browsing_context().active_document())
        document->invalidate_style();
}

void PageHost::set_preferred_color_scheme(Web::CSS::PreferredColorScheme color_scheme)
{
    m_preferred_color_scheme = color_scheme;
    if (auto* document = page().top_level_browsing_context().active_document())
        document->invalidate_style();
}

void PageHost::set_is_scripting_enabled(bool is_scripting_enabled)
{
    page().set_is_scripting_enabled(is_scripting_enabled);
}

void PageHost::set_window_position(Web::DevicePixelPoint position)
{
    page().set_window_position(position);
}

void PageHost::set_window_size(Web::DevicePixelSize size)
{
    page().set_window_size(size);
}

ErrorOr<void> PageHost::connect_to_webdriver(DeprecatedString const& webdriver_ipc_path)
{
    VERIFY(!m_webdriver);
    m_webdriver = TRY(WebDriverConnection::connect(*this, webdriver_ipc_path));

    if (on_webdriver_connection)
        on_webdriver_connection(*m_webdriver);

    return {};
}

Web::Layout::Viewport* PageHost::layout_root()
{
    auto* document = page().top_level_browsing_context().active_document();
    if (!document)
        return nullptr;
    return document->layout_node();
}

Gfx::Color PageHost::background_color() const
{
    auto document = page().top_level_browsing_context().active_document();
    if (!document)
        return Gfx::Color::Transparent;
    return document->background_color();
}

void PageHost::paint(Web::DevicePixelRect const& content_rect, Gfx::Bitmap& target)
{
    Gfx::Painter painter(target);
    Gfx::IntRect bitmap_rect { {}, content_rect.size().to_type<int>() };

    auto document = page().top_level_browsing_context().active_document();
    if (document) {
        document->update_layout();
    }

    auto background_color = this->background_color();

    if (background_color.alpha() < 255)
        painter.clear_rect(bitmap_rect, Web::CSS::SystemColor::canvas());
    painter.fill_rect(bitmap_rect, background_color);

    if (!document->paintable())
        return;

    Web::PaintContext context(painter, palette(), device_pixels_per_css_pixel());
    context.set_should_show_line_box_borders(m_should_show_line_box_borders);
    context.set_device_viewport_rect(content_rect);
    context.set_has_focus(m_has_focus);
    document->paintable()->paint_all_phases(context);
}

void PageHost::set_viewport_rect(Web::DevicePixelRect const& rect)
{
    page().top_level_browsing_context().set_viewport_rect(page().device_to_css_rect(rect));
}

void PageHost::page_did_invalidate(Web::CSSPixelRect const& content_rect)
{
    m_invalidation_rect = m_invalidation_rect.united(page().enclosing_device_rect(content_rect));
    if (!m_invalidation_coalescing_timer->is_active())
        m_invalidation_coalescing_timer->start();
}

void PageHost::page_did_change_selection()
{
    m_client.async_did_change_selection();
}

void PageHost::page_did_request_cursor_change(Gfx::StandardCursor cursor)
{
    m_client.async_did_request_cursor_change((u32)cursor);
}

void PageHost::page_did_layout()
{
    auto* layout_root = this->layout_root();
    VERIFY(layout_root);
    if (layout_root->paintable_box()->has_scrollable_overflow())
        m_content_size = page().enclosing_device_rect(layout_root->paintable_box()->scrollable_overflow_rect().value()).size();
    else
        m_content_size = page().enclosing_device_rect(layout_root->paintable_box()->absolute_rect()).size();
    m_client.async_did_layout(m_content_size.to_type<int>());
}

void PageHost::page_did_change_title(DeprecatedString const& title)
{
    m_client.async_did_change_title(title);
}

void PageHost::page_did_request_navigate_back()
{
    m_client.async_did_request_navigate_back();
}

void PageHost::page_did_request_navigate_forward()
{
    m_client.async_did_request_navigate_forward();
}

void PageHost::page_did_request_refresh()
{
    m_client.async_did_request_refresh();
}

Gfx::IntSize PageHost::page_did_request_resize_window(Gfx::IntSize size)
{
    return m_client.did_request_resize_window(size);
}

Gfx::IntPoint PageHost::page_did_request_reposition_window(Gfx::IntPoint position)
{
    return m_client.did_request_reposition_window(position);
}

void PageHost::page_did_request_restore_window()
{
    m_client.async_did_request_restore_window();
}

Gfx::IntRect PageHost::page_did_request_maximize_window()
{
    return m_client.did_request_maximize_window();
}

Gfx::IntRect PageHost::page_did_request_minimize_window()
{
    return m_client.did_request_minimize_window();
}

Gfx::IntRect PageHost::page_did_request_fullscreen_window()
{
    return m_client.did_request_fullscreen_window();
}

void PageHost::page_did_request_scroll(i32 x_delta, i32 y_delta)
{
    m_client.async_did_request_scroll(x_delta, y_delta);
}

void PageHost::page_did_request_scroll_to(Web::CSSPixelPoint scroll_position)
{
    m_client.async_did_request_scroll_to({ scroll_position.x().to_int(), scroll_position.y().to_int() });
}

void PageHost::page_did_request_scroll_into_view(Web::CSSPixelRect const& rect)
{
    auto device_pixel_rect = page().enclosing_device_rect(rect);
    m_client.async_did_request_scroll_into_view({ device_pixel_rect.x().value(),
        device_pixel_rect.y().value(),
        device_pixel_rect.width().value(),
        device_pixel_rect.height().value() });
}

void PageHost::page_did_enter_tooltip_area(Web::CSSPixelPoint content_position, DeprecatedString const& title)
{
    m_client.async_did_enter_tooltip_area({ content_position.x().to_int(), content_position.y().to_int() }, title);
}

void PageHost::page_did_leave_tooltip_area()
{
    m_client.async_did_leave_tooltip_area();
}

void PageHost::page_did_hover_link(const URL& url)
{
    m_client.async_did_hover_link(url);
}

void PageHost::page_did_unhover_link()
{
    m_client.async_did_unhover_link();
}

void PageHost::page_did_click_link(const URL& url, DeprecatedString const& target, unsigned modifiers)
{
    m_client.async_did_click_link(url, target, modifiers);
}

void PageHost::page_did_middle_click_link(const URL& url, [[maybe_unused]] DeprecatedString const& target, [[maybe_unused]] unsigned modifiers)
{
    m_client.async_did_middle_click_link(url, target, modifiers);
}

void PageHost::page_did_start_loading(const URL& url, bool is_redirect)
{
    m_client.async_did_start_loading(url, is_redirect);
}

void PageHost::page_did_create_main_document()
{
    m_client.initialize_js_console({});
}

void PageHost::page_did_finish_loading(const URL& url)
{
    m_client.async_did_finish_loading(url);
}

void PageHost::page_did_request_context_menu(Web::CSSPixelPoint content_position)
{
    m_client.async_did_request_context_menu(page().css_to_device_point(content_position).to_type<int>());
}

void PageHost::page_did_request_link_context_menu(Web::CSSPixelPoint content_position, URL const& url, DeprecatedString const& target, unsigned modifiers)
{
    m_client.async_did_request_link_context_menu(page().css_to_device_point(content_position).to_type<int>(), url, target, modifiers);
}

void PageHost::page_did_request_image_context_menu(Web::CSSPixelPoint content_position, URL const& url, DeprecatedString const& target, unsigned modifiers, Gfx::Bitmap const* bitmap_pointer)
{
    auto bitmap = bitmap_pointer ? bitmap_pointer->to_shareable_bitmap() : Gfx::ShareableBitmap();
    m_client.async_did_request_image_context_menu(page().css_to_device_point(content_position).to_type<int>(), url, target, modifiers, bitmap);
}

void PageHost::page_did_request_media_context_menu(Web::CSSPixelPoint content_position, DeprecatedString const& target, unsigned modifiers, Web::Page::MediaContextMenu menu)
{
    m_client.async_did_request_media_context_menu(page().css_to_device_point(content_position).to_type<int>(), target, modifiers, move(menu));
}

void PageHost::page_did_request_alert(String const& message)
{
    m_client.async_did_request_alert(message);
}

void PageHost::alert_closed()
{
    page().alert_closed();
}

void PageHost::page_did_request_confirm(String const& message)
{
    m_client.async_did_request_confirm(message);
}

void PageHost::confirm_closed(bool accepted)
{
    page().confirm_closed(accepted);
}

void PageHost::page_did_request_prompt(String const& message, String const& default_)
{
    m_client.async_did_request_prompt(message, default_);
}

void PageHost::page_did_request_set_prompt_text(String const& text)
{
    m_client.async_did_request_set_prompt_text(text);
}

void PageHost::prompt_closed(Optional<String> response)
{
    page().prompt_closed(move(response));
}

Web::WebIDL::ExceptionOr<void> PageHost::toggle_media_play_state()
{
    return page().toggle_media_play_state();
}

void PageHost::toggle_media_mute_state()
{
    page().toggle_media_mute_state();
}

Web::WebIDL::ExceptionOr<void> PageHost::toggle_media_loop_state()
{
    return page().toggle_media_loop_state();
}

Web::WebIDL::ExceptionOr<void> PageHost::toggle_media_controls_state()
{
    return page().toggle_media_controls_state();
}

void PageHost::set_user_style(String source)
{
    page().set_user_style(source);
}

void PageHost::page_did_request_accept_dialog()
{
    m_client.async_did_request_accept_dialog();
}

void PageHost::page_did_request_dismiss_dialog()
{
    m_client.async_did_request_dismiss_dialog();
}

void PageHost::page_did_change_favicon(Gfx::Bitmap const& favicon)
{
    m_client.async_did_change_favicon(favicon.to_shareable_bitmap());
}

Vector<Web::Cookie::Cookie> PageHost::page_did_request_all_cookies(URL const& url)
{
    return m_client.did_request_all_cookies(url);
}

Optional<Web::Cookie::Cookie> PageHost::page_did_request_named_cookie(URL const& url, DeprecatedString const& name)
{
    return m_client.did_request_named_cookie(url, name);
}

DeprecatedString PageHost::page_did_request_cookie(const URL& url, Web::Cookie::Source source)
{
    auto response = m_client.send_sync_but_allow_failure<Messages::WebContentClient::DidRequestCookie>(move(url), static_cast<u8>(source));
    if (!response) {
        dbgln("WebContent client disconnected during DidRequestCookie. Exiting peacefully.");
        exit(0);
    }
    return response->take_cookie();
}

void PageHost::page_did_set_cookie(const URL& url, Web::Cookie::ParsedCookie const& cookie, Web::Cookie::Source source)
{
    m_client.async_did_set_cookie(url, cookie, static_cast<u8>(source));
}

void PageHost::page_did_update_cookie(Web::Cookie::Cookie cookie)
{
    m_client.async_did_update_cookie(move(cookie));
}

void PageHost::page_did_update_resource_count(i32 count_waiting)
{
    m_client.async_did_update_resource_count(count_waiting);
}

String PageHost::page_did_request_new_tab(Web::HTML::ActivateTab activate_tab)
{
    return m_client.did_request_new_tab(activate_tab);
}

void PageHost::page_did_request_activate_tab()
{
    m_client.async_did_request_activate_tab();
}

void PageHost::page_did_close_browsing_context(Web::HTML::BrowsingContext const&)
{
    m_client.async_did_close_browsing_context();
}

void PageHost::request_file(Web::FileRequest file_request)
{
    m_client.request_file(move(file_request));
}

}
