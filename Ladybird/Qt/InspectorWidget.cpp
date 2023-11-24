/*
 * Copyright (c) 2022, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "InspectorWidget.h"
#include <AK/JsonObject.h>
#include <LibWebView/InspectorClient.h>
#include <QCloseEvent>
#include <QHeaderView>
#include <QSplitter>
#include <QTabWidget>
#include <QTableView>
#include <QVBoxLayout>

namespace Ladybird {

extern bool is_using_dark_system_theme(QWidget&);

InspectorWidget::InspectorWidget(WebContentView& content_view)
{
    m_inspector_view = make<WebContentView>(StringView {}, WebView::EnableCallgrindProfiling::No, UseLagomNetworking::No, WebView::EnableGPUPainting::No);

    if (is_using_dark_system_theme(*this))
        m_inspector_view->update_palette(WebContentView::PaletteMode::Dark);

    m_inspector_client = make<WebView::InspectorClient>(content_view, *m_inspector_view);

    m_inspector_client->on_dom_node_properties_received = [this](auto properties_or_error) {
        if (properties_or_error.is_error()) {
            clear_style_json();
        } else {
            auto properties = properties_or_error.release_value();
            load_style_json(properties.computed_style_json, properties.resolved_style_json, properties.custom_properties_json);
        }
    };

    setLayout(new QVBoxLayout);

    auto* splitter = new QSplitter(this);
    splitter->setOrientation(Qt::Vertical);
    layout()->addWidget(splitter);

    splitter->addWidget(m_inspector_view.ptr());
    splitter->setStretchFactor(0, 2);

    auto add_table_tab = [&](auto* tab_widget, auto name) {
        auto* table_view = new QTableView;
        table_view->horizontalHeader()->setSectionResizeMode(QHeaderView::Stretch);
        table_view->verticalHeader()->setVisible(false);
        table_view->horizontalHeader()->setVisible(false);

        auto* container = new QWidget;
        container->setLayout(new QVBoxLayout);
        container->layout()->addWidget(table_view);
        tab_widget->addTab(container, name);

        return table_view;
    };

    auto* node_tabs = new QTabWidget;
    m_computed_style_table = add_table_tab(node_tabs, "Computed");
    m_resolved_style_table = add_table_tab(node_tabs, "Resolved");
    m_custom_properties_table = add_table_tab(node_tabs, "Variables");
    splitter->addWidget(node_tabs);

    setWindowTitle("Inspector");
    resize(875, 825);
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
}

void InspectorWidget::select_hovered_node()
{
    m_inspector_client->select_hovered_node();
}

void InspectorWidget::select_default_node()
{
    m_inspector_client->select_default_node();
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
}

void InspectorWidget::closeEvent(QCloseEvent* event)
{
    event->accept();
    m_inspector_client->clear_selection();
}

}
