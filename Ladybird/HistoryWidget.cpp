/*
 * Copyright (c) 2023, Tobias Soppa <tobias@soppa.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "HistoryWidget.h"
#include <Browser/History/HistoryModel.h>
#include <QHeaderView>
#include <QTableView>

namespace Ladybird {

HistoryWidget::HistoryWidget()
{
    auto* table_view = new QTableView;
    table_view->setModel(&m_history_model);
    table_view->horizontalHeader()->setSectionResizeMode(QHeaderView::Stretch);
    table_view->verticalHeader()->setVisible(false);
    table_view->horizontalHeader()->setVisible(false);
    m_table_view = table_view;
}

void HistoryWidget::set_history_entries(Vector<Browser::History::URLTitlePair> entries)
{
    auto& browser_history_model = *new Browser::HistoryModel;
    browser_history_model.set_items(entries);
    m_history_model.set_underlying_model(browser_history_model);
}

void HistoryWidget::show()
{
    if (!m_table_view)
        return;
    m_table_view->show();
}

}
