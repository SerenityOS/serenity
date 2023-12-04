/*
 * Copyright (c) 2022, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "InspectorWidget.h"
#include <LibWebView/InspectorClient.h>
#include <QCloseEvent>
#include <QVBoxLayout>

namespace Ladybird {

extern bool is_using_dark_system_theme(QWidget&);

InspectorWidget::InspectorWidget(WebContentView& content_view)
{
    m_inspector_view = new WebContentView({}, {});

    if (is_using_dark_system_theme(*this))
        m_inspector_view->update_palette(WebContentView::PaletteMode::Dark);

    m_inspector_client = make<WebView::InspectorClient>(content_view, *m_inspector_view);

    setLayout(new QVBoxLayout);
    layout()->addWidget(m_inspector_view);

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
}

void InspectorWidget::select_hovered_node()
{
    m_inspector_client->select_hovered_node();
}

void InspectorWidget::select_default_node()
{
    m_inspector_client->select_default_node();
}

void InspectorWidget::closeEvent(QCloseEvent* event)
{
    event->accept();
    m_inspector_client->clear_selection();
}

}
