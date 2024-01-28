/*
 * Copyright (c) 2024, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/Macros.h>
#include <LibTest/TestCase.h>

#include <AK/Optional.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <LibDateTime/ISOCalendar.h>
#include <LibDateTime/ZonedDateTime.h>
#include <LibTimeZone/TimeZoneData.h>

TEST_CASE(basic)
{
    auto now_in_zone = DateTime::ZonedDateTime::now_in(TimeZone::TimeZone::UTC).offset_to_utc_epoch();
    auto now_unix = UnixDateTime::now().offset_to_epoch();
    // NOTE: This will break as soon as one of these two happen:
    //       - More than 100 leap seconds since 1970 exist. (Currently there are less than 30.)
    //       - The above two lines of code are more than 100 seconds of execution apart.
    EXPECT_EQ(now_in_zone.to_seconds() / 100, now_unix.to_seconds() / 100);

    auto now_in_unix = DateTime::ZonedDateTime::now_in(TimeZone::TimeZone::UTC);
    EXPECT_EQ(now_in_unix.time_zone(), TimeZone::TimeZone::UTC);
    EXPECT_EQ(now_in_unix.offset_to_utc().seconds, 0);
}

TEST_CASE(iso_round_trip)
{
#define CHECK_ROUND_TRIP(expression)                                                                                                                                                     \
    {                                                                                                                                                                                    \
        auto value = (expression);                                                                                                                                                       \
        EXPECT_EQ(TRY_OR_FAIL(DateTime::ISOCalendar::zoned_date_time_from_parts(DateTime::ISOCalendar::InputParts(value.to_parts<DateTime::ISOCalendar>()), value.time_zone())), value); \
    }

    CHECK_ROUND_TRIP(DateTime::ZonedDateTime::now());
    CHECK_ROUND_TRIP(TRY_OR_FAIL(DateTime::ZonedDateTime::from_parts<DateTime::ISOCalendar>({}, TimeZone::TimeZone::UTC)));
    CHECK_ROUND_TRIP(TRY_OR_FAIL(DateTime::ZonedDateTime::from_parts<DateTime::ISOCalendar>({ .year = 1900, .month = 12, .day_of_month = 31 }, TimeZone::TimeZone::UTC)));
    CHECK_ROUND_TRIP(TRY_OR_FAIL(DateTime::ZonedDateTime::from_parts<DateTime::ISOCalendar>({ .year = 1, .month = 1, .day_of_month = 3 }, TimeZone::TimeZone::UTC)));
    CHECK_ROUND_TRIP(TRY_OR_FAIL(DateTime::ZonedDateTime::from_parts<DateTime::ISOCalendar>({ .year = -1, .month = 4, .day_of_month = 7 }, TimeZone::TimeZone::UTC)));
    CHECK_ROUND_TRIP(TRY_OR_FAIL(DateTime::ZonedDateTime::from_parts<DateTime::ISOCalendar>({ .year = -30, .month = 11, .day_of_month = 1 }, TimeZone::TimeZone::UTC)));
    // There never was a 6th of October 1582. The switch from Julian to Gregorian calendar, at least in Catholic regions like Italy, meant that the day after the 4th was the 15th.
    // However, the ISO calendar retroactively uses the Gregorian calendar even before it was historically introduced. Therefore, this should work.
    // Also, this point in time has a funky timezone offset since Rome used local time before 1866.
    CHECK_ROUND_TRIP(TRY_OR_FAIL(DateTime::ZonedDateTime::from_parts<DateTime::ISOCalendar>({ .year = 1582, .month = 10, .day_of_month = 6 }, TimeZone::TimeZone::Europe_Rome)));
    CHECK_ROUND_TRIP(TRY_OR_FAIL(DateTime::ZonedDateTime::from_parts<DateTime::ISOCalendar>({ .year = 2000, .month = 2, .day_of_month = 29 }, TimeZone::TimeZone::Asia_Calcutta)));
    CHECK_ROUND_TRIP(TRY_OR_FAIL(DateTime::ZonedDateTime::from_parts<DateTime::ISOCalendar>({ .year = 1900, .month = 2, .day_of_month = 28 }, TimeZone::TimeZone::Asia_Kabul)));
    CHECK_ROUND_TRIP(TRY_OR_FAIL(DateTime::ZonedDateTime::from_parts<DateTime::ISOCalendar>({ .year = 2038, .month = 1, .day_of_month = 19, .hour = 3, .minute = 14, .second = 8 }, TimeZone::TimeZone::UTC)));
}

TEST_CASE(invalid_iso_date)
{
    // 1900 does not have a leap year
    EXPECT(DateTime::ZonedDateTime::from_parts<DateTime::ISOCalendar>({ .year = 1986, .month = 0, .day_of_month = 29 }, TimeZone::TimeZone::Asia_Kabul).is_error());
    EXPECT(DateTime::ZonedDateTime::from_parts<DateTime::ISOCalendar>({ .year = 1900, .month = 13, .day_of_month = 29 }, TimeZone::TimeZone::Europe_Jersey).is_error());
    EXPECT(DateTime::ZonedDateTime::from_parts<DateTime::ISOCalendar>({ .year = 1900, .month = 4, .day_of_month = 31 }, TimeZone::TimeZone::Asia_Kuwait).is_error());
    EXPECT(DateTime::ZonedDateTime::from_parts<DateTime::ISOCalendar>({ .year = 1900, .month = 2, .day_of_month = 2, .hour = 24 }, TimeZone::TimeZone::Asia_Kabul).is_error());
    EXPECT(DateTime::ZonedDateTime::from_parts<DateTime::ISOCalendar>({ .year = 1900, .month = 2, .day_of_month = 2, .minute = 60 }, TimeZone::TimeZone::Asia_Vladivostok).is_error());
    EXPECT(DateTime::ZonedDateTime::from_parts<DateTime::ISOCalendar>({ .year = 1900, .month = 2, .day_of_month = 2, .second = 60 }, TimeZone::TimeZone::Europe_Bucharest).is_error());
    EXPECT(DateTime::ZonedDateTime::from_parts<DateTime::ISOCalendar>({ .nanosecond = 2'000'000'000, .year = 1900, .month = 2, .day_of_month = 2 }, TimeZone::TimeZone::Europe_Belgrade).is_error());
}
