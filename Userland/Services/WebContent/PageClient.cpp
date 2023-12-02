/*
 * Copyright (c) 2020-2023, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2022, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2023, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/ShareableBitmap.h>
#include <LibGfx/SystemTheme.h>
#include <LibWeb/CSS/SystemColor.h>
#include <LibWeb/Cookie/ParsedCookie.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/HTML/TraversableNavigable.h>
#include <LibWeb/Layout/Viewport.h>
#include <LibWeb/Painting/PaintableBox.h>
#include <LibWeb/Painting/PaintingCommandExecutorCPU.h>
#include <LibWeb/Painting/ViewportPaintable.h>
#include <LibWeb/Platform/Timer.h>
#include <WebContent/ConnectionFromClient.h>
#include <WebContent/PageClient.h>
#include <WebContent/PageHost.h>
#include <WebContent/WebContentClientEndpoint.h>
#include <WebContent/WebDriverConnection.h>

#ifdef HAS_ACCELERATED_GRAPHICS
#    include <LibWeb/Painting/PaintingCommandExecutorGPU.h>
#endif

namespace WebContent {

static bool s_use_gpu_painter = false;

void PageClient::set_use_gpu_painter()
{
    s_use_gpu_painter = true;
}

PageClient::PageClient(PageHost& owner, u64 id)
    : m_owner(owner)
    , m_page(make<Web::Page>(*this))
    , m_id(id)
{
    setup_palette();
    m_invalidation_coalescing_timer = Web::Platform::Timer::create_single_shot(0, [this] {
        client().async_did_invalidate_content_rect({ m_invalidation_rect.x().value(), m_invalidation_rect.y().value(), m_invalidation_rect.width().value(), m_invalidation_rect.height().value() });
        m_invalidation_rect = {};
    });
}

ConnectionFromClient& PageClient::client() const
{
    return m_owner.client();
}

void PageClient::set_has_focus(bool has_focus)
{
    m_has_focus = has_focus;
}

void PageClient::setup_palette()
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

bool PageClient::is_connection_open() const
{
    return client().is_open();
}

Gfx::Palette PageClient::palette() const
{
    return Gfx::Palette(*m_palette_impl);
}

void PageClient::set_palette_impl(Gfx::PaletteImpl& impl)
{
    m_palette_impl = impl;
    if (auto* document = page().top_level_browsing_context().active_document())
        document->invalidate_style();
}

void PageClient::set_preferred_color_scheme(Web::CSS::PreferredColorScheme color_scheme)
{
    m_preferred_color_scheme = color_scheme;
    if (auto* document = page().top_level_browsing_context().active_document())
        document->invalidate_style();
}

void PageClient::set_is_scripting_enabled(bool is_scripting_enabled)
{
    page().set_is_scripting_enabled(is_scripting_enabled);
}

void PageClient::set_window_position(Web::DevicePixelPoint position)
{
    page().set_window_position(position);
}

void PageClient::set_window_size(Web::DevicePixelSize size)
{
    page().set_window_size(size);
}

Web::Layout::Viewport* PageClient::layout_root()
{
    auto* document = page().top_level_browsing_context().active_document();
    if (!document)
        return nullptr;
    return document->layout_node();
}

Gfx::Color PageClient::background_color() const
{
    auto document = page().top_level_browsing_context().active_document();
    if (!document)
        return Gfx::Color::Transparent;
    return document->background_color();
}

void PageClient::paint(Web::DevicePixelRect const& content_rect, Gfx::Bitmap& target)
{
    Gfx::IntRect bitmap_rect { {}, content_rect.size().to_type<int>() };

    auto document = page().top_level_browsing_context().active_document();
    if (document) {
        document->update_layout();
    }

    auto background_color = this->background_color();

    Web::Painting::RecordingPainter recording_painter;
    Web::PaintContext context(recording_painter, palette(), device_pixels_per_css_pixel());

    if (background_color.alpha() < 255)
        recording_painter.fill_rect(bitmap_rect, Web::CSS::SystemColor::canvas());
    recording_painter.fill_rect(bitmap_rect, background_color);

    if (!document->paintable())
        return;

    context.set_should_show_line_box_borders(m_should_show_line_box_borders);
    context.set_device_viewport_rect(content_rect);
    context.set_has_focus(m_has_focus);
    document->paintable()->paint_all_phases(context);

    if (s_use_gpu_painter) {
#ifdef HAS_ACCELERATED_GRAPHICS
        Web::Painting::PaintingCommandExecutorGPU painting_command_executor(target);
        recording_painter.execute(painting_command_executor);
#else
        static bool has_warned_about_configuration = false;

        if (!has_warned_about_configuration) {
            warnln("\033[31;1mConfigured to use GPU painter, but current platform does not have accelerated graphics\033[0m");
            has_warned_about_configuration = true;
        }
#endif
    } else {
        Web::Painting::PaintingCommandExecutorCPU painting_command_executor(target);
        recording_painter.execute(painting_command_executor);
    }
}

void PageClient::set_viewport_rect(Web::DevicePixelRect const& rect)
{
    page().top_level_traversable()->set_viewport_rect(page().device_to_css_rect(rect));
}

void PageClient::page_did_invalidate(Web::CSSPixelRect const& content_rect)
{
    m_invalidation_rect = m_invalidation_rect.united(page().enclosing_device_rect(content_rect));
    if (!m_invalidation_coalescing_timer->is_active())
        m_invalidation_coalescing_timer->start();
}

void PageClient::page_did_change_selection()
{
    client().async_did_change_selection();
}

void PageClient::page_did_request_cursor_change(Gfx::StandardCursor cursor)
{
    client().async_did_request_cursor_change((u32)cursor);
}

void PageClient::page_did_layout()
{
    auto* layout_root = this->layout_root();
    VERIFY(layout_root);
    if (layout_root->paintable_box()->has_scrollable_overflow())
        m_content_size = page().enclosing_device_rect(layout_root->paintable_box()->scrollable_overflow_rect().value()).size();
    else
        m_content_size = page().enclosing_device_rect(layout_root->paintable_box()->absolute_rect()).size();
    client().async_did_layout(m_content_size.to_type<int>());
}

void PageClient::page_did_change_title(DeprecatedString const& title)
{
    client().async_did_change_title(title);
}

void PageClient::page_did_request_navigate_back()
{
    client().async_did_request_navigate_back();
}

void PageClient::page_did_request_navigate_forward()
{
    client().async_did_request_navigate_forward();
}

void PageClient::page_did_request_refresh()
{
    client().async_did_request_refresh();
}

Gfx::IntSize PageClient::page_did_request_resize_window(Gfx::IntSize size)
{
    return client().did_request_resize_window(size);
}

Gfx::IntPoint PageClient::page_did_request_reposition_window(Gfx::IntPoint position)
{
    return client().did_request_reposition_window(position);
}

void PageClient::page_did_request_restore_window()
{
    client().async_did_request_restore_window();
}

Gfx::IntRect PageClient::page_did_request_maximize_window()
{
    return client().did_request_maximize_window();
}

Gfx::IntRect PageClient::page_did_request_minimize_window()
{
    return client().did_request_minimize_window();
}

Gfx::IntRect PageClient::page_did_request_fullscreen_window()
{
    return client().did_request_fullscreen_window();
}

void PageClient::page_did_request_scroll(i32 x_delta, i32 y_delta)
{
    client().async_did_request_scroll(x_delta, y_delta);
}

void PageClient::page_did_request_scroll_to(Web::CSSPixelPoint scroll_position)
{
    auto device_scroll_position = page().css_to_device_point(scroll_position);
    client().async_did_request_scroll_to(device_scroll_position.to_type<int>());
}

void PageClient::page_did_request_scroll_into_view(Web::CSSPixelRect const& rect)
{
    auto device_pixel_rect = page().enclosing_device_rect(rect);
    client().async_did_request_scroll_into_view({ device_pixel_rect.x().value(),
        device_pixel_rect.y().value(),
        device_pixel_rect.width().value(),
        device_pixel_rect.height().value() });
}

void PageClient::page_did_enter_tooltip_area(Web::CSSPixelPoint content_position, DeprecatedString const& title)
{
    client().async_did_enter_tooltip_area({ content_position.x().to_int(), content_position.y().to_int() }, title);
}

void PageClient::page_did_leave_tooltip_area()
{
    client().async_did_leave_tooltip_area();
}

void PageClient::page_did_hover_link(const URL& url)
{
    client().async_did_hover_link(url);
}

void PageClient::page_did_unhover_link()
{
    client().async_did_unhover_link();
}

void PageClient::page_did_click_link(const URL& url, DeprecatedString const& target, unsigned modifiers)
{
    client().async_did_click_link(url, target, modifiers);
}

void PageClient::page_did_middle_click_link(const URL& url, [[maybe_unused]] DeprecatedString const& target, [[maybe_unused]] unsigned modifiers)
{
    client().async_did_middle_click_link(url, target, modifiers);
}

void PageClient::page_did_start_loading(const URL& url, bool is_redirect)
{
    client().async_did_start_loading(url, is_redirect);
}

void PageClient::page_did_create_new_document(Web::DOM::Document& document)
{
    client().initialize_js_console({}, document);
}

void PageClient::page_did_destroy_document(Web::DOM::Document& document)
{
    client().destroy_js_console({}, document);
}

void PageClient::page_did_finish_loading(const URL& url)
{
    client().async_did_finish_loading(url);
}

void PageClient::page_did_finish_text_test()
{
    client().async_did_finish_text_test();
}

void PageClient::page_did_request_context_menu(Web::CSSPixelPoint content_position)
{
    client().async_did_request_context_menu(page().css_to_device_point(content_position).to_type<int>());
}

void PageClient::page_did_request_link_context_menu(Web::CSSPixelPoint content_position, URL const& url, DeprecatedString const& target, unsigned modifiers)
{
    client().async_did_request_link_context_menu(page().css_to_device_point(content_position).to_type<int>(), url, target, modifiers);
}

void PageClient::page_did_request_image_context_menu(Web::CSSPixelPoint content_position, URL const& url, DeprecatedString const& target, unsigned modifiers, Gfx::Bitmap const* bitmap_pointer)
{
    auto bitmap = bitmap_pointer ? bitmap_pointer->to_shareable_bitmap() : Gfx::ShareableBitmap();
    client().async_did_request_image_context_menu(page().css_to_device_point(content_position).to_type<int>(), url, target, modifiers, bitmap);
}

void PageClient::page_did_request_media_context_menu(Web::CSSPixelPoint content_position, DeprecatedString const& target, unsigned modifiers, Web::Page::MediaContextMenu menu)
{
    client().async_did_request_media_context_menu(page().css_to_device_point(content_position).to_type<int>(), target, modifiers, move(menu));
}

void PageClient::page_did_request_alert(String const& message)
{
    client().async_did_request_alert(message);
}

void PageClient::alert_closed()
{
    page().alert_closed();
}

void PageClient::page_did_request_confirm(String const& message)
{
    client().async_did_request_confirm(message);
}

void PageClient::confirm_closed(bool accepted)
{
    page().confirm_closed(accepted);
}

void PageClient::page_did_request_prompt(String const& message, String const& default_)
{
    client().async_did_request_prompt(message, default_);
}

void PageClient::page_did_request_set_prompt_text(String const& text)
{
    client().async_did_request_set_prompt_text(text);
}

void PageClient::prompt_closed(Optional<String> response)
{
    page().prompt_closed(move(response));
}

void PageClient::color_picker_closed(Optional<Color> picked_color)
{
    page().color_picker_closed(picked_color);
}

Web::WebIDL::ExceptionOr<void> PageClient::toggle_media_play_state()
{
    return page().toggle_media_play_state();
}

void PageClient::toggle_media_mute_state()
{
    page().toggle_media_mute_state();
}

Web::WebIDL::ExceptionOr<void> PageClient::toggle_media_loop_state()
{
    return page().toggle_media_loop_state();
}

Web::WebIDL::ExceptionOr<void> PageClient::toggle_media_controls_state()
{
    return page().toggle_media_controls_state();
}

void PageClient::set_user_style(String source)
{
    page().set_user_style(source);
}

void PageClient::page_did_request_accept_dialog()
{
    client().async_did_request_accept_dialog();
}

void PageClient::page_did_request_dismiss_dialog()
{
    client().async_did_request_dismiss_dialog();
}

void PageClient::page_did_change_favicon(Gfx::Bitmap const& favicon)
{
    client().async_did_change_favicon(favicon.to_shareable_bitmap());
}

Vector<Web::Cookie::Cookie> PageClient::page_did_request_all_cookies(URL const& url)
{
    return client().did_request_all_cookies(url);
}

Optional<Web::Cookie::Cookie> PageClient::page_did_request_named_cookie(URL const& url, DeprecatedString const& name)
{
    return client().did_request_named_cookie(url, name);
}

DeprecatedString PageClient::page_did_request_cookie(const URL& url, Web::Cookie::Source source)
{
    auto response = client().send_sync_but_allow_failure<Messages::WebContentClient::DidRequestCookie>(move(url), static_cast<u8>(source));
    if (!response) {
        dbgln("WebContent client disconnected during DidRequestCookie. Exiting peacefully.");
        exit(0);
    }
    return response->take_cookie();
}

void PageClient::page_did_set_cookie(const URL& url, Web::Cookie::ParsedCookie const& cookie, Web::Cookie::Source source)
{
    client().async_did_set_cookie(url, cookie, static_cast<u8>(source));
}

void PageClient::page_did_update_cookie(Web::Cookie::Cookie cookie)
{
    client().async_did_update_cookie(move(cookie));
}

void PageClient::page_did_update_resource_count(i32 count_waiting)
{
    client().async_did_update_resource_count(count_waiting);
}

String PageClient::page_did_request_new_tab(Web::HTML::ActivateTab activate_tab)
{
    return client().did_request_new_tab(activate_tab);
}

void PageClient::page_did_request_activate_tab()
{
    client().async_did_request_activate_tab();
}

void PageClient::page_did_close_browsing_context(Web::HTML::BrowsingContext const&)
{
    client().async_did_close_browsing_context();
}

void PageClient::request_file(Web::FileRequest file_request)
{
    client().request_file(move(file_request));
}

void PageClient::page_did_request_color_picker(Color current_color)
{
    client().async_did_request_color_picker(current_color);
}

void PageClient::page_did_change_theme_color(Gfx::Color color)
{
    client().async_did_change_theme_color(color);
}

void PageClient::page_did_insert_clipboard_entry(String data, String presentation_style, String mime_type)
{
    client().async_did_insert_clipboard_entry(move(data), move(presentation_style), move(mime_type));
}

void PageClient::inspector_did_load()
{
    client().async_inspector_did_load();
}

void PageClient::inspector_did_select_dom_node(i32 node_id, Optional<Web::CSS::Selector::PseudoElement> const& pseudo_element)
{
    client().async_inspector_did_select_dom_node(node_id, pseudo_element);
}

void PageClient::inspector_did_execute_console_script(String const& script)
{
    client().async_inspector_did_execute_console_script(script);
}

ErrorOr<void> PageClient::connect_to_webdriver(DeprecatedString const& webdriver_ipc_path)
{
    VERIFY(!m_webdriver);
    m_webdriver = TRY(WebDriverConnection::connect(*this, webdriver_ipc_path));

    if (m_owner.on_webdriver_connection)
        m_owner.on_webdriver_connection(*m_webdriver);

    return {};
}

}
