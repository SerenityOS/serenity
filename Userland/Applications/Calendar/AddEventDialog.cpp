/*
 * Copyright (c) 2019-2020, Ryan Grieb <ryan.m.grieb@gmail.com>
 * Copyright (c) 2022-2023, the SerenityOS developers.
 * Copyright (c) 2023, David Ganz <david.g.ganz@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "AddEventDialog.h"
#include "AddEventWidget.h"
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

AddEventDialog::AddEventDialog(Core::DateTime date_time, EventManager& event_manager, Window* parent_window)
    : Dialog(parent_window)
    , m_event_manager(event_manager)
{
    resize(360, 140);
    set_title("Add Event");
    set_resizable(false);
    set_icon(parent_window->icon());

    auto start_date_time = Core::DateTime::create(date_time.year(), date_time.month(), date_time.day(), 12, 0);
    auto main_widget = MUST(AddEventWidget::create(this,
        start_date_time, Core::DateTime::from_timestamp(start_date_time.timestamp() + (15 * 60))));

    set_main_widget(main_widget);
}

ErrorOr<bool> AddEventDialog::add_event_to_calendar(Core::DateTime start_date_time, Core::DateTime end_date_time)
{
    if (end_date_time < start_date_time) {
        GUI::MessageBox::show_error(this, "The end date has to be after the start date."sv);
        return false;
    }

    auto summary = find_descendant_of_type_named<GUI::TextBox>("event_title_textbox")->get_text();
    m_event_manager.add_event(Event {
        .summary = TRY(String::from_byte_string(summary)),
        .start = start_date_time,
        .end = end_date_time,
    });

    return true;
}

}
