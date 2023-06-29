/*
 * Copyright (c) 2023, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "EventManager.h"
#include <LibFileSystemAccessClient/Client.h>
#include <LibGUI/Calendar.h>

namespace Calendar {

class EventCalendar final : public GUI::Calendar {
    C_OBJECT(EventCalendar);

public:
    virtual ~EventCalendar() override = default;

    EventManager& event_manager() const { return *m_event_manager; }

private:
    EventCalendar(Core::DateTime date_time = Core::DateTime::now(), Mode mode = Month);

    ErrorOr<void> save(FileSystemAccessClient::File& file);
    ErrorOr<void> load_file(FileSystemAccessClient::File& file);

    virtual void paint_tile(GUI::Painter&, GUI::Calendar::Tile&, Gfx::IntRect&, int x_offset, int y_offset, int day_offset) override;

    OwnPtr<EventManager> m_event_manager;
};

}
