/*
 * Copyright (c) 2021, Max Wipfli <max.wipfli@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/JsonObject.h>
#include <AK/Optional.h>
#include <LibCore/DateTime.h>
#include <LibMatrix/Id.h>

namespace Matrix {

class EventMetadata {
public:
    static Optional<EventMetadata> create_from_json(JsonObject const&);

    EventMetadata(EventId id, String type, UserId sender, u64 timestamp_in_milliseconds)
        : m_id(move(id))
        , m_type(move(type))
        , m_sender(move(sender))
        , m_timestamp_in_milliseconds(timestamp_in_milliseconds)
    {
    }

    EventId const& id() const { return m_id; }
    String const& type() const { return m_type; }
    UserId const& sender() const { return m_sender; }
    u64 timestamp_in_milliseconds() const { return m_timestamp_in_milliseconds; }
    Core::DateTime date_time() const { return Core::DateTime::from_timestamp(m_timestamp_in_milliseconds / 1000); }

private:
    EventId m_id;
    String m_type;
    UserId m_sender;
    u64 m_timestamp_in_milliseconds { 0 };
};

}
