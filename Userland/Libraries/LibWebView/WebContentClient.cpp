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
{
    m_views.set(0, &view);
}

void WebContentClient::die()
{
    VERIFY(on_web_content_process_crash);
    on_web_content_process_crash();
}

void WebContentClient::register_view(u64 page_id, ViewImplementation& view)
{
    VERIFY(page_id > 0);
    m_views.set(page_id, &view);
}

void WebContentClient::unregister_view(u64 page_id)
{
    m_views.remove(page_id);
}

void WebContentClient::did_paint(u64 page_id, Gfx::IntRect const& rect, i32 bitmap_id)
{
    auto maybe_view = m_views.get(page_id);
    if (!maybe_view.has_value()) {
        dbgln("Received paint for unknown page ID {}", page_id);
        return;
    }
    auto& view = *maybe_view.value();

    view.server_did_paint({}, bitmap_id, rect.size());
}

void WebContentClient::did_start_loading(u64 page_id, AK::URL const& url, bool is_redirect)
{
    auto maybe_view = m_views.get(page_id);
    if (!maybe_view.has_value()) {
        dbgln("Received start loading for unknown page ID {}", page_id);
        return;
    }
    auto& view = *maybe_view.value();

    view.set_url({}, url);

    if (view.on_load_start)
        view.on_load_start(url, is_redirect);
}

void WebContentClient::did_finish_loading(u64 page_id, AK::URL const& url)
{
    auto maybe_view = m_views.get(page_id);
    if (!maybe_view.has_value()) {
        dbgln("Received finish loading for unknown page ID {}", page_id);
        return;
    }
    auto& view = *maybe_view.value();

    view.set_url({}, url);

    if (view.on_load_finish)
        view.on_load_finish(url);
}

void WebContentClient::did_finish_text_test(u64 page_id)
{
    auto maybe_view = m_views.get(page_id);
    if (!maybe_view.has_value()) {
        dbgln("Received finish text test for unknown page ID {}", page_id);
        return;
    }
    auto& view = *maybe_view.value();

    if (view.on_text_test_finish)
        view.on_text_test_finish();
}

void WebContentClient::did_request_navigate_back(u64 page_id)
{
    auto maybe_view = m_views.get(page_id);
    if (!maybe_view.has_value()) {
        dbgln("Received navigate back for unknown page ID {}", page_id);
        return;
    }
    auto& view = *maybe_view.value();

    if (view.on_navigate_back)
        view.on_navigate_back();
}

void WebContentClient::did_request_navigate_forward(u64 page_id)
{
    auto maybe_view = m_views.get(page_id);
    if (!maybe_view.has_value()) {
        dbgln("Received navigate forward for unknown page ID {}", page_id);
        return;
    }
    auto& view = *maybe_view.value();

    if (view.on_navigate_forward)
        view.on_navigate_forward();
}

void WebContentClient::did_request_refresh(u64 page_id)
{
    auto maybe_view = m_views.get(page_id);
    if (!maybe_view.has_value()) {
        dbgln("Received refresh for unknown page ID {}", page_id);
        return;
    }
    auto& view = *maybe_view.value();

    if (view.on_refresh)
        view.on_refresh();
}

void WebContentClient::did_request_cursor_change(u64 page_id, i32 cursor_type)
{
    auto maybe_view = m_views.get(page_id);
    if (!maybe_view.has_value()) {
        dbgln("Received cursor change for unknown page ID {}", page_id);
        return;
    }
    auto& view = *maybe_view.value();

    if (cursor_type < 0 || cursor_type >= (i32)Gfx::StandardCursor::__Count) {
        dbgln("DidRequestCursorChange: Bad cursor type");
        return;
    }

    if (view.on_cursor_change)
        view.on_cursor_change(static_cast<Gfx::StandardCursor>(cursor_type));
}

void WebContentClient::did_layout(u64 page_id, Gfx::IntSize content_size)
{
    dbgln_if(SPAM_DEBUG, "handle: WebContentClient::DidLayout! content_size={}", content_size);
    auto maybe_view = m_views.get(page_id);
    if (!maybe_view.has_value()) {
        dbgln("Received layout for unknown page ID {}", page_id);
        return;
    }
    auto& view = *maybe_view.value();

    if (view.on_did_layout)
        view.on_did_layout(content_size);
}

void WebContentClient::did_change_title(u64 page_id, ByteString const& title)
{
    dbgln_if(SPAM_DEBUG, "handle: WebContentClient::DidChangeTitle! title={}", title);
    auto maybe_view = m_views.get(page_id);
    if (!maybe_view.has_value()) {
        dbgln("Received title change for unknown page ID {}", page_id);
        return;
    }
    auto& view = *maybe_view.value();

    if (!view.on_title_change)
        return;

    if (title.is_empty())
        view.on_title_change(view.url().to_byte_string());
    else
        view.on_title_change(title);
}

void WebContentClient::did_request_scroll(u64 page_id, i32 x_delta, i32 y_delta)
{
    auto maybe_view = m_views.get(page_id);
    if (!maybe_view.has_value()) {
        dbgln("Received scroll request for unknown page ID {}", page_id);
        return;
    }
    auto& view = *maybe_view.value();

    if (view.on_scroll_by_delta)
        view.on_scroll_by_delta(x_delta, y_delta);
}

void WebContentClient::did_request_scroll_to(u64 page_id, Gfx::IntPoint scroll_position)
{
    auto maybe_view = m_views.get(page_id);
    if (!maybe_view.has_value()) {
        dbgln("Received scroll request for unknown page ID {}", page_id);
        return;
    }
    auto& view = *maybe_view.value();

    if (view.on_scroll_to_point)
        view.on_scroll_to_point(scroll_position);
}

void WebContentClient::did_enter_tooltip_area(u64 page_id, Gfx::IntPoint content_position, ByteString const& title)
{
    auto maybe_view = m_views.get(page_id);
    if (!maybe_view.has_value()) {
        dbgln("Received tooltip enter for unknown page ID {}", page_id);
        return;
    }
    auto& view = *maybe_view.value();

    if (view.on_enter_tooltip_area)
        view.on_enter_tooltip_area(view.to_widget_position(content_position), title);
}

void WebContentClient::did_leave_tooltip_area(u64 page_id)
{
    auto maybe_view = m_views.get(page_id);
    if (!maybe_view.has_value()) {
        dbgln("Received tooltip leave for unknown page ID {}", page_id);
        return;
    }
    auto& view = *maybe_view.value();

    if (view.on_leave_tooltip_area)
        view.on_leave_tooltip_area();
}

void WebContentClient::did_hover_link(u64 page_id, AK::URL const& url)
{
    dbgln_if(SPAM_DEBUG, "handle: WebContentClient::DidHoverLink! url={}", url);
    auto maybe_view = m_views.get(page_id);
    if (!maybe_view.has_value()) {
        dbgln("Received hover link for unknown page ID {}", page_id);
        return;
    }
    auto& view = *maybe_view.value();

    if (view.on_link_hover)
        view.on_link_hover(url);
}

void WebContentClient::did_unhover_link(u64 page_id)
{
    dbgln_if(SPAM_DEBUG, "handle: WebContentClient::DidUnhoverLink!");
    auto maybe_view = m_views.get(page_id);
    if (!maybe_view.has_value()) {
        dbgln("Received unhover link for unknown page ID {}", page_id);
        return;
    }
    auto& view = *maybe_view.value();

    if (view.on_link_unhover)
        view.on_link_unhover();
}

void WebContentClient::did_click_link(u64 page_id, AK::URL const& url, ByteString const& target, unsigned modifiers)
{
    auto maybe_view = m_views.get(page_id);
    if (!maybe_view.has_value()) {
        dbgln("Received click link for unknown page ID {}", page_id);
        return;
    }
    auto& view = *maybe_view.value();

    if (view.on_link_click)
        view.on_link_click(url, target, modifiers);
}

void WebContentClient::did_middle_click_link(u64 page_id, AK::URL const& url, ByteString const& target, unsigned modifiers)
{
    auto maybe_view = m_views.get(page_id);
    if (!maybe_view.has_value()) {
        dbgln("Received middle click link for unknown page ID {}", page_id);
        return;
    }
    auto& view = *maybe_view.value();

    if (view.on_link_middle_click)
        view.on_link_middle_click(url, target, modifiers);
}

void WebContentClient::did_request_context_menu(u64 page_id, Gfx::IntPoint content_position)
{
    auto maybe_view = m_views.get(page_id);
    if (!maybe_view.has_value()) {
        dbgln("Received context menu request for unknown page ID {}", page_id);
        return;
    }
    auto& view = *maybe_view.value();

    if (view.on_context_menu_request)
        view.on_context_menu_request(view.to_widget_position(content_position));
}

void WebContentClient::did_request_link_context_menu(u64 page_id, Gfx::IntPoint content_position, AK::URL const& url, ByteString const&, unsigned)
{
    auto maybe_view = m_views.get(page_id);
    if (!maybe_view.has_value()) {
        dbgln("Received link context menu request for unknown page ID {}", page_id);
        return;
    }
    auto& view = *maybe_view.value();

    if (view.on_link_context_menu_request)
        view.on_link_context_menu_request(url, view.to_widget_position(content_position));
}

void WebContentClient::did_request_image_context_menu(u64 page_id, Gfx::IntPoint content_position, AK::URL const& url, ByteString const&, unsigned, Gfx::ShareableBitmap const& bitmap)
{
    auto maybe_view = m_views.get(page_id);
    if (!maybe_view.has_value()) {
        dbgln("Received image context menu request for unknown page ID {}", page_id);
        return;
    }
    auto& view = *maybe_view.value();

    if (view.on_image_context_menu_request)
        view.on_image_context_menu_request(url, view.to_widget_position(content_position), bitmap);
}

void WebContentClient::did_request_media_context_menu(u64 page_id, Gfx::IntPoint content_position, ByteString const&, unsigned, Web::Page::MediaContextMenu const& menu)
{
    auto maybe_view = m_views.get(page_id);
    if (!maybe_view.has_value()) {
        dbgln("Received media context menu request for unknown page ID {}", page_id);
        return;
    }
    auto& view = *maybe_view.value();

    if (view.on_media_context_menu_request)
        view.on_media_context_menu_request(view.to_widget_position(content_position), menu);
}

void WebContentClient::did_get_source(u64 page_id, AK::URL const& url, ByteString const& source)
{
    auto maybe_view = m_views.get(page_id);
    if (!maybe_view.has_value()) {
        dbgln("Received get source for unknown page ID {}", page_id);
        return;
    }
    auto& view = *maybe_view.value();

    if (view.on_received_source)
        view.on_received_source(url, source);
}

void WebContentClient::did_inspect_dom_tree(u64 page_id, ByteString const& dom_tree)
{
    auto maybe_view = m_views.get(page_id);
    if (!maybe_view.has_value()) {
        dbgln("Received Inspect DOM tree for unknown page ID {}", page_id);
        return;
    }
    auto& view = *maybe_view.value();

    if (view.on_received_dom_tree)
        view.on_received_dom_tree(dom_tree);
}

void WebContentClient::did_inspect_dom_node(u64 page_id, bool has_style, ByteString const& computed_style, ByteString const& resolved_style, ByteString const& custom_properties, ByteString const& node_box_sizing, ByteString const& aria_properties_state)
{
    auto maybe_view = m_views.get(page_id);
    if (!maybe_view.has_value()) {
        dbgln("Received Inspect DOM node for unknown page ID {}", page_id);
        return;
    }
    auto& view = *maybe_view.value();

    if (!view.on_received_dom_node_properties)
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

    view.on_received_dom_node_properties(move(properties));
}

void WebContentClient::did_inspect_accessibility_tree(u64 page_id, ByteString const& accessibility_tree)
{
    auto maybe_view = m_views.get(page_id);
    if (!maybe_view.has_value()) {
        dbgln("Received Inspect Accessibility tree for unknown page ID {}", page_id);
        return;
    }
    auto& view = *maybe_view.value();

    if (view.on_received_accessibility_tree)
        view.on_received_accessibility_tree(accessibility_tree);
}

void WebContentClient::did_get_hovered_node_id(u64 page_id, i32 node_id)
{
    auto maybe_view = m_views.get(page_id);
    if (!maybe_view.has_value()) {
        dbgln("Received get hovered node ID for unknown page ID {}", page_id);
        return;
    }
    auto& view = *maybe_view.value();

    if (view.on_received_hovered_node_id)
        view.on_received_hovered_node_id(node_id);
}

void WebContentClient::did_finish_editing_dom_node(u64 page_id, Optional<i32> const& node_id)
{
    auto maybe_view = m_views.get(page_id);
    if (!maybe_view.has_value()) {
        dbgln("Received finish editing DOM node for unknown page ID {}", page_id);
        return;
    }
    auto& view = *maybe_view.value();

    if (view.on_finshed_editing_dom_node)
        view.on_finshed_editing_dom_node(node_id);
}

void WebContentClient::did_get_dom_node_html(u64 page_id, String const& html)
{
    auto maybe_view = m_views.get(page_id);
    if (!maybe_view.has_value()) {
        dbgln("Received get DOM node HTML for unknown page ID {}", page_id);
        return;
    }
    auto& view = *maybe_view.value();

    if (view.on_received_dom_node_html)
        view.on_received_dom_node_html(html);
}

void WebContentClient::did_take_screenshot(u64 page_id, Gfx::ShareableBitmap const& screenshot)
{
    auto maybe_view = m_views.get(page_id);
    if (!maybe_view.has_value()) {
        dbgln("Received take screenshot for unknown page ID {}", page_id);
        return;
    }
    auto& view = *maybe_view.value();

    view.did_receive_screenshot({}, screenshot);
}

void WebContentClient::did_output_js_console_message(u64 page_id, i32 message_index)
{
    auto maybe_view = m_views.get(page_id);
    if (!maybe_view.has_value()) {
        dbgln("Received output JS console message for unknown page ID {}", page_id);
        return;
    }
    auto& view = *maybe_view.value();

    if (view.on_received_console_message)
        view.on_received_console_message(message_index);
}

void WebContentClient::did_get_js_console_messages(u64 page_id, i32 start_index, Vector<ByteString> const& message_types, Vector<ByteString> const& messages)
{
    auto maybe_view = m_views.get(page_id);
    if (!maybe_view.has_value()) {
        dbgln("Received get JS console messages for unknown page ID {}", page_id);
        return;
    }
    auto& view = *maybe_view.value();

    if (view.on_received_console_messages)
        view.on_received_console_messages(start_index, message_types, messages);
}

void WebContentClient::did_request_alert(u64 page_id, String const& message)
{
    auto maybe_view = m_views.get(page_id);
    if (!maybe_view.has_value()) {
        dbgln("Received alert request for unknown page ID {}", page_id);
        return;
    }
    auto& view = *maybe_view.value();

    if (view.on_request_alert)
        view.on_request_alert(message);
}

void WebContentClient::did_request_confirm(u64 page_id, String const& message)
{
    auto maybe_view = m_views.get(page_id);
    if (!maybe_view.has_value()) {
        dbgln("Received confirm request for unknown page ID {}", page_id);
        return;
    }
    auto& view = *maybe_view.value();

    if (view.on_request_confirm)
        view.on_request_confirm(message);
}

void WebContentClient::did_request_prompt(u64 page_id, String const& message, String const& default_)
{
    auto maybe_view = m_views.get(page_id);
    if (!maybe_view.has_value()) {
        dbgln("Received prompt request for unknown page ID {}", page_id);
        return;
    }
    auto& view = *maybe_view.value();

    if (view.on_request_prompt)
        view.on_request_prompt(message, default_);
}

void WebContentClient::did_request_set_prompt_text(u64 page_id, String const& message)
{
    auto maybe_view = m_views.get(page_id);
    if (!maybe_view.has_value()) {
        dbgln("Received set prompt text request for unknown page ID {}", page_id);
        return;
    }
    auto& view = *maybe_view.value();

    if (view.on_request_set_prompt_text)
        view.on_request_set_prompt_text(message);
}

void WebContentClient::did_request_accept_dialog(u64 page_id)
{
    auto maybe_view = m_views.get(page_id);
    if (!maybe_view.has_value()) {
        dbgln("Received accept dialog request for unknown page ID {}", page_id);
        return;
    }
    auto& view = *maybe_view.value();

    if (view.on_request_accept_dialog)
        view.on_request_accept_dialog();
}

void WebContentClient::did_request_dismiss_dialog(u64 page_id)
{
    auto maybe_view = m_views.get(page_id);
    if (!maybe_view.has_value()) {
        dbgln("Received dismiss dialog request for unknown page ID {}", page_id);
        return;
    }
    auto& view = *maybe_view.value();

    if (view.on_request_dismiss_dialog)
        view.on_request_dismiss_dialog();
}

void WebContentClient::did_change_favicon(u64 page_id, Gfx::ShareableBitmap const& favicon)
{
    auto maybe_view = m_views.get(page_id);
    if (!maybe_view.has_value()) {
        dbgln("Received change favicon for unknown page ID {}", page_id);
        return;
    }
    auto& view = *maybe_view.value();

    if (!favicon.is_valid()) {
        dbgln("DidChangeFavicon: Received invalid favicon");
        return;
    }

    if (view.on_favicon_change)
        view.on_favicon_change(*favicon.bitmap());
}

Messages::WebContentClient::DidRequestAllCookiesResponse WebContentClient::did_request_all_cookies(u64 page_id, AK::URL const& url)
{
    auto maybe_view = m_views.get(page_id);
    if (!maybe_view.has_value()) {
        dbgln("Received request all cookies for unknown page ID {}", page_id);
        return Vector<Web::Cookie::Cookie> {};
    }
    auto& view = *maybe_view.value();

    if (view.on_get_all_cookies)
        return view.on_get_all_cookies(url);
    return Vector<Web::Cookie::Cookie> {};
}

Messages::WebContentClient::DidRequestNamedCookieResponse WebContentClient::did_request_named_cookie(u64 page_id, AK::URL const& url, String const& name)
{
    auto maybe_view = m_views.get(page_id);
    if (!maybe_view.has_value()) {
        dbgln("Received request named cookie for unknown page ID {}", page_id);
        return OptionalNone {};
    }
    auto& view = *maybe_view.value();

    if (view.on_get_named_cookie)
        return view.on_get_named_cookie(url, name);
    return OptionalNone {};
}

Messages::WebContentClient::DidRequestCookieResponse WebContentClient::did_request_cookie(u64 page_id, AK::URL const& url, Web::Cookie::Source source)
{
    auto maybe_view = m_views.get(page_id);
    if (!maybe_view.has_value()) {
        dbgln("Received request cookie for unknown page ID {}", page_id);
        return String {};
    }
    auto& view = *maybe_view.value();

    if (view.on_get_cookie)
        return view.on_get_cookie(url, source);
    return String {};
}

void WebContentClient::did_set_cookie(u64 page_id, AK::URL const& url, Web::Cookie::ParsedCookie const& cookie, Web::Cookie::Source source)
{
    auto maybe_view = m_views.get(page_id);
    if (!maybe_view.has_value()) {
        dbgln("Received set cookie for unknown page ID {}", page_id);
        return;
    }
    auto& view = *maybe_view.value();

    if (view.on_set_cookie)
        view.on_set_cookie(url, cookie, source);
}

void WebContentClient::did_update_cookie(u64 page_id, Web::Cookie::Cookie const& cookie)
{
    auto maybe_view = m_views.get(page_id);
    if (!maybe_view.has_value()) {
        dbgln("Received update cookie for unknown page ID {}", page_id);
        return;
    }
    auto& view = *maybe_view.value();

    if (view.on_update_cookie)
        view.on_update_cookie(cookie);
}

Messages::WebContentClient::DidRequestNewWebViewResponse WebContentClient::did_request_new_web_view(u64 page_id, Web::HTML::ActivateTab const& activate_tab, Web::HTML::WebViewHints const& hints, Optional<u64> const& page_index)
{
    auto maybe_view = m_views.get(page_id);
    if (!maybe_view.has_value()) {
        dbgln("Received request new web view for unknown page ID {}", page_id);
        return String {};
    }
    auto& view = *maybe_view.value();

    if (view.on_new_web_view)
        return view.on_new_web_view(activate_tab, hints, page_index);
    return String {};
}

void WebContentClient::did_request_activate_tab(u64 page_id)
{
    auto maybe_view = m_views.get(page_id);
    if (!maybe_view.has_value()) {
        dbgln("Received request activate tab for unknown page ID {}", page_id);
        return;
    }
    auto& view = *maybe_view.value();

    if (view.on_activate_tab)
        view.on_activate_tab();
}

void WebContentClient::did_close_browsing_context(u64 page_id)
{
    auto maybe_view = m_views.get(page_id);
    if (!maybe_view.has_value()) {
        dbgln("Received close browsing context for unknown page ID {}", page_id);
        return;
    }
    auto& view = *maybe_view.value();

    if (view.on_close)
        view.on_close();
}

void WebContentClient::did_update_resource_count(u64 page_id, i32 count_waiting)
{
    auto maybe_view = m_views.get(page_id);
    if (!maybe_view.has_value()) {
        dbgln("Received update resource count for unknown page ID {}", page_id);
        return;
    }
    auto& view = *maybe_view.value();

    if (view.on_resource_status_change)
        view.on_resource_status_change(count_waiting);
}

void WebContentClient::did_request_restore_window(u64 page_id)
{
    auto maybe_view = m_views.get(page_id);
    if (!maybe_view.has_value()) {
        dbgln("Received request restore window for unknown page ID {}", page_id);
        return;
    }
    auto& view = *maybe_view.value();

    if (view.on_restore_window)
        view.on_restore_window();
}

Messages::WebContentClient::DidRequestRepositionWindowResponse WebContentClient::did_request_reposition_window(u64 page_id, Gfx::IntPoint position)
{
    auto maybe_view = m_views.get(page_id);
    if (!maybe_view.has_value()) {
        dbgln("Received request reposition window for unknown page ID {}", page_id);
        return Gfx::IntPoint {};
    }
    auto& view = *maybe_view.value();

    if (view.on_reposition_window)
        return view.on_reposition_window(position);
    return Gfx::IntPoint {};
}

Messages::WebContentClient::DidRequestResizeWindowResponse WebContentClient::did_request_resize_window(u64 page_id, Gfx::IntSize size)
{
    auto maybe_view = m_views.get(page_id);
    if (!maybe_view.has_value()) {
        dbgln("Received request resize window for unknown page ID {}", page_id);
        return Gfx::IntSize {};
    }
    auto& view = *maybe_view.value();

    if (view.on_resize_window)
        return view.on_resize_window(size);
    return Gfx::IntSize {};
}

Messages::WebContentClient::DidRequestMaximizeWindowResponse WebContentClient::did_request_maximize_window(u64 page_id)
{
    auto maybe_view = m_views.get(page_id);
    if (!maybe_view.has_value()) {
        dbgln("Received request maximize window for unknown page ID {}", page_id);
        return Gfx::IntRect {};
    }
    auto& view = *maybe_view.value();

    if (view.on_maximize_window)
        return view.on_maximize_window();
    return Gfx::IntRect {};
}

Messages::WebContentClient::DidRequestMinimizeWindowResponse WebContentClient::did_request_minimize_window(u64 page_id)
{
    auto maybe_view = m_views.get(page_id);
    if (!maybe_view.has_value()) {
        dbgln("Received request minimize window for unknown page ID {}", page_id);
        return Gfx::IntRect {};
    }
    auto& view = *maybe_view.value();

    if (view.on_minimize_window)
        return view.on_minimize_window();
    return Gfx::IntRect {};
}

Messages::WebContentClient::DidRequestFullscreenWindowResponse WebContentClient::did_request_fullscreen_window(u64 page_id)
{
    auto maybe_view = m_views.get(page_id);
    if (!maybe_view.has_value()) {
        dbgln("Received request fullscreen window for unknown page ID {}", page_id);
        return Gfx::IntRect {};
    }
    auto& view = *maybe_view.value();

    if (view.on_fullscreen_window)
        return view.on_fullscreen_window();
    return Gfx::IntRect {};
}

void WebContentClient::did_request_file(u64 page_id, ByteString const& path, i32 request_id)
{
    auto maybe_view = m_views.get(page_id);
    if (!maybe_view.has_value()) {
        dbgln("Received request file for unknown page ID {}", page_id);
        return;
    }
    auto& view = *maybe_view.value();

    if (view.on_request_file)
        view.on_request_file(path, request_id);
}

void WebContentClient::did_request_color_picker(u64 page_id, Color const& current_color)
{
    auto maybe_view = m_views.get(page_id);
    if (!maybe_view.has_value()) {
        dbgln("Received request color picker for unknown page ID {}", page_id);
        return;
    }
    auto& view = *maybe_view.value();

    if (view.on_request_color_picker)
        view.on_request_color_picker(current_color);
}

void WebContentClient::did_request_select_dropdown(u64 page_id, Gfx::IntPoint content_position, i32 minimum_width, Vector<Web::HTML::SelectItem> const& items)
{
    auto maybe_view = m_views.get(page_id);
    if (!maybe_view.has_value()) {
        dbgln("Received request select dropdown for unknown page ID {}", page_id);
        return;
    }
    auto& view = *maybe_view.value();

    if (view.on_request_select_dropdown)
        view.on_request_select_dropdown(content_position, minimum_width, items);
}

void WebContentClient::did_finish_handling_input_event(u64 page_id, bool event_was_accepted)
{
    auto maybe_view = m_views.get(page_id);
    if (!maybe_view.has_value()) {
        dbgln("Received finish handling input event for unknown page ID {}", page_id);
        return;
    }
    auto& view = *maybe_view.value();

    if (view.on_finish_handling_input_event)
        view.on_finish_handling_input_event(event_was_accepted);
}

void WebContentClient::did_change_theme_color(u64 page_id, Gfx::Color color)
{
    auto maybe_view = m_views.get(page_id);
    if (!maybe_view.has_value()) {
        dbgln("Received change theme color for unknown page ID {}", page_id);
        return;
    }
    auto& view = *maybe_view.value();

    if (view.on_theme_color_change)
        view.on_theme_color_change(color);
}

void WebContentClient::did_insert_clipboard_entry(u64 page_id, String const& data, String const& presentation_style, String const& mime_type)
{
    auto maybe_view = m_views.get(page_id);
    if (!maybe_view.has_value()) {
        dbgln("Received insert clipboard entry for unknown page ID {}", page_id);
        return;
    }
    auto& view = *maybe_view.value();

    if (view.on_insert_clipboard_entry)
        view.on_insert_clipboard_entry(data, presentation_style, mime_type);
}

void WebContentClient::inspector_did_load(u64 page_id)
{
    auto maybe_view = m_views.get(page_id);
    if (!maybe_view.has_value()) {
        dbgln("Received inspector loaded for unknown page ID {}", page_id);
        return;
    }
    auto& view = *maybe_view.value();

    if (view.on_inspector_loaded)
        view.on_inspector_loaded();
}

void WebContentClient::inspector_did_select_dom_node(u64 page_id, i32 node_id, Optional<Web::CSS::Selector::PseudoElement::Type> const& pseudo_element)
{
    auto maybe_view = m_views.get(page_id);
    if (!maybe_view.has_value()) {
        dbgln("Received inspector select DOM node for unknown page ID {}", page_id);
        return;
    }
    auto& view = *maybe_view.value();

    if (view.on_inspector_selected_dom_node)
        view.on_inspector_selected_dom_node(node_id, pseudo_element);
}

void WebContentClient::inspector_did_set_dom_node_text(u64 page_id, i32 node_id, String const& text)
{
    auto maybe_view = m_views.get(page_id);
    if (!maybe_view.has_value()) {
        dbgln("Received inspector set DOM node text for unknown page ID {}", page_id);
        return;
    }
    auto& view = *maybe_view.value();

    if (view.on_inspector_set_dom_node_text)
        view.on_inspector_set_dom_node_text(node_id, text);
}

void WebContentClient::inspector_did_set_dom_node_tag(u64 page_id, i32 node_id, String const& tag)
{
    auto maybe_view = m_views.get(page_id);
    if (!maybe_view.has_value()) {
        dbgln("Received inspector set DOM node tag for unknown page ID {}", page_id);
        return;
    }
    auto& view = *maybe_view.value();

    if (view.on_inspector_set_dom_node_tag)
        view.on_inspector_set_dom_node_tag(node_id, tag);
}

void WebContentClient::inspector_did_add_dom_node_attributes(u64 page_id, i32 node_id, Vector<Attribute> const& attributes)
{
    auto maybe_view = m_views.get(page_id);
    if (!maybe_view.has_value()) {
        dbgln("Received inspector add DOM node attributes for unknown page ID {}", page_id);
        return;
    }
    auto& view = *maybe_view.value();

    if (view.on_inspector_added_dom_node_attributes)
        view.on_inspector_added_dom_node_attributes(node_id, attributes);
}

void WebContentClient::inspector_did_replace_dom_node_attribute(u64 page_id, i32 node_id, String const& name, Vector<Attribute> const& replacement_attributes)
{
    auto maybe_view = m_views.get(page_id);
    if (!maybe_view.has_value()) {
        dbgln("Received inspector replace DOM node attribute for unknown page ID {}", page_id);
        return;
    }
    auto& view = *maybe_view.value();

    if (view.on_inspector_replaced_dom_node_attribute)
        view.on_inspector_replaced_dom_node_attribute(node_id, name, replacement_attributes);
}

void WebContentClient::inspector_did_request_dom_tree_context_menu(u64 page_id, i32 node_id, Gfx::IntPoint position, String const& type, Optional<String> const& tag, Optional<Attribute> const& attribute)
{
    auto maybe_view = m_views.get(page_id);
    if (!maybe_view.has_value()) {
        dbgln("Received inspector request DOM tree context menu for unknown page ID {}", page_id);
        return;
    }
    auto& view = *maybe_view.value();

    if (view.on_inspector_requested_dom_tree_context_menu)
        view.on_inspector_requested_dom_tree_context_menu(node_id, view.to_widget_position(position), type, tag, attribute);
}

void WebContentClient::inspector_did_execute_console_script(u64 page_id, String const& script)
{
    auto maybe_view = m_views.get(page_id);
    if (!maybe_view.has_value()) {
        dbgln("Received inspector execute console script for unknown page ID {}", page_id);
        return;
    }
    auto& view = *maybe_view.value();

    if (view.on_inspector_executed_console_script)
        view.on_inspector_executed_console_script(script);
}

Messages::WebContentClient::RequestWorkerAgentResponse WebContentClient::request_worker_agent(u64 page_id)
{
    auto maybe_view = m_views.get(page_id);
    if (!maybe_view.has_value()) {
        dbgln("Received request worker agent for unknown page ID {}", page_id);
        return WebView::SocketPair { -1, -1 };
    }
    auto& view = *maybe_view.value();

    if (view.on_request_worker_agent)
        return view.on_request_worker_agent();

    return Messages::WebContentClient::RequestWorkerAgentResponse { WebView::SocketPair { -1, -1 } };
}

}
