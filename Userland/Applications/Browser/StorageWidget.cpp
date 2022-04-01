/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "StorageWidget.h"
#include "CookiesModel.h"
#include "LocalStorageModel.h"
#include <AK/Variant.h>
#include <Applications/Browser/CookiesTabGML.h>
#include <Applications/Browser/StorageWidgetGML.h>
#include <LibGUI/TabWidget.h>
#include <LibGUI/TableView.h>
#include <LibWeb/Cookie/Cookie.h>

namespace Browser {

StorageWidget::StorageWidget()
{
    load_from_gml(storage_widget_gml);
    auto& tab_widget = *find_descendant_of_type_named<GUI::TabWidget>("tab_widget");

    auto cookies_tab = tab_widget.try_add_tab<GUI::Widget>("Cookies").release_value_but_fixme_should_propagate_errors();
    cookies_tab->load_from_gml(cookies_tab_gml);

    m_cookies_table_view = cookies_tab->find_descendant_of_type_named<GUI::TableView>("cookies_tableview");
    m_cookies_model = adopt_ref(*new CookiesModel());

    m_cookie_sorting_model = MUST(GUI::SortingProxyModel::create(*m_cookies_model));
    m_cookie_sorting_model->set_sort_role(GUI::ModelRole::Display);

    m_cookies_table_view->set_model(m_cookie_sorting_model);
    m_cookies_table_view->set_column_headers_visible(true);
    m_cookies_table_view->set_alternating_row_colors(true);

    auto local_storage_tab = tab_widget.try_add_tab<GUI::Widget>("Local Storage").release_value_but_fixme_should_propagate_errors();
    local_storage_tab->load_from_gml(cookies_tab_gml);

    m_local_storage_table_view = local_storage_tab->find_descendant_of_type_named<GUI::TableView>("cookies_tableview");
    m_local_storage_model = adopt_ref(*new LocalStorageModel());

    m_local_storage_sorting_model = MUST(GUI::SortingProxyModel::create(*m_local_storage_model));
    m_local_storage_sorting_model->set_sort_role(GUI::ModelRole::Display);

    m_local_storage_table_view->set_model(m_local_storage_sorting_model);
    m_local_storage_table_view->set_column_headers_visible(true);
    m_local_storage_table_view->set_alternating_row_colors(true);
}

void StorageWidget::add_cookie(Web::Cookie::Cookie const& cookie)
{
    m_cookies_model->add_item(cookie);
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
