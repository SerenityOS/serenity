/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Function.h>
#include <AK/HashMap.h>
#include <AK/JsonValue.h>
#include <AK/StringView.h>
#include <AK/Vector.h>
#include <LibGfx/Point.h>
#include <LibWebView/Attribute.h>
#include <LibWebView/ViewImplementation.h>

#pragma once

namespace WebView {

class InspectorClient {
public:
    InspectorClient(ViewImplementation& content_web_view, ViewImplementation& inspector_web_view);
    ~InspectorClient();

    void inspect();
    void reset();

    void select_hovered_node();
    void select_default_node();
    void clear_selection();

    void context_menu_edit_dom_node();
    void context_menu_copy_dom_node();
    void context_menu_screenshot_dom_node();
    void context_menu_create_child_element();
    void context_menu_create_child_text_node();
    void context_menu_clone_dom_node();
    void context_menu_remove_dom_node();
    void context_menu_add_dom_node_attribute();
    void context_menu_remove_dom_node_attribute();
    void context_menu_copy_dom_node_attribute_value();

    Function<void(Gfx::IntPoint)> on_requested_dom_node_text_context_menu;
    Function<void(Gfx::IntPoint, String const&)> on_requested_dom_node_tag_context_menu;
    Function<void(Gfx::IntPoint, String const&, Attribute const&)> on_requested_dom_node_attribute_context_menu;

private:
    void load_inspector();

    String generate_dom_tree(JsonObject const&);
    String generate_accessibility_tree(JsonObject const&);
    void select_node(i32 node_id);

    void request_console_messages();
    void handle_console_message(i32 message_index);
    void handle_console_messages(i32 start_index, ReadonlySpan<ByteString> message_types, ReadonlySpan<ByteString> messages);

    void append_console_source(StringView);
    void append_console_message(StringView);
    void append_console_warning(StringView);
    void append_console_output(StringView);
    void clear_console_output();

    void begin_console_group(StringView label, bool start_expanded);
    void end_console_group();

    ViewImplementation& m_content_web_view;
    ViewImplementation& m_inspector_web_view;

    Optional<i32> m_body_node_id;
    Optional<i32> m_pending_selection;

    bool m_inspector_loaded { false };
    bool m_dom_tree_loaded { false };

    struct ContextMenuData {
        i32 dom_node_id { 0 };
        Optional<String> tag;
        Optional<Attribute> attribute;
    };
    Optional<ContextMenuData> m_context_menu_data;

    HashMap<int, Vector<Attribute>> m_dom_node_attributes;

    i32 m_highest_notified_message_index { -1 };
    i32 m_highest_received_message_index { -1 };
    bool m_waiting_for_messages { false };
};

}
