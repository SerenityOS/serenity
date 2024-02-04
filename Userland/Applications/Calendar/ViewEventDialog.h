/*
 * Copyright (c) 2024, Sanil Gupta <sanilg566@gmail.com>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "EventManager.h"
#include <LibGUI/Calendar.h>
#include <LibGUI/Dialog.h>
#include <LibGUI/Window.h>

namespace Calendar {

class ViewEventDialog final : public GUI::Dialog {
    C_OBJECT(ViewEventDialog)
public:
    virtual ~ViewEventDialog() override = default;

    static void show(Core::DateTime date, EventManager& event_manager, Window* parent_window = nullptr)
    {
        auto dialog = ViewEventDialog::construct(date, event_manager, parent_window);
        dialog->exec();
    }
    void close_and_open_add_event_dialog();

private:
    ViewEventDialog(Core::DateTime, EventManager&, Window*);
    void update_events();

    EventManager& m_event_manager;
    Core::DateTime m_date_time;

    Vector<Event> m_events;
};

}
