/*
 * Copyright (c) 2023-2024, kleines Filmröllchen <filmroellchen@serenityos.org>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ZonedDateTime.h"
#include <AK/FormatParser.h>
#include <LibTimeZone/TimeZone.h>
#include <LibTimeZone/TimeZoneData.h>

namespace DateTime {

ZonedDateTime::ZonedDateTime(UnixDateTime unix_offset, TimeZone::TimeZone time_zone)
    : m_unix_offset(unix_offset)
    , m_time_zone(time_zone)
{
}

ZonedDateTime ZonedDateTime::now_in(TimeZone::TimeZone time_zone)
{
    return { UnixDateTime::now(), time_zone };
}

ZonedDateTime ZonedDateTime::now()
{
    return now_in(current_time_zone());
}

TimeZone::TimeZone ZonedDateTime::current_time_zone()
{
    // In case the current time zone is bogus, we fall back to UTC which is also available without a time zone database.
    return TimeZone::time_zone_from_string(TimeZone::current_time_zone()).value_or(TimeZone::TimeZone::UTC);
}

TimeZone::Offset ZonedDateTime::offset_to_utc() const
{
    // This only returns an empty optional if the time zone data is broken or missing, in which case we are UTC anyways.
    return TimeZone::get_time_zone_offset(m_time_zone, m_unix_offset).value_or({});
}

bool ZonedDateTime::operator==(ZonedDateTime const& other) const
{
    return m_unix_offset == other.m_unix_offset;
}

LocalDateTime ZonedDateTime::as_local_time() const
{
    // FIXME: Add leap seconds as required.
    auto zone_offset = Duration::from_seconds(offset_to_utc().seconds);
    auto own_epoch_offset = m_unix_offset + zone_offset;
    return LocalDateTime { own_epoch_offset };
}

ZonedDateTime ZonedDateTime::in_time_zone(TimeZone::TimeZone new_time_zone) const
{
    return ZonedDateTime { m_unix_offset, new_time_zone };
}

}
