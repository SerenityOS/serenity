/*
 * Copyright (c) 2024, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "AddEventWidget.h"
#include <LibDateTime/Format.h>
#include <LibDateTime/ISOCalendar.h>
#include <LibDateTime/LocalDateTime.h>
#include <LibGUI/Button.h>
#include <LibGUI/DatePicker.h>
#include <LibGUI/Dialog.h>
#include <LibGUI/SpinBox.h>
#include <LibGUI/TextBox.h>

namespace Calendar {

ErrorOr<NonnullRefPtr<AddEventWidget>> AddEventWidget::create(AddEventDialog* window, DateTime::LocalDateTime start_time, DateTime::LocalDateTime end_time)
{
    auto widget = TRY(try_create());
    widget->m_start_date_time = start_time;
    widget->m_end_date_time = end_time;

    auto& event_title_textbox = *widget->find_descendant_of_type_named<GUI::TextBox>("event_title_textbox");
    event_title_textbox.set_focus(true);

    widget->m_start_date_box = *widget->find_descendant_of_type_named<GUI::TextBox>("start_date");

    auto calendar_date_icon = Gfx::Bitmap::load_from_file("/res/icons/16x16/calendar-date.png"sv).release_value_but_fixme_should_propagate_errors();

    auto& pick_start_date_button = *widget->find_descendant_of_type_named<GUI::Button>("pick_start_date");
    pick_start_date_button.set_icon(calendar_date_icon);
    pick_start_date_button.on_click = [widget = widget, window = window](auto) {
        if (auto new_date = GUI::DatePicker::show(window, "Pick Start Date"_string, widget->m_start_date_time); new_date.has_value()) {
            widget->m_start_date_time = new_date.value();
            if (widget->m_end_date_time < widget->m_start_date_time) {
                widget->m_end_date_time = new_date.value();
                widget->update_end_date();
            }
            widget->update_duration();

            widget->m_start_date_box->set_text(MUST(widget->m_start_date_time.format(DateTime::ISO8601_DATE_FORMAT)));
        }
    };

    widget->m_start_hour_box = *widget->find_descendant_of_type_named<GUI::SpinBox>("start_hour");
    widget->m_start_minute_box = *widget->find_descendant_of_type_named<GUI::SpinBox>("start_minute");

    widget->m_end_date_box = *widget->find_descendant_of_type_named<GUI::TextBox>("end_date");

    auto& pick_end_date_button = *widget->find_descendant_of_type_named<GUI::Button>("pick_end_date");
    pick_end_date_button.set_icon(calendar_date_icon);
    pick_end_date_button.on_click = [widget = widget, window = window](auto) {
        if (auto new_date = GUI::DatePicker::show(window, "Pick End Date"_string, widget->m_end_date_time); new_date.has_value()) {
            widget->m_end_date_time = new_date.value();
            if (widget->m_end_date_time < widget->m_start_date_time) {
                widget->m_start_date_time = new_date.value();
                widget->update_start_date();
            }
            widget->update_duration();

            widget->m_end_date_box->set_text(MUST(widget->m_end_date_time.format(DateTime::ISO8601_DATE_FORMAT)));
        }
    };

    widget->m_end_hour_box = *widget->find_descendant_of_type_named<GUI::SpinBox>("end_hour");
    widget->m_end_minute_box = *widget->find_descendant_of_type_named<GUI::SpinBox>("end_minute");

    widget->m_duration_hour_box = *widget->find_descendant_of_type_named<GUI::SpinBox>("duration_hour");
    widget->m_duration_minute_box = *widget->find_descendant_of_type_named<GUI::SpinBox>("duration_minute");

    auto& ok_button = *widget->find_descendant_of_type_named<GUI::Button>("ok_button");
    ok_button.on_click = [widget = widget, window = window](auto) {
        auto successful = window->add_event_to_calendar(widget->m_start_date_time, widget->m_end_date_time).release_value_but_fixme_should_propagate_errors();
        if (!successful)
            return;
        window->done(GUI::Dialog::ExecResult::OK);
    };

    auto& cancel_button = *widget->find_descendant_of_type_named<GUI::Button>("cancel_button");
    cancel_button.on_click = [window = window](auto) { window->done(GUI::Dialog::ExecResult::Cancel); };

    auto update_starting_input_values = [widget = widget]() {
        auto hour = widget->m_start_hour_box->value();
        auto minute = widget->m_start_minute_box->value();
        auto new_start_time = DateTime::ISOCalendar::with_time(widget->m_start_date_time, hour, minute);
        if (new_start_time.is_error())
            return;
        widget->m_start_date_time = new_start_time.release_value();
        if (widget->m_end_date_time < widget->m_start_date_time) {
            widget->m_end_date_time = widget->m_start_date_time;
            widget->update_end_date();
        }
        widget->update_duration();
    };
    auto update_ending_input_values = [widget = widget]() {
        auto hour = widget->m_end_hour_box->value();
        auto minute = widget->m_end_minute_box->value();
        auto new_end_time = DateTime::ISOCalendar::with_time(widget->m_end_date_time, hour, minute);
        if (new_end_time.is_error())
            return;
        widget->m_end_date_time = new_end_time.release_value();
        if (widget->m_end_date_time < widget->m_start_date_time) {
            widget->m_start_date_time = widget->m_end_date_time;
            widget->update_start_date();
        }
        widget->update_duration();
    };
    auto update_duration_input_values = [widget]() {
        auto hour = widget->m_duration_hour_box->value();
        auto minute = widget->m_duration_minute_box->value();
        widget->m_end_date_time = widget->m_start_date_time + Duration::from_seconds((hour * 60 + minute) * 60);
        widget->update_end_date();
    };

    widget->m_start_hour_box->on_change = [update_starting_input_values](auto) { update_starting_input_values(); };
    widget->m_start_minute_box->on_change = [update_starting_input_values](auto) { update_starting_input_values(); };
    widget->m_end_hour_box->on_change = [update_ending_input_values](auto) { update_ending_input_values(); };
    widget->m_end_minute_box->on_change = [update_ending_input_values](auto) { update_ending_input_values(); };
    widget->m_duration_hour_box->on_change = [update_duration_input_values](auto) { update_duration_input_values(); };
    widget->m_duration_minute_box->on_change = [update_duration_input_values](auto) { update_duration_input_values(); };

    widget->update_start_date();
    widget->update_end_date();
    widget->update_duration();

    return widget;
}

void AddEventWidget::update_start_date()
{
    m_start_date_box->set_text(MUST(m_start_date_time.format(DateTime::ISO8601_DATE_FORMAT)));
    auto start_date_parts = m_start_date_time.to_parts<DateTime::ISOCalendar>();
    m_start_hour_box->set_value(start_date_parts.hour, GUI::AllowCallback::No);
    m_start_minute_box->set_value(start_date_parts.minute, GUI::AllowCallback::No);
}

void AddEventWidget::update_end_date()
{
    m_end_date_box->set_text(MUST(m_end_date_time.format(DateTime::ISO8601_DATE_FORMAT)));
    auto end_date_parts = m_end_date_time.to_parts<DateTime::ISOCalendar>();
    m_end_hour_box->set_value(end_date_parts.hour, GUI::AllowCallback::No);
    m_end_minute_box->set_value(end_date_parts.minute, GUI::AllowCallback::No);
}

void AddEventWidget::update_duration()
{
    auto difference = m_end_date_time - m_start_date_time;
    auto hours = difference.to_truncated_seconds() / (60 * 60);
    auto minutes = (difference.to_truncated_seconds() - hours * (60 * 60)) / 60;

    m_duration_hour_box->set_value(hours, GUI::AllowCallback::No);
    m_duration_minute_box->set_value(minutes, GUI::AllowCallback::No);
}

}
