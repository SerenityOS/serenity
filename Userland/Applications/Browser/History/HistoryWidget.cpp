/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "HistoryWidget.h"
#include <Applications/Browser/HistoryWidgetGML.h>
#include <LibGUI/TableView.h>

namespace Browser {

ErrorOr<NonnullRefPtr<HistoryWidget>> HistoryWidget::try_create()
{
    auto main_widget = TRY(AK::adopt_nonnull_ref_or_enomem(new (nothrow) HistoryWidget()));

    TRY(main_widget->load_from_gml(history_widget_gml));

    main_widget->m_table_view = main_widget->find_descendant_of_type_named<GUI::TableView>("history_tableview");
    main_widget->m_textbox = main_widget->find_descendant_of_type_named<GUI::TextBox>("history_filter_textbox");

    main_widget->m_model = adopt_ref(*new HistoryModel());

    main_widget->m_filtering_model = TRY(GUI::FilteringProxyModel::create(*main_widget->m_model));
    main_widget->m_filtering_model->set_filter_term(""sv);

    main_widget->m_textbox->on_change = [main_widget] {
        main_widget->m_filtering_model->set_filter_term(main_widget->m_textbox->text());
        if (main_widget->m_filtering_model->row_count() != 0)
            main_widget->m_table_view->set_cursor(main_widget->m_filtering_model->index(0, 0), GUI::AbstractView::SelectionUpdate::Set);
    };

    main_widget->m_table_view->set_model(main_widget->m_filtering_model);
    main_widget->m_table_view->set_alternating_row_colors(true);

    return main_widget;
}

HistoryWidget::HistoryWidget()
{
}

void HistoryWidget::set_history_entries(Vector<History::URLTitlePair> entries)
{
    m_model->set_items(entries);
}

void HistoryWidget::clear_history_entries()
{
    m_model->clear_items();
}

}
