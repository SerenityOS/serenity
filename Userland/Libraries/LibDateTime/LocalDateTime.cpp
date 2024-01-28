/*
 * Copyright (c) 2024, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "LocalDateTime.h"
#include "ZonedDateTime.h"

namespace DateTime {

LocalDateTime LocalDateTime::now()
{
    auto now = UnixDateTime::now();
    if (auto current_offset = TimeZone::get_time_zone_offset(TimeZone::current_time_zone(), now); current_offset.has_value())
        now += Duration::from_seconds(current_offset->seconds);

    return LocalDateTime {
        now
    };
}

bool LocalDateTime::operator==(LocalDateTime const& other) const
{
    return this->m_offset == other.m_offset;
}

int LocalDateTime::operator<=>(LocalDateTime const& other) const
{
    return this->m_offset <=> other.m_offset;
}

Duration LocalDateTime::operator-(LocalDateTime const& other) const
{
    return this->m_offset - other.m_offset;
}

}
