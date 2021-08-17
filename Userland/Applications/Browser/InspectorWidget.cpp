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
#include <LibWeb/DOMTreeJSONModel.h>
#include <LibWeb/DOMTreeModel.h>
#include <LibWeb/LayoutTreeModel.h>
#include <LibWeb/StylePropertiesModel.h>

namespace Browser {

void InspectorWidget::set_inspected_node(GUI::ModelIndex const index)
{
    if (!m_document) {
        // FIXME: Handle this for OutOfProcessWebView
        return;
    }
    auto* node = static_cast<Web::DOM::Node*>(index.internal_data());
    m_inspected_node = node;
    m_document->set_inspected_node(node);
    if (node && node->is_element()) {
        auto& element = verify_cast<Web::DOM::Element>(*node);
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

void InspectorWidget::set_document(Web::DOM::Document* document)
{
    document->set_inspected_node(m_inspected_node);
    if (m_document == document)
        return;
    m_document = document;
    m_dom_tree_view->set_model(Web::DOMTreeModel::create(*document));
    m_layout_tree_view->set_model(Web::LayoutTreeModel::create(*document));
}

void InspectorWidget::set_dom_json(String json)
{
    if (m_dom_json.has_value() && m_dom_json.value() == json)
        return;

    m_dom_json = json;
    m_dom_tree_view->set_model(Web::DOMTreeJSONModel::create(m_dom_json->view()));

    // FIXME: Support the LayoutTreeModel
    // m_layout_tree_view->set_model(Web::LayoutTreeModel::create(*document));
}

}
