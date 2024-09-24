/*
 * Copyright (c) 2023, the SerenityOS developers.
 * Copyright (c) 2023, David Ganz <david.g.ganz@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "EventManager.h"
#include <AK/JsonParser.h>
#include <AK/QuickSort.h>
#include <LibConfig/Client.h>
#include <LibFileSystemAccessClient/Client.h>
#include <LibTimeZone/TimeZone.h>

namespace Calendar {

static constexpr StringView DATE_FORMAT = "%Y-%m-%dT%H:%M:%S"sv;

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
    quick_sort(m_events, [&](auto& a, auto& b) { return a.start < b.start; });
    m_dirty = true;
    on_events_change();
}

void EventManager::set_events(Vector<Event> events)
{
    m_events = move(events);
    quick_sort(m_events, [&](auto& a, auto& b) { return a.start < b.start; });
    m_dirty = true;
    on_events_change();
}

ErrorOr<void> EventManager::save(FileSystemAccessClient::File& file)
{
    set_filename(file.filename());

    auto stream = file.release_stream();
    auto json = TRY(serialize_events()).to_byte_string();
    TRY(stream->write_some(json.bytes()));
    stream->close();

    m_dirty = false;

    return {};
}

ErrorOr<JsonArray> EventManager::serialize_events()
{
    JsonArray result;
    for (auto const& event : m_events) {
        JsonObject object;
        object.set("start", JsonValue(event.start.to_byte_string(DATE_FORMAT)));
        object.set("end", JsonValue(event.end.to_byte_string(DATE_FORMAT)));
        object.set("summary", JsonValue(event.summary));
        TRY(result.append(object));
    }

    return result;
}

ErrorOr<Vector<Event>> EventManager::deserialize_events(JsonArray const& json)
{
    Vector<Event> result;

    for (auto const& value : json.values()) {
        auto const& object = value.as_object();
        if (!object.has("summary"sv) || !object.has("start"sv) || !object.has("end"sv))
            continue;

        auto summary = TRY(String::from_byte_string(object.get("summary"sv).release_value().as_string()));
        auto start = Core::DateTime::parse(DATE_FORMAT, object.get("start"sv).release_value().as_string());
        if (!start.has_value())
            continue;

        auto end = Core::DateTime::parse(DATE_FORMAT, object.get("end"sv).release_value().as_string());
        if (!end.has_value())
            continue;

        Event event = {
            .summary = summary,
            .start = start.release_value(),
            .end = end.release_value(),
        };
        result.append(event);
    }

    return result;
}

Core::DateTime EventManager::format_icalendar_vevent_datetime(String const& parameter)
{
    auto invalid_datetime = Core::DateTime::create(0);
    auto date_time_bytes = parameter.bytes();

    // https://datatracker.ietf.org/doc/html/rfc5545#section-3.3.5
    // 3.3.5.  Date-Time
    //     date-time  = date "T" time ;As specified in the DATE and TIME
    //                                ;value definitions
    if (date_time_bytes.size() < 15 || date_time_bytes[8] != 'T')
        return invalid_datetime;

    auto formatted_string = String::formatted("{:c}-{:c}-{:c}T{:c}:{:c}:{:c}",
        date_time_bytes.slice(0, 4), date_time_bytes.slice(4, 2),
        date_time_bytes.slice(6, 2), date_time_bytes.slice(9, 2),
        date_time_bytes.slice(11, 2), date_time_bytes.slice(13, 2));
    if (formatted_string.is_error())
        return invalid_datetime;
    auto datetime = Core::DateTime::parse(DATE_FORMAT, formatted_string.value());
    if (!datetime.has_value())
        return invalid_datetime;

    // FORM #1: DATE WITH LOCAL TIME
    if (date_time_bytes.size() == 15)
        return datetime.value();

    // FORM #2: DATE WITH UTC TIME
    if (date_time_bytes.size() == 16 && date_time_bytes[15] == 'Z') {
        auto offset = TimeZone::get_time_zone_offset(TimeZone::system_time_zone(), UnixDateTime::epoch());
        if (!offset.has_value())
            return invalid_datetime;
        auto utc_timestamp = datetime.value().timestamp();
        datetime = Core::DateTime::from_timestamp(utc_timestamp + offset.value().seconds);
        return datetime.has_value() ? datetime.value() : invalid_datetime;
    }

    // FIXME: Implement FORM #3: DATE WITH LOCAL TIME AND TIME ZONE REFERENCE
    return invalid_datetime;
}

// https://datatracker.ietf.org/doc/html/rfc5545
ErrorOr<Vector<Event>> EventManager::parse_icalendar_vevents(ByteBuffer const& content)
{
    Event event;
    Vector<Event> events;
    ICalendarParserState state = ICalendarParserState::Idle;
    auto lines = StringView(content.bytes()).split_view('\n');
    for (size_t i = 0; i < lines.size(); i++) {
        auto section = TRY(String::formatted("{}", lines[i]).value().split_limit(':', 2));
        if (section.size() > 1) {
            auto property = section[0];
            auto parameter = TRY(section[1].trim_ascii_whitespace());
            switch (state) {
            case ICalendarParserState::InVEvent:
                if (property.bytes().starts_with("DTSTART"sv.bytes()))
                    event.start = format_icalendar_vevent_datetime(parameter);
                if (property.bytes().starts_with("DTEND"sv.bytes()))
                    event.end = format_icalendar_vevent_datetime(parameter);
                if (property == "SUMMARY")
                    event.summary = parameter;
                if (property == "END" && parameter == "VEVENT") {
                    if (event.start.year() && event.end.year())
                        events.append(event);
                    state = ICalendarParserState::Idle;
                }
                break;
            case ICalendarParserState::Idle:
                if (property == "BEGIN" && parameter == "VEVENT")
                    state = ICalendarParserState::InVEvent;
                break;
            default:
                break;
            }
        }
    }
    return events;
}

ErrorOr<Vector<Event>> EventManager::parse_events(ByteBuffer const& content)
{
    // If content is iCalendar format, try to parse VEVENTs
    if (content.span().starts_with("BEGIN:VCALENDAR"sv.bytes())) {
        set_filename("");
        return parse_icalendar_vevents(content);
    }
    // Otherwise, try to parse content as JSON
    auto json = TRY(AK::JsonParser(content).parse());
    return deserialize_events(json.as_array());
}

ErrorOr<void> EventManager::load_file(FileSystemAccessClient::File& file)
{
    set_filename(file.filename());

    auto content = TRY(file.stream().read_until_eof());
    auto events = parse_events(content);
    set_events(TRY(events));

    m_dirty = false;

    return {};
}

}
