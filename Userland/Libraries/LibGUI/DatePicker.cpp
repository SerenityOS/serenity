/*
 * Copyright (c) 2023, David Ganz <david.g.ganz@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibDateTime/ISOCalendar.h>
#include <LibGUI/Button.h>
#include <LibGUI/ComboBox.h>
#include <LibGUI/DatePicker.h>
#include <LibGUI/DatePickerDialogGML.h>
#include <LibGUI/ImageWidget.h>
#include <LibGUI/Label.h>
#include <LibGUI/SpinBox.h>
#include <LibGUI/TextBox.h>

namespace GUI {

DatePicker::DatePicker(Window* parent_window, String const& title, DateTime::LocalDateTime focused_date)
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
        auto selected_date = m_selected_date.to_parts<DateTime::ISOCalendar>();
        m_month_box->set_selected_index(selected_date.month - 1, AllowCallback::No);
        m_year_box->set_value(static_cast<int>(selected_date.year), AllowCallback::No);
    };
    calendar.on_tile_doubleclick = [&]() {
        m_selected_date = calendar.selected_date();
        done(ExecResult::OK);
    };
    auto focused_date_parts = focused_date.to_parts<DateTime::ISOCalendar>();
    calendar.set_selected_date(focused_date);
    calendar.update_tiles(focused_date_parts.year, focused_date_parts.month);

    m_month_box = widget->find_descendant_of_type_named<GUI::ComboBox>("month_box");
    m_month_box->set_model(GUI::MonthListModel::create(GUI::MonthListModel::DisplayMode::Long));
    m_month_box->set_selected_index(focused_date_parts.month - 1, AllowCallback::No);
    m_month_box->on_change = [&](ByteString const&, ModelIndex const& index) {
        auto selected_date_parts = m_selected_date.to_parts<DateTime::ISOCalendar>();
        selected_date_parts.month = index.row() + 1;
        auto new_selection = DateTime::LocalDateTime::from_parts<DateTime::ISOCalendar>(DateTime::ISOCalendar::InputParts(selected_date_parts));
        if (new_selection.is_error())
            return;
        m_selected_date = new_selection.release_value();
        calendar.set_selected_date(m_selected_date);
        calendar.update_tiles(selected_date_parts.year, selected_date_parts.month);
        calendar.update();
    };

    m_year_box = widget->find_descendant_of_type_named<GUI::SpinBox>("year_box");
    m_year_box->set_value(static_cast<int>(focused_date_parts.year), AllowCallback::No);
    m_year_box->on_change = [&](int year) {
        auto selected_date_parts = m_selected_date.to_parts<DateTime::ISOCalendar>();
        selected_date_parts.year = year;
        auto new_selection = DateTime::LocalDateTime::from_parts<DateTime::ISOCalendar>(DateTime::ISOCalendar::InputParts(selected_date_parts));
        if (new_selection.is_error())
            return;
        calendar.set_selected_date(m_selected_date);
        calendar.update_tiles(selected_date_parts.year, selected_date_parts.month);
        calendar.update();
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

Optional<DateTime::LocalDateTime> DatePicker::show(Window* parent_window, String title, DateTime::LocalDateTime focused_date)
{
    auto box = DatePicker::construct(parent_window, title, focused_date);
    if (box->exec() == ExecResult::OK) {
        return box->m_selected_date;
    }

    return {};
}

}
