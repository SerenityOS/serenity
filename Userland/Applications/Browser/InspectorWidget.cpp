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
#include <LibWeb/DOMTreeJSONModel.h>
#include <LibWeb/DOMTreeModel.h>
#include <LibWeb/StylePropertiesModel.h>

namespace Browser {

void InspectorWidget::set_inspected_node(GUI::ModelIndex const)
{
    // FIXME: Handle this for OutOfProcessWebView
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
    m_dom_tree_view->set_model(Web::DOMTreeJSONModel::create(m_dom_json->view()));

    // FIXME: Support the LayoutTreeModel
    // m_layout_tree_view->set_model(Web::LayoutTreeModel::create(*document));
}

}
