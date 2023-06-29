/*
 * Copyright (c) 2023, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "EventManager.h"
#include <AK/JsonParser.h>
#include <LibConfig/Client.h>
#include <LibFileSystemAccessClient/Client.h>

namespace Calendar {

EventManager::EventManager()
{
}

OwnPtr<EventManager> EventManager::create()
{
    return adopt_own(*new EventManager());
}

ErrorOr<void> EventManager::add_event(JsonObject event)
{
    TRY(m_events.append(move(event)));
    set_dirty(true);
    on_events_change();

    return {};
}

void EventManager::set_events(JsonArray events)
{
    m_events = move(events);
    on_events_change();
}

ErrorOr<void> EventManager::save(FileSystemAccessClient::File& file)
{
    set_filename(file.filename());
    set_dirty(false);

    auto stream = file.release_stream();
    TRY(stream->write_some(m_events.to_deprecated_string().bytes()));
    stream->close();

    return {};
}

ErrorOr<void> EventManager::load_file(FileSystemAccessClient::File& file)
{
    set_filename(file.filename());
    set_dirty(false);

    auto content = TRY(file.stream().read_until_eof());
    auto events = TRY(AK::JsonParser(content).parse());

    set_events(events.as_array());

    return {};
}

}
