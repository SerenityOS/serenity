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
    m_view.server_did_paint({}, bitmap_id, rect.size());
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

void WebContentClient::did_finish_text_test()
{
    if (m_view.on_text_test_finish)
        m_view.on_text_test_finish();
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

void WebContentClient::did_request_cursor_change(i32 cursor_type)
{
    if (cursor_type < 0 || cursor_type >= (i32)Gfx::StandardCursor::__Count) {
        dbgln("DidRequestCursorChange: Bad cursor type");
        return;
    }

    if (m_view.on_cursor_change)
        m_view.on_cursor_change(static_cast<Gfx::StandardCursor>(cursor_type));
}

void WebContentClient::did_layout(Gfx::IntSize content_size)
{
    dbgln_if(SPAM_DEBUG, "handle: WebContentClient::DidLayout! content_size={}", content_size);
    if (m_view.on_did_layout)
        m_view.on_did_layout(content_size);
}

void WebContentClient::did_change_title(ByteString const& title)
{
    dbgln_if(SPAM_DEBUG, "handle: WebContentClient::DidChangeTitle! title={}", title);

    if (!m_view.on_title_change)
        return;

    if (title.is_empty())
        m_view.on_title_change(m_view.url().to_byte_string());
    else
        m_view.on_title_change(title);
}

void WebContentClient::did_request_scroll(i32 x_delta, i32 y_delta)
{
    if (m_view.on_scroll_by_delta)
        m_view.on_scroll_by_delta(x_delta, y_delta);
}

void WebContentClient::did_request_scroll_to(Gfx::IntPoint scroll_position)
{
    if (m_view.on_scroll_to_point)
        m_view.on_scroll_to_point(scroll_position);
}

void WebContentClient::did_enter_tooltip_area(Gfx::IntPoint content_position, ByteString const& title)
{
    if (m_view.on_enter_tooltip_area)
        m_view.on_enter_tooltip_area(m_view.to_widget_position(content_position), title);
}

void WebContentClient::did_leave_tooltip_area()
{
    if (m_view.on_leave_tooltip_area)
        m_view.on_leave_tooltip_area();
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

void WebContentClient::did_click_link(AK::URL const& url, ByteString const& target, unsigned modifiers)
{
    if (m_view.on_link_click)
        m_view.on_link_click(url, target, modifiers);
}

void WebContentClient::did_middle_click_link(AK::URL const& url, ByteString const& target, unsigned modifiers)
{
    if (m_view.on_link_middle_click)
        m_view.on_link_middle_click(url, target, modifiers);
}

void WebContentClient::did_request_context_menu(Gfx::IntPoint content_position)
{
    if (m_view.on_context_menu_request)
        m_view.on_context_menu_request(m_view.to_widget_position(content_position));
}

void WebContentClient::did_request_link_context_menu(Gfx::IntPoint content_position, AK::URL const& url, ByteString const&, unsigned)
{
    if (m_view.on_link_context_menu_request)
        m_view.on_link_context_menu_request(url, m_view.to_widget_position(content_position));
}

void WebContentClient::did_request_image_context_menu(Gfx::IntPoint content_position, AK::URL const& url, ByteString const&, unsigned, Gfx::ShareableBitmap const& bitmap)
{
    if (m_view.on_image_context_menu_request)
        m_view.on_image_context_menu_request(url, m_view.to_widget_position(content_position), bitmap);
}

void WebContentClient::did_request_media_context_menu(Gfx::IntPoint content_position, ByteString const&, unsigned, Web::Page::MediaContextMenu const& menu)
{
    if (m_view.on_media_context_menu_request)
        m_view.on_media_context_menu_request(m_view.to_widget_position(content_position), menu);
}

void WebContentClient::did_get_source(AK::URL const& url, ByteString const& source)
{
    if (m_view.on_received_source)
        m_view.on_received_source(url, source);
}

void WebContentClient::did_inspect_dom_tree(ByteString const& dom_tree)
{
    if (m_view.on_received_dom_tree)
        m_view.on_received_dom_tree(dom_tree);
}

void WebContentClient::did_inspect_dom_node(bool has_style, ByteString const& computed_style, ByteString const& resolved_style, ByteString const& custom_properties, ByteString const& node_box_sizing, ByteString const& aria_properties_state)
{
    if (!m_view.on_received_dom_node_properties)
        return;

    Optional<ViewImplementation::DOMNodeProperties> properties;

    if (has_style) {
        properties = ViewImplementation::DOMNodeProperties {
            .computed_style_json = MUST(String::from_byte_string(computed_style)),
            .resolved_style_json = MUST(String::from_byte_string(resolved_style)),
            .custom_properties_json = MUST(String::from_byte_string(custom_properties)),
            .node_box_sizing_json = MUST(String::from_byte_string(node_box_sizing)),
            .aria_properties_state_json = MUST(String::from_byte_string(aria_properties_state)),
        };
    }

    m_view.on_received_dom_node_properties(move(properties));
}

void WebContentClient::did_inspect_accessibility_tree(ByteString const& accessibility_tree)
{
    if (m_view.on_received_accessibility_tree)
        m_view.on_received_accessibility_tree(accessibility_tree);
}

void WebContentClient::did_get_hovered_node_id(i32 node_id)
{
    if (m_view.on_received_hovered_node_id)
        m_view.on_received_hovered_node_id(node_id);
}

void WebContentClient::did_finish_editing_dom_node(Optional<i32> const& node_id)
{
    if (m_view.on_finshed_editing_dom_node)
        m_view.on_finshed_editing_dom_node(node_id);
}

void WebContentClient::did_get_dom_node_html(String const& html)
{
    if (m_view.on_received_dom_node_html)
        m_view.on_received_dom_node_html(html);
}

void WebContentClient::did_take_screenshot(Gfx::ShareableBitmap const& screenshot)
{
    m_view.did_receive_screenshot({}, screenshot);
}

void WebContentClient::did_output_js_console_message(i32 message_index)
{
    if (m_view.on_received_console_message)
        m_view.on_received_console_message(message_index);
}

void WebContentClient::did_get_js_console_messages(i32 start_index, Vector<ByteString> const& message_types, Vector<ByteString> const& messages)
{
    if (m_view.on_received_console_messages)
        m_view.on_received_console_messages(start_index, message_types, messages);
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

Messages::WebContentClient::DidRequestNamedCookieResponse WebContentClient::did_request_named_cookie(AK::URL const& url, String const& name)
{
    if (m_view.on_get_named_cookie)
        return m_view.on_get_named_cookie(url, name);
    return OptionalNone {};
}

Messages::WebContentClient::DidRequestCookieResponse WebContentClient::did_request_cookie(AK::URL const& url, Web::Cookie::Source source)
{
    if (m_view.on_get_cookie)
        return m_view.on_get_cookie(url, source);
    return String {};
}

void WebContentClient::did_set_cookie(AK::URL const& url, Web::Cookie::ParsedCookie const& cookie, Web::Cookie::Source source)
{
    if (m_view.on_set_cookie)
        m_view.on_set_cookie(url, cookie, source);
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

void WebContentClient::did_request_file(ByteString const& path, i32 request_id)
{
    if (m_view.on_request_file)
        m_view.on_request_file(path, request_id);
}

void WebContentClient::did_request_color_picker(Color const& current_color)
{
    if (m_view.on_request_color_picker)
        m_view.on_request_color_picker(current_color);
}

void WebContentClient::did_request_select_dropdown(Gfx::IntPoint content_position, i32 minimum_width, Vector<Web::HTML::SelectItem> const& items)
{
    if (m_view.on_request_select_dropdown)
        m_view.on_request_select_dropdown(content_position, minimum_width, items);
}

void WebContentClient::did_finish_handling_input_event(bool event_was_accepted)
{
    if (m_view.on_finish_handling_input_event)
        m_view.on_finish_handling_input_event(event_was_accepted);
}

void WebContentClient::did_change_theme_color(Gfx::Color color)
{
    if (m_view.on_theme_color_change)
        m_view.on_theme_color_change(color);
}

void WebContentClient::did_insert_clipboard_entry(String const& data, String const& presentation_style, String const& mime_type)
{
    if (m_view.on_insert_clipboard_entry)
        m_view.on_insert_clipboard_entry(data, presentation_style, mime_type);
}

void WebContentClient::inspector_did_load()
{
    if (m_view.on_inspector_loaded)
        m_view.on_inspector_loaded();
}

void WebContentClient::inspector_did_select_dom_node(i32 node_id, Optional<Web::CSS::Selector::PseudoElement::Type> const& pseudo_element)
{
    if (m_view.on_inspector_selected_dom_node)
        m_view.on_inspector_selected_dom_node(node_id, pseudo_element);
}

void WebContentClient::inspector_did_set_dom_node_text(i32 node_id, String const& text)
{
    if (m_view.on_inspector_set_dom_node_text)
        m_view.on_inspector_set_dom_node_text(node_id, text);
}

void WebContentClient::inspector_did_set_dom_node_tag(i32 node_id, String const& tag)
{
    if (m_view.on_inspector_set_dom_node_tag)
        m_view.on_inspector_set_dom_node_tag(node_id, tag);
}

void WebContentClient::inspector_did_add_dom_node_attributes(i32 node_id, Vector<Attribute> const& attributes)
{
    if (m_view.on_inspector_added_dom_node_attributes)
        m_view.on_inspector_added_dom_node_attributes(node_id, attributes);
}

void WebContentClient::inspector_did_replace_dom_node_attribute(i32 node_id, String const& name, Vector<Attribute> const& replacement_attributes)
{
    if (m_view.on_inspector_replaced_dom_node_attribute)
        m_view.on_inspector_replaced_dom_node_attribute(node_id, name, replacement_attributes);
}

void WebContentClient::inspector_did_request_dom_tree_context_menu(i32 node_id, Gfx::IntPoint position, String const& type, Optional<String> const& tag, Optional<Attribute> const& attribute)
{
    if (m_view.on_inspector_requested_dom_tree_context_menu)
        m_view.on_inspector_requested_dom_tree_context_menu(node_id, m_view.to_widget_position(position), type, tag, attribute);
}

void WebContentClient::inspector_did_execute_console_script(String const& script)
{
    if (m_view.on_inspector_executed_console_script)
        m_view.on_inspector_executed_console_script(script);
}

Messages::WebContentClient::RequestWorkerAgentResponse WebContentClient::request_worker_agent()
{
    if (m_view.on_request_worker_agent)
        return m_view.on_request_worker_agent();

    return Messages::WebContentClient::RequestWorkerAgentResponse { WebView::SocketPair { -1, -1 } };
}

}
