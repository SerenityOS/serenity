/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "WebContentClient.h"
#include "OutOfProcessWebView.h"
#include <AK/Debug.h>
#include <LibWeb/Cookie/ParsedCookie.h>

namespace Web {

WebContentClient::WebContentClient(NonnullOwnPtr<Core::Stream::LocalSocket> socket, OutOfProcessWebView& view)
    : IPC::ServerConnection<WebContentClientEndpoint, WebContentServerEndpoint>(*this, move(socket))
    , m_view(view)
{
}

void WebContentClient::die()
{
    VERIFY(on_web_content_process_crash);
    on_web_content_process_crash();
}

void WebContentClient::did_paint(const Gfx::IntRect&, i32 bitmap_id)
{
    m_view.notify_server_did_paint({}, bitmap_id);
}

void WebContentClient::did_finish_loading(AK::URL const& url)
{
    m_view.notify_server_did_finish_loading({}, url);
}

void WebContentClient::did_invalidate_content_rect(Gfx::IntRect const& content_rect)
{
    dbgln_if(SPAM_DEBUG, "handle: WebContentClient::DidInvalidateContentRect! content_rect={}", content_rect);

    // FIXME: Figure out a way to coalesce these messages to reduce unnecessary painting
    m_view.notify_server_did_invalidate_content_rect({}, content_rect);
}

void WebContentClient::did_change_selection()
{
    dbgln_if(SPAM_DEBUG, "handle: WebContentClient::DidChangeSelection!");
    m_view.notify_server_did_change_selection({});
}

void WebContentClient::did_request_cursor_change(i32 cursor_type)
{
    if (cursor_type < 0 || cursor_type >= (i32)Gfx::StandardCursor::__Count) {
        dbgln("DidRequestCursorChange: Bad cursor type");
        return;
    }
    m_view.notify_server_did_request_cursor_change({}, (Gfx::StandardCursor)cursor_type);
}

void WebContentClient::did_layout(Gfx::IntSize const& content_size)
{
    dbgln_if(SPAM_DEBUG, "handle: WebContentClient::DidLayout! content_size={}", content_size);
    m_view.notify_server_did_layout({}, content_size);
}

void WebContentClient::did_change_title(String const& title)
{
    dbgln_if(SPAM_DEBUG, "handle: WebContentClient::DidChangeTitle! title={}", title);
    m_view.notify_server_did_change_title({}, title);
}

void WebContentClient::did_request_scroll(i32 x_delta, i32 y_delta)
{
    m_view.notify_server_did_request_scroll({}, x_delta, y_delta);
}

void WebContentClient::did_request_scroll_to(Gfx::IntPoint const& scroll_position)
{
    m_view.notify_server_did_request_scroll_to({}, scroll_position);
}

void WebContentClient::did_request_scroll_into_view(Gfx::IntRect const& rect)
{
    dbgln_if(SPAM_DEBUG, "handle: WebContentClient::DidRequestScrollIntoView! rect={}", rect);
    m_view.notify_server_did_request_scroll_into_view({}, rect);
}

void WebContentClient::did_enter_tooltip_area(Gfx::IntPoint const& content_position, String const& title)
{
    m_view.notify_server_did_enter_tooltip_area({}, content_position, title);
}

void WebContentClient::did_leave_tooltip_area()
{
    m_view.notify_server_did_leave_tooltip_area({});
}

void WebContentClient::did_hover_link(AK::URL const& url)
{
    dbgln_if(SPAM_DEBUG, "handle: WebContentClient::DidHoverLink! url={}", url);
    m_view.notify_server_did_hover_link({}, url);
}

void WebContentClient::did_unhover_link()
{
    dbgln_if(SPAM_DEBUG, "handle: WebContentClient::DidUnhoverLink!");
    m_view.notify_server_did_unhover_link({});
}

void WebContentClient::did_click_link(AK::URL const& url, String const& target, unsigned modifiers)
{
    m_view.notify_server_did_click_link({}, url, target, modifiers);
}

void WebContentClient::did_middle_click_link(AK::URL const& url, String const& target, unsigned modifiers)
{
    m_view.notify_server_did_middle_click_link({}, url, target, modifiers);
}

void WebContentClient::did_start_loading(AK::URL const& url)
{
    m_view.notify_server_did_start_loading({}, url);
}

void WebContentClient::did_request_context_menu(Gfx::IntPoint const& content_position)
{
    m_view.notify_server_did_request_context_menu({}, content_position);
}

void WebContentClient::did_request_link_context_menu(Gfx::IntPoint const& content_position, AK::URL const& url, String const& target, unsigned modifiers)
{
    m_view.notify_server_did_request_link_context_menu({}, content_position, url, target, modifiers);
}

void WebContentClient::did_request_image_context_menu(Gfx::IntPoint const& content_position, AK::URL const& url, String const& target, unsigned modifiers, Gfx::ShareableBitmap const& bitmap)
{
    m_view.notify_server_did_request_image_context_menu({}, content_position, url, target, modifiers, bitmap);
}

void WebContentClient::did_get_source(AK::URL const& url, String const& source)
{
    m_view.notify_server_did_get_source(url, source);
}

void WebContentClient::did_get_dom_tree(String const& dom_tree)
{
    m_view.notify_server_did_get_dom_tree(dom_tree);
}

void WebContentClient::did_get_dom_node_properties(i32 node_id, String const& specified_style, String const& computed_style, String const& custom_properties)
{
    m_view.notify_server_did_get_dom_node_properties(node_id, specified_style, computed_style, custom_properties);
}

void WebContentClient::did_output_js_console_message(i32 message_index)
{
    m_view.notify_server_did_output_js_console_message(message_index);
}

void WebContentClient::did_get_js_console_messages(i32 start_index, Vector<String> const& message_types, Vector<String> const& messages)
{
    m_view.notify_server_did_get_js_console_messages(start_index, message_types, messages);
}

void WebContentClient::did_request_alert(String const& message)
{
    m_view.notify_server_did_request_alert({}, message);
}

Messages::WebContentClient::DidRequestConfirmResponse WebContentClient::did_request_confirm(String const& message)
{
    return m_view.notify_server_did_request_confirm({}, message);
}

Messages::WebContentClient::DidRequestPromptResponse WebContentClient::did_request_prompt(String const& message, String const& default_)
{
    return m_view.notify_server_did_request_prompt({}, message, default_);
}

void WebContentClient::did_change_favicon(Gfx::ShareableBitmap const& favicon)
{
    if (!favicon.is_valid()) {
        dbgln("DidChangeFavicon: Received invalid favicon");
        return;
    }
    m_view.notify_server_did_change_favicon(*favicon.bitmap());
}

Messages::WebContentClient::DidRequestCookieResponse WebContentClient::did_request_cookie(AK::URL const& url, u8 source)
{
    return m_view.notify_server_did_request_cookie({}, url, static_cast<Cookie::Source>(source));
}

void WebContentClient::did_set_cookie(AK::URL const& url, Web::Cookie::ParsedCookie const& cookie, u8 source)
{
    m_view.notify_server_did_set_cookie({}, url, cookie, static_cast<Cookie::Source>(source));
}

}
