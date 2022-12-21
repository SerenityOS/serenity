/*
 * Copyright (c) 2022, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#define AK_DONT_REPLACE_STD

#include <LibWebView/DOMTreeModel.h>
#include <LibWebView/StylePropertiesModel.h>

#include "InspectorWidget.h"
#include <QCloseEvent>
#include <QHeaderView>
#include <QSplitter>
#include <QStringList>
#include <QTabWidget>
#include <QTableView>
#include <QTreeView>
#include <QVBoxLayout>

namespace Ladybird {

InspectorWidget::InspectorWidget()
{
    setLayout(new QVBoxLayout);
    auto splitter = new QSplitter(this);
    layout()->addWidget(splitter);
    splitter->setOrientation(Qt::Vertical);
    auto tree_view = new QTreeView;
    tree_view->setHeaderHidden(true);
    tree_view->expandToDepth(3);
    splitter->addWidget(tree_view);
    tree_view->setModel(&m_dom_model);
    QObject::connect(tree_view->selectionModel(), &QItemSelectionModel::selectionChanged,
        [this](QItemSelection const& selected, QItemSelection const&) {
            auto indexes = selected.indexes();
            if (indexes.size()) {
                auto index = m_dom_model.to_gui(indexes.first());
                set_selection(index);
            }
        });

    auto add_table_tab = [&](auto* tab_widget, auto& model, auto name) {
        auto container = new QWidget;
        auto table_view = new QTableView;
        table_view->setModel(&model);
        container->setLayout(new QVBoxLayout);
        container->layout()->addWidget(table_view);
        table_view->horizontalHeader()->setSectionResizeMode(QHeaderView::Stretch);
        table_view->verticalHeader()->setVisible(false);
        table_view->horizontalHeader()->setVisible(false);
        tab_widget->addTab(container, name);
    };

    auto node_tabs = new QTabWidget;
    add_table_tab(node_tabs, m_computed_style_model, "Computed");
    add_table_tab(node_tabs, m_resolved_style_model, "Resolved");
    add_table_tab(node_tabs, m_custom_properties_model, "Variables");
    splitter->addWidget(node_tabs);
}

void InspectorWidget::set_dom_json(StringView dom_json)
{
    m_dom_model.set_underlying_model(WebView::DOMTreeModel::create(dom_json));
}

void InspectorWidget::clear_dom_json()
{
    m_dom_model.set_underlying_model(nullptr);
    clear_style_json();
}

void InspectorWidget::load_style_json(StringView computed_style_json, StringView resolved_style_json, StringView custom_properties_json)
{
    m_computed_style_model.set_underlying_model(WebView::StylePropertiesModel::create(computed_style_json));
    m_resolved_style_model.set_underlying_model(WebView::StylePropertiesModel::create(resolved_style_json));
    m_custom_properties_model.set_underlying_model(WebView::StylePropertiesModel::create(custom_properties_json));
}

void InspectorWidget::clear_style_json()
{
    m_computed_style_model.set_underlying_model(nullptr);
    m_resolved_style_model.set_underlying_model(nullptr);
    m_custom_properties_model.set_underlying_model(nullptr);
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
        selection.dom_node_id = json->get_deprecated("parent-id"sv).to_i32();
        selection.pseudo_element = static_cast<Web::CSS::Selector::PseudoElement>(json->get_deprecated("pseudo-element"sv).to_u32());
    } else {
        selection.dom_node_id = json->get_deprecated("id"sv).to_i32();
    }

    if (selection == m_selection)
        return;
    m_selection = selection;

    VERIFY(on_dom_node_inspected);
    auto maybe_inspected_node_properties = on_dom_node_inspected(m_selection.dom_node_id, m_selection.pseudo_element);
    if (!maybe_inspected_node_properties.is_error()) {
        auto properties = maybe_inspected_node_properties.release_value();
        load_style_json(properties.computed_style_json, properties.resolved_style_json, properties.custom_properties_json);
    } else {
        clear_style_json();
    }
}

}
