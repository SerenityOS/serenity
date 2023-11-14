/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "InspectorWidget.h"
#include "ModelAdapter.h"
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Splitter.h>
#include <LibGUI/TabWidget.h>
#include <LibGUI/TableView.h>
#include <LibWebView/InspectorClient.h>
#include <LibWebView/OutOfProcessWebView.h>

namespace Browser {

NonnullRefPtr<InspectorWidget> InspectorWidget::create(WebView::OutOfProcessWebView& content_view)
{
    return adopt_ref(*new (nothrow) InspectorWidget(content_view));
}

InspectorWidget::InspectorWidget(WebView::OutOfProcessWebView& content_view)
{
    set_layout<GUI::VerticalBoxLayout>(4);
    set_fill_with_background_color(true);

    auto& splitter = add<GUI::VerticalSplitter>();

    m_inspector_view = splitter.add<WebView::OutOfProcessWebView>();
    m_inspector_client = make<WebView::InspectorClient>(content_view, *m_inspector_view);

    m_inspector_client->on_dom_node_properties_received = [this](auto properties_or_error) {
        if (properties_or_error.is_error()) {
            clear_style_json();
            clear_node_box_model();
        } else {
            auto properties = properties_or_error.release_value();

            load_style_json(properties.computed_style_json, properties.resolved_style_json, properties.custom_properties_json);
            update_node_box_model(properties.node_box_sizing_json);
            update_aria_properties_state_model(properties.aria_properties_state_json);
        }
    };

    auto& bottom_tab_widget = splitter.add<GUI::TabWidget>();

    auto& computed_style_table_container = bottom_tab_widget.add_tab<GUI::Widget>("Computed"_string);
    computed_style_table_container.set_layout<GUI::VerticalBoxLayout>(4);
    m_computed_style_table_view = computed_style_table_container.add<GUI::TableView>();

    auto& resolved_style_table_container = bottom_tab_widget.add_tab<GUI::Widget>("Resolved"_string);
    resolved_style_table_container.set_layout<GUI::VerticalBoxLayout>(4);
    m_resolved_style_table_view = resolved_style_table_container.add<GUI::TableView>();

    auto& custom_properties_table_container = bottom_tab_widget.add_tab<GUI::Widget>("Variables"_string);
    custom_properties_table_container.set_layout<GUI::VerticalBoxLayout>(4);
    m_custom_properties_table_view = custom_properties_table_container.add<GUI::TableView>();

    auto& box_model_widget = bottom_tab_widget.add_tab<GUI::Widget>("Box Model"_string);
    box_model_widget.set_layout<GUI::VerticalBoxLayout>(4);
    m_element_size_view = box_model_widget.add<ElementSizePreviewWidget>();
    m_element_size_view->set_should_hide_unnecessary_scrollbars(true);

    auto& aria_properties_state_widget = bottom_tab_widget.add_tab<GUI::Widget>("ARIA"_string);
    aria_properties_state_widget.set_layout<GUI::VerticalBoxLayout>(4);
    m_aria_properties_state_view = aria_properties_state_widget.add<GUI::TableView>();

    m_inspector_view->set_focus(true);
}

InspectorWidget::~InspectorWidget() = default;

void InspectorWidget::inspect()
{
    m_inspector_client->inspect();
}

void InspectorWidget::reset()
{
    m_inspector_client->reset();
    clear_style_json();
    clear_node_box_model();
}

void InspectorWidget::select_default_node()
{
    m_inspector_client->select_default_node();
}

void InspectorWidget::select_hovered_node()
{
    m_inspector_client->select_hovered_node();
}

void InspectorWidget::load_style_json(StringView computed_values_json, StringView resolved_values_json, StringView custom_properties_json)
{
    m_computed_style_table_view->set_model(PropertyTableModel::create(PropertyTableModel::Type::StyleProperties, computed_values_json).release_value_but_fixme_should_propagate_errors());
    m_computed_style_table_view->set_searchable(true);

    m_resolved_style_table_view->set_model(PropertyTableModel::create(PropertyTableModel::Type::StyleProperties, resolved_values_json).release_value_but_fixme_should_propagate_errors());
    m_resolved_style_table_view->set_searchable(true);

    m_custom_properties_table_view->set_model(PropertyTableModel::create(PropertyTableModel::Type::StyleProperties, custom_properties_json).release_value_but_fixme_should_propagate_errors());
    m_custom_properties_table_view->set_searchable(true);
}

void InspectorWidget::clear_style_json()
{
    m_computed_style_table_view->set_model(nullptr);
    m_resolved_style_table_view->set_model(nullptr);
    m_custom_properties_table_view->set_model(nullptr);
    m_aria_properties_state_view->set_model(nullptr);

    m_element_size_view->set_box_model({});
    m_element_size_view->set_node_content_width(0);
    m_element_size_view->set_node_content_height(0);
}

void InspectorWidget::update_node_box_model(StringView node_box_sizing_json)
{
    auto json_or_error = JsonValue::from_string(node_box_sizing_json);
    if (json_or_error.is_error() || !json_or_error.value().is_object()) {
        return;
    }
    auto json_value = json_or_error.release_value();
    auto const& json_object = json_value.as_object();

    m_node_box_sizing.margin.top = Web::CSSPixels(json_object.get_float_with_precision_loss("margin_top"sv).value_or(0));
    m_node_box_sizing.margin.right = Web::CSSPixels(json_object.get_float_with_precision_loss("margin_right"sv).value_or(0));
    m_node_box_sizing.margin.bottom = Web::CSSPixels(json_object.get_float_with_precision_loss("margin_bottom"sv).value_or(0));
    m_node_box_sizing.margin.left = Web::CSSPixels(json_object.get_float_with_precision_loss("margin_left"sv).value_or(0));
    m_node_box_sizing.padding.top = Web::CSSPixels(json_object.get_float_with_precision_loss("padding_top"sv).value_or(0));
    m_node_box_sizing.padding.right = Web::CSSPixels(json_object.get_float_with_precision_loss("padding_right"sv).value_or(0));
    m_node_box_sizing.padding.bottom = Web::CSSPixels(json_object.get_float_with_precision_loss("padding_bottom"sv).value_or(0));
    m_node_box_sizing.padding.left = Web::CSSPixels(json_object.get_float_with_precision_loss("padding_left"sv).value_or(0));
    m_node_box_sizing.border.top = Web::CSSPixels(json_object.get_float_with_precision_loss("border_top"sv).value_or(0));
    m_node_box_sizing.border.right = Web::CSSPixels(json_object.get_float_with_precision_loss("border_right"sv).value_or(0));
    m_node_box_sizing.border.bottom = Web::CSSPixels(json_object.get_float_with_precision_loss("border_bottom"sv).value_or(0));
    m_node_box_sizing.border.left = Web::CSSPixels(json_object.get_float_with_precision_loss("border_left"sv).value_or(0));

    m_element_size_view->set_node_content_width(json_object.get_float_with_precision_loss("content_width"sv).value_or(0));
    m_element_size_view->set_node_content_height(json_object.get_float_with_precision_loss("content_height"sv).value_or(0));
    m_element_size_view->set_box_model(m_node_box_sizing);
}

void InspectorWidget::clear_node_box_model()
{
    m_node_box_sizing = Web::Layout::BoxModelMetrics {};
    m_element_size_view->set_node_content_width(0);
    m_element_size_view->set_node_content_height(0);
    m_element_size_view->set_box_model(m_node_box_sizing);
}

void InspectorWidget::update_aria_properties_state_model(StringView aria_properties_state_json)
{
    m_aria_properties_state_view->set_model(PropertyTableModel::create(PropertyTableModel::Type::ARIAProperties, aria_properties_state_json).release_value_but_fixme_should_propagate_errors());
    m_aria_properties_state_view->set_searchable(true);
}

}
