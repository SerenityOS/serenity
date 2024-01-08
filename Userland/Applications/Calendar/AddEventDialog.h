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
#include <LibGUI/Model.h>
#include <LibGUI/Window.h>

namespace Calendar {

class AddEventDialog final : public GUI::Dialog {
    C_OBJECT(AddEventDialog)
public:
    virtual ~AddEventDialog() override = default;

    static void show(Core::DateTime date_time, EventManager& event_manager, Window* parent_window = nullptr)
    {
        auto dialog = AddEventDialog::construct(date_time, event_manager, parent_window);
        dialog->exec();
    }

private:
    AddEventDialog(Core::DateTime date_time, EventManager& event_manager, Window* parent_window = nullptr);

    ErrorOr<bool> add_event_to_calendar();

    void update_start_date();
    void update_end_date();
    void update_duration();

    Core::DateTime m_start_date_time;
    Core::DateTime m_end_date_time;
    EventManager& m_event_manager;

    RefPtr<GUI::TextBox> m_start_date_box;
    RefPtr<GUI::TextBox> m_end_date_box;
    RefPtr<GUI::SpinBox> m_start_hour_box;
    RefPtr<GUI::SpinBox> m_start_minute_box;
    RefPtr<GUI::SpinBox> m_end_hour_box;
    RefPtr<GUI::SpinBox> m_end_minute_box;
    RefPtr<GUI::SpinBox> m_duration_hour_box;
    RefPtr<GUI::SpinBox> m_duration_minute_box;
};

}
