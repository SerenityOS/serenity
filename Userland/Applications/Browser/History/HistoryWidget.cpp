/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "HistoryWidget.h"

namespace Browser::History {

ErrorOr<NonnullRefPtr<HistoryWidget>> HistoryWidget::create()
{
    auto widget = TRY(try_create());
    TRY(widget->setup());
    return widget;
}

ErrorOr<void> HistoryWidget::setup()
{
    m_table_view = find_descendant_of_type_named<GUI::TableView>("history_tableview");
    m_textbox = find_descendant_of_type_named<GUI::TextBox>("history_filter_textbox");

    m_model = adopt_ref(*new HistoryModel());

    m_filtering_model = MUST(GUI::FilteringProxyModel::create(*m_model));
    m_filtering_model->set_filter_term(""sv);

    m_textbox->on_change = [this] {
        m_filtering_model->set_filter_term(m_textbox->text());
        if (m_filtering_model->row_count() != 0)
            m_table_view->set_cursor(m_filtering_model->index(0, 0), GUI::AbstractView::SelectionUpdate::Set);
    };

    m_table_view->set_model(m_filtering_model);
    m_table_view->set_alternating_row_colors(true);

    return {};
}

void HistoryWidget::set_history_entries(Vector<URLTitlePair> entries)
{
    m_model->set_items(move(entries));
}

void HistoryWidget::clear_history_entries()
{
    m_model->clear_items();
}

}
