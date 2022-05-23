/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "InspectorWidget.h"
#include "ElementSizePreviewWidget.h"
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Splitter.h>
#include <LibGUI/TabWidget.h>
#include <LibGUI/TableView.h>
#include <LibGUI/TreeView.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Element.h>
#include <LibWebView/DOMTreeModel.h>
#include <LibWebView/OutOfProcessWebView.h>
#include <LibWebView/StylePropertiesModel.h>

namespace Browser {

void InspectorWidget::set_selection(Selection selection)
{
    if (!m_dom_json.has_value()) {
        // DOM Tree hasn't been loaded yet, so make a note to inspect it later.
        m_pending_selection = move(selection);
        return;
    }

    auto* model = verify_cast<WebView::DOMTreeModel>(m_dom_tree_view->model());
    auto index = model->index_for_node(selection.dom_node_id, selection.pseudo_element);
    if (!index.is_valid()) {
        dbgln("InspectorWidget told to inspect non-existent node: {}", selection.to_string());
        return;
    }

    m_dom_tree_view->expand_all_parents_of(index);
    m_dom_tree_view->set_cursor(index, GUI::AbstractView::SelectionUpdate::Set);
    set_selection(index);
}

void InspectorWidget::set_selection(GUI::ModelIndex const index)
{
    if (!index.is_valid())
        return;

    auto* json = static_cast<JsonObject const*>(index.internal_data());
    VERIFY(json);

    Selection selection {};
    if (json->has_u32("pseudo-element")) {
        selection.dom_node_id = json->get("parent-id").to_i32();
        selection.pseudo_element = static_cast<Web::CSS::Selector::PseudoElement>(json->get("pseudo-element").to_u32());
    } else {
        selection.dom_node_id = json->get("id").to_i32();
    }

    if (selection == m_selection)
        return;
    m_selection = move(selection);

    auto maybe_inspected_node_properties = m_web_view->inspect_dom_node(m_selection.dom_node_id, m_selection.pseudo_element);
    if (maybe_inspected_node_properties.has_value()) {
        auto inspected_node_properties = maybe_inspected_node_properties.value();
        load_style_json(inspected_node_properties.specified_values_json, inspected_node_properties.computed_values_json, inspected_node_properties.custom_properties_json);
        update_node_box_model(inspected_node_properties.node_box_sizing_json);
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
        set_selection(index);
    };

    auto& bottom_tab_widget = splitter.add<GUI::TabWidget>();

    auto& computed_style_table_container = bottom_tab_widget.add_tab<GUI::Widget>("Computed");
    computed_style_table_container.set_layout<GUI::VerticalBoxLayout>();
    computed_style_table_container.layout()->set_margins({ 4, 4, 4, 4 });
    m_computed_style_table_view = computed_style_table_container.add<GUI::TableView>();

    auto& resolved_style_table_container = bottom_tab_widget.add_tab<GUI::Widget>("Resolved");
    resolved_style_table_container.set_layout<GUI::VerticalBoxLayout>();
    resolved_style_table_container.layout()->set_margins({ 4, 4, 4, 4 });
    m_resolved_style_table_view = resolved_style_table_container.add<GUI::TableView>();

    auto& custom_properties_table_container = bottom_tab_widget.add_tab<GUI::Widget>("Variables");
    custom_properties_table_container.set_layout<GUI::VerticalBoxLayout>();
    custom_properties_table_container.layout()->set_margins({ 4, 4, 4, 4 });
    m_custom_properties_table_view = custom_properties_table_container.add<GUI::TableView>();

    auto& box_model_widget = bottom_tab_widget.add_tab<GUI::Widget>("Box Model");
    box_model_widget.set_layout<GUI::VerticalBoxLayout>();
    box_model_widget.layout()->set_margins({ 4, 4, 4, 4 });
    m_element_size_view = box_model_widget.add<ElementSizePreviewWidget>();
    m_element_size_view->set_should_hide_unnecessary_scrollbars(true);

    m_dom_tree_view->set_focus(true);
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
    m_dom_tree_view->set_model(WebView::DOMTreeModel::create(m_dom_json->view(), *m_dom_tree_view));

    if (m_pending_selection.has_value()) {
        set_selection(m_pending_selection.release_value());
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

void InspectorWidget::set_dom_node_properties_json(Selection selection, String specified_values_json, String computed_values_json, String custom_properties_json, String node_box_sizing_json)
{
    if (selection != m_selection) {
        dbgln("Got data for the wrong node id! Wanted ({}), got ({})", m_selection.to_string(), selection.to_string());
        return;
    }

    load_style_json(specified_values_json, computed_values_json, custom_properties_json);
    update_node_box_model(node_box_sizing_json);
}

void InspectorWidget::load_style_json(String specified_values_json, String computed_values_json, String custom_properties_json)
{
    m_selection_specified_values_json = specified_values_json;
    m_computed_style_table_view->set_model(WebView::StylePropertiesModel::create(m_selection_specified_values_json.value().view()));
    m_computed_style_table_view->set_searchable(true);

    m_selection_computed_values_json = computed_values_json;
    m_resolved_style_table_view->set_model(WebView::StylePropertiesModel::create(m_selection_computed_values_json.value().view()));
    m_resolved_style_table_view->set_searchable(true);

    m_selection_custom_properties_json = custom_properties_json;
    m_custom_properties_table_view->set_model(WebView::StylePropertiesModel::create(m_selection_custom_properties_json.value().view()));
    m_custom_properties_table_view->set_searchable(true);
}

void InspectorWidget::update_node_box_model(Optional<String> node_box_sizing_json)
{
    if (!node_box_sizing_json.has_value()) {
        return;
    }
    auto json_or_error = JsonValue::from_string(node_box_sizing_json.value());
    if (json_or_error.is_error() || !json_or_error.value().is_object()) {
        return;
    }
    auto json_value = json_or_error.release_value();
    auto const& json_object = json_value.as_object();

    m_node_box_sizing.margin.top = json_object.get("margin_top").to_float();
    m_node_box_sizing.margin.right = json_object.get("margin_right").to_float();
    m_node_box_sizing.margin.bottom = json_object.get("margin_bottom").to_float();
    m_node_box_sizing.margin.left = json_object.get("margin_left").to_float();
    m_node_box_sizing.padding.top = json_object.get("padding_top").to_float();
    m_node_box_sizing.padding.right = json_object.get("padding_right").to_float();
    m_node_box_sizing.padding.bottom = json_object.get("padding_bottom").to_float();
    m_node_box_sizing.padding.left = json_object.get("padding_left").to_float();
    m_node_box_sizing.border.top = json_object.get("border_top").to_float();
    m_node_box_sizing.border.right = json_object.get("border_right").to_float();
    m_node_box_sizing.border.bottom = json_object.get("border_bottom").to_float();
    m_node_box_sizing.border.left = json_object.get("border_left").to_float();

    m_element_size_view->set_node_content_width(json_object.get("content_width").to_float());
    m_element_size_view->set_node_content_height(json_object.get("content_height").to_float());
    m_element_size_view->set_box_model(m_node_box_sizing);
}

void InspectorWidget::clear_style_json()
{
    m_selection_specified_values_json.clear();
    m_computed_style_table_view->set_model(nullptr);

    m_selection_computed_values_json.clear();
    m_resolved_style_table_view->set_model(nullptr);

    m_selection_custom_properties_json.clear();
    m_custom_properties_table_view->set_model(nullptr);

    m_element_size_view->set_box_model({});
    m_element_size_view->set_node_content_width(0);
    m_element_size_view->set_node_content_height(0);
}

}
