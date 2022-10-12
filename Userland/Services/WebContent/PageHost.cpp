/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "PageHost.h"
#include "ConnectionFromClient.h"
#include <LibGfx/Painter.h>
#include <LibGfx/ShareableBitmap.h>
#include <LibGfx/SystemTheme.h>
#include <LibWeb/Cookie/ParsedCookie.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/Layout/InitialContainingBlock.h>
#include <LibWeb/Painting/PaintableBox.h>
#include <LibWeb/Platform/Timer.h>
#include <WebContent/WebContentClientEndpoint.h>

namespace WebContent {

PageHost::PageHost(ConnectionFromClient& client)
    : m_client(client)
    , m_page(make<Web::Page>(*this))
{
    setup_palette();
    m_invalidation_coalescing_timer = Web::Platform::Timer::create_single_shot(0, [this] {
        m_client.async_did_invalidate_content_rect(m_invalidation_rect);
        m_invalidation_rect = {};
    });
}

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

Gfx::Palette PageHost::palette() const
{
    return Gfx::Palette(*m_palette_impl);
}

void PageHost::set_palette_impl(Gfx::PaletteImpl const& impl)
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

void PageHost::set_is_webdriver_active(bool is_webdriver_active)
{
    page().set_is_webdriver_active(is_webdriver_active);
}

Web::Layout::InitialContainingBlock* PageHost::layout_root()
{
    auto* document = page().top_level_browsing_context().active_document();
    if (!document)
        return nullptr;
    return document->layout_node();
}

void PageHost::paint(Gfx::IntRect const& content_rect, Gfx::Bitmap& target)
{
    Gfx::Painter painter(target);
    Gfx::IntRect bitmap_rect { {}, content_rect.size() };

    if (auto* document = page().top_level_browsing_context().active_document())
        document->update_layout();

    auto* layout_root = this->layout_root();
    if (!layout_root) {
        painter.fill_rect(bitmap_rect, palette().base());
        return;
    }

    Web::PaintContext context(painter, palette(), content_rect.top_left());
    context.set_should_show_line_box_borders(m_should_show_line_box_borders);
    context.set_viewport_rect(content_rect);
    context.set_has_focus(m_has_focus);
    layout_root->paint_all_phases(context);
}

void PageHost::set_viewport_rect(Gfx::IntRect const& rect)
{
    page().top_level_browsing_context().set_viewport_rect(rect);
}

void PageHost::page_did_invalidate(Gfx::IntRect const& content_rect)
{
    m_invalidation_rect = m_invalidation_rect.united(content_rect);
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
    Gfx::IntSize content_size;
    if (layout_root->paint_box()->has_overflow())
        content_size = enclosing_int_rect(layout_root->paint_box()->scrollable_overflow_rect().value()).size();
    else
        content_size = enclosing_int_rect(layout_root->paint_box()->absolute_rect()).size();
    m_client.async_did_layout(content_size);
}

void PageHost::page_did_change_title(String const& title)
{
    m_client.async_did_change_title(title);
}

void PageHost::page_did_request_scroll(i32 x_delta, i32 y_delta)
{
    m_client.async_did_request_scroll(x_delta, y_delta);
}

void PageHost::page_did_request_scroll_to(Gfx::IntPoint const& scroll_position)
{
    m_client.async_did_request_scroll_to(scroll_position);
}

void PageHost::page_did_request_scroll_into_view(Gfx::IntRect const& rect)
{
    m_client.async_did_request_scroll_into_view(rect);
}

void PageHost::page_did_enter_tooltip_area(Gfx::IntPoint const& content_position, String const& title)
{
    m_client.async_did_enter_tooltip_area(content_position, title);
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

void PageHost::page_did_click_link(const URL& url, String const& target, unsigned modifiers)
{
    m_client.async_did_click_link(url, target, modifiers);
}

void PageHost::page_did_middle_click_link(const URL& url, [[maybe_unused]] String const& target, [[maybe_unused]] unsigned modifiers)
{
    m_client.async_did_middle_click_link(url, target, modifiers);
}

void PageHost::page_did_start_loading(const URL& url)
{
    m_client.async_did_start_loading(url);
}

void PageHost::page_did_create_main_document()
{
    m_client.initialize_js_console({});
}

void PageHost::page_did_finish_loading(const URL& url)
{
    m_client.async_did_finish_loading(url);
}

void PageHost::page_did_request_context_menu(Gfx::IntPoint const& content_position)
{
    m_client.async_did_request_context_menu(content_position);
}

void PageHost::page_did_request_link_context_menu(Gfx::IntPoint const& content_position, const URL& url, String const& target, unsigned modifiers)
{
    m_client.async_did_request_link_context_menu(content_position, url, target, modifiers);
}

void PageHost::page_did_request_alert(String const& message)
{
    auto response = m_client.send_sync_but_allow_failure<Messages::WebContentClient::DidRequestAlert>(message);
    if (!response) {
        dbgln("WebContent client disconnected during DidRequestAlert. Exiting peacefully.");
        exit(0);
    }
}

bool PageHost::page_did_request_confirm(String const& message)
{
    auto response = m_client.send_sync_but_allow_failure<Messages::WebContentClient::DidRequestConfirm>(message);
    if (!response) {
        dbgln("WebContent client disconnected during DidRequestConfirm. Exiting peacefully.");
        exit(0);
    }
    return response->take_result();
}

String PageHost::page_did_request_prompt(String const& message, String const& default_)
{
    auto response = m_client.send_sync_but_allow_failure<Messages::WebContentClient::DidRequestPrompt>(message, default_);
    if (!response) {
        dbgln("WebContent client disconnected during DidRequestPrompt. Exiting peacefully.");
        exit(0);
    }
    return response->take_response();
}

void PageHost::page_did_change_favicon(Gfx::Bitmap const& favicon)
{
    m_client.async_did_change_favicon(favicon.to_shareable_bitmap());
}

void PageHost::page_did_request_image_context_menu(Gfx::IntPoint const& content_position, const URL& url, String const& target, unsigned modifiers, Gfx::Bitmap const* bitmap_pointer)
{
    auto bitmap = bitmap_pointer ? bitmap_pointer->to_shareable_bitmap() : Gfx::ShareableBitmap();
    m_client.async_did_request_image_context_menu(content_position, url, target, modifiers, bitmap);
}

String PageHost::page_did_request_cookie(const URL& url, Web::Cookie::Source source)
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

void PageHost::page_did_update_resource_count(i32 count_waiting)
{
    m_client.async_did_update_resource_count(count_waiting);
}

void PageHost::request_file(NonnullRefPtr<Web::FileRequest>& file_request)
{
    m_client.request_file(file_request);
}

}
