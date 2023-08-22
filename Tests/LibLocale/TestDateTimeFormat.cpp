/*
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/Array.h>
#include <AK/StringView.h>
#include <AK/Time.h>
#include <LibLocale/DateTimeFormat.h>

TEST_CASE(time_zone_name)
{
    struct TestData {
        StringView locale;
        Locale::CalendarPatternStyle style;
        StringView time_zone;
        StringView expected_result;
    };

    constexpr auto test_data = Array {
        TestData { "en"sv, Locale::CalendarPatternStyle::Long, "UTC"sv, "Coordinated Universal Time"sv },
        TestData { "en"sv, Locale::CalendarPatternStyle::Short, "UTC"sv, "UTC"sv },
        TestData { "en"sv, Locale::CalendarPatternStyle::LongGeneric, "UTC"sv, "GMT"sv },
        TestData { "en"sv, Locale::CalendarPatternStyle::ShortGeneric, "UTC"sv, "GMT"sv },

        TestData { "ar"sv, Locale::CalendarPatternStyle::Long, "UTC"sv, "التوقيت العالمي المنسق"sv },
        TestData { "ar"sv, Locale::CalendarPatternStyle::Short, "UTC"sv, "UTC"sv },
        TestData { "ar"sv, Locale::CalendarPatternStyle::LongGeneric, "UTC"sv, "غرينتش"sv },
        TestData { "ar"sv, Locale::CalendarPatternStyle::ShortGeneric, "UTC"sv, "غرينتش"sv },

        TestData { "en"sv, Locale::CalendarPatternStyle::Long, "America/Los_Angeles"sv, "Pacific Standard Time"sv },
        TestData { "en"sv, Locale::CalendarPatternStyle::Short, "America/Los_Angeles"sv, "PST"sv },
        TestData { "en"sv, Locale::CalendarPatternStyle::LongGeneric, "America/Los_Angeles"sv, "Pacific Time"sv },
        TestData { "en"sv, Locale::CalendarPatternStyle::ShortGeneric, "America/Los_Angeles"sv, "PT"sv },

        TestData { "ar"sv, Locale::CalendarPatternStyle::Long, "America/Los_Angeles"sv, "توقيت المحيط الهادي الرسمي"sv },
        TestData { "ar"sv, Locale::CalendarPatternStyle::Short, "America/Los_Angeles"sv, "غرينتش-٨"sv },
        TestData { "ar"sv, Locale::CalendarPatternStyle::LongGeneric, "America/Los_Angeles"sv, "توقيت المحيط الهادي"sv },
        TestData { "ar"sv, Locale::CalendarPatternStyle::ShortGeneric, "America/Los_Angeles"sv, "غرينتش-٨"sv },

        TestData { "en"sv, Locale::CalendarPatternStyle::Long, "America/Vancouver"sv, "Pacific Standard Time"sv },
        TestData { "en"sv, Locale::CalendarPatternStyle::Short, "America/Vancouver"sv, "PST"sv },
        TestData { "en"sv, Locale::CalendarPatternStyle::LongGeneric, "America/Vancouver"sv, "Pacific Time"sv },
        TestData { "en"sv, Locale::CalendarPatternStyle::ShortGeneric, "America/Vancouver"sv, "PT"sv },

        TestData { "ar"sv, Locale::CalendarPatternStyle::Long, "America/Vancouver"sv, "توقيت المحيط الهادي الرسمي"sv },
        TestData { "ar"sv, Locale::CalendarPatternStyle::Short, "America/Vancouver"sv, "غرينتش-٨"sv },
        TestData { "ar"sv, Locale::CalendarPatternStyle::LongGeneric, "America/Vancouver"sv, "توقيت المحيط الهادي"sv },
        TestData { "ar"sv, Locale::CalendarPatternStyle::ShortGeneric, "America/Vancouver"sv, "غرينتش-٨"sv },

        TestData { "en"sv, Locale::CalendarPatternStyle::Long, "Europe/London"sv, "Greenwich Mean Time"sv },
        TestData { "en"sv, Locale::CalendarPatternStyle::Short, "Europe/London"sv, "GMT"sv },
        TestData { "en"sv, Locale::CalendarPatternStyle::LongGeneric, "Europe/London"sv, "GMT"sv },
        TestData { "en"sv, Locale::CalendarPatternStyle::ShortGeneric, "Europe/London"sv, "GMT"sv },

        TestData { "ar"sv, Locale::CalendarPatternStyle::Long, "Europe/London"sv, "توقيت غرينتش"sv },
        TestData { "ar"sv, Locale::CalendarPatternStyle::Short, "Europe/London"sv, "غرينتش"sv },
        TestData { "ar"sv, Locale::CalendarPatternStyle::LongGeneric, "Europe/London"sv, "غرينتش"sv },
        TestData { "ar"sv, Locale::CalendarPatternStyle::ShortGeneric, "Europe/London"sv, "غرينتش"sv },

        TestData { "en"sv, Locale::CalendarPatternStyle::Long, "Africa/Accra"sv, "Greenwich Mean Time"sv },
        TestData { "en"sv, Locale::CalendarPatternStyle::Short, "Africa/Accra"sv, "GMT"sv },
        TestData { "en"sv, Locale::CalendarPatternStyle::LongGeneric, "Africa/Accra"sv, "GMT"sv },
        TestData { "en"sv, Locale::CalendarPatternStyle::ShortGeneric, "Africa/Accra"sv, "GMT"sv },

        TestData { "ar"sv, Locale::CalendarPatternStyle::Long, "Africa/Accra"sv, "توقيت غرينتش"sv },
        TestData { "ar"sv, Locale::CalendarPatternStyle::Short, "Africa/Accra"sv, "غرينتش"sv },
        TestData { "ar"sv, Locale::CalendarPatternStyle::LongGeneric, "Africa/Accra"sv, "غرينتش"sv },
        TestData { "ar"sv, Locale::CalendarPatternStyle::ShortGeneric, "Africa/Accra"sv, "غرينتش"sv },
    };

    constexpr auto jan_1_2022 = AK::UnixDateTime::from_seconds_since_epoch(1640995200); // Saturday, January 1, 2022 12:00:00 AM

    for (auto const& test : test_data) {
        auto time_zone = Locale::format_time_zone(test.locale, test.time_zone, test.style, jan_1_2022);
        EXPECT_EQ(time_zone, test.expected_result);
    }
}

TEST_CASE(time_zone_name_dst)
{
    struct TestData {
        StringView locale;
        Locale::CalendarPatternStyle style;
        StringView time_zone;
        StringView expected_result;
    };

    constexpr auto test_data = Array {
        TestData { "en"sv, Locale::CalendarPatternStyle::Long, "UTC"sv, "Coordinated Universal Time"sv },
        TestData { "en"sv, Locale::CalendarPatternStyle::Short, "UTC"sv, "UTC"sv },

        TestData { "ar"sv, Locale::CalendarPatternStyle::Long, "UTC"sv, "التوقيت العالمي المنسق"sv },
        TestData { "ar"sv, Locale::CalendarPatternStyle::Short, "UTC"sv, "UTC"sv },

        TestData { "en"sv, Locale::CalendarPatternStyle::Long, "America/Los_Angeles"sv, "Pacific Daylight Time"sv },
        TestData { "en"sv, Locale::CalendarPatternStyle::Short, "America/Los_Angeles"sv, "PDT"sv },

        TestData { "ar"sv, Locale::CalendarPatternStyle::Long, "America/Los_Angeles"sv, "توقيت المحيط الهادي الصيفي"sv },
        TestData { "ar"sv, Locale::CalendarPatternStyle::Short, "America/Los_Angeles"sv, "غرينتش-٧"sv },

        TestData { "en"sv, Locale::CalendarPatternStyle::Long, "America/Vancouver"sv, "Pacific Daylight Time"sv },
        TestData { "en"sv, Locale::CalendarPatternStyle::Short, "America/Vancouver"sv, "PDT"sv },

        TestData { "ar"sv, Locale::CalendarPatternStyle::Long, "America/Vancouver"sv, "توقيت المحيط الهادي الصيفي"sv },
        TestData { "ar"sv, Locale::CalendarPatternStyle::Short, "America/Vancouver"sv, "غرينتش-٧"sv },

        // FIXME: This should be "British Summer Time", but the CLDR puts that one name in a section we aren't parsing.
        TestData { "en"sv, Locale::CalendarPatternStyle::Long, "Europe/London"sv, "GMT+01:00"sv },
        TestData { "en"sv, Locale::CalendarPatternStyle::Short, "Europe/London"sv, "GMT+1"sv },

        TestData { "ar"sv, Locale::CalendarPatternStyle::Long, "Europe/London"sv, "غرينتش+٠١:٠٠"sv },
        TestData { "ar"sv, Locale::CalendarPatternStyle::Short, "Europe/London"sv, "غرينتش+١"sv },

        TestData { "en"sv, Locale::CalendarPatternStyle::Long, "Africa/Accra"sv, "Greenwich Mean Time"sv },
        TestData { "en"sv, Locale::CalendarPatternStyle::Short, "Africa/Accra"sv, "GMT"sv },

        TestData { "ar"sv, Locale::CalendarPatternStyle::Long, "Africa/Accra"sv, "توقيت غرينتش"sv },
        TestData { "ar"sv, Locale::CalendarPatternStyle::Short, "Africa/Accra"sv, "غرينتش"sv },
    };

    constexpr auto sep_19_2022 = AK::UnixDateTime::from_seconds_since_epoch(1663553728); // Monday, September 19, 2022 2:15:28 AM

    for (auto const& test : test_data) {
        auto time_zone = Locale::format_time_zone(test.locale, test.time_zone, test.style, sep_19_2022);
        EXPECT_EQ(time_zone, test.expected_result);
    }
}

TEST_CASE(format_time_zone_offset)
{
    constexpr auto jan_1_1833 = AK::UnixDateTime::from_seconds_since_epoch(-4323283200); // Tuesday, January 1, 1833 12:00:00 AM
    constexpr auto jan_1_2022 = AK::UnixDateTime::from_seconds_since_epoch(1640995200);  // Saturday, January 1, 2022 12:00:00 AM

    struct TestData {
        StringView locale;
        Locale::CalendarPatternStyle style;
        AK::UnixDateTime time;
        StringView time_zone;
        StringView expected_result;
    };

    constexpr auto test_data = Array {
        TestData { "en"sv, Locale::CalendarPatternStyle::ShortOffset, {}, "UTC"sv, "GMT"sv },
        TestData { "en"sv, Locale::CalendarPatternStyle::LongOffset, {}, "UTC"sv, "GMT"sv },

        TestData { "ar"sv, Locale::CalendarPatternStyle::ShortOffset, {}, "UTC"sv, "غرينتش"sv },
        TestData { "ar"sv, Locale::CalendarPatternStyle::LongOffset, {}, "UTC"sv, "غرينتش"sv },

        TestData { "en"sv, Locale::CalendarPatternStyle::ShortOffset, jan_1_1833, "America/Los_Angeles"sv, "GMT-7:52:58"sv },
        TestData { "en"sv, Locale::CalendarPatternStyle::ShortOffset, jan_1_2022, "America/Los_Angeles"sv, "GMT-8"sv },
        TestData { "en"sv, Locale::CalendarPatternStyle::LongOffset, jan_1_1833, "America/Los_Angeles"sv, "GMT-07:52:58"sv },
        TestData { "en"sv, Locale::CalendarPatternStyle::LongOffset, jan_1_2022, "America/Los_Angeles"sv, "GMT-08:00"sv },

        TestData { "ar"sv, Locale::CalendarPatternStyle::ShortOffset, jan_1_1833, "America/Los_Angeles"sv, "غرينتش-٧:٥٢:٥٨"sv },
        TestData { "ar"sv, Locale::CalendarPatternStyle::ShortOffset, jan_1_2022, "America/Los_Angeles"sv, "غرينتش-٨"sv },
        TestData { "ar"sv, Locale::CalendarPatternStyle::LongOffset, jan_1_1833, "America/Los_Angeles"sv, "غرينتش-٠٧:٥٢:٥٨"sv },
        TestData { "ar"sv, Locale::CalendarPatternStyle::LongOffset, jan_1_2022, "America/Los_Angeles"sv, "غرينتش-٠٨:٠٠"sv },

        TestData { "en"sv, Locale::CalendarPatternStyle::ShortOffset, jan_1_1833, "Europe/London"sv, "GMT-0:01:15"sv },
        TestData { "en"sv, Locale::CalendarPatternStyle::ShortOffset, jan_1_2022, "Europe/London"sv, "GMT"sv },
        TestData { "en"sv, Locale::CalendarPatternStyle::LongOffset, jan_1_1833, "Europe/London"sv, "GMT-00:01:15"sv },
        TestData { "en"sv, Locale::CalendarPatternStyle::LongOffset, jan_1_2022, "Europe/London"sv, "GMT"sv },

        TestData { "ar"sv, Locale::CalendarPatternStyle::ShortOffset, jan_1_1833, "Europe/London"sv, "غرينتش-٠:٠١:١٥"sv },
        TestData { "ar"sv, Locale::CalendarPatternStyle::ShortOffset, jan_1_2022, "Europe/London"sv, "غرينتش"sv },
        TestData { "ar"sv, Locale::CalendarPatternStyle::LongOffset, jan_1_1833, "Europe/London"sv, "غرينتش-٠٠:٠١:١٥"sv },
        TestData { "ar"sv, Locale::CalendarPatternStyle::LongOffset, jan_1_2022, "Europe/London"sv, "غرينتش"sv },

        TestData { "en"sv, Locale::CalendarPatternStyle::ShortOffset, jan_1_1833, "Asia/Kathmandu"sv, "GMT+5:41:16"sv },
        TestData { "en"sv, Locale::CalendarPatternStyle::ShortOffset, jan_1_2022, "Asia/Kathmandu"sv, "GMT+5:45"sv },
        TestData { "en"sv, Locale::CalendarPatternStyle::LongOffset, jan_1_1833, "Asia/Kathmandu"sv, "GMT+05:41:16"sv },
        TestData { "en"sv, Locale::CalendarPatternStyle::LongOffset, jan_1_2022, "Asia/Kathmandu"sv, "GMT+05:45"sv },

        TestData { "ar"sv, Locale::CalendarPatternStyle::ShortOffset, jan_1_1833, "Asia/Kathmandu"sv, "غرينتش+٥:٤١:١٦"sv },
        TestData { "ar"sv, Locale::CalendarPatternStyle::ShortOffset, jan_1_2022, "Asia/Kathmandu"sv, "غرينتش+٥:٤٥"sv },
        TestData { "ar"sv, Locale::CalendarPatternStyle::LongOffset, jan_1_1833, "Asia/Kathmandu"sv, "غرينتش+٠٥:٤١:١٦"sv },
        TestData { "ar"sv, Locale::CalendarPatternStyle::LongOffset, jan_1_2022, "Asia/Kathmandu"sv, "غرينتش+٠٥:٤٥"sv },
    };

    for (auto const& test : test_data) {
        auto time_zone = Locale::format_time_zone(test.locale, test.time_zone, test.style, test.time);
        EXPECT_EQ(time_zone, test.expected_result);
    }
}
