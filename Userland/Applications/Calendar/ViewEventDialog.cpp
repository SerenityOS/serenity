/*
 * Copyright (c) 2024, Sanil Gupta <sanilg566@gmail.com>.
 * Copyright (c) 2026, RiffPointer <riffpointer@gmail.com>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ViewEventDialog.h"
#include "AddEventDialog.h"
#include "ViewEventWidget.h"
#include <LibGUI/Label.h>
#include <LibGUI/MessageBox.h>

namespace Calendar {

ViewEventDialog::ViewEventDialog(Core::DateTime date_time, EventManager& event_manager, GUI::Window* parent_window)
    : GUI::Dialog(parent_window)
    , m_event_manager(event_manager)
    , m_date_time(date_time)
{
    resize(360, 200);
    set_title("Events");
    set_resizable(true);
    set_icon(parent_window->icon());

    update_events();

    auto main_widget = MUST(ViewEventWidget::create(this, m_events));
    set_main_widget(main_widget);
}

void ViewEventDialog::update_events()
{
    for (auto const& event : m_event_manager.events()) {
        auto start_date = event.start;
        if (start_date.year() == m_date_time.year() && start_date.month() == m_date_time.month() && start_date.day() == m_date_time.day()) {
            m_events.append(event);
        }
    }
}

void ViewEventDialog::close_and_open_add_event_dialog()
{
    close();
    AddEventDialog::show(m_date_time, m_event_manager, find_parent_window());
}

void ViewEventDialog::close_and_open_edit_event_dialog(Event const& event)
{
    close();
    AddEventDialog::show(m_date_time, m_event_manager, find_parent_window(), &event);
}

void ViewEventDialog::delete_event(Event const& event)
{
    auto result = GUI::MessageBox::show(this, "Are you sure you want to delete this event?"sv, "Delete Event"sv, GUI::MessageBox::Type::Warning, GUI::MessageBox::InputType::YesNo);
    if (result == GUI::MessageBox::ExecResult::Yes) {
        m_event_manager.delete_event(event);
        close();
    }
}

}
