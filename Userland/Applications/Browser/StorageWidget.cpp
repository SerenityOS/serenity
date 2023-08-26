/*
 * Copyright (c) 2022, the SerenityOS developers.
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "StorageWidget.h"
#include "CookiesModel.h"
#include "StorageModel.h"
#include <Applications/Browser/StorageWidgetGML.h>
#include <LibGUI/Menu.h>
#include <LibGUI/TabWidget.h>
#include <LibGUI/TableView.h>
#include <LibWeb/Cookie/Cookie.h>

namespace Browser {

StorageWidget::StorageWidget()
{
    load_from_gml(storage_widget_gml).release_value_but_fixme_should_propagate_errors();
    auto& tab_widget = *find_descendant_of_type_named<GUI::TabWidget>("tab_widget");

    m_cookies_table_view = tab_widget.find_descendant_of_type_named<GUI::TableView>("cookies_tableview");
    m_cookies_textbox = tab_widget.find_descendant_of_type_named<GUI::TextBox>("cookies_filter_textbox");
    m_cookies_model = adopt_ref(*new CookiesModel());

    m_cookies_filtering_model = MUST(GUI::FilteringProxyModel::create(*m_cookies_model));
    m_cookies_filtering_model->set_filter_term(""sv);

    m_cookies_textbox->on_change = [this] {
        m_cookies_filtering_model->set_filter_term(m_cookies_textbox->text());
        if (m_cookies_filtering_model->row_count() != 0)
            m_cookies_table_view->set_cursor(m_cookies_filtering_model->index(0, 0), GUI::AbstractView::SelectionUpdate::Set);
    };

    m_cookies_table_view->set_model(m_cookies_filtering_model);
    m_cookies_table_view->set_column_headers_visible(true);
    m_cookies_table_view->set_alternating_row_colors(true);

    auto delete_cookie_action = GUI::Action::create(
        "&Delete Cookie", { Key_Delete }, Gfx::Bitmap::load_from_file("/res/icons/16x16/delete.png"sv).release_value_but_fixme_should_propagate_errors(), [&](auto const&) {
            auto cookie_index = m_cookies_table_view->selection().first();
            delete_cookie(m_cookies_model->take_cookie(cookie_index));
        },
        m_cookies_table_view);

    auto delete_all_cookies_action = GUI::Action::create(
        "Delete &All Cookies", [&](auto const&) {
            auto cookies = m_cookies_model->take_all_cookies();

            for (auto& cookie : cookies)
                delete_cookie(move(cookie));
        },
        m_cookies_table_view);

    m_cookies_context_menu = GUI::Menu::construct();
    m_cookies_context_menu->add_action(delete_cookie_action);
    m_cookies_context_menu->add_action(delete_all_cookies_action);
    m_cookies_table_view->on_context_menu_request = [&](auto& index, auto& event) {
        if (index.is_valid())
            m_cookies_context_menu->popup(event.screen_position());
    };

    m_local_storage_table_view = tab_widget.find_descendant_of_type_named<GUI::TableView>("local_storage_tableview");
    m_local_storage_textbox = tab_widget.find_descendant_of_type_named<GUI::TextBox>("local_storage_filter_textbox");
    m_local_storage_model = adopt_ref(*new StorageModel());

    m_local_storage_filtering_model = MUST(GUI::FilteringProxyModel::create(*m_local_storage_model));
    m_local_storage_filtering_model->set_filter_term(""sv);

    m_local_storage_textbox->on_change = [this] {
        m_local_storage_filtering_model->set_filter_term(m_local_storage_textbox->text());
        if (m_local_storage_filtering_model->row_count() != 0)
            m_local_storage_table_view->set_cursor(m_local_storage_filtering_model->index(0, 0), GUI::AbstractView::SelectionUpdate::Set);
    };

    m_local_storage_table_view->set_model(m_local_storage_filtering_model);
    m_local_storage_table_view->set_column_headers_visible(true);
    m_local_storage_table_view->set_alternating_row_colors(true);

    m_session_storage_table_view = tab_widget.find_descendant_of_type_named<GUI::TableView>("session_storage_tableview");
    m_session_storage_textbox = tab_widget.find_descendant_of_type_named<GUI::TextBox>("session_storage_filter_textbox");
    m_session_storage_model = adopt_ref(*new StorageModel());

    m_session_storage_filtering_model = MUST(GUI::FilteringProxyModel::create(*m_session_storage_model));
    m_session_storage_filtering_model->set_filter_term(""sv);

    m_session_storage_textbox->on_change = [this] {
        m_session_storage_filtering_model->set_filter_term(m_session_storage_textbox->text());
        if (m_session_storage_filtering_model->row_count() != 0)
            m_session_storage_table_view->set_cursor(m_session_storage_filtering_model->index(0, 0), GUI::AbstractView::SelectionUpdate::Set);
    };

    m_session_storage_table_view->set_model(m_session_storage_filtering_model);
    m_session_storage_table_view->set_column_headers_visible(true);
    m_session_storage_table_view->set_alternating_row_colors(true);
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
    m_local_storage_model->set_items(move(entries));
}

void StorageWidget::clear_local_storage_entries()
{
    m_local_storage_model->clear_items();
}

void StorageWidget::set_session_storage_entries(OrderedHashMap<String, String> entries)
{
    m_session_storage_model->set_items(move(entries));
}

void StorageWidget::clear_session_storage_entries()
{
    m_session_storage_model->clear_items();
}

void StorageWidget::delete_cookie(Web::Cookie::Cookie cookie)
{
    // Delete cookie by making its expiry time in the past.
    cookie.expiry_time = UnixDateTime::earliest();
    if (on_update_cookie)
        on_update_cookie(move(cookie));
}

}
