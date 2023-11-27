/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonValue.h>
#include <AK/StringView.h>
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

private:
    void maybe_load_inspector();
    void generate_dom_tree(StringBuilder&);
    void generate_accessibility_tree(StringBuilder&);

    void select_node(i32 node_id);

    ViewImplementation& m_content_web_view;
    ViewImplementation& m_inspector_web_view;

    Optional<JsonValue> m_dom_tree;
    Optional<JsonValue> m_accessibility_tree;

    Optional<i32> m_body_node_id;
    Optional<i32> m_pending_selection;

    bool m_inspector_loaded { false };
};

}
