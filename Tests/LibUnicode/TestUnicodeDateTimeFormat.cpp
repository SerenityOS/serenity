/*
 * Copyright (c) 2022, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/Array.h>
#include <AK/StringView.h>
#include <AK/Time.h>
#include <LibUnicode/DateTimeFormat.h>

TEST_CASE(time_zone_name)
{
    struct TestData {
        StringView locale;
        Unicode::CalendarPatternStyle style;
        StringView time_zone;
        StringView expected_result;
    };

    constexpr auto test_data = Array {
        TestData { "en"sv, Unicode::CalendarPatternStyle::Long, "UTC"sv, "Coordinated Universal Time"sv },
        TestData { "en"sv, Unicode::CalendarPatternStyle::Short, "UTC"sv, "UTC"sv },
        TestData { "en"sv, Unicode::CalendarPatternStyle::ShortGeneric, "UTC"sv, "GMT"sv },
        TestData { "en"sv, Unicode::CalendarPatternStyle::LongGeneric, "UTC"sv, "GMT"sv },

        TestData { "ar"sv, Unicode::CalendarPatternStyle::Long, "UTC"sv, "التوقيت العالمي المنسق"sv },
        TestData { "ar"sv, Unicode::CalendarPatternStyle::Short, "UTC"sv, "UTC"sv },
        TestData { "ar"sv, Unicode::CalendarPatternStyle::ShortGeneric, "UTC"sv, "غرينتش"sv },
        TestData { "ar"sv, Unicode::CalendarPatternStyle::LongGeneric, "UTC"sv, "غرينتش"sv },

        TestData { "en"sv, Unicode::CalendarPatternStyle::Long, "America/Los_Angeles"sv, "Pacific Daylight Time"sv },
        TestData { "en"sv, Unicode::CalendarPatternStyle::Short, "America/Los_Angeles"sv, "PDT"sv },

        TestData { "ar"sv, Unicode::CalendarPatternStyle::Long, "America/Los_Angeles"sv, "توقيت المحيط الهادي الصيفي"sv },
        // The "ar" locale does not have a short name for PDT. LibUnicode will need to fall back to GMT offset when we have that data.

        TestData { "en"sv, Unicode::CalendarPatternStyle::Long, "America/Vancouver"sv, "Pacific Daylight Time"sv },
        TestData { "en"sv, Unicode::CalendarPatternStyle::Short, "America/Vancouver"sv, "PDT"sv },

        TestData { "ar"sv, Unicode::CalendarPatternStyle::Long, "America/Vancouver"sv, "توقيت المحيط الهادي الصيفي"sv },
        // The "ar" locale does not have a short name for PDT. LibUnicode will need to fall back to GMT offset when we have that data.

        TestData { "en"sv, Unicode::CalendarPatternStyle::Long, "Europe/London"sv, "Greenwich Mean Time"sv },
        TestData { "en"sv, Unicode::CalendarPatternStyle::Short, "Europe/London"sv, "GMT"sv },

        TestData { "ar"sv, Unicode::CalendarPatternStyle::Long, "Europe/London"sv, "توقيت غرينتش"sv },
        // The "ar" locale does not have a short name for GMT. LibUnicode will need to fall back to GMT offset when we have that data.

        TestData { "en"sv, Unicode::CalendarPatternStyle::Long, "Africa/Accra"sv, "Greenwich Mean Time"sv },
        TestData { "en"sv, Unicode::CalendarPatternStyle::Short, "Africa/Accra"sv, "GMT"sv },

        TestData { "ar"sv, Unicode::CalendarPatternStyle::Long, "Africa/Accra"sv, "توقيت غرينتش"sv },
        // The "ar" locale does not have a short name for GMT. LibUnicode will need to fall back to GMT offset when we have that data.
    };

    for (auto const& test : test_data) {
        auto time_zone = Unicode::get_time_zone_name(test.locale, test.time_zone, test.style);
        VERIFY(time_zone.has_value());
        EXPECT_EQ(*time_zone, test.expected_result);
    }
}

TEST_CASE(format_time_zone_offset)
{
    constexpr auto jan_1_1833 = AK::Time::from_seconds(-4323283200); // Tuesday, January 1, 1833 12:00:00 AM
    constexpr auto jan_1_2022 = AK::Time::from_seconds(1640995200);  // Saturday, January 1, 2022 12:00:00 AM

    struct TestData {
        StringView locale;
        Unicode::CalendarPatternStyle style;
        AK::Time time;
        StringView time_zone;
        StringView expected_result;
    };

    constexpr auto test_data = Array {
        TestData { "en"sv, Unicode::CalendarPatternStyle::ShortOffset, {}, "UTC"sv, "GMT"sv },
        TestData { "en"sv, Unicode::CalendarPatternStyle::LongOffset, {}, "UTC"sv, "GMT"sv },

        TestData { "ar"sv, Unicode::CalendarPatternStyle::ShortOffset, {}, "UTC"sv, "غرينتش"sv },
        TestData { "ar"sv, Unicode::CalendarPatternStyle::LongOffset, {}, "UTC"sv, "غرينتش"sv },

        TestData { "en"sv, Unicode::CalendarPatternStyle::ShortOffset, jan_1_1833, "America/Los_Angeles"sv, "GMT-7:52:58"sv },
        TestData { "en"sv, Unicode::CalendarPatternStyle::ShortOffset, jan_1_2022, "America/Los_Angeles"sv, "GMT-8"sv },
        TestData { "en"sv, Unicode::CalendarPatternStyle::LongOffset, jan_1_1833, "America/Los_Angeles"sv, "GMT-07:52:58"sv },
        TestData { "en"sv, Unicode::CalendarPatternStyle::LongOffset, jan_1_2022, "America/Los_Angeles"sv, "GMT-08:00"sv },

        TestData { "ar"sv, Unicode::CalendarPatternStyle::ShortOffset, jan_1_1833, "America/Los_Angeles"sv, "غرينتش-٧:٥٢:٥٨"sv },
        TestData { "ar"sv, Unicode::CalendarPatternStyle::ShortOffset, jan_1_2022, "America/Los_Angeles"sv, "غرينتش-٨"sv },
        TestData { "ar"sv, Unicode::CalendarPatternStyle::LongOffset, jan_1_1833, "America/Los_Angeles"sv, "غرينتش-٠٧:٥٢:٥٨"sv },
        TestData { "ar"sv, Unicode::CalendarPatternStyle::LongOffset, jan_1_2022, "America/Los_Angeles"sv, "غرينتش-٠٨:٠٠"sv },

        TestData { "en"sv, Unicode::CalendarPatternStyle::ShortOffset, jan_1_1833, "Europe/London"sv, "GMT-0:01:15"sv },
        TestData { "en"sv, Unicode::CalendarPatternStyle::ShortOffset, jan_1_2022, "Europe/London"sv, "GMT"sv },
        TestData { "en"sv, Unicode::CalendarPatternStyle::LongOffset, jan_1_1833, "Europe/London"sv, "GMT-00:01:15"sv },
        TestData { "en"sv, Unicode::CalendarPatternStyle::LongOffset, jan_1_2022, "Europe/London"sv, "GMT"sv },

        TestData { "ar"sv, Unicode::CalendarPatternStyle::ShortOffset, jan_1_1833, "Europe/London"sv, "غرينتش-٠:٠١:١٥"sv },
        TestData { "ar"sv, Unicode::CalendarPatternStyle::ShortOffset, jan_1_2022, "Europe/London"sv, "غرينتش"sv },
        TestData { "ar"sv, Unicode::CalendarPatternStyle::LongOffset, jan_1_1833, "Europe/London"sv, "غرينتش-٠٠:٠١:١٥"sv },
        TestData { "ar"sv, Unicode::CalendarPatternStyle::LongOffset, jan_1_2022, "Europe/London"sv, "غرينتش"sv },

        TestData { "en"sv, Unicode::CalendarPatternStyle::ShortOffset, jan_1_1833, "Asia/Kathmandu"sv, "GMT+5:41:16"sv },
        TestData { "en"sv, Unicode::CalendarPatternStyle::ShortOffset, jan_1_2022, "Asia/Kathmandu"sv, "GMT+5:45"sv },
        TestData { "en"sv, Unicode::CalendarPatternStyle::LongOffset, jan_1_1833, "Asia/Kathmandu"sv, "GMT+05:41:16"sv },
        TestData { "en"sv, Unicode::CalendarPatternStyle::LongOffset, jan_1_2022, "Asia/Kathmandu"sv, "GMT+05:45"sv },

        TestData { "ar"sv, Unicode::CalendarPatternStyle::ShortOffset, jan_1_1833, "Asia/Kathmandu"sv, "غرينتش+٥:٤١:١٦"sv },
        TestData { "ar"sv, Unicode::CalendarPatternStyle::ShortOffset, jan_1_2022, "Asia/Kathmandu"sv, "غرينتش+٥:٤٥"sv },
        TestData { "ar"sv, Unicode::CalendarPatternStyle::LongOffset, jan_1_1833, "Asia/Kathmandu"sv, "غرينتش+٠٥:٤١:١٦"sv },
        TestData { "ar"sv, Unicode::CalendarPatternStyle::LongOffset, jan_1_2022, "Asia/Kathmandu"sv, "غرينتش+٠٥:٤٥"sv },
    };

    for (auto const& test : test_data) {
        auto time_zone = Unicode::format_time_zone(test.locale, test.time_zone, test.style, test.time);
        EXPECT_EQ(time_zone, test.expected_result);
    }
}
