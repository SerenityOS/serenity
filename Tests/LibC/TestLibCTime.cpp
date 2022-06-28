/*
 * Copyright (c) 2021, Brian Gianforcaro <bgianf@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <LibTest/TestCase.h>
#include <time.h>

auto const expected_epoch = "Thu Jan  1 00:00:00 1970\n"sv;

class TimeZoneGuard {
public:
    TimeZoneGuard()
        : m_tz(getenv("TZ"))
    {
    }

    explicit TimeZoneGuard(char const* tz)
        : m_tz(getenv("TZ"))
    {
        setenv("TZ", tz, 1);
    }

    ~TimeZoneGuard()
    {
        if (m_tz)
            setenv("TZ", m_tz, 1);
        else
            unsetenv("TZ");
    }

private:
    char const* m_tz { nullptr };
};

TEST_CASE(asctime)
{
    TimeZoneGuard guard("UTC");

    time_t epoch = 0;
    auto result = asctime(localtime(&epoch));
    EXPECT_EQ(expected_epoch, StringView(result));
}

TEST_CASE(asctime_r)
{
    TimeZoneGuard guard("UTC");

    char buffer[26] {};
    time_t epoch = 0;
    auto result = asctime_r(localtime(&epoch), buffer);
    EXPECT_EQ(expected_epoch, StringView(result));
}

TEST_CASE(ctime)
{
    TimeZoneGuard guard("UTC");

    time_t epoch = 0;
    auto result = ctime(&epoch);

    EXPECT_EQ(expected_epoch, StringView(result));
}

TEST_CASE(ctime_r)
{
    TimeZoneGuard guard("UTC");

    char buffer[26] {};
    time_t epoch = 0;
    auto result = ctime_r(&epoch, buffer);

    EXPECT_EQ(expected_epoch, StringView(result));
}

TEST_CASE(tzset)
{
    TimeZoneGuard guard;

    auto set_tz = [](char const* tz) {
        setenv("TZ", tz, 1);
        tzset();
    };

    set_tz("UTC");
    EXPECT_EQ(timezone, 0);
    EXPECT_EQ(altzone, 0);
    EXPECT_EQ(daylight, 0);
    EXPECT_EQ(tzname[0], "UTC"sv);
    EXPECT_EQ(tzname[1], "UTC"sv);

    set_tz("America/New_York");
    EXPECT_EQ(timezone, 5 * 60 * 60);
    EXPECT_EQ(altzone, 4 * 60 * 60);
    EXPECT_EQ(daylight, 1);
    EXPECT_EQ(tzname[0], "EST"sv);
    EXPECT_EQ(tzname[1], "EDT"sv);

    set_tz("America/Phoenix");
    EXPECT_EQ(timezone, 7 * 60 * 60);
    EXPECT_EQ(altzone, 7 * 60 * 60);
    EXPECT_EQ(daylight, 0);
    EXPECT_EQ(tzname[0], "MST"sv);
    EXPECT_EQ(tzname[1], "MST"sv);

    set_tz("America/Asuncion");
    EXPECT_EQ(timezone, 4 * 60 * 60);
    EXPECT_EQ(altzone, 3 * 60 * 60);
    EXPECT_EQ(daylight, 1);
    EXPECT_EQ(tzname[0], "-04"sv);
    EXPECT_EQ(tzname[1], "-03"sv);

    set_tz("CET");
    EXPECT_EQ(timezone, -1 * 60 * 60);
    EXPECT_EQ(altzone, -2 * 60 * 60);
    EXPECT_EQ(daylight, 1);
    EXPECT_EQ(tzname[0], "CET"sv);
    EXPECT_EQ(tzname[1], "CEST"sv);
}

TEST_CASE(localtime)
{
    TimeZoneGuard guard("UTC");

    // Minimum valid: Thu Jan  1 00:00:00 -2147481748;
    time_t minimum_possible_epoch = -67768040609740800;
    auto minimum_tm = localtime(&minimum_possible_epoch);

    EXPECT_EQ(minimum_tm->tm_sec, 0);
    EXPECT_EQ(minimum_tm->tm_min, 0);
    EXPECT_EQ(minimum_tm->tm_hour, 0);
    EXPECT_EQ(minimum_tm->tm_mday, 1);
    EXPECT_EQ(minimum_tm->tm_mon, 0);
    EXPECT_EQ(minimum_tm->tm_year, -2147481748 - 1900);
    EXPECT_EQ(minimum_tm->tm_wday, 4);
    EXPECT_EQ(minimum_tm->tm_yday, 0);

    // Maximum allowed: Wed Dec 31 23:59:59 2147485547
    time_t max_possible_epoch = 67768036191676799;
    auto maximum_tm = localtime(&max_possible_epoch);

    EXPECT_EQ(maximum_tm->tm_sec, 59);
    EXPECT_EQ(maximum_tm->tm_min, 59);
    EXPECT_EQ(maximum_tm->tm_hour, 23);
    EXPECT_EQ(maximum_tm->tm_mday, 31);
    EXPECT_EQ(maximum_tm->tm_mon, 11);
    EXPECT_EQ(maximum_tm->tm_year, 2147485547 - 1900);
    EXPECT_EQ(maximum_tm->tm_wday, 3);
    EXPECT_EQ(maximum_tm->tm_yday, 364);
}
