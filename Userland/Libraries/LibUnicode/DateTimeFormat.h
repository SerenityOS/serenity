/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Optional.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <LibUnicode/Forward.h>

namespace Unicode {

enum class HourCycle : u8 {
    H11,
    H12,
    H23,
    H24,
};

enum class CalendarPatternStyle : u8 {
    Narrow,
    Short,
    Long,
    Numeric,
    TwoDigit,
};

struct CalendarPattern {
    String pattern {};
    Optional<String> pattern12 {};

    // https://unicode.org/reports/tr35/tr35-dates.html#Calendar_Fields
    Optional<CalendarPatternStyle> era {};
    Optional<CalendarPatternStyle> year {};
    Optional<CalendarPatternStyle> month {};
    Optional<CalendarPatternStyle> weekday {};
    Optional<CalendarPatternStyle> day {};
    Optional<CalendarPatternStyle> day_period {};
    Optional<CalendarPatternStyle> hour {};
    Optional<CalendarPatternStyle> minute {};
    Optional<CalendarPatternStyle> second {};
    Optional<u8> fractional_second_digits {};
    Optional<CalendarPatternStyle> time_zone_name {};
};

enum class CalendarFormatType : u8 {
    Date,
    Time,
    DateTime,
};

struct CalendarFormat {
    CalendarPattern full_format {};
    CalendarPattern long_format {};
    CalendarPattern medium_format {};
    CalendarPattern short_format {};
};

HourCycle hour_cycle_from_string(StringView hour_cycle);
StringView hour_cycle_to_string(HourCycle hour_cycle);
CalendarPatternStyle calendar_pattern_style_from_string(StringView style);
StringView calendar_pattern_style_to_string(CalendarPatternStyle style);
Vector<Unicode::HourCycle> get_regional_hour_cycles(StringView locale);
Optional<Unicode::HourCycle> get_default_regional_hour_cycle(StringView locale);
Optional<CalendarFormat> get_calendar_format(StringView locale, StringView calendar, CalendarFormatType type);
Vector<CalendarPattern> get_calendar_available_formats(StringView locale, StringView calendar);

}
