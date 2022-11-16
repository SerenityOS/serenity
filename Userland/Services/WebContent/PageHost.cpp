/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "PageHost.h"
#include "ConnectionFromClient.h"
#include <AK/SourceLocation.h>
#include <LibGfx/Painter.h>
#include <LibGfx/ShareableBitmap.h>
#include <LibGfx/SystemTheme.h>
#include <LibWeb/Cookie/ParsedCookie.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/HTML/EventLoop/EventLoop.h>
#include <LibWeb/HTML/Scripting/Environments.h>
#include <LibWeb/Layout/InitialContainingBlock.h>
#include <LibWeb/Painting/PaintableBox.h>
#include <LibWeb/Platform/EventLoopPlugin.h>
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
        m_client.async_did_invalidate_content_rect(m_invalidation_rect);
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

void PageHost::set_window_position(Gfx::IntPoint const& position)
{
    page().set_window_position(position);
}

void PageHost::set_window_size(Gfx::IntSize const& size)
{
    page().set_window_size(size);
}

ErrorOr<void> PageHost::connect_to_webdriver(String const& webdriver_ipc_path)
{
    VERIFY(!m_webdriver);
    m_webdriver = TRY(WebDriverConnection::connect(m_client, *this, webdriver_ipc_path));
    return {};
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
    if (layout_root->paint_box()->has_overflow())
        m_content_size = enclosing_int_rect(layout_root->paint_box()->scrollable_overflow_rect().value()).size();
    else
        m_content_size = enclosing_int_rect(layout_root->paint_box()->absolute_rect()).size();
    m_client.async_did_layout(m_content_size);
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

template<typename ResponseType>
static ResponseType spin_event_loop_until_dialog_closed(ConnectionFromClient& client, Optional<ResponseType>& response, SourceLocation location = SourceLocation::current())
{
    auto& event_loop = Web::HTML::current_settings_object().responsible_event_loop();

    ScopeGuard guard { [&] { event_loop.set_execution_paused(false); } };
    event_loop.set_execution_paused(true);

    Web::Platform::EventLoopPlugin::the().spin_until([&]() {
        return response.has_value() || !client.is_open();
    });

    if (!client.is_open()) {
        dbgln("WebContent client disconnected during {}. Exiting peacefully.", location.function_name());
        exit(0);
    }

    return response.release_value();
}

void PageHost::page_did_request_alert(String const& message)
{
    m_pending_dialog = PendingDialog::Alert;
    m_client.async_did_request_alert(message);

    spin_event_loop_until_dialog_closed(m_client, m_pending_alert_response);
}

void PageHost::alert_closed()
{
    if (m_pending_dialog == PendingDialog::Alert) {
        m_pending_dialog = PendingDialog::None;
        m_pending_alert_response = Empty {};
    }
}

bool PageHost::page_did_request_confirm(String const& message)
{
    m_pending_dialog = PendingDialog::Confirm;
    m_client.async_did_request_confirm(message);

    return spin_event_loop_until_dialog_closed(m_client, m_pending_confirm_response);
}

void PageHost::confirm_closed(bool accepted)
{
    if (m_pending_dialog == PendingDialog::Confirm) {
        m_pending_dialog = PendingDialog::None;
        m_pending_confirm_response = accepted;
    }
}

String PageHost::page_did_request_prompt(String const& message, String const& default_)
{
    m_pending_dialog = PendingDialog::Prompt;
    m_client.async_did_request_prompt(message, default_);

    return spin_event_loop_until_dialog_closed(m_client, m_pending_prompt_response);
}

void PageHost::prompt_closed(String response)
{
    if (m_pending_dialog == PendingDialog::Prompt) {
        m_pending_dialog = PendingDialog::None;
        m_pending_prompt_response = move(response);
    }
}

void PageHost::dismiss_dialog()
{
    switch (m_pending_dialog) {
    case PendingDialog::None:
        break;
    case PendingDialog::Alert:
        m_client.async_did_request_accept_dialog();
        break;
    case PendingDialog::Confirm:
    case PendingDialog::Prompt:
        m_client.async_did_request_dismiss_dialog();
        break;
    }
}

void PageHost::accept_dialog()
{
    switch (m_pending_dialog) {
    case PendingDialog::None:
        break;
    case PendingDialog::Alert:
    case PendingDialog::Confirm:
    case PendingDialog::Prompt:
        m_client.async_did_request_accept_dialog();
        break;
    }
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
