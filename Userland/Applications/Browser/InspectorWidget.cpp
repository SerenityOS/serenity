/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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
#include <LibWeb/LayoutTreeModel.h>
#include <LibWeb/StylePropertiesModel.h>

namespace Browser {

void InspectorWidget::set_inspected_node(Web::DOM::Node* node)
{
    m_document->set_inspected_node(node);
    if (node && node->is_element()) {
        auto& element = downcast<Web::DOM::Element>(*node);
        if (element.specified_css_values()) {
            m_style_table_view->set_model(Web::StylePropertiesModel::create(*element.specified_css_values()));
            m_computed_style_table_view->set_model(Web::StylePropertiesModel::create(*element.computed_style()));
        }
    } else {
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
        auto* node = static_cast<Web::DOM::Node*>(index.internal_data());
        set_inspected_node(node);
    };

    m_layout_tree_view = top_tab_widget.add_tab<GUI::TreeView>("Layout");
    m_layout_tree_view->on_selection_change = [this] {
        const auto& index = m_layout_tree_view->selection().first();
        auto* node = static_cast<Web::Layout::Node*>(index.internal_data());
        set_inspected_node(node->dom_node());
    };

    auto& bottom_tab_widget = splitter.add<GUI::TabWidget>();

    m_style_table_view = bottom_tab_widget.add_tab<GUI::TableView>("Styles");
    m_computed_style_table_view = bottom_tab_widget.add_tab<GUI::TableView>("Computed");
}

InspectorWidget::~InspectorWidget()
{
}

void InspectorWidget::set_document(Web::DOM::Document* document)
{
    if (m_document == document)
        return;
    m_document = document;
    m_dom_tree_view->set_model(Web::DOMTreeModel::create(*document));
    m_layout_tree_view->set_model(Web::LayoutTreeModel::create(*document));
}

}
