/*
 * Copyright (c) 2023, Tobias Soppa <tobias@soppa.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "ModelTranslator.h"
#include <Browser/History.h>
#include <QTableView>
#include <QWidget>

namespace Ladybird {

class HistoryWidget final : public QWidget {
    Q_OBJECT
public:
    HistoryWidget();
    virtual ~HistoryWidget() override = default;

    void set_history_entries(Vector<Browser::History::URLTitlePair> entries);
    void show();

private:
    ModelTranslator m_history_model {};
    QTableView* m_table_view { nullptr };
};

}
