/*
 * Copyright (c) 2019-2020, Ryan Grieb <ryan.m.grieb@gmail.com>
 * Copyright (c) 2022-2023, the SerenityOS developers.
 * Copyright (c) 2023, David Ganz <david.g.ganz@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "EventManager.h"
#include <LibGUI/Calendar.h>
#include <LibGUI/Dialog.h>
#include <LibGUI/Window.h>

namespace Calendar {

class AddEventDialog final : public GUI::Dialog {
    C_OBJECT(AddEventDialog)
public:
    virtual ~AddEventDialog() override = default;

    static void show(DateTime::LocalDateTime date_time, EventManager& event_manager, Window* parent_window = nullptr)
    {
        auto dialog = AddEventDialog::construct(date_time, event_manager, parent_window);
        dialog->exec();
    }

    ErrorOr<bool> add_event_to_calendar(DateTime::LocalDateTime start_date_time, DateTime::LocalDateTime end_date_time);

private:
    AddEventDialog(DateTime::LocalDateTime date_time, EventManager& event_manager, Window* parent_window = nullptr);

    EventManager& m_event_manager;
};

}
