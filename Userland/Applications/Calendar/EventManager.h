/*
 * Copyright (c) 2023, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <AK/Noncopyable.h>
#include <AK/OwnPtr.h>
#include <LibFileSystemAccessClient/Client.h>
#include <LibGUI/Window.h>

namespace Calendar {

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
    ErrorOr<void> add_event(JsonObject);
    void set_events(JsonArray events);
    void clear() { m_events.clear(); }

    JsonArray const& events() const { return m_events; }

    Function<void()> on_events_change;

private:
    explicit EventManager();

    JsonArray m_events;

    String m_current_filename;
    bool m_dirty { false };
};

}
