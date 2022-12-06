/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "WebContentClient.h"
#include "OutOfProcessWebView.h"
#include <AK/Debug.h>
#include <LibWeb/Cookie/ParsedCookie.h>

namespace WebView {

WebContentClient::WebContentClient(NonnullOwnPtr<Core::Stream::LocalSocket> socket, ViewImplementation& view)
    : IPC::ConnectionToServer<WebContentClientEndpoint, WebContentServerEndpoint>(*this, move(socket))
    , m_view(view)
{
}

void WebContentClient::die()
{
    VERIFY(on_web_content_process_crash);
    on_web_content_process_crash();
}

void WebContentClient::did_paint(Gfx::IntRect const&, i32 bitmap_id)
{
    m_view.notify_server_did_paint({}, bitmap_id);
}

void WebContentClient::did_finish_loading(AK::URL const& url)
{
    m_view.notify_server_did_finish_loading({}, url);
}

void WebContentClient::did_request_navigate_back()
{
    m_view.notify_server_did_request_navigate_back({});
}

void WebContentClient::did_request_navigate_forward()
{
    m_view.notify_server_did_request_navigate_forward({});
}

void WebContentClient::did_request_refresh()
{
    m_view.notify_server_did_request_refresh({});
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

void WebContentClient::did_layout(Gfx::IntSize content_size)
{
    dbgln_if(SPAM_DEBUG, "handle: WebContentClient::DidLayout! content_size={}", content_size);
    m_view.notify_server_did_layout({}, content_size);
}

void WebContentClient::did_change_title(DeprecatedString const& title)
{
    dbgln_if(SPAM_DEBUG, "handle: WebContentClient::DidChangeTitle! title={}", title);
    m_view.notify_server_did_change_title({}, title);
}

void WebContentClient::did_request_scroll(i32 x_delta, i32 y_delta)
{
    m_view.notify_server_did_request_scroll({}, x_delta, y_delta);
}

void WebContentClient::did_request_scroll_to(Gfx::IntPoint scroll_position)
{
    m_view.notify_server_did_request_scroll_to({}, scroll_position);
}

void WebContentClient::did_request_scroll_into_view(Gfx::IntRect const& rect)
{
    dbgln_if(SPAM_DEBUG, "handle: WebContentClient::DidRequestScrollIntoView! rect={}", rect);
    m_view.notify_server_did_request_scroll_into_view({}, rect);
}

void WebContentClient::did_enter_tooltip_area(Gfx::IntPoint content_position, DeprecatedString const& title)
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

void WebContentClient::did_click_link(AK::URL const& url, DeprecatedString const& target, unsigned modifiers)
{
    m_view.notify_server_did_click_link({}, url, target, modifiers);
}

void WebContentClient::did_middle_click_link(AK::URL const& url, DeprecatedString const& target, unsigned modifiers)
{
    m_view.notify_server_did_middle_click_link({}, url, target, modifiers);
}

void WebContentClient::did_start_loading(AK::URL const& url, bool is_redirect)
{
    m_view.notify_server_did_start_loading({}, url, is_redirect);
}

void WebContentClient::did_request_context_menu(Gfx::IntPoint content_position)
{
    m_view.notify_server_did_request_context_menu({}, content_position);
}

void WebContentClient::did_request_link_context_menu(Gfx::IntPoint content_position, AK::URL const& url, DeprecatedString const& target, unsigned modifiers)
{
    m_view.notify_server_did_request_link_context_menu({}, content_position, url, target, modifiers);
}

void WebContentClient::did_request_image_context_menu(Gfx::IntPoint content_position, AK::URL const& url, DeprecatedString const& target, unsigned modifiers, Gfx::ShareableBitmap const& bitmap)
{
    m_view.notify_server_did_request_image_context_menu({}, content_position, url, target, modifiers, bitmap);
}

void WebContentClient::did_get_source(AK::URL const& url, DeprecatedString const& source)
{
    m_view.notify_server_did_get_source(url, source);
}

void WebContentClient::did_get_dom_tree(DeprecatedString const& dom_tree)
{
    m_view.notify_server_did_get_dom_tree(dom_tree);
}

void WebContentClient::did_get_dom_node_properties(i32 node_id, DeprecatedString const& specified_style, DeprecatedString const& computed_style, DeprecatedString const& custom_properties, DeprecatedString const& node_box_sizing)
{
    m_view.notify_server_did_get_dom_node_properties(node_id, specified_style, computed_style, custom_properties, node_box_sizing);
}

void WebContentClient::did_output_js_console_message(i32 message_index)
{
    m_view.notify_server_did_output_js_console_message(message_index);
}

void WebContentClient::did_get_js_console_messages(i32 start_index, Vector<DeprecatedString> const& message_types, Vector<DeprecatedString> const& messages)
{
    m_view.notify_server_did_get_js_console_messages(start_index, message_types, messages);
}

void WebContentClient::did_request_alert(DeprecatedString const& message)
{
    m_view.notify_server_did_request_alert({}, message);
}

void WebContentClient::did_request_confirm(DeprecatedString const& message)
{
    m_view.notify_server_did_request_confirm({}, message);
}

void WebContentClient::did_request_prompt(DeprecatedString const& message, DeprecatedString const& default_)
{
    m_view.notify_server_did_request_prompt({}, message, default_);
}

void WebContentClient::did_request_set_prompt_text(DeprecatedString const& message)
{
    m_view.notify_server_did_request_set_prompt_text({}, message);
}

void WebContentClient::did_request_accept_dialog()
{
    m_view.notify_server_did_request_accept_dialog({});
}

void WebContentClient::did_request_dismiss_dialog()
{
    m_view.notify_server_did_request_dismiss_dialog({});
}

void WebContentClient::did_change_favicon(Gfx::ShareableBitmap const& favicon)
{
    if (!favicon.is_valid()) {
        dbgln("DidChangeFavicon: Received invalid favicon");
        return;
    }
    m_view.notify_server_did_change_favicon(*favicon.bitmap());
}

Messages::WebContentClient::DidRequestAllCookiesResponse WebContentClient::did_request_all_cookies(AK::URL const& url)
{
    return m_view.notify_server_did_request_all_cookies({}, url);
}

Messages::WebContentClient::DidRequestNamedCookieResponse WebContentClient::did_request_named_cookie(AK::URL const& url, DeprecatedString const& name)
{
    return m_view.notify_server_did_request_named_cookie({}, url, name);
}

Messages::WebContentClient::DidRequestCookieResponse WebContentClient::did_request_cookie(AK::URL const& url, u8 source)
{
    return m_view.notify_server_did_request_cookie({}, url, static_cast<Web::Cookie::Source>(source));
}

void WebContentClient::did_set_cookie(AK::URL const& url, Web::Cookie::ParsedCookie const& cookie, u8 source)
{
    m_view.notify_server_did_set_cookie({}, url, cookie, static_cast<Web::Cookie::Source>(source));
}

void WebContentClient::did_update_cookie(AK::URL const& url, Web::Cookie::Cookie const& cookie)
{
    m_view.notify_server_did_update_cookie({}, url, cookie);
}

void WebContentClient::did_update_resource_count(i32 count_waiting)
{
    m_view.notify_server_did_update_resource_count(count_waiting);
}

void WebContentClient::did_request_restore_window()
{
    m_view.notify_server_did_request_restore_window();
}

Messages::WebContentClient::DidRequestRepositionWindowResponse WebContentClient::did_request_reposition_window(Gfx::IntPoint position)
{
    return m_view.notify_server_did_request_reposition_window(position);
}

Messages::WebContentClient::DidRequestResizeWindowResponse WebContentClient::did_request_resize_window(Gfx::IntSize size)
{
    return m_view.notify_server_did_request_resize_window(size);
}

Messages::WebContentClient::DidRequestMaximizeWindowResponse WebContentClient::did_request_maximize_window()
{
    return m_view.notify_server_did_request_maximize_window();
}

Messages::WebContentClient::DidRequestMinimizeWindowResponse WebContentClient::did_request_minimize_window()
{
    return m_view.notify_server_did_request_minimize_window();
}

Messages::WebContentClient::DidRequestFullscreenWindowResponse WebContentClient::did_request_fullscreen_window()
{
    return m_view.notify_server_did_request_fullscreen_window();
}

void WebContentClient::did_request_file(DeprecatedString const& path, i32 request_id)
{
    m_view.notify_server_did_request_file({}, path, request_id);
}

void WebContentClient::did_finish_handling_input_event(bool event_was_accepted)
{
    m_view.notify_server_did_finish_handling_input_event(event_was_accepted);
}

}
