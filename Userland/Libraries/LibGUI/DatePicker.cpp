/*
 * Copyright (c) 2023, David Ganz <david.g.ganz@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/Button.h>
#include <LibGUI/ComboBox.h>
#include <LibGUI/DatePicker.h>
#include <LibGUI/DatePickerDialogGML.h>
#include <LibGUI/SpinBox.h>
#include <LibGUI/TextBox.h>

namespace GUI {

DatePicker::DatePicker(Window* parent_window, String const& title, Core::DateTime focused_date)
    : Dialog(parent_window)
    , m_selected_date(focused_date)
{
    if (parent_window)
        set_icon(parent_window->icon());

    set_resizable(false);
    set_title(title.to_byte_string());

    auto widget = set_main_widget<Widget>();
    widget->load_from_gml(date_picker_dialog_gml).release_value_but_fixme_should_propagate_errors();

    auto& calendar = *widget->find_descendant_of_type_named<GUI::Calendar>("calendar_view");
    calendar.on_tile_click = [&]() {
        m_selected_date = calendar.selected_date();
        m_month_box->set_selected_index(m_selected_date.month() - 1, AllowCallback::No);
        m_year_box->set_value(static_cast<int>(m_selected_date.year()), AllowCallback::No);
    };
    calendar.on_tile_doubleclick = [&]() {
        m_selected_date = calendar.selected_date();
        done(ExecResult::OK);
    };
    calendar.set_selected_date(focused_date);
    calendar.update_tiles(focused_date.year(), focused_date.month());

    m_month_box = widget->find_descendant_of_type_named<GUI::ComboBox>("month_box");
    m_month_box->set_model(GUI::MonthListModel::create(GUI::MonthListModel::DisplayMode::Long));
    m_month_box->set_selected_index(focused_date.month() - 1, AllowCallback::No);
    m_month_box->on_change = [&](ByteString const&, ModelIndex const& index) {
        m_selected_date.set_time(static_cast<int>(m_selected_date.year()), index.row() + 1);
        calendar.set_selected_date(m_selected_date);
        calendar.update_tiles(m_selected_date.year(), m_selected_date.month());
    };

    m_year_box = widget->find_descendant_of_type_named<GUI::SpinBox>("year_box");
    m_year_box->set_value(static_cast<int>(focused_date.year()), AllowCallback::No);
    m_year_box->on_change = [&](int year) {
        m_selected_date.set_time(year, static_cast<int>(m_selected_date.month()));
        calendar.set_selected_date(m_selected_date);
        calendar.update_tiles(m_selected_date.year(), m_selected_date.month());
    };

    auto& ok_button = *widget->find_descendant_of_type_named<GUI::Button>("ok_button");
    ok_button.on_click = [&](auto) {
        dbgln("GUI::DatePicker: OK button clicked");
        m_selected_date = calendar.selected_date();
        done(ExecResult::OK);
    };

    auto& cancel_button = *widget->find_descendant_of_type_named<GUI::Button>("cancel_button");
    cancel_button.on_click = [this](auto) {
        dbgln("GUI::DatePicker: Cancel button clicked");
        done(ExecResult::Cancel);
    };
}

Optional<Core::DateTime> DatePicker::show(Window* parent_window, String title, Core::DateTime focused_date)
{
    auto box = DatePicker::construct(parent_window, title, focused_date);
    if (box->exec() == ExecResult::OK) {
        return box->m_selected_date;
    }

    return {};
}

}
