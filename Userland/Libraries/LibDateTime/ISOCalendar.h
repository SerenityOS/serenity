/*
 * Copyright (c) 2024, kleines Filmröllchen <filmroellchen@serenityos.org>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibDateTime/Calendar.h>
#include <LibDateTime/ZonedDateTime.h>
#include <LibTimeZone/TimeZoneData.h>

namespace DateTime {

// An ISO 8601/Gregorian calendar.
class ISOCalendar {
public:
    ISOCalendar() = delete;

    struct InputParts;

    // Parts of a date in the ISO calendar.
    // NOTE: Struct fields are arranged to alleviate padding.
    struct OutputParts {
        // Nanosecond in second (0 - 1^12)
        u64 nanosecond {};
        // Second within a day (0 - 86399)
        u32 second_in_day {};
        // Gregorian year; there is no year 0!
        i32 year { 1970 };
        // Day within a year (1 - 365)
        u16 day_of_year { 1 };
        // Time zone offset from UTC in seconds.
        // Will be zero for UTC *and* time-zone-less datetimes.
        i16 time_zone_offset_seconds {};
        // One-based month (1 - 12)
        u8 month { 1 };
        // Day of a month (1 - 31 depending on month)
        u8 day_of_month { 1 };
        // Day of the week (1 - 7); weeks start on mondays.
        u8 weekday {};

        // Hour in a day (0 - 23)
        u8 hour {};
        // Minute in an hour (0 - 59)
        u8 minute {};
        // Seconds in a minute (0 - 59)
        u8 second {};

        explicit operator InputParts() const;
        u8 total_days_in_month() const;
    };
    struct InputParts {
        // Nanosecond in second (0 - 1^12)
        u64 nanosecond {};
        // Gregorian year
        i32 year { 1970 };
        // Time zone offset from UTC in seconds.
        // Should be zero for UTC or time-zone-less datetimes.
        i16 time_zone_offset_seconds {};
        // One-based month (1 - 12)
        u8 month { 1 };
        // Day of a month (1 - 31 depending on month)
        u8 day_of_month { 1 };

        // Hour in a day (0 - 23)
        u8 hour {};
        // Minute in an hour (0 - 59)
        u8 minute {};
        // Seconds in a minute (0 - 59)
        u8 second {};
    };

    // ^Calendar
    static ErrorOr<String> format(ZonedDateTime const& date_time);
    static OutputParts to_parts(ZonedDateTime const& date_time);
    static ErrorOr<String> format(LocalDateTime const& date_time);
    static OutputParts to_parts(LocalDateTime const& date_time);
    static ErrorOr<ZonedDateTime> zoned_date_time_from_parts(InputParts const& input_parts, TimeZone::TimeZone const& time_zone);
    static ErrorOr<LocalDateTime> local_date_time_from_parts(InputParts const& input_parts);

    static ErrorOr<LocalDateTime> with_time(LocalDateTime const& date_time, u8 hour = 0, u8 minute = 0, u8 second = 0);
    static ErrorOr<LocalDateTime> with_date(LocalDateTime const& date_time, i32 year, u8 month, u8 day_of_month);

    static i32 year(ZonedDateTime const& date_time) { return to_parts(date_time).year; }
    static u8 month(ZonedDateTime const& date_time) { return to_parts(date_time).month; }
    static u8 day_of_month(ZonedDateTime const& date_time) { return to_parts(date_time).day_of_month; }
    static u8 hour(ZonedDateTime const& date_time) { return to_parts(date_time).hour; }
    static u8 minute(ZonedDateTime const& date_time) { return to_parts(date_time).minute; }
    static u8 second(ZonedDateTime const& date_time) { return to_parts(date_time).second; }
    static i64 millisecond(ZonedDateTime const& date_time) { return to_parts(date_time).nanosecond / 1'000'000; }
    static i64 nanosecond(ZonedDateTime const& date_time) { return to_parts(date_time).nanosecond; }
};

static_assert(IsCalendar<ISOCalendar>);

}
