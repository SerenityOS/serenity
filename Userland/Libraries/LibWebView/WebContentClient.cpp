/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "WebContentClient.h"
#include "ViewImplementation.h"
#include <AK/Debug.h>
#include <LibWeb/Cookie/ParsedCookie.h>

namespace WebView {

WebContentClient::WebContentClient(NonnullOwnPtr<Core::LocalSocket> socket, ViewImplementation& view)
    : IPC::ConnectionToServer<WebContentClientEndpoint, WebContentServerEndpoint>(*this, move(socket))
    , m_view(view)
{
}

void WebContentClient::die()
{
    VERIFY(on_web_content_process_crash);
    on_web_content_process_crash();
}

void WebContentClient::did_paint(Gfx::IntRect const& rect, i32 bitmap_id)
{
    m_view.notify_server_did_paint({}, bitmap_id, rect.size());
}

void WebContentClient::did_start_loading(AK::URL const& url, bool is_redirect)
{
    m_view.set_url({}, url);

    if (m_view.on_load_start)
        m_view.on_load_start(url, is_redirect);
}

void WebContentClient::did_finish_loading(AK::URL const& url)
{
    m_view.set_url({}, url);

    if (m_view.on_load_finish)
        m_view.on_load_finish(url);
}

void WebContentClient::did_request_navigate_back()
{
    if (m_view.on_navigate_back)
        m_view.on_navigate_back();
}

void WebContentClient::did_request_navigate_forward()
{
    if (m_view.on_navigate_forward)
        m_view.on_navigate_forward();
}

void WebContentClient::did_request_refresh()
{
    if (m_view.on_refresh)
        m_view.on_refresh();
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

    if (!m_view.on_title_change)
        return;

    if (title.is_empty())
        m_view.on_title_change(m_view.url().to_deprecated_string());
    else
        m_view.on_title_change(title);
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

    if (m_view.on_link_hover)
        m_view.on_link_hover(url);
}

void WebContentClient::did_unhover_link()
{
    dbgln_if(SPAM_DEBUG, "handle: WebContentClient::DidUnhoverLink!");

    if (m_view.on_link_unhover)
        m_view.on_link_unhover();
}

void WebContentClient::did_click_link(AK::URL const& url, DeprecatedString const& target, unsigned modifiers)
{
    if (m_view.on_link_click)
        m_view.on_link_click(url, target, modifiers);
}

void WebContentClient::did_middle_click_link(AK::URL const& url, DeprecatedString const& target, unsigned modifiers)
{
    if (m_view.on_link_middle_click)
        m_view.on_link_middle_click(url, target, modifiers);
}

void WebContentClient::did_request_context_menu(Gfx::IntPoint content_position)
{
    if (m_view.on_context_menu_request)
        m_view.on_context_menu_request(m_view.to_widget_position(content_position));
}

void WebContentClient::did_request_link_context_menu(Gfx::IntPoint content_position, AK::URL const& url, DeprecatedString const&, unsigned)
{
    if (m_view.on_link_context_menu_request)
        m_view.on_link_context_menu_request(url, m_view.to_widget_position(content_position));
}

void WebContentClient::did_request_image_context_menu(Gfx::IntPoint content_position, AK::URL const& url, DeprecatedString const&, unsigned, Gfx::ShareableBitmap const& bitmap)
{
    if (m_view.on_image_context_menu_request)
        m_view.on_image_context_menu_request(url, m_view.to_widget_position(content_position), bitmap);
}

void WebContentClient::did_request_media_context_menu(Gfx::IntPoint content_position, DeprecatedString const&, unsigned, Web::Page::MediaContextMenu const& menu)
{
    if (m_view.on_media_context_menu_request)
        m_view.on_media_context_menu_request(m_view.to_widget_position(content_position), menu);
}

void WebContentClient::did_get_source(AK::URL const& url, DeprecatedString const& source)
{
    if (m_view.on_get_source)
        m_view.on_get_source(url, source);
}

void WebContentClient::did_get_dom_tree(DeprecatedString const& dom_tree)
{
    if (m_view.on_get_dom_tree)
        m_view.on_get_dom_tree(dom_tree);
}

void WebContentClient::did_get_dom_node_properties(i32 node_id, DeprecatedString const& computed_style, DeprecatedString const& resolved_style, DeprecatedString const& custom_properties, DeprecatedString const& node_box_sizing, DeprecatedString const& aria_properties_state)
{
    if (m_view.on_get_dom_node_properties)
        m_view.on_get_dom_node_properties(node_id, computed_style, resolved_style, custom_properties, node_box_sizing, aria_properties_state);
}

void WebContentClient::did_get_accessibility_tree(DeprecatedString const& accessibility_tree)
{
    if (m_view.on_get_accessibility_tree)
        m_view.on_get_accessibility_tree(accessibility_tree);
}

void WebContentClient::did_output_js_console_message(i32 message_index)
{
    if (m_view.on_js_console_new_message)
        m_view.on_js_console_new_message(message_index);
}

void WebContentClient::did_get_js_console_messages(i32 start_index, Vector<DeprecatedString> const& message_types, Vector<DeprecatedString> const& messages)
{
    if (m_view.on_get_js_console_messages)
        m_view.on_get_js_console_messages(start_index, message_types, messages);
}

void WebContentClient::did_request_alert(String const& message)
{
    if (m_view.on_request_alert)
        m_view.on_request_alert(message);
}

void WebContentClient::did_request_confirm(String const& message)
{
    if (m_view.on_request_confirm)
        m_view.on_request_confirm(message);
}

void WebContentClient::did_request_prompt(String const& message, String const& default_)
{
    if (m_view.on_request_prompt)
        m_view.on_request_prompt(message, default_);
}

void WebContentClient::did_request_set_prompt_text(String const& message)
{
    if (m_view.on_request_set_prompt_text)
        m_view.on_request_set_prompt_text(message);
}

void WebContentClient::did_request_accept_dialog()
{
    if (m_view.on_request_accept_dialog)
        m_view.on_request_accept_dialog();
}

void WebContentClient::did_request_dismiss_dialog()
{
    if (m_view.on_request_dismiss_dialog)
        m_view.on_request_dismiss_dialog();
}

void WebContentClient::did_change_favicon(Gfx::ShareableBitmap const& favicon)
{
    if (!favicon.is_valid()) {
        dbgln("DidChangeFavicon: Received invalid favicon");
        return;
    }

    if (m_view.on_favicon_change)
        m_view.on_favicon_change(*favicon.bitmap());
}

Messages::WebContentClient::DidRequestAllCookiesResponse WebContentClient::did_request_all_cookies(AK::URL const& url)
{
    if (m_view.on_get_all_cookies)
        return m_view.on_get_all_cookies(url);
    return Vector<Web::Cookie::Cookie> {};
}

Messages::WebContentClient::DidRequestNamedCookieResponse WebContentClient::did_request_named_cookie(AK::URL const& url, DeprecatedString const& name)
{
    if (m_view.on_get_named_cookie)
        return m_view.on_get_named_cookie(url, name);
    return OptionalNone {};
}

Messages::WebContentClient::DidRequestCookieResponse WebContentClient::did_request_cookie(AK::URL const& url, u8 source)
{
    if (m_view.on_get_cookie)
        return m_view.on_get_cookie(url, static_cast<Web::Cookie::Source>(source));
    return DeprecatedString {};
}

void WebContentClient::did_set_cookie(AK::URL const& url, Web::Cookie::ParsedCookie const& cookie, u8 source)
{
    if (m_view.on_set_cookie)
        m_view.on_set_cookie(url, cookie, static_cast<Web::Cookie::Source>(source));
}

void WebContentClient::did_update_cookie(Web::Cookie::Cookie const& cookie)
{
    if (m_view.on_update_cookie)
        m_view.on_update_cookie(cookie);
}

Messages::WebContentClient::DidRequestNewTabResponse WebContentClient::did_request_new_tab(Web::HTML::ActivateTab const& activate_tab)
{
    if (m_view.on_new_tab)
        return m_view.on_new_tab(activate_tab);
    return String {};
}

void WebContentClient::did_request_activate_tab()
{
    if (m_view.on_activate_tab)
        m_view.on_activate_tab();
}

void WebContentClient::did_close_browsing_context()
{
    if (m_view.on_close)
        m_view.on_close();
}

void WebContentClient::did_update_resource_count(i32 count_waiting)
{
    if (m_view.on_resource_status_change)
        m_view.on_resource_status_change(count_waiting);
}

void WebContentClient::did_request_restore_window()
{
    if (m_view.on_restore_window)
        m_view.on_restore_window();
}

Messages::WebContentClient::DidRequestRepositionWindowResponse WebContentClient::did_request_reposition_window(Gfx::IntPoint position)
{
    if (m_view.on_reposition_window)
        return m_view.on_reposition_window(position);
    return Gfx::IntPoint {};
}

Messages::WebContentClient::DidRequestResizeWindowResponse WebContentClient::did_request_resize_window(Gfx::IntSize size)
{
    if (m_view.on_resize_window)
        return m_view.on_resize_window(size);
    return Gfx::IntSize {};
}

Messages::WebContentClient::DidRequestMaximizeWindowResponse WebContentClient::did_request_maximize_window()
{
    if (m_view.on_maximize_window)
        return m_view.on_maximize_window();
    return Gfx::IntRect {};
}

Messages::WebContentClient::DidRequestMinimizeWindowResponse WebContentClient::did_request_minimize_window()
{
    if (m_view.on_minimize_window)
        return m_view.on_minimize_window();
    return Gfx::IntRect {};
}

Messages::WebContentClient::DidRequestFullscreenWindowResponse WebContentClient::did_request_fullscreen_window()
{
    if (m_view.on_fullscreen_window)
        return m_view.on_fullscreen_window();
    return Gfx::IntRect {};
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
