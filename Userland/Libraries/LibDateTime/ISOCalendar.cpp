/*
 * Copyright (c) 2024, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibDateTime/ISOCalendar.h>

namespace DateTime {

constexpr i64 seconds_per_day = 60L * 60L * 24L;

static ISOCalendar::OutputParts to_parts_impl(Duration offset)
{
    auto year = 1970;
    while (offset >= Duration::from_seconds(days_in_year(year) * seconds_per_day)) {
        offset -= Duration::from_seconds(days_in_year(year) * seconds_per_day);
        ++year;
    }
    while (offset < Duration::zero()) {
        offset += Duration::from_seconds(days_in_year(year - 1) * seconds_per_day);
        --year;
    }

    VERIFY(offset >= Duration::zero());

    ISOCalendar::OutputParts parts;
    parts.nanosecond = offset.nanoseconds_within_second();
    i64 days = offset.to_truncated_seconds() / seconds_per_day;
    i64 remaining = offset.to_truncated_seconds() % seconds_per_day;
    parts.second = remaining % 60;
    remaining /= 60;
    parts.minute = remaining % 60;
    parts.hour = remaining / 60;

    i64 month;
    for (month = 1; month < 12 && days >= days_in_month(year, month); ++month)
        days -= days_in_month(year, month);

    parts.day_of_month = days + 1;
    parts.month = month;
    parts.weekday = day_of_week(year, parts.month, parts.day_of_month) + 1;

    // There is no year 0; the year after 1 BCE (-1) is 1 CE (+1).
    if (year <= 0)
        --year;
    parts.year = year;

    return parts;
}

ISOCalendar::OutputParts ISOCalendar::to_parts(ZonedDateTime const& date_time)
{
    // FIXME: Leap seconds are not accounted for here, but this *would* be the place to handle them.
    auto own_epoch_offset = date_time.offset_to_utc_epoch();
    auto parts = to_parts_impl(own_epoch_offset);
    parts.time_zone_offset_seconds = date_time.offset_to_utc().seconds;
    return parts;
}

ISOCalendar::OutputParts ISOCalendar::to_parts(LocalDateTime const& date_time)
{
    return to_parts_impl(date_time.offset_to_local_epoch());
}

static ErrorOr<i64> timestamp_from_parts(ISOCalendar::InputParts const& input_parts)
{
    if (input_parts.hour > 23)
        return Error::from_string_view("hour out of range"sv);
    if (input_parts.minute > 59)
        return Error::from_string_view("minute out of range"sv);
    if (input_parts.second > 59)
        return Error::from_string_view("second out of range"sv);
    if (input_parts.nanosecond >= 1'000'000'000)
        return Error::from_string_view("nanosecond out of range"sv);
    if (input_parts.month < 1 || input_parts.month > 12)
        return Error::from_string_view("month out of range"sv);
    if (input_parts.year == 0)
        return Error::from_string_view("year is zero"sv);

    auto year = (input_parts.year < 0) ? input_parts.year + 1 : input_parts.year;

    if (input_parts.day_of_month < 1 || input_parts.day_of_month > days_in_month(year, input_parts.month))
        return Error::from_string_view("day of month out of range"sv);

    auto day_in_year = day_of_year(year, input_parts.month, input_parts.day_of_month);
    auto days_since_epoch = years_to_days_since_epoch(year) + day_in_year;
    auto zoned_timestamp = ((days_since_epoch * 24 + input_parts.hour) * 60 + input_parts.minute) * 60 + input_parts.second;
    return zoned_timestamp;
}

ErrorOr<ZonedDateTime> ISOCalendar::zoned_date_time_from_parts(InputParts const& input_parts, TimeZone::TimeZone const& time_zone)
{
    // Readjust into UTC by subtracting the constant offset.
    // FIXME: Does not account for leap seconds!
    auto unix_timestamp = TRY(timestamp_from_parts(input_parts)) - input_parts.time_zone_offset_seconds;

    return ZonedDateTime { CalendarBadge<ISOCalendar> {}, UnixDateTime::from_unix_timespec({
                                                              .tv_sec = unix_timestamp,
                                                              .tv_nsec = static_cast<long>(input_parts.nanosecond),
                                                          }),
        time_zone };
}

ErrorOr<LocalDateTime> ISOCalendar::local_date_time_from_parts(InputParts const& input_parts)
{
    // Time zone offsets don't make sense for local time.
    // This also prevents naive uses of the "parts" APIs to convert zoned to unzoned time objects.
    VERIFY(input_parts.time_zone_offset_seconds == 0);
    auto unix_timestamp = TRY(timestamp_from_parts(input_parts));

    return LocalDateTime { CalendarBadge<ISOCalendar> {}, UnixDateTime::from_unix_timespec({
                                                              .tv_sec = unix_timestamp,
                                                              .tv_nsec = static_cast<long>(input_parts.nanosecond),
                                                          }) };
}

ISOCalendar::OutputParts::operator InputParts() const
{
    return InputParts {
        .nanosecond = nanosecond,
        .year = year,
        .time_zone_offset_seconds = time_zone_offset_seconds,
        .month = month,
        .day_of_month = day_of_month,
        .hour = hour,
        .minute = minute,
        .second = second,
    };
}

u8 ISOCalendar::OutputParts::total_days_in_month() const
{
    // Correct back into contiguous years so the function below uses correct leap years.
    auto corrected_year = this->year;
    if (corrected_year < 0)
        ++corrected_year;
    return days_in_month(corrected_year, month);
}

ErrorOr<String> ISOCalendar::format(ZonedDateTime const& date_time)
{
    auto const parts = date_time.to_parts<ISOCalendar>();
    auto const timezone_offset_hours = parts.time_zone_offset_seconds / (60 * 60);
    auto const timezone_offset_minutes = AK::abs((parts.time_zone_offset_seconds / 60) % 60);
    return String::formatted("{:04}-{:02}-{:02}T{:02}:{:02}:{:02}.{:09}{:+02}{:02}", parts.year, parts.month, parts.day_of_month, parts.hour, parts.minute, parts.second, parts.nanosecond, timezone_offset_hours, timezone_offset_minutes);
}

ErrorOr<String> ISOCalendar::format(LocalDateTime const& date_time)
{
    auto const parts = date_time.to_parts<ISOCalendar>();
    return String::formatted("{:04}-{:02}-{:02}T{:02}:{:02}:{:02}.{:09}", parts.year, parts.month, parts.day_of_month, parts.hour, parts.minute, parts.second, parts.nanosecond);
}

ErrorOr<LocalDateTime> ISOCalendar::with_time(LocalDateTime const& date_time, u8 hour, u8 minute, u8 second)
{
    auto parts = to_parts(date_time);
    parts.hour = hour;
    parts.minute = minute;
    parts.second = second;
    return local_date_time_from_parts(InputParts(parts));
}

ErrorOr<LocalDateTime> ISOCalendar::with_date(LocalDateTime const& date_time, i32 year, u8 month, u8 day_of_month)
{
    auto parts = to_parts(date_time);
    parts.year = year;
    parts.month = month;
    parts.day_of_month = day_of_month;
    return local_date_time_from_parts(InputParts(parts));
}

}
