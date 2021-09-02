/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Sam Atkins <atkinssj@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "InspectorWidget.h"
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Splitter.h>
#include <LibGUI/TabWidget.h>
#include <LibGUI/TableView.h>
#include <LibGUI/TreeView.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Element.h>
#include <LibWeb/DOMTreeModel.h>
#include <LibWeb/OutOfProcessWebView.h>
#include <LibWeb/StylePropertiesModel.h>

namespace Browser {

void InspectorWidget::set_inspected_node(GUI::ModelIndex const index)
{
    auto* json = static_cast<JsonObject const*>(index.internal_data());
    i32 inspected_node = json ? json->get("id").to_i32() : 0;
    if (inspected_node == m_inspected_node_id)
        return;
    m_inspected_node_id = inspected_node;

    m_dom_tree_view->set_cursor(index, GUI::AbstractView::SelectionUpdate::Set);
    m_dom_tree_view->expand_all_parents_of(index);

    auto maybe_inspected_node_properties = m_web_view->inspect_dom_node(m_inspected_node_id);
    if (maybe_inspected_node_properties.has_value()) {
        auto inspected_node_properties = maybe_inspected_node_properties.value();
        m_inspected_node_specified_values_json = inspected_node_properties.specified_values_json;
        m_inspected_node_computed_values_json = inspected_node_properties.computed_values_json;
        m_style_table_view->set_model(Web::StylePropertiesModel::create(m_inspected_node_specified_values_json.value().view()));
        m_computed_style_table_view->set_model(Web::StylePropertiesModel::create(m_inspected_node_computed_values_json.value().view()));
    } else {
        m_inspected_node_specified_values_json.clear();
        m_inspected_node_computed_values_json.clear();
        m_style_table_view->set_model(nullptr);
        m_computed_style_table_view->set_model(nullptr);
    }
}

InspectorWidget::InspectorWidget()
{
    set_layout<GUI::VerticalBoxLayout>();
    auto& splitter = add<GUI::VerticalSplitter>();

    auto& top_tab_widget = splitter.add<GUI::TabWidget>();

    m_dom_tree_view = top_tab_widget.add_tab<GUI::TreeView>("DOM");
    m_dom_tree_view->on_selection_change = [this] {
        const auto& index = m_dom_tree_view->selection().first();
        set_inspected_node(index);
    };

    m_layout_tree_view = top_tab_widget.add_tab<GUI::TreeView>("Layout");
    m_layout_tree_view->on_selection_change = [this] {
        const auto& index = m_layout_tree_view->selection().first();
        set_inspected_node(index);
    };

    auto& bottom_tab_widget = splitter.add<GUI::TabWidget>();

    m_style_table_view = bottom_tab_widget.add_tab<GUI::TableView>("Styles");
    m_computed_style_table_view = bottom_tab_widget.add_tab<GUI::TableView>("Computed");
}

InspectorWidget::~InspectorWidget()
{
}

void InspectorWidget::set_dom_json(String json)
{
    if (m_dom_json.has_value() && m_dom_json.value() == json)
        return;

    m_dom_json = json;
    m_dom_tree_view->set_model(Web::DOMTreeModel::create(m_dom_json->view()));

    // FIXME: Support the LayoutTreeModel
    // m_layout_tree_view->set_model(Web::LayoutTreeModel::create(*document));
}

void InspectorWidget::set_dom_node_properties_json(i32 node_id, String specified_values_json, String computed_values_json)
{
    if (node_id != m_inspected_node_id) {
        dbgln("Got data for the wrong node id! Wanted {}, got {}", m_inspected_node_id, node_id);
        return;
    }

    m_inspected_node_specified_values_json = specified_values_json;
    m_inspected_node_computed_values_json = computed_values_json;
    m_style_table_view->set_model(Web::StylePropertiesModel::create(m_inspected_node_specified_values_json.value().view()));
    m_computed_style_table_view->set_model(Web::StylePropertiesModel::create(m_inspected_node_computed_values_json.value().view()));
}

}
