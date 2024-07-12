/*
 * Copyright (c) 2023, the SerenityOS developers.
 * Copyright (c) 2023, David Ganz <david.g.ganz@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <AK/Noncopyable.h>
#include <AK/OwnPtr.h>
#include <LibCore/DateTime.h>
#include <LibFileSystemAccessClient/Client.h>
#include <LibGUI/Window.h>

namespace Calendar {

struct Event {
    String summary;
    Core::DateTime start;
    Core::DateTime end;
};

class EventManager {
    AK_MAKE_NONCOPYABLE(EventManager);
    AK_MAKE_NONMOVABLE(EventManager);

public:
    static OwnPtr<EventManager> create();

    ByteString const& current_filename() const { return m_current_filename; }
    void set_filename(ByteString filename) { m_current_filename = move(filename); }

    ErrorOr<void> save(FileSystemAccessClient::File& file);
    ErrorOr<void> load_file(FileSystemAccessClient::File& file);
    void add_event(Event);
    void set_events(Vector<Event>);
    void clear() { m_events.clear(); }

    bool is_dirty() const { return m_dirty; }
    Span<Event const> events() const { return m_events.span(); }

    Function<void()> on_events_change;

private:
    explicit EventManager();

    enum class ICalendarParserState {
        Idle = 0,
        InVEvent
    };

    ErrorOr<JsonArray> serialize_events();
    ErrorOr<Vector<Event>> deserialize_events(JsonArray const& json);
    ErrorOr<Vector<Event>> parse_events(ByteBuffer const& content);
    ErrorOr<Vector<Event>> parse_icalendar_vevents(ByteBuffer const& content);
    Core::DateTime format_icalendar_vevent_datetime(String const& parameter);

    Vector<Event> m_events;

    bool m_dirty { false };
    ByteString m_current_filename;
};

}
