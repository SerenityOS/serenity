/*
 * Copyright (c) 2022, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "InspectorWidget.h"
#include <AK/JsonObject.h>
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

    auto add_tab = [&](auto* tab_widget, auto* widget, auto name) {
        auto container = new QWidget;
        container->setLayout(new QVBoxLayout);
        container->layout()->addWidget(widget);
        tab_widget->addTab(container, name);
    };

    auto* top_tab_widget = new QTabWidget;
    splitter->addWidget(top_tab_widget);

    m_dom_tree_view = new QTreeView;
    m_dom_tree_view->setHeaderHidden(true);
    add_tab(top_tab_widget, m_dom_tree_view, "DOM");

    m_accessibility_tree_view = new QTreeView;
    m_accessibility_tree_view->setHeaderHidden(true);
    add_tab(top_tab_widget, m_accessibility_tree_view, "Accessibility");

    auto add_table_tab = [&](auto* tab_widget, auto name) {
        auto* table_view = new QTableView;
        table_view->horizontalHeader()->setSectionResizeMode(QHeaderView::Stretch);
        table_view->verticalHeader()->setVisible(false);
        table_view->horizontalHeader()->setVisible(false);
        add_tab(tab_widget, table_view, name);
        return table_view;
    };

    auto* node_tabs = new QTabWidget;
    m_computed_style_table = add_table_tab(node_tabs, "Computed");
    m_resolved_style_table = add_table_tab(node_tabs, "Resolved");
    m_custom_properties_table = add_table_tab(node_tabs, "Variables");
    splitter->addWidget(node_tabs);
}

void InspectorWidget::set_dom_json(StringView dom_json)
{
    m_dom_model = TreeModel::create(TreeModel::Type::DOMTree, dom_json).release_value_but_fixme_should_propagate_errors();
    m_dom_tree_view->setModel(m_dom_model);
    m_dom_loaded = true;

    QObject::connect(m_dom_tree_view->selectionModel(), &QItemSelectionModel::selectionChanged,
        [this](QItemSelection const& selected, QItemSelection const&) {
            if (auto indexes = selected.indexes(); !indexes.empty())
                set_selection(indexes.first());
        });

    if (m_pending_selection.has_value())
        set_selection(m_pending_selection.release_value());
    else
        select_default_node();
}

void InspectorWidget::set_accessibility_json(StringView accessibility_json)
{
    m_accessibility_model = TreeModel::create(TreeModel::Type::AccessibilityTree, accessibility_json).release_value_but_fixme_should_propagate_errors();
    m_accessibility_tree_view->setModel(m_accessibility_model);
}

void InspectorWidget::clear_dom_json()
{
    m_dom_tree_view->setModel(nullptr);
    m_dom_model = nullptr;

    // The accessibility tree is pretty much another form of the DOM tree, so should be cleared at the time time.
    m_accessibility_tree_view->setModel(nullptr);
    m_accessibility_model = nullptr;

    clear_style_json();
    clear_selection();
    m_dom_loaded = false;
}

void InspectorWidget::load_style_json(StringView computed_style_json, StringView resolved_style_json, StringView custom_properties_json)
{
    m_computed_style_model = PropertyTableModel::create(PropertyTableModel::Type::StyleProperties, computed_style_json).release_value_but_fixme_should_propagate_errors();
    m_computed_style_table->setModel(m_computed_style_model);

    m_resolved_style_model = PropertyTableModel::create(PropertyTableModel::Type::StyleProperties, resolved_style_json).release_value_but_fixme_should_propagate_errors();
    m_resolved_style_table->setModel(m_resolved_style_model);

    m_custom_properties_model = PropertyTableModel::create(PropertyTableModel::Type::StyleProperties, custom_properties_json).release_value_but_fixme_should_propagate_errors();
    m_custom_properties_table->setModel(m_custom_properties_model);
}

void InspectorWidget::clear_style_json()
{
    m_computed_style_table->setModel(nullptr);
    m_computed_style_model = nullptr;

    m_resolved_style_table->setModel(nullptr);
    m_resolved_style_model = nullptr;

    m_custom_properties_table->setModel(nullptr);
    m_custom_properties_model = nullptr;

    clear_selection();
}

void InspectorWidget::closeEvent(QCloseEvent* event)
{
    event->accept();
    if (on_close)
        on_close();
    clear_selection();
}

void InspectorWidget::clear_selection()
{
    m_selection = {};
    m_dom_tree_view->clearSelection();
}

void InspectorWidget::set_selection(Selection selection)
{
    if (!m_dom_loaded) {
        m_pending_selection = selection;
        return;
    }

    auto index = m_dom_model->index_for_node(selection.dom_node_id, selection.pseudo_element);
    if (!index.isValid()) {
        dbgln("Failed to set DOM inspector selection! Could not find valid model index for node: {}", selection.dom_node_id);
        return;
    }

    m_dom_tree_view->scrollTo(index);
    m_dom_tree_view->setCurrentIndex(index);
}

void InspectorWidget::select_default_node()
{
    clear_style_json();
    m_dom_tree_view->collapseAll();
    m_dom_tree_view->setCurrentIndex({});
}

void InspectorWidget::set_selection(QModelIndex const& index)
{
    if (!index.isValid())
        return;

    auto const* json = static_cast<JsonObject const*>(index.constInternalPointer());
    VERIFY(json);

    Selection selection {};
    if (json->has_u32("pseudo-element"sv)) {
        selection.dom_node_id = json->get_i32("parent-id"sv).value();
        selection.pseudo_element = static_cast<Web::CSS::Selector::PseudoElement>(json->get_u32("pseudo-element"sv).value());
    } else {
        selection.dom_node_id = json->get_i32("id"sv).value();
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
