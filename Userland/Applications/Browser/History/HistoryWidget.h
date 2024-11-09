/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "HistoryModel.h"
#include <LibGUI/FilteringProxyModel.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/Widget.h>
#include <LibGUI/TableView.h>

namespace Browser::History {

class HistoryWidget final : public GUI::Widget {
    C_OBJECT_ABSTRACT(HistoryWidget);

public:
    virtual ~HistoryWidget() override = default;

    void set_history_entries(Vector<URLTitlePair> entries);
    void clear_history_entries();

    static ErrorOr<NonnullRefPtr<HistoryWidget>> create();

protected:
    static ErrorOr<NonnullRefPtr<HistoryWidget>> try_create();

private:
    ErrorOr<void> setup();
    HistoryWidget() = default;

    RefPtr<GUI::TableView> m_table_view;
    RefPtr<GUI::TextBox> m_textbox;
    RefPtr<HistoryModel> m_model;
    RefPtr<GUI::FilteringProxyModel> m_filtering_model;
};

}
