/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Widget.h>
#include <LibWeb/Forward.h>

namespace Browser {

class InspectorWidget final : public GUI::Widget {
    C_OBJECT(InspectorWidget)
public:
    virtual ~InspectorWidget() = default;

    void set_web_view(NonnullRefPtr<Web::OutOfProcessWebView> web_view) { m_web_view = web_view; }
    void set_dom_json(String);
    void clear_dom_json();
    void set_dom_node_properties_json(i32 node_id, String specified_values_json, String computed_values_json, String custom_properties_json);

    void set_inspected_node(i32 node_id);
    void select_default_node();

private:
    InspectorWidget();

    void set_inspected_node(GUI::ModelIndex);
    void load_style_json(String specified_values_json, String computed_values_json, String custom_properties_json);
    void clear_style_json();

    RefPtr<Web::OutOfProcessWebView> m_web_view;

    RefPtr<GUI::TreeView> m_dom_tree_view;
    RefPtr<GUI::TableView> m_style_table_view;
    RefPtr<GUI::TableView> m_computed_style_table_view;
    RefPtr<GUI::TableView> m_custom_properties_table_view;

    // Multi-process mode
    Optional<i32> m_pending_inspect_node_id;
    i32 m_inspected_node_id;
    Optional<String> m_dom_json;
    Optional<String> m_inspected_node_specified_values_json;
    Optional<String> m_inspected_node_computed_values_json;
    Optional<String> m_inspected_node_custom_properties_json;
};

}
