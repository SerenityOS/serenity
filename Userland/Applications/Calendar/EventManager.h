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

    String const& current_filename() const { return m_current_filename; }
    void set_filename(String const& filename) { m_current_filename = filename; }
    bool dirty() const { return m_dirty; }
    void set_dirty(bool dirty) { m_dirty = dirty; }

    ErrorOr<void> save(FileSystemAccessClient::File& file);
    ErrorOr<void> load_file(FileSystemAccessClient::File& file);
    void add_event(Event);
    void set_events(Vector<Event>);
    void clear() { m_events.clear(); }

    Span<Event const> events() const { return m_events.span(); }

    Function<void()> on_events_change;

private:
    explicit EventManager();

    ErrorOr<JsonArray> serialize_events();
    ErrorOr<Vector<Event>> deserialize_events(JsonArray const& json);

    Vector<Event> m_events;

    String m_current_filename;
    bool m_dirty { false };
};

}
