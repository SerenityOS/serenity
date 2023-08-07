/*
 * Copyright (c) 2019-2020, Ryan Grieb <ryan.m.grieb@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "AddEventDialog.h"
#include <LibCore/DateTime.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/ComboBox.h>
#include <LibGUI/Label.h>
#include <LibGUI/Painter.h>
#include <LibGUI/SpinBox.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>
#include <LibGfx/Color.h>
#include <LibGfx/Font/FontDatabase.h>

AddEventDialog::AddEventDialog(Core::DateTime date_time, Window* parent_window)
    : Dialog(parent_window)
    , m_date_time(date_time)
{
    resize(158, 130);
    set_title("Add Event");
    set_resizable(false);
    set_icon(parent_window->icon());

    auto widget = set_main_widget<GUI::Widget>().release_value_but_fixme_should_propagate_errors();
    widget->set_fill_with_background_color(true);
    widget->set_layout<GUI::VerticalBoxLayout>();

    auto& top_container = widget->add<GUI::Widget>();
    top_container.set_layout<GUI::VerticalBoxLayout>(4);
    top_container.set_fixed_height(45);

    auto& add_label = top_container.add<GUI::Label>("Add title & date:"_string);
    add_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
    add_label.set_fixed_height(14);
    add_label.set_font(Gfx::FontDatabase::default_font().bold_variant());

    auto& event_title_textbox = top_container.add<GUI::TextBox>();
    event_title_textbox.set_fixed_height(20);

    auto& middle_container = widget->add<GUI::Widget>();
    middle_container.set_layout<GUI::HorizontalBoxLayout>(4);
    middle_container.set_fixed_height(25);

    auto& time_container = widget->add<GUI::Widget>();
    time_container.set_layout<GUI::HorizontalBoxLayout>(4);
    time_container.set_fixed_height(25);

    auto& starting_month_combo = middle_container.add<GUI::ComboBox>();
    starting_month_combo.set_only_allow_values_from_model(true);
    starting_month_combo.set_fixed_size(50, 20);
    starting_month_combo.set_model(MonthListModel::create());
    starting_month_combo.set_selected_index(m_date_time.month() - 1);

    auto& starting_day_combo = middle_container.add<GUI::SpinBox>();
    starting_day_combo.set_fixed_size(40, 20);
    starting_day_combo.set_range(1, m_date_time.days_in_month());
    starting_day_combo.set_value(m_date_time.day());

    auto& starting_year_combo = middle_container.add<GUI::SpinBox>();
    starting_year_combo.set_fixed_size(55, 20);
    starting_year_combo.set_range(0, 9999);
    starting_year_combo.set_value(m_date_time.year());

    auto& starting_hour_combo = time_container.add<GUI::SpinBox>();
    starting_hour_combo.set_fixed_size(50, 20);
    starting_hour_combo.set_range(1, 12);
    starting_hour_combo.set_value(12);

    auto& starting_minute_combo = time_container.add<GUI::SpinBox>();
    starting_minute_combo.set_fixed_size(40, 20);
    starting_minute_combo.set_range(0, 59);
    starting_minute_combo.set_value(0);

    auto& starting_meridiem_combo = time_container.add<GUI::ComboBox>();
    starting_meridiem_combo.set_only_allow_values_from_model(true);
    starting_meridiem_combo.set_fixed_size(55, 20);
    starting_meridiem_combo.set_model(MeridiemListModel::create());
    starting_meridiem_combo.set_selected_index(0);

    widget->add_spacer().release_value_but_fixme_should_propagate_errors();

    auto& button_container = widget->add<GUI::Widget>();
    button_container.set_fixed_height(20);
    button_container.set_layout<GUI::HorizontalBoxLayout>();
    button_container.add_spacer().release_value_but_fixme_should_propagate_errors();
    auto& ok_button = button_container.add<GUI::Button>("OK"_short_string);
    ok_button.set_fixed_size(80, 20);
    ok_button.on_click = [this](auto) {
        dbgln("TODO: Add event icon on specific tile");
        done(ExecResult::OK);
    };

    auto update_starting_day_range = [&starting_day_combo, &starting_year_combo, &starting_month_combo]() {
        auto year = starting_year_combo.value();
        auto month = starting_month_combo.selected_index();

        starting_day_combo.set_range(1, days_in_month(year, month + 1));
    };

    starting_year_combo.on_change = [update_starting_day_range](auto) { update_starting_day_range(); };
    starting_month_combo.on_change = [update_starting_day_range](auto, auto) { update_starting_day_range(); };

    event_title_textbox.set_focus(true);
}

int AddEventDialog::MonthListModel::row_count(const GUI::ModelIndex&) const
{
    return 12;
}

int AddEventDialog::MeridiemListModel::row_count(const GUI::ModelIndex&) const
{
    return 2;
}

ErrorOr<String> AddEventDialog::MonthListModel::column_name(int column) const
{
    switch (column) {
    case Column::Month:
        return "Month"_short_string;
    default:
        VERIFY_NOT_REACHED();
    }
}

ErrorOr<String> AddEventDialog::MeridiemListModel::column_name(int column) const
{
    switch (column) {
    case Column::Meridiem:
        return "Meridiem"_string;
    default:
        VERIFY_NOT_REACHED();
    }
}

GUI::Variant AddEventDialog::MonthListModel::data(const GUI::ModelIndex& index, GUI::ModelRole role) const
{
    constexpr Array short_month_names = {
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    };

    auto& month = short_month_names[index.row()];
    if (role == GUI::ModelRole::Display) {
        switch (index.column()) {
        case Column::Month:
            return month;
        default:
            VERIFY_NOT_REACHED();
        }
    }
    return {};
}

GUI::Variant AddEventDialog::MeridiemListModel::data(const GUI::ModelIndex& index, GUI::ModelRole role) const
{
    constexpr Array meridiem_names = {
        "AM", "PM"
    };

    auto& meridiem = meridiem_names[index.row()];
    if (role == GUI::ModelRole::Display) {
        switch (index.column()) {
        case Column::Meridiem:
            return meridiem;
        default:
            VERIFY_NOT_REACHED();
        }
    }
    return {};
}
