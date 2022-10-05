/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <LibGfx/Forward.h>
#include <LibGfx/StandardCursor.h>
#include <LibWeb/Forward.h>
#include <LibWebView/Forward.h>

namespace WebView {

class ViewImplementation {
public:
    virtual ~ViewImplementation() { }

    virtual void notify_server_did_layout(Badge<WebContentClient>, Gfx::IntSize const& content_size) = 0;
    virtual void notify_server_did_paint(Badge<WebContentClient>, i32 bitmap_id) = 0;
    virtual void notify_server_did_invalidate_content_rect(Badge<WebContentClient>, Gfx::IntRect const&) = 0;
    virtual void notify_server_did_change_selection(Badge<WebContentClient>) = 0;
    virtual void notify_server_did_request_cursor_change(Badge<WebContentClient>, Gfx::StandardCursor cursor) = 0;
    virtual void notify_server_did_change_title(Badge<WebContentClient>, String const&) = 0;
    virtual void notify_server_did_request_scroll(Badge<WebContentClient>, i32, i32) = 0;
    virtual void notify_server_did_request_scroll_to(Badge<WebContentClient>, Gfx::IntPoint const&) = 0;
    virtual void notify_server_did_request_scroll_into_view(Badge<WebContentClient>, Gfx::IntRect const&) = 0;
    virtual void notify_server_did_enter_tooltip_area(Badge<WebContentClient>, Gfx::IntPoint const&, String const&) = 0;
    virtual void notify_server_did_leave_tooltip_area(Badge<WebContentClient>) = 0;
    virtual void notify_server_did_hover_link(Badge<WebContentClient>, const AK::URL&) = 0;
    virtual void notify_server_did_unhover_link(Badge<WebContentClient>) = 0;
    virtual void notify_server_did_click_link(Badge<WebContentClient>, const AK::URL&, String const& target, unsigned modifiers) = 0;
    virtual void notify_server_did_middle_click_link(Badge<WebContentClient>, const AK::URL&, String const& target, unsigned modifiers) = 0;
    virtual void notify_server_did_start_loading(Badge<WebContentClient>, const AK::URL&) = 0;
    virtual void notify_server_did_finish_loading(Badge<WebContentClient>, const AK::URL&) = 0;
    virtual void notify_server_did_request_context_menu(Badge<WebContentClient>, Gfx::IntPoint const&) = 0;
    virtual void notify_server_did_request_link_context_menu(Badge<WebContentClient>, Gfx::IntPoint const&, const AK::URL&, String const& target, unsigned modifiers) = 0;
    virtual void notify_server_did_request_image_context_menu(Badge<WebContentClient>, Gfx::IntPoint const&, const AK::URL&, String const& target, unsigned modifiers, Gfx::ShareableBitmap const&) = 0;
    virtual void notify_server_did_request_alert(Badge<WebContentClient>, String const& message) = 0;
    virtual bool notify_server_did_request_confirm(Badge<WebContentClient>, String const& message) = 0;
    virtual String notify_server_did_request_prompt(Badge<WebContentClient>, String const& message, String const& default_) = 0;
    virtual void notify_server_did_get_source(const AK::URL& url, String const& source) = 0;
    virtual void notify_server_did_get_dom_tree(String const& dom_tree) = 0;
    virtual void notify_server_did_get_dom_node_properties(i32 node_id, String const& specified_style, String const& computed_style, String const& custom_properties, String const& node_box_sizing) = 0;
    virtual void notify_server_did_output_js_console_message(i32 message_index) = 0;
    virtual void notify_server_did_get_js_console_messages(i32 start_index, Vector<String> const& message_types, Vector<String> const& messages) = 0;
    virtual void notify_server_did_change_favicon(Gfx::Bitmap const& favicon) = 0;
    virtual String notify_server_did_request_cookie(Badge<WebContentClient>, const AK::URL& url, Web::Cookie::Source source) = 0;
    virtual void notify_server_did_set_cookie(Badge<WebContentClient>, const AK::URL& url, Web::Cookie::ParsedCookie const& cookie, Web::Cookie::Source source) = 0;
    virtual void notify_server_did_update_resource_count(i32 count_waiting) = 0;
    virtual void notify_server_did_request_file(Badge<WebContentClient>, String const& path, i32) = 0;
};

}
