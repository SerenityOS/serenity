/*
 * Copyright (c) 2023, the SerenityOS developers.
 * Copyright (c) 2023, David Ganz <david.g.ganz@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "EventCalendar.h"
#include <AK/NonnullRefPtr.h>
#include <LibFileSystemAccessClient/Client.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/Calendar.h>
#include <LibGUI/Widget.h>

namespace Calendar {

class CalendarWidget final : public GUI::Widget {
    C_OBJECT(CalendarWidget);

public:
    static ErrorOr<NonnullRefPtr<CalendarWidget>> create(GUI::Window*);
    virtual ~CalendarWidget() override = default;

    void update_window_title();
    void load_file(FileSystemAccessClient::File file);

    bool request_close();

private:
    CalendarWidget() = default;
    static ErrorOr<NonnullRefPtr<CalendarWidget>> try_create();

    void create_on_tile_doubleclick();

    ByteString const& current_filename() const { return m_event_calendar->event_manager().current_filename(); }

    void create_on_events_change();
    NonnullRefPtr<GUI::Action> create_save_as_action();
    NonnullRefPtr<GUI::Action> create_save_action(GUI::Action& save_as_action);
    ErrorOr<NonnullRefPtr<GUI::Action>> create_new_calendar_action();
    NonnullRefPtr<GUI::Action> create_open_calendar_action();
    ErrorOr<NonnullRefPtr<GUI::Action>> create_prev_date_action();
    ErrorOr<NonnullRefPtr<GUI::Action>> create_next_date_action();
    ErrorOr<NonnullRefPtr<GUI::Action>> create_add_event_action();
    ErrorOr<NonnullRefPtr<GUI::Action>> create_jump_to_action();
    ErrorOr<NonnullRefPtr<GUI::Action>> create_view_month_action();
    ErrorOr<NonnullRefPtr<GUI::Action>> create_view_year_action();
    ErrorOr<NonnullRefPtr<GUI::Action>> create_open_settings_action();

    OwnPtr<GUI::ActionGroup> m_view_type_action_group;
    RefPtr<GUI::Action> m_save_action;

    RefPtr<EventCalendar> m_event_calendar;
};

}
