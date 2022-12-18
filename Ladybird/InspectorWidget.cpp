/*
 * Copyright (c) 2022, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#define AK_DONT_REPLACE_STD

#include "InspectorWidget.h"
#include <LibWebView/DOMTreeModel.h>
#include <QCloseEvent>
#include <QTreeView>
#include <QVBoxLayout>

namespace Ladybird {

InspectorWidget::InspectorWidget()
{
    setLayout(new QVBoxLayout);
    m_tree_view = new QTreeView;
    m_tree_view->setHeaderHidden(true);
    m_tree_view->expandToDepth(3);
    layout()->addWidget(m_tree_view);
    m_tree_view->setModel(&m_dom_model);
    QObject::connect(m_tree_view->selectionModel(), &QItemSelectionModel::selectionChanged,
        [this](QItemSelection const& selected, QItemSelection const&) {
            auto indexes = selected.indexes();
            if (indexes.size()) {
                auto index = m_dom_model.to_gui(indexes.first());
                set_selection(index);
            }
        });
}

void InspectorWidget::clear_dom_json()
{
    m_dom_model.set_underlying_model(nullptr);
}

void InspectorWidget::set_dom_json(StringView dom_json)
{
    m_dom_model.set_underlying_model(::WebView::DOMTreeModel::create(dom_json));
}

void InspectorWidget::closeEvent(QCloseEvent* event)
{
    event->accept();
    if (on_close)
        on_close();
}

void InspectorWidget::set_selection(GUI::ModelIndex index)
{
    if (!index.is_valid())
        return;

    auto* json = static_cast<JsonObject const*>(index.internal_data());
    VERIFY(json);

    Selection selection {};
    if (json->has_u32("pseudo-element"sv)) {
        selection.dom_node_id = json->get("parent-id"sv).to_i32();
        selection.pseudo_element = static_cast<Web::CSS::Selector::PseudoElement>(json->get("pseudo-element"sv).to_u32());
    } else {
        selection.dom_node_id = json->get("id"sv).to_i32();
    }

    if (selection == m_selection)
        return;
    m_selection = selection;

    VERIFY(on_dom_node_inspected);
    on_dom_node_inspected(m_selection.dom_node_id, m_selection.pseudo_element);
}

}
