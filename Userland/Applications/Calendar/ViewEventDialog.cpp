/*
 * Copyright (c) 2024, Sanil Gupta <sanilg566@gmail.com>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ViewEventDialog.h"
#include "AddEventDialog.h"
#include "ViewEventWidget.h"
#include <LibGUI/Label.h>

namespace Calendar {

ViewEventDialog::ViewEventDialog(Core::DateTime date_time, EventManager& event_manager, GUI::Window* parent_window)
    : GUI::Dialog(parent_window)
    , m_event_manager(event_manager)
    , m_date_time(date_time)
{
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

}
