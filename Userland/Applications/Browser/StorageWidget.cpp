/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "StorageWidget.h"
#include "CookiesModel.h"
#include "LocalStorageModel.h"
#include <AK/Variant.h>
#include <Applications/Browser/StorageWidgetGML.h>
#include <LibGUI/TabWidget.h>
#include <LibGUI/TableView.h>
#include <LibWeb/Cookie/Cookie.h>

namespace Browser {

StorageWidget::StorageWidget()
{
    load_from_gml(storage_widget_gml);
    auto& tab_widget = *find_descendant_of_type_named<GUI::TabWidget>("tab_widget");

    m_cookies_table_view = tab_widget.find_descendant_of_type_named<GUI::TableView>("cookies_tableview");
    m_cookies_textbox = tab_widget.find_descendant_of_type_named<GUI::TextBox>("cookies_filter_textbox");
    m_cookies_model = adopt_ref(*new CookiesModel());

    m_cookies_filtering_model = MUST(GUI::FilteringProxyModel::create(*m_cookies_model));
    m_cookies_filtering_model->set_filter_term("");

    m_cookies_textbox->on_change = [this] {
        m_cookies_filtering_model->set_filter_term(m_cookies_textbox->text());
        if (m_cookies_filtering_model->row_count() != 0)
            m_cookies_table_view->set_cursor(m_cookies_filtering_model->index(0, 0), GUI::AbstractView::SelectionUpdate::Set);
    };

    m_cookies_table_view->set_model(m_cookies_filtering_model);
    m_cookies_table_view->set_column_headers_visible(true);
    m_cookies_table_view->set_alternating_row_colors(true);

    m_local_storage_table_view = tab_widget.find_descendant_of_type_named<GUI::TableView>("local_storage_tableview");
    m_local_storage_textbox = tab_widget.find_descendant_of_type_named<GUI::TextBox>("local_storage_filter_textbox");
    m_local_storage_model = adopt_ref(*new LocalStorageModel());

    m_local_storage_filtering_model = MUST(GUI::FilteringProxyModel::create(*m_local_storage_model));
    m_local_storage_filtering_model->set_filter_term("");

    m_local_storage_textbox->on_change = [this] {
        m_local_storage_filtering_model->set_filter_term(m_local_storage_textbox->text());
        if (m_local_storage_filtering_model->row_count() != 0)
            m_local_storage_table_view->set_cursor(m_local_storage_filtering_model->index(0, 0), GUI::AbstractView::SelectionUpdate::Set);
    };

    m_local_storage_table_view->set_model(m_local_storage_filtering_model);
    m_local_storage_table_view->set_column_headers_visible(true);
    m_local_storage_table_view->set_alternating_row_colors(true);
}

void StorageWidget::set_cookies_entries(Vector<Web::Cookie::Cookie> entries)
{
    m_cookies_model->set_items(entries);
}

void StorageWidget::clear_cookies()
{
    m_cookies_model->clear_items();
}

void StorageWidget::set_local_storage_entries(OrderedHashMap<String, String> entries)
{
    m_local_storage_model->set_items(entries);
}

void StorageWidget::clear_local_storage_entries()
{
    m_local_storage_model->clear_items();
}

}
