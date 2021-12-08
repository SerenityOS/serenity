/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
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

void InspectorWidget::set_inspected_node(i32 node_id)
{
    if (!m_dom_json.has_value()) {
        // DOM Tree hasn't been loaded yet, so make a note to inspect it later.
        m_pending_inspect_node_id = node_id;
        return;
    }

    auto* model = verify_cast<Web::DOMTreeModel>(m_dom_tree_view->model());
    auto index = model->index_for_node(node_id);
    if (!index.is_valid()) {
        dbgln("InspectorWidget told to inspect non-existent node, id={}", node_id);
        return;
    }

    m_dom_tree_view->expand_all_parents_of(index);
    m_dom_tree_view->set_cursor(index, GUI::AbstractView::SelectionUpdate::Set);
    set_inspected_node(index);
}

void InspectorWidget::set_inspected_node(GUI::ModelIndex const index)
{
    auto* json = static_cast<JsonObject const*>(index.internal_data());
    i32 inspected_node = json ? json->get("id").to_i32() : 0;
    if (inspected_node == m_inspected_node_id)
        return;
    m_inspected_node_id = inspected_node;

    auto maybe_inspected_node_properties = m_web_view->inspect_dom_node(m_inspected_node_id);
    if (maybe_inspected_node_properties.has_value()) {
        auto inspected_node_properties = maybe_inspected_node_properties.value();
        load_style_json(inspected_node_properties.specified_values_json, inspected_node_properties.computed_values_json, inspected_node_properties.custom_properties_json);
    } else {
        clear_style_json();
    }
}

InspectorWidget::InspectorWidget()
{
    set_fill_with_background_color(true);

    auto& layout = set_layout<GUI::VerticalBoxLayout>();
    layout.set_margins({ 4, 4, 4, 4 });
    auto& splitter = add<GUI::VerticalSplitter>();

    auto& top_tab_widget = splitter.add<GUI::TabWidget>();

    auto& dom_tree_container = top_tab_widget.add_tab<GUI::Widget>("DOM");
    dom_tree_container.set_layout<GUI::VerticalBoxLayout>();
    dom_tree_container.layout()->set_margins({ 4, 4, 4, 4 });
    m_dom_tree_view = dom_tree_container.add<GUI::TreeView>();
    m_dom_tree_view->on_selection_change = [this] {
        const auto& index = m_dom_tree_view->selection().first();
        set_inspected_node(index);
    };

    auto& bottom_tab_widget = splitter.add<GUI::TabWidget>();

    auto& style_table_container = bottom_tab_widget.add_tab<GUI::Widget>("Styles");
    style_table_container.set_layout<GUI::VerticalBoxLayout>();
    style_table_container.layout()->set_margins({ 4, 4, 4, 4 });
    m_style_table_view = style_table_container.add<GUI::TableView>();

    auto& computed_style_table_container = bottom_tab_widget.add_tab<GUI::Widget>("Computed");
    computed_style_table_container.set_layout<GUI::VerticalBoxLayout>();
    computed_style_table_container.layout()->set_margins({ 4, 4, 4, 4 });
    m_computed_style_table_view = computed_style_table_container.add<GUI::TableView>();

    auto& custom_properties_table_container = bottom_tab_widget.add_tab<GUI::Widget>("Variables");
    custom_properties_table_container.set_layout<GUI::VerticalBoxLayout>();
    custom_properties_table_container.layout()->set_margins({ 4, 4, 4, 4 });
    m_custom_properties_table_view = custom_properties_table_container.add<GUI::TableView>();

    m_dom_tree_view->set_focus(true);
}

InspectorWidget::~InspectorWidget()
{
}

void InspectorWidget::select_default_node()
{
    clear_style_json();

    // FIXME: Select the <body> element, or else the root node.
    m_dom_tree_view->collapse_tree();
    m_dom_tree_view->set_cursor({}, GUI::AbstractView::SelectionUpdate::ClearIfNotSelected);
}

void InspectorWidget::set_dom_json(String json)
{
    if (m_dom_json.has_value() && m_dom_json.value() == json)
        return;

    m_dom_json = json;
    m_dom_tree_view->set_model(Web::DOMTreeModel::create(m_dom_json->view(), *m_dom_tree_view));

    if (m_pending_inspect_node_id.has_value()) {
        i32 node_id = m_pending_inspect_node_id.value();
        m_pending_inspect_node_id.clear();
        set_inspected_node(node_id);
    } else {
        select_default_node();
    }
}

void InspectorWidget::clear_dom_json()
{
    m_dom_json.clear();
    m_dom_tree_view->set_model(nullptr);
    clear_style_json();
}

void InspectorWidget::set_dom_node_properties_json(i32 node_id, String specified_values_json, String computed_values_json, String custom_properties_json)
{
    if (node_id != m_inspected_node_id) {
        dbgln("Got data for the wrong node id! Wanted {}, got {}", m_inspected_node_id, node_id);
        return;
    }

    load_style_json(specified_values_json, computed_values_json, custom_properties_json);
}

void InspectorWidget::load_style_json(String specified_values_json, String computed_values_json, String custom_properties_json)
{
    m_inspected_node_specified_values_json = specified_values_json;
    m_style_table_view->set_model(Web::StylePropertiesModel::create(m_inspected_node_specified_values_json.value().view()));

    m_inspected_node_computed_values_json = computed_values_json;
    m_computed_style_table_view->set_model(Web::StylePropertiesModel::create(m_inspected_node_computed_values_json.value().view()));

    m_inspected_node_custom_properties_json = custom_properties_json;
    m_custom_properties_table_view->set_model(Web::StylePropertiesModel::create(m_inspected_node_custom_properties_json.value().view()));
}

void InspectorWidget::clear_style_json()
{
    m_inspected_node_specified_values_json.clear();
    m_style_table_view->set_model(nullptr);

    m_inspected_node_computed_values_json.clear();
    m_computed_style_table_view->set_model(nullptr);

    m_inspected_node_custom_properties_json.clear();
    m_custom_properties_table_view->set_model(nullptr);
}

}
