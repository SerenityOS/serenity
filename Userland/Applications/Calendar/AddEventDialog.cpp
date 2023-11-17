/*
 * Copyright (c) 2019-2020, Ryan Grieb <ryan.m.grieb@gmail.com>
 * Copyright (c) 2022-2023, the SerenityOS developers.
 * Copyright (c) 2023, David Ganz <david.g.ganz@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "AddEventDialog.h"
#include <Applications/Calendar/AddEventDialogGML.h>
#include <LibCore/DateTime.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/ComboBox.h>
#include <LibGUI/DatePicker.h>
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

    auto& event_title_textbox = *widget->find_descendant_of_type_named<GUI::TextBox>("event_title_textbox");
    event_title_textbox.set_focus(true);

    auto& start_date_box = *widget->find_descendant_of_type_named<GUI::TextBox>("start_date");
    start_date_box.set_text(MUST(m_start_date_time.to_string("%Y-%m-%d"sv)));

    auto calendar_date_icon = Gfx::Bitmap::load_from_file("/res/icons/16x16/calendar-date.png"sv).release_value_but_fixme_should_propagate_errors();

    auto& pick_start_date_button = *widget->find_descendant_of_type_named<GUI::Button>("pick_start_date");
    pick_start_date_button.set_icon(calendar_date_icon);
    pick_start_date_button.on_click = [&](auto) {
        if (auto new_date = GUI::DatePicker::show(this, "Pick Start Date"_string, m_start_date_time); new_date.has_value()) {
            m_start_date_time.set_date(new_date.release_value());
            start_date_box.set_text(MUST(m_start_date_time.to_string("%Y-%m-%d"sv)));
        }
    };

    auto& starting_hour_input = *widget->find_descendant_of_type_named<GUI::SpinBox>("start_hour");
    starting_hour_input.set_value(m_start_date_time.hour());

    auto& starting_minute_input = *widget->find_descendant_of_type_named<GUI::SpinBox>("start_minute");
    starting_minute_input.set_value(m_start_date_time.minute());

    auto& end_date_box = *widget->find_descendant_of_type_named<GUI::TextBox>("end_date");
    end_date_box.set_text(MUST(m_start_date_time.to_string("%Y-%m-%d"sv)));

    auto& pick_end_date_button = *widget->find_descendant_of_type_named<GUI::Button>("pick_end_date");
    pick_end_date_button.set_icon(calendar_date_icon);
    pick_end_date_button.on_click = [&](auto) {
        if (auto new_date = GUI::DatePicker::show(this, "Pick End Date"_string, m_end_date_time); new_date.has_value()) {
            m_end_date_time.set_date(new_date.release_value());
            end_date_box.set_text(MUST(m_end_date_time.to_string("%Y-%m-%d"sv)));
        }
    };

    auto& ending_hour_input = *widget->find_descendant_of_type_named<GUI::SpinBox>("end_hour");
    ending_hour_input.set_value(m_end_date_time.hour());

    auto& ending_minute_input = *widget->find_descendant_of_type_named<GUI::SpinBox>("end_minute");
    ending_minute_input.set_value(m_end_date_time.minute());

    auto& ok_button = *widget->find_descendant_of_type_named<GUI::Button>("ok_button");
    ok_button.on_click = [&](auto) {
        add_event_to_calendar().release_value_but_fixme_should_propagate_errors();
        done(ExecResult::OK);
    };

    auto update_starting_input_values = [&, this]() {
        auto hour = starting_hour_input.value();
        auto minute = starting_minute_input.value();
        m_start_date_time.set_time_only(hour, minute);
    };
    auto update_ending_input_values = [&, this]() {
        auto hour = ending_hour_input.value();
        auto minute = ending_minute_input.value();
        m_end_date_time.set_time_only(hour, minute);
    };
    starting_hour_input.on_change = [update_starting_input_values](auto) { update_starting_input_values(); };
    starting_minute_input.on_change = [update_starting_input_values](auto) { update_starting_input_values(); };

    ending_hour_input.on_change = [update_ending_input_values](auto) { update_ending_input_values(); };
    ending_minute_input.on_change = [update_ending_input_values](auto) { update_ending_input_values(); };
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

}
