/*
 * Copyright (c) 2023, the SerenityOS developers.
 * Copyright (c) 2023, David Ganz <david.g.ganz@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "EventManager.h"
#include <AK/JsonParser.h>
#include <LibConfig/Client.h>
#include <LibDateTime/Format.h>
#include <LibDateTime/ZonedDateTime.h>
#include <LibFileSystemAccessClient/Client.h>

namespace Calendar {

EventManager::EventManager()
{
}

OwnPtr<EventManager> EventManager::create()
{
    return adopt_own(*new EventManager());
}

void EventManager::add_event(Event event)
{
    m_events.append(move(event));
    m_dirty = true;
    on_events_change();
}

void EventManager::set_events(Vector<Event> events)
{
    m_events = move(events);
    m_dirty = true;
    on_events_change();
}

ErrorOr<void> EventManager::save(FileSystemAccessClient::File& file)
{
    set_filename(file.filename());

    auto stream = file.release_stream();
    auto json = TRY(serialize_events());
    TRY(stream->write_some(json.to_byte_string().bytes()));
    stream->close();

    m_dirty = false;

    return {};
}

ErrorOr<JsonArray> EventManager::serialize_events()
{
    JsonArray result;
    for (auto const& event : m_events) {
        JsonObject object;
        // FIXME: The timezone is part of a Core::DateTime circumvention hack explained below.
        //        Unless we want timezone information in the events, this should be removed eventually.
        object.set("start", JsonValue(TRY(String::formatted("{}{:{0z}}", TRY(event.start.format(DateTime::ISO8601_SHORT_FORMAT)), DateTime::ZonedDateTime::now()))));
        object.set("end", JsonValue(TRY(String::formatted("{}{:{0z}}", TRY(event.end.format(DateTime::ISO8601_SHORT_FORMAT)), DateTime::ZonedDateTime::now()))));
        object.set("summary", JsonValue(event.summary));
        TRY(result.append(object));
    }

    return result;
}

ErrorOr<Vector<Event>> EventManager::deserialize_events(JsonArray const& json)
{
    Vector<Event> result;

    auto local_timezone = TRY(DateTime::ZonedDateTime::now().format("{0z}"sv));

    for (auto const& value : json.values()) {
        auto const& object = value.as_object();
        if (!object.has("summary"sv) || !object.has("start"sv) || !object.has("end"sv))
            continue;

        auto summary = TRY(String::from_byte_string(object.get("summary"sv).release_value().as_string()));
        // FIXME: Implement and use a Core::LocalDateTime parser.
        //        Get rid of the timezone hack which is currently needed to prevent UTC-localtime adjustments.
        auto start = Core::DateTime::parse("%Y-%m-%dT%H:%M:%S%z"sv, object.get("start"sv).release_value().as_string());
        if (!start.has_value())
            continue;

        auto end = Core::DateTime::parse("%Y-%m-%dT%H:%M:%S%z"sv, object.get("end"sv).release_value().as_string());
        if (!end.has_value())
            continue;

        Event event = {
            .summary = summary,
            .start = DateTime::LocalDateTime { start.release_value() },
            .end = DateTime::LocalDateTime { end.release_value() },
        };
        result.append(event);
    }

    return result;
}

ErrorOr<void> EventManager::load_file(FileSystemAccessClient::File& file)
{
    set_filename(file.filename());

    auto content = TRY(file.stream().read_until_eof());
    auto json = TRY(AK::JsonParser(content).parse());
    auto events = TRY(deserialize_events(json.as_array()));
    set_events(events);

    m_dirty = false;

    return {};
}

}
