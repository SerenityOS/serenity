/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <AK/URL.h>
#include <LibGfx/Forward.h>
#include <LibWeb/Forward.h>

namespace Web {

class WebViewHooks {
public:
    Function<void(const Gfx::IntPoint& screen_position)> on_context_menu_request;
    Function<void(const AK::URL&, const String& target, unsigned modifiers)> on_link_click;
    Function<void(const AK::URL&, const Gfx::IntPoint& screen_position)> on_link_context_menu_request;
    Function<void(const AK::URL&, const Gfx::IntPoint& screen_position, const Gfx::ShareableBitmap&)> on_image_context_menu_request;
    Function<void(const AK::URL&, const String& target, unsigned modifiers)> on_link_middle_click;
    Function<void(const AK::URL&)> on_link_hover;
    Function<void(const String&)> on_title_change;
    Function<void(const AK::URL&)> on_load_start;
    Function<void(const AK::URL&)> on_load_finish;
    Function<void(const Gfx::Bitmap&)> on_favicon_change;
    Function<void(const AK::URL&)> on_url_drop;
    Function<void(DOM::Document*)> on_set_document;
    Function<void(const AK::URL&, const String&)> on_get_source;
    Function<void(const String&)> on_get_dom_tree;
    Function<void(i32 node_id, String const& specified_style, String const& computed_style, String const& custom_properties)> on_get_dom_node_properties;
    Function<void(i32 message_id)> on_js_console_new_message;
    Function<void(i32 start_index, Vector<String> const& message_types, Vector<String> const& messages)> on_get_js_console_messages;
    Function<String(const AK::URL& url, Cookie::Source source)> on_get_cookie;
    Function<void(const AK::URL& url, const Cookie::ParsedCookie& cookie, Cookie::Source source)> on_set_cookie;
};

}
