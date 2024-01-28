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
#include <LibDateTime/Format.h>
#include <LibDateTime/ISOCalendar.h>
#include <LibDateTime/ZonedDateTime.h>
#include <LibTimeZone/TimeZoneData.h>

TEST_CASE(calendar_formats)
{
    EXPECT_EQ(TRY_OR_FAIL(DateTime::ISOCalendar::format(TRY_OR_FAIL(DateTime::ZonedDateTime::from_parts<DateTime::ISOCalendar>(
                  { .year = 2015, .month = 6, .day_of_month = 10, .hour = 18, .minute = 2, .second = 49 }, TimeZone::TimeZone::UTC)))),
        "2015-06-10T18:02:49.000000000+0000"_string);

    EXPECT_EQ(TRY_OR_FAIL(DateTime::ISOCalendar::format(TRY_OR_FAIL(DateTime::ZonedDateTime::from_parts<DateTime::ISOCalendar>(
                  { .nanosecond = 100203, .year = 2004, .time_zone_offset_seconds = 2 * 60 * 60, .month = 6, .day_of_month = 10, .hour = 18, .minute = 2, .second = 49 }, TimeZone::TimeZone::Europe_Amsterdam)))),
        "2004-06-10T18:02:49.000100203+0200"_string);
}

TEST_CASE(standard_format_strings)
{
    EXPECT_EQ(TRY_OR_FAIL(TRY_OR_FAIL(DateTime::ZonedDateTime::from_parts<DateTime::ISOCalendar>(
                                          { .year = 2015, .month = 6, .day_of_month = 10, .hour = 18, .minute = 2, .second = 49 }, TimeZone::TimeZone::UTC))
                              .format(DateTime::ISO8601_FULL_FORMAT)),
        "2015-06-10T18:02:49.000000000+0000"_string);

    EXPECT_EQ(TRY_OR_FAIL(TRY_OR_FAIL(DateTime::ZonedDateTime::from_parts<DateTime::ISOCalendar>(
                                          { .year = 2015, .month = 6, .day_of_month = 10, .hour = 18, .minute = 2, .second = 49 }, TimeZone::TimeZone::UTC))
                              .format(DateTime::ISO8601_SHORT_FORMAT)),
        "2015-06-10T18:02:49+0000"_string);

    EXPECT_EQ(TRY_OR_FAIL(TRY_OR_FAIL(DateTime::ZonedDateTime::from_parts<DateTime::ISOCalendar>(
                                          { .year = 2015, .month = 6, .day_of_month = 10, .hour = 18, .minute = 2, .second = 49 }, TimeZone::TimeZone::UTC))
                              .format(DateTime::ISO8601_DATE_FORMAT)),
        "2015-06-10"_string);

    EXPECT_EQ(TRY_OR_FAIL(TRY_OR_FAIL(DateTime::ZonedDateTime::from_parts<DateTime::ISOCalendar>(
                                          { .year = 2015, .month = 6, .day_of_month = 10, .hour = 18, .minute = 2, .second = 49 }, TimeZone::TimeZone::UTC))
                              .format(DateTime::ISO8601_SHORT_TIME_FORMAT)),
        "18:02:49+0000"_string);
}

TEST_CASE(unusual_formats)
{
    EXPECT_EQ(TRY_OR_FAIL(TRY_OR_FAIL(DateTime::ZonedDateTime::from_parts<DateTime::ISOCalendar>(
                                          { .year = 2023, .month = 12, .day_of_month = 31 }, TimeZone::TimeZone::UTC))
                              .format("{Y:1}年{m:1}月{d:1}日"sv)),
        "2023年12月31日"_string);

    EXPECT_EQ(TRY_OR_FAIL(TRY_OR_FAIL(DateTime::ZonedDateTime::from_parts<DateTime::ISOCalendar>(
                                          { .year = 2006, .month = 2, .day_of_month = 7 }, TimeZone::TimeZone::UTC))
                              .format("{d:1}.{m:1}.{Y:1}"sv)),
        "7.2.2006"_string);
}

TEST_CASE(string_formatting)
{
    EXPECT_EQ(TRY_OR_FAIL(String::formatted("before: {:{Y}-{m}-{d}} and after",
                  TRY_OR_FAIL(DateTime::ZonedDateTime::from_parts<DateTime::ISOCalendar>(
                      { .year = 2015, .month = 6, .day_of_month = 10, .hour = 18, .minute = 2, .second = 49 }, TimeZone::TimeZone::UTC)))),
        "before: 2015-06-10 and after"_string);
}

TEST_CASE(single_fields)
{
    EXPECT_EQ(TRY_OR_FAIL(TRY_OR_FAIL(DateTime::ZonedDateTime::from_parts<DateTime::ISOCalendar>(
                                          { .year = 2015, .month = 6, .day_of_month = 10, .hour = 18, .minute = 2, .second = 49 }, TimeZone::TimeZone::UTC))
                              .format("{Y:08}"sv)),
        "00002015"_string);
    EXPECT_EQ(TRY_OR_FAIL(TRY_OR_FAIL(DateTime::ZonedDateTime::from_parts<DateTime::ISOCalendar>(
                                          { .year = 2015, .month = 6, .day_of_month = 10, .hour = 18, .minute = 2, .second = 49 }, TimeZone::TimeZone::UTC))
                              .format("{m:+05}"sv)),
        "+00006"_string);
    EXPECT_EQ(TRY_OR_FAIL(TRY_OR_FAIL(DateTime::ZonedDateTime::from_parts<DateTime::ISOCalendar>(
                                          { .year = 2015, .month = 6, .day_of_month = 10, .hour = 18, .minute = 2, .second = 49 }, TimeZone::TimeZone::UTC))
                              .format("{d:1}"sv)),
        "10"_string);
    EXPECT_EQ(TRY_OR_FAIL(TRY_OR_FAIL(DateTime::ZonedDateTime::from_parts<DateTime::ISOCalendar>(
                                          { .year = 2015, .month = 6, .day_of_month = 10, .hour = 18, .minute = 2, .second = 49 }, TimeZone::TimeZone::UTC))
                              .format("{H}"sv)),
        "18"_string);
    EXPECT_EQ(TRY_OR_FAIL(TRY_OR_FAIL(DateTime::ZonedDateTime::from_parts<DateTime::ISOCalendar>(
                                          { .year = 2015, .month = 6, .day_of_month = 10, .hour = 18, .minute = 2, .second = 49 }, TimeZone::TimeZone::UTC))
                              .format("{I:1}"sv)),
        "6"_string);
    EXPECT_EQ(TRY_OR_FAIL(TRY_OR_FAIL(DateTime::ZonedDateTime::from_parts<DateTime::ISOCalendar>(
                                          { .year = 2015, .month = 6, .day_of_month = 10, .hour = 0, .minute = 2, .second = 49 }, TimeZone::TimeZone::UTC))
                              .format("{I:1}"sv)),
        "12"_string);
    EXPECT_EQ(TRY_OR_FAIL(TRY_OR_FAIL(DateTime::ZonedDateTime::from_parts<DateTime::ISOCalendar>(
                                          { .year = 2015, .month = 6, .day_of_month = 10, .hour = 18, .minute = 2, .second = 49 }, TimeZone::TimeZone::UTC))
                              .format("{M: >3}"sv)),
        "  2"_string);
    EXPECT_EQ(TRY_OR_FAIL(TRY_OR_FAIL(DateTime::ZonedDateTime::from_parts<DateTime::ISOCalendar>(
                                          { .year = 2015, .month = 6, .day_of_month = 10, .hour = 18, .minute = 2, .second = 49 }, TimeZone::TimeZone::UTC))
                              .format("{S:03}"sv)),
        "049"_string);
    EXPECT_EQ(TRY_OR_FAIL(TRY_OR_FAIL(DateTime::ZonedDateTime::from_parts<DateTime::ISOCalendar>(
                                          { .nanosecond = 2000 }, TimeZone::TimeZone::UTC))
                              .format("{f}"sv)),
        "000002000"_string);
    EXPECT_EQ(TRY_OR_FAIL(TRY_OR_FAIL(DateTime::ZonedDateTime::from_parts<DateTime::ISOCalendar>(
                                          {}, TimeZone::TimeZone::UTC))
                              .format("{Z}"sv)),
        "Etc/UTC"_string);
    EXPECT_EQ(TRY_OR_FAIL(TRY_OR_FAIL(DateTime::ZonedDateTime::from_parts<DateTime::ISOCalendar>(
                                          {}, TimeZone::TimeZone::Africa_Brazzaville))
                              .format("{z}"sv)),
        "+0100"_string);
    EXPECT_EQ(TRY_OR_FAIL(TRY_OR_FAIL(DateTime::ZonedDateTime::from_parts<DateTime::ISOCalendar>(
                                          {}, TimeZone::TimeZone::Africa_Brazzaville))
                              .format("{0z}"sv)),
        "+01:00"_string);
}
