/*
 * Copyright (c) 2022, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/Array.h>
#include <AK/StringView.h>
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
        TestData { "en"sv, Unicode::CalendarPatternStyle::ShortOffset, "UTC"sv, "GMT"sv },
        TestData { "en"sv, Unicode::CalendarPatternStyle::LongOffset, "UTC"sv, "GMT"sv },
        TestData { "en"sv, Unicode::CalendarPatternStyle::ShortGeneric, "UTC"sv, "GMT"sv },
        TestData { "en"sv, Unicode::CalendarPatternStyle::LongGeneric, "UTC"sv, "GMT"sv },

        TestData { "ar"sv, Unicode::CalendarPatternStyle::Long, "UTC"sv, "التوقيت العالمي المنسق"sv },
        TestData { "ar"sv, Unicode::CalendarPatternStyle::Short, "UTC"sv, "UTC"sv },
        TestData { "ar"sv, Unicode::CalendarPatternStyle::ShortOffset, "UTC"sv, "غرينتش"sv },
        TestData { "ar"sv, Unicode::CalendarPatternStyle::LongOffset, "UTC"sv, "غرينتش"sv },
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
