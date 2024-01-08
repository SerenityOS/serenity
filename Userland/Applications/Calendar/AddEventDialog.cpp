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
#include <LibGUI/MessageBox.h>
#include <LibGUI/Painter.h>
#include <LibGUI/SpinBox.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>
#include <LibGfx/Color.h>
#include <LibGfx/Font/FontDatabase.h>

namespace Calendar {

static constexpr StringView DATE_FORMAT = "%Y-%m-%d"sv;

AddEventDialog::AddEventDialog(Core::DateTime date_time, EventManager& event_manager, Window* parent_window)
    : Dialog(parent_window)
    , m_start_date_time(Core::DateTime::create(date_time.year(), date_time.month(), date_time.day(), 12, 0))
    , m_end_date_time(Core::DateTime::from_timestamp(m_start_date_time.timestamp() + (15 * 60)))
    , m_event_manager(event_manager)
{
    resize(360, 140);
    set_title("Add Event");
    set_resizable(false);
    set_icon(parent_window->icon());

    dbgln("start time: {}", m_start_date_time.to_string().release_value_but_fixme_should_propagate_errors());
    dbgln("end time: {}", m_end_date_time.to_string().release_value_but_fixme_should_propagate_errors());

    auto widget = set_main_widget<GUI::Widget>();
    widget->load_from_gml(add_event_dialog_gml).release_value_but_fixme_should_propagate_errors();

    auto& event_title_textbox = *widget->find_descendant_of_type_named<GUI::TextBox>("event_title_textbox");
    event_title_textbox.set_focus(true);

    m_start_date_box = *widget->find_descendant_of_type_named<GUI::TextBox>("start_date");

    auto calendar_date_icon = Gfx::Bitmap::load_from_file("/res/icons/16x16/calendar-date.png"sv).release_value_but_fixme_should_propagate_errors();

    auto& pick_start_date_button = *widget->find_descendant_of_type_named<GUI::Button>("pick_start_date");
    pick_start_date_button.set_icon(calendar_date_icon);
    pick_start_date_button.on_click = [&](auto) {
        if (auto new_date = GUI::DatePicker::show(this, "Pick Start Date"_string, m_start_date_time); new_date.has_value()) {
            m_start_date_time.set_date(new_date.value());
            if (m_end_date_time < m_start_date_time) {
                m_end_date_time.set_date(new_date.value());
                update_end_date();
            }
            update_duration();

            m_start_date_box->set_text(MUST(m_start_date_time.to_string(DATE_FORMAT)));
        }
    };

    m_start_hour_box = *widget->find_descendant_of_type_named<GUI::SpinBox>("start_hour");
    m_start_minute_box = *widget->find_descendant_of_type_named<GUI::SpinBox>("start_minute");

    m_end_date_box = *widget->find_descendant_of_type_named<GUI::TextBox>("end_date");

    auto& pick_end_date_button = *widget->find_descendant_of_type_named<GUI::Button>("pick_end_date");
    pick_end_date_button.set_icon(calendar_date_icon);
    pick_end_date_button.on_click = [&](auto) {
        if (auto new_date = GUI::DatePicker::show(this, "Pick End Date"_string, m_end_date_time); new_date.has_value()) {
            m_end_date_time.set_date(new_date.value());
            if (m_end_date_time < m_start_date_time) {
                m_start_date_time.set_date(new_date.value());
                update_start_date();
            }
            update_duration();

            m_end_date_box->set_text(MUST(m_end_date_time.to_string(DATE_FORMAT)));
        }
    };

    m_end_hour_box = *widget->find_descendant_of_type_named<GUI::SpinBox>("end_hour");
    m_end_minute_box = *widget->find_descendant_of_type_named<GUI::SpinBox>("end_minute");

    m_duration_hour_box = *widget->find_descendant_of_type_named<GUI::SpinBox>("duration_hour");
    m_duration_minute_box = *widget->find_descendant_of_type_named<GUI::SpinBox>("duration_minute");

    auto& ok_button = *widget->find_descendant_of_type_named<GUI::Button>("ok_button");
    ok_button.on_click = [&](auto) {
        auto successful = add_event_to_calendar().release_value_but_fixme_should_propagate_errors();
        if (!successful)
            return;
        done(ExecResult::OK);
    };

    auto& cancel_button = *widget->find_descendant_of_type_named<GUI::Button>("cancel_button");
    cancel_button.on_click = [&](auto) { done(ExecResult::Cancel); };

    auto update_starting_input_values = [&, this]() {
        auto hour = m_start_hour_box->value();
        auto minute = m_start_minute_box->value();
        m_start_date_time.set_time_only(hour, minute);
        if (m_end_date_time < m_start_date_time) {
            m_end_date_time.set_time_only(hour, minute);
            update_end_date();
        }
        update_duration();
    };
    auto update_ending_input_values = [&, this]() {
        auto hour = m_end_hour_box->value();
        auto minute = m_end_minute_box->value();
        m_end_date_time.set_time_only(hour, minute);
        if (m_end_date_time < m_start_date_time) {
            m_start_date_time.set_time_only(hour, minute);
            update_start_date();
        }
        update_duration();
    };
    auto update_duration_input_values = [&, this]() {
        auto hour = m_duration_hour_box->value();
        auto minute = m_duration_minute_box->value();
        m_end_date_time = Core::DateTime::from_timestamp(m_start_date_time.timestamp() + (hour * 60 + minute) * 60);
        update_end_date();
    };

    m_start_hour_box->on_change = [update_starting_input_values](auto) { update_starting_input_values(); };
    m_start_minute_box->on_change = [update_starting_input_values](auto) { update_starting_input_values(); };
    m_end_hour_box->on_change = [update_ending_input_values](auto) { update_ending_input_values(); };
    m_end_minute_box->on_change = [update_ending_input_values](auto) { update_ending_input_values(); };
    m_duration_hour_box->on_change = [update_duration_input_values](auto) { update_duration_input_values(); };
    m_duration_minute_box->on_change = [update_duration_input_values](auto) { update_duration_input_values(); };

    update_start_date();
    update_end_date();
    update_duration();
}

ErrorOr<bool> AddEventDialog::add_event_to_calendar()
{
    if (m_end_date_time < m_start_date_time) {
        GUI::MessageBox::show_error(this, "The end date has to be after the start date."sv);
        return false;
    }

    auto summary = find_descendant_of_type_named<GUI::TextBox>("event_title_textbox")->get_text();
    m_event_manager.add_event(Event {
        .summary = TRY(String::from_byte_string(summary)),
        .start = m_start_date_time,
        .end = m_end_date_time,
    });

    return true;
}

void AddEventDialog::update_start_date()
{
    m_start_date_box->set_text(MUST(m_start_date_time.to_string(DATE_FORMAT)));
    m_start_hour_box->set_value(m_start_date_time.hour(), GUI::AllowCallback::No);
    m_start_minute_box->set_value(m_start_date_time.minute(), GUI::AllowCallback::No);
}

void AddEventDialog::update_end_date()
{
    m_end_date_box->set_text(MUST(m_end_date_time.to_string(DATE_FORMAT)));
    m_end_hour_box->set_value(m_end_date_time.hour(), GUI::AllowCallback::No);
    m_end_minute_box->set_value(m_end_date_time.minute(), GUI::AllowCallback::No);
}

void AddEventDialog::update_duration()
{
    auto difference_in_seconds = m_end_date_time.timestamp() - m_start_date_time.timestamp();
    auto hours = difference_in_seconds / (60 * 60);
    auto minutes = (difference_in_seconds - hours * (60 * 60)) / 60;

    m_duration_hour_box->set_value(hours, GUI::AllowCallback::No);
    m_duration_minute_box->set_value(minutes, GUI::AllowCallback::No);
}

}
