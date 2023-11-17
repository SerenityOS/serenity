/*
 * Copyright (c) 2019-2020, Ryan Grieb <ryan.m.grieb@gmail.com>
 * Copyright (c) 2022-2023, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "AddEventDialog.h"
#include <Applications/Calendar/AddEventDialogGML.h>
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

namespace Calendar {

AddEventDialog::AddEventDialog(Core::DateTime date_time, EventManager& event_manager, Window* parent_window)
    : Dialog(parent_window)
    , m_start_date_time(date_time)
    , m_end_date_time(Core::DateTime::from_timestamp(date_time.timestamp() + (15 * 60)))
    , m_event_manager(event_manager)
{
    resize(360, 140);
    set_title("Add Event");
    set_resizable(false);
    set_icon(parent_window->icon());

    dbgln("start time: {}", m_start_date_time.to_string().release_value_but_fixme_should_propagate_errors());
    dbgln("end time: {}", m_end_date_time.to_string().release_value_but_fixme_should_propagate_errors());

    m_start_date_time = Core::DateTime::create(m_start_date_time.year(), m_start_date_time.month(), m_start_date_time.day(), 12, 0);

    auto widget = set_main_widget<GUI::Widget>();
    widget->load_from_gml(add_event_dialog_gml).release_value_but_fixme_should_propagate_errors();

    auto event_title_textbox = widget->find_descendant_of_type_named<GUI::TextBox>("event_title_textbox");
    event_title_textbox->set_focus(true);

    auto starting_month_input = widget->find_descendant_of_type_named<GUI::ComboBox>("start_month");
    starting_month_input->set_model(GUI::MonthListModel::create());
    starting_month_input->set_selected_index(m_start_date_time.month() - 1);

    auto starting_day_input = widget->find_descendant_of_type_named<GUI::SpinBox>("start_day");
    starting_day_input->set_value(m_start_date_time.day());

    auto starting_year_input = widget->find_descendant_of_type_named<GUI::SpinBox>("start_year");
    starting_year_input->set_value(m_start_date_time.year());

    auto starting_hour_input = widget->find_descendant_of_type_named<GUI::SpinBox>("start_hour");
    starting_hour_input->set_value(m_start_date_time.hour());

    auto starting_minute_input = widget->find_descendant_of_type_named<GUI::SpinBox>("start_minute");
    starting_minute_input->set_value(m_start_date_time.minute());

    auto starting_meridiem_input = widget->find_descendant_of_type_named<GUI::ComboBox>("start_meridiem");
    starting_meridiem_input->set_model(MeridiemListModel::create());
    starting_meridiem_input->set_selected_index(0);

    auto ending_month_input = widget->find_descendant_of_type_named<GUI::ComboBox>("end_month");
    ending_month_input->set_model(GUI::MonthListModel::create());
    ending_month_input->set_selected_index(m_end_date_time.month() - 1);

    auto ending_day_input = widget->find_descendant_of_type_named<GUI::SpinBox>("end_day");
    ending_day_input->set_value(m_end_date_time.day());

    auto ending_year_input = widget->find_descendant_of_type_named<GUI::SpinBox>("end_year");
    ending_year_input->set_value(m_end_date_time.year());

    auto ending_hour_input = widget->find_descendant_of_type_named<GUI::SpinBox>("end_hour");
    ending_hour_input->set_value(m_end_date_time.hour());

    auto ending_minute_input = widget->find_descendant_of_type_named<GUI::SpinBox>("end_minute");
    ending_minute_input->set_value(m_end_date_time.minute());

    auto ending_meridiem_input = widget->find_descendant_of_type_named<GUI::ComboBox>("end_meridiem");
    ending_meridiem_input->set_model(MeridiemListModel::create());
    ending_meridiem_input->set_selected_index(0);

    auto ok_button = widget->find_descendant_of_type_named<GUI::Button>("ok_button");
    ok_button->on_click = [&](auto) {
        add_event_to_calendar().release_value_but_fixme_should_propagate_errors();

        done(ExecResult::OK);
    };

    auto update_starting_day_range = [=]() {
        auto year = starting_year_input->value();
        auto month = starting_month_input->selected_index();

        starting_day_input->set_range(1, days_in_month(year, month + 1));
    };
    auto update_ending_day_range = [=]() {
        auto year = ending_year_input->value();
        auto month = ending_month_input->selected_index();

        ending_day_input->set_range(1, days_in_month(year, month + 1));
    };
    auto update_starting_input_values = [=, this]() {
        auto year = starting_year_input->value();
        auto month = starting_month_input->selected_index() + 1;
        auto day = starting_day_input->value();
        auto hour = starting_hour_input->value();
        auto minute = starting_minute_input->value();

        m_start_date_time = Core::DateTime::create(year, month, day, hour, minute);
    };
    auto update_ending_input_values = [=, this]() {
        auto year = ending_year_input->value();
        auto month = ending_month_input->selected_index() + 1;
        auto day = ending_day_input->value();
        auto hour = ending_hour_input->value();
        auto minute = ending_minute_input->value();

        m_end_date_time = Core::DateTime::create(year, month, day, hour, minute);
    };
    starting_year_input->on_change = [update_starting_input_values, update_starting_day_range](auto) {
        update_starting_input_values();
        update_starting_day_range();
    };
    starting_month_input->on_change = [update_starting_input_values, update_starting_day_range](auto, auto) {
        update_starting_input_values();
        update_starting_day_range();
    };
    starting_day_input->on_change = [update_starting_input_values](auto) { update_starting_input_values(); };
    starting_hour_input->on_change = [update_starting_input_values](auto) { update_starting_input_values(); };
    starting_minute_input->on_change = [update_starting_input_values](auto) { update_starting_input_values(); };

    ending_year_input->on_change = [update_ending_input_values, update_ending_day_range](auto) {
        update_ending_input_values();
        update_ending_day_range();
    };
    ending_month_input->on_change = [update_ending_input_values, update_ending_day_range](auto, auto) {
        update_ending_input_values();
        update_ending_day_range();
    };
    ending_day_input->on_change = [update_ending_input_values](auto) { update_ending_input_values(); };
    ending_hour_input->on_change = [update_ending_input_values](auto) { update_ending_input_values(); };
    ending_minute_input->on_change = [update_ending_input_values](auto) { update_ending_input_values(); };
}

ErrorOr<void> AddEventDialog::add_event_to_calendar()
{
    auto to_date_string = [](Core::DateTime date_time) -> ErrorOr<String> {
        return String::formatted("{}-{:0>2d}-{:0>2d}", date_time.year(), date_time.month(), date_time.day());
    };
    auto to_time_string = [](Core::DateTime date_time) -> ErrorOr<String> {
        return String::formatted("{}:{:0>2d}", date_time.hour(), date_time.minute());
    };

    JsonObject event;
    auto start_date = TRY(to_date_string(m_start_date_time));
    auto start_time = TRY(to_time_string(m_start_date_time));
    auto end_date = TRY(to_date_string(m_end_date_time));
    auto end_time = TRY(to_time_string(m_end_date_time));
    auto summary = find_descendant_of_type_named<GUI::TextBox>("event_title_textbox")->get_text();
    event.set("start_date", JsonValue(start_date));
    event.set("start_time", JsonValue(start_time));
    event.set("end_date", JsonValue(end_date));
    event.set("end_time", JsonValue(end_time));
    event.set("summary", JsonValue(summary));
    TRY(m_event_manager.add_event(event));
    m_event_manager.set_dirty(true);

    return {};
}

int AddEventDialog::MeridiemListModel::row_count(const GUI::ModelIndex&) const
{
    return 2;
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

}
