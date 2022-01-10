/*
 * Copyright (c) 2022, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/StringView.h>
#include <LibTimeZone/TimeZone.h>

#if ENABLE_TIME_ZONE_DATA

#    include <LibTimeZone/TimeZoneData.h>

TEST_CASE(time_zone_from_string)
{
    EXPECT_EQ(TimeZone::time_zone_from_string("America/New_York"sv), TimeZone::TimeZone::America_New_York);
    EXPECT_EQ(TimeZone::time_zone_from_string("Europe/Paris"sv), TimeZone::TimeZone::Europe_Paris);
    EXPECT_EQ(TimeZone::time_zone_from_string("Etc/GMT+2"sv), TimeZone::TimeZone::Etc_GMT_Ahead_2);
    EXPECT_EQ(TimeZone::time_zone_from_string("Etc/GMT-5"sv), TimeZone::TimeZone::Etc_GMT_Behind_5);

    EXPECT(!TimeZone::time_zone_from_string("I don't exist"sv).has_value());
}

TEST_CASE(time_zone_from_string_link)
{
    auto test_link = [](auto tz1, auto tz2) {
        auto result1 = TimeZone::time_zone_from_string(tz1);
        EXPECT(result1.has_value());

        auto result2 = TimeZone::time_zone_from_string(tz2);
        EXPECT(result2.has_value());

        EXPECT_EQ(*result1, *result2);
    };

    test_link("America/New_York"sv, "US/Eastern"sv);

    test_link("Etc/GMT"sv, "GMT"sv);
    test_link("Etc/GMT+0"sv, "GMT"sv);
    test_link("Etc/GMT-0"sv, "GMT"sv);

    test_link("Etc/UTC"sv, "UTC"sv);
    test_link("Etc/Universal"sv, "UTC"sv);
    test_link("Universal"sv, "UTC"sv);
}

#endif
