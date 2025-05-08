/*
 * Copyright (c) 2022, Undefine <undefine@undefine.pl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/DOSPackedTime.h>
#include <AK/Error.h>

namespace AK {

constexpr auto seconds_per_day = 86'400;

UnixDateTime time_from_packed_dos(DOSPackedDate date, DOSPackedTime time)
{
    if (date.value == 0)
        return UnixDateTime::from_unix_time_parts(first_dos_year, 1, 1, 0, 0, 0, 0);

    return UnixDateTime::from_unix_time_parts(first_dos_year + date.year, date.month, date.day, time.hour, time.minute, time.second * 2, 0);
}

DOSPackedDate to_packed_dos_date(unsigned year, unsigned month, unsigned day)
{
    DOSPackedDate date;
    date.year = year - first_dos_year;
    date.month = month;
    date.day = day;

    return date;
}

DOSPackedTime to_packed_dos_time(unsigned hour, unsigned minute, unsigned second)
{
    DOSPackedTime time;
    time.hour = hour;
    time.minute = minute;
    time.second = second / 2;

    return time;
}

// FIXME: Improve these naive algorithms.
ErrorOr<DOSPackedDate> to_packed_dos_date(UnixDateTime const& unix_date_time)
{
    auto truncated_seconds_since_epoch = unix_date_time.truncated_seconds_since_epoch();
    if (truncated_seconds_since_epoch < first_dos_representable_unix_timestamp || truncated_seconds_since_epoch > static_cast<i64>(last_dos_representable_unix_timestamp))
        return EINVAL;

    auto years_since_epoch = 0;
    auto leftover_seconds = truncated_seconds_since_epoch;
    while (leftover_seconds >= days_in_year(years_since_epoch + 1970) * seconds_per_day) {
        leftover_seconds -= days_in_year(years_since_epoch + 1970) * seconds_per_day;
        ++years_since_epoch;
    }

    auto month_of_year = 1;
    for (; month_of_year <= 12; ++month_of_year) {
        auto seconds_in_current_month = days_in_month(years_since_epoch + 1970, month_of_year) * seconds_per_day;
        if (leftover_seconds < seconds_in_current_month)
            break;
        leftover_seconds -= seconds_in_current_month;
    }

    VERIFY(month_of_year <= 12);

    auto day = leftover_seconds / seconds_per_day + 1;

    return to_packed_dos_date(years_since_epoch + 1970, month_of_year, day);
}

ErrorOr<DOSPackedTime> to_packed_dos_time(UnixDateTime const& unix_date_time)
{
    constexpr auto seconds_per_hour = 3'600;
    constexpr auto seconds_per_minute = 60;

    auto date = TRY(to_packed_dos_date(unix_date_time));

    auto truncated_seconds_since_epoch = unix_date_time.truncated_seconds_since_epoch();
    auto leftover_seconds = truncated_seconds_since_epoch - days_since_epoch(date.year + first_dos_year, date.month, date.day) * seconds_per_day;

    auto hours = leftover_seconds / seconds_per_hour;
    leftover_seconds -= hours * seconds_per_hour;

    auto minutes = leftover_seconds / seconds_per_minute;
    leftover_seconds -= minutes * seconds_per_minute;

    return to_packed_dos_time(hours, minutes, leftover_seconds);
}

}
