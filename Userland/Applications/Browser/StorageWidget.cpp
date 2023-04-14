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

ErrorOr<NonnullRefPtr<StorageWidget>> StorageWidget::try_create()
{
    auto main_widget = TRY(AK::adopt_nonnull_ref_or_enomem(new (nothrow) StorageWidget()));

    TRY(main_widget->load_from_gml(storage_widget_gml));
    auto& tab_widget = *main_widget->find_descendant_of_type_named<GUI::TabWidget>("tab_widget");

    main_widget->m_cookies_table_view = tab_widget.find_descendant_of_type_named<GUI::TableView>("cookies_tableview");
    main_widget->m_cookies_textbox = tab_widget.find_descendant_of_type_named<GUI::TextBox>("cookies_filter_textbox");
    main_widget->m_cookies_model = adopt_ref(*new CookiesModel());

    main_widget->m_cookies_filtering_model = MUST(GUI::FilteringProxyModel::create(*main_widget->m_cookies_model));
    main_widget->m_cookies_filtering_model->set_filter_term(""sv);

    main_widget->m_cookies_textbox->on_change = [main_widget] {
        main_widget->m_cookies_filtering_model->set_filter_term(main_widget->m_cookies_textbox->text());
        if (main_widget->m_cookies_filtering_model->row_count() != 0)
            main_widget->m_cookies_table_view->set_cursor(main_widget->m_cookies_filtering_model->index(0, 0), GUI::AbstractView::SelectionUpdate::Set);
    };

    main_widget->m_cookies_table_view->set_model(main_widget->m_cookies_filtering_model);
    main_widget->m_cookies_table_view->set_column_headers_visible(true);
    main_widget->m_cookies_table_view->set_alternating_row_colors(true);

    auto delete_cookie_action = TRY(GUI::Action::try_create(
        "&Delete Cookie", GUI::Shortcut { Key_Delete }, Gfx::Bitmap::load_from_file("/res/icons/16x16/delete.png"sv).release_value_but_fixme_should_propagate_errors(), [main_widget](auto const&) {
            auto cookie_index = main_widget->m_cookies_table_view->selection().first();
            main_widget->delete_cookie(main_widget->m_cookies_model->take_cookie(cookie_index));
        },
        main_widget->m_cookies_table_view));

    auto delete_all_cookies_action = TRY(GUI::Action::try_create(
        "Delete &All Cookies", [main_widget](auto const&) {
            auto cookies = main_widget->m_cookies_model->take_all_cookies();

            for (auto& cookie : cookies)
                main_widget->delete_cookie(move(cookie));
        },
        main_widget->m_cookies_table_view));

    main_widget->m_cookies_context_menu = TRY(GUI::Menu::try_create());
    TRY(main_widget->m_cookies_context_menu->try_add_action(delete_cookie_action));
    TRY(main_widget->m_cookies_context_menu->try_add_action(delete_all_cookies_action));
    main_widget->m_cookies_table_view->on_context_menu_request = [main_widget](auto& index, auto& event) {
        if (index.is_valid())
            main_widget->m_cookies_context_menu->popup(event.screen_position());
    };

    main_widget->m_local_storage_table_view = tab_widget.find_descendant_of_type_named<GUI::TableView>("local_storage_tableview");
    main_widget->m_local_storage_textbox = tab_widget.find_descendant_of_type_named<GUI::TextBox>("local_storage_filter_textbox");
    main_widget->m_local_storage_model = adopt_ref(*new StorageModel());

    main_widget->m_local_storage_filtering_model = MUST(GUI::FilteringProxyModel::create(*main_widget->m_local_storage_model));
    main_widget->m_local_storage_filtering_model->set_filter_term(""sv);

    main_widget->m_local_storage_textbox->on_change = [main_widget] {
        main_widget->m_local_storage_filtering_model->set_filter_term(main_widget->m_local_storage_textbox->text());
        if (main_widget->m_local_storage_filtering_model->row_count() != 0)
            main_widget->m_local_storage_table_view->set_cursor(main_widget->m_local_storage_filtering_model->index(0, 0), GUI::AbstractView::SelectionUpdate::Set);
    };

    main_widget->m_local_storage_table_view->set_model(main_widget->m_local_storage_filtering_model);
    main_widget->m_local_storage_table_view->set_column_headers_visible(true);
    main_widget->m_local_storage_table_view->set_alternating_row_colors(true);

    main_widget->m_session_storage_table_view = tab_widget.find_descendant_of_type_named<GUI::TableView>("session_storage_tableview");
    main_widget->m_session_storage_textbox = tab_widget.find_descendant_of_type_named<GUI::TextBox>("session_storage_filter_textbox");
    main_widget->m_session_storage_model = adopt_ref(*new StorageModel());

    main_widget->m_session_storage_filtering_model = MUST(GUI::FilteringProxyModel::create(*main_widget->m_session_storage_model));
    main_widget->m_session_storage_filtering_model->set_filter_term(""sv);

    main_widget->m_session_storage_textbox->on_change = [main_widget] {
        main_widget->m_session_storage_filtering_model->set_filter_term(main_widget->m_session_storage_textbox->text());
        if (main_widget->m_session_storage_filtering_model->row_count() != 0)
            main_widget->m_session_storage_table_view->set_cursor(main_widget->m_session_storage_filtering_model->index(0, 0), GUI::AbstractView::SelectionUpdate::Set);
    };

    main_widget->m_session_storage_table_view->set_model(main_widget->m_session_storage_filtering_model);
    main_widget->m_session_storage_table_view->set_column_headers_visible(true);
    main_widget->m_session_storage_table_view->set_alternating_row_colors(true);

    return main_widget;
}

StorageWidget::StorageWidget()
{
}

void StorageWidget::set_cookies_entries(Vector<Web::Cookie::Cookie> entries)
{
    m_cookies_model->set_items(entries);
}

void StorageWidget::clear_cookies()
{
    m_cookies_model->clear_items();
}

void StorageWidget::set_local_storage_entries(OrderedHashMap<DeprecatedString, DeprecatedString> entries)
{
    m_local_storage_model->set_items(entries);
}

void StorageWidget::clear_local_storage_entries()
{
    m_local_storage_model->clear_items();
}

void StorageWidget::set_session_storage_entries(OrderedHashMap<DeprecatedString, DeprecatedString> entries)
{
    m_session_storage_model->set_items(entries);
}

void StorageWidget::clear_session_storage_entries()
{
    m_session_storage_model->clear_items();
}

void StorageWidget::delete_cookie(Web::Cookie::Cookie cookie)
{
    // Delete cookie by making its expiry time in the past.
    cookie.expiry_time = Time::from_seconds(0);
    if (on_update_cookie)
        on_update_cookie(move(cookie));
}

}
