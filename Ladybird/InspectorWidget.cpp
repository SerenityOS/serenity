/*
 * Copyright (c) 2022, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWebView/AccessibilityTreeModel.h>
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

    auto add_tab = [&](auto* tab_widget, auto* widget, auto name) {
        auto container = new QWidget;
        container->setLayout(new QVBoxLayout);
        container->layout()->addWidget(widget);
        tab_widget->addTab(container, name);
    };

    auto top_tap_widget = new QTabWidget;
    splitter->addWidget(top_tap_widget);

    auto dom_tree_view = new QTreeView;
    dom_tree_view->setHeaderHidden(true);
    dom_tree_view->setModel(&m_dom_model);
    QObject::connect(dom_tree_view->selectionModel(), &QItemSelectionModel::selectionChanged,
        [this](QItemSelection const& selected, QItemSelection const&) {
            auto indexes = selected.indexes();
            if (indexes.size()) {
                auto index = m_dom_model.to_gui(indexes.first());
                set_selection(index);
            }
        });
    add_tab(top_tap_widget, dom_tree_view, "DOM");

    auto accessibility_tree_view = new QTreeView;
    accessibility_tree_view->setHeaderHidden(true);
    accessibility_tree_view->setModel(&m_accessibility_model);
    add_tab(top_tap_widget, accessibility_tree_view, "Accessibility");

    auto add_table_tab = [&](auto* tab_widget, auto& model, auto name) {
        auto table_view = new QTableView;
        table_view->setModel(&model);
        table_view->horizontalHeader()->setSectionResizeMode(QHeaderView::Stretch);
        table_view->verticalHeader()->setVisible(false);
        table_view->horizontalHeader()->setVisible(false);
        add_tab(tab_widget, table_view, name);
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

void InspectorWidget::set_accessibility_json(StringView accessibility_json)
{
    m_accessibility_model.set_underlying_model(WebView::AccessibilityTreeModel::create(accessibility_json));
}

void InspectorWidget::clear_dom_json()
{
    m_dom_model.set_underlying_model(nullptr);
    // The accessibility tree is pretty much another form of the DOM tree, so should be cleared at the time time.
    m_accessibility_model.set_underlying_model(nullptr);
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
