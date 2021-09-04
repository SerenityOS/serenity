/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <LibGfx/Forward.h>
#include <LibWeb/Forward.h>

namespace Web {

class WebViewHooks {
public:
    Function<void(const Gfx::IntPoint& screen_position)> on_context_menu_request;
    Function<void(URL const&, String const& target, unsigned modifiers)> on_link_click;
    Function<void(URL const&, const Gfx::IntPoint& screen_position)> on_link_context_menu_request;
    Function<void(URL const&, const Gfx::IntPoint& screen_position, const Gfx::ShareableBitmap&)> on_image_context_menu_request;
    Function<void(URL const&, String const& target, unsigned modifiers)> on_link_middle_click;
    Function<void(URL const&)> on_link_hover;
    Function<void(String const&)> on_title_change;
    Function<void(URL const&)> on_load_start;
    Function<void(URL const&)> on_load_finish;
    Function<void(const Gfx::Bitmap&)> on_favicon_change;
    Function<void(URL const&)> on_url_drop;
    Function<void(DOM::Document*)> on_set_document;
    Function<void(URL const&, String const&)> on_get_source;
    Function<void(String const&)> on_get_dom_tree;
    Function<void(i32 node_id, String const& specified_style, String const& computed_style)> on_get_dom_node_properties;
    Function<void(String const& method, String const& line)> on_js_console_output;
    Function<String(URL const& url, Cookie::Source source)> on_get_cookie;
    Function<void(URL const& url, const Cookie::ParsedCookie& cookie, Cookie::Source source)> on_set_cookie;
};

}
