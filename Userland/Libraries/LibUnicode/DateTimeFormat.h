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

enum class Era : u8 {
    BC,
    AD,
};

enum class Month : u8 {
    January,
    February,
    March,
    April,
    May,
    June,
    July,
    August,
    September,
    October,
    November,
    December,
};

enum class Weekday : u8 {
    Sunday,
    Monday,
    Tuesday,
    Wednesday,
    Thursday,
    Friday,
    Saturday,
};

enum class DayPeriod : u8 {
    AM,
    PM,
    Morning1,
    Morning2,
    Afternoon1,
    Afternoon2,
    Evening1,
    Evening2,
    Night1,
    Night2,
};

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
    enum class Field {
        Era,
        Year,
        Month,
        Weekday,
        Day,
        DayPeriod,
        Hour,
        Minute,
        Second,
        FractionalSecondDigits,
        TimeZoneName,
    };

    template<typename Callback>
    void for_each_calendar_field_zipped_with(CalendarPattern const& other, Callback&& callback)
    {
        callback(era, other.era, Field::Era);
        callback(year, other.year, Field::Year);
        callback(month, other.month, Field::Month);
        callback(weekday, other.weekday, Field::Weekday);
        callback(day, other.day, Field::Day);
        callback(day_period, other.day_period, Field::DayPeriod);
        callback(hour, other.hour, Field::Hour);
        callback(minute, other.minute, Field::Minute);
        callback(second, other.second, Field::Second);
        callback(fractional_second_digits, other.fractional_second_digits, Field::FractionalSecondDigits);
        callback(time_zone_name, other.time_zone_name, Field::TimeZoneName);
    }

    String skeleton {};
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

struct CalendarRangePattern : public CalendarPattern {
    enum class Field {
        Era,
        Year,
        Month,
        Day,
        AmPm,
        DayPeriod,
        Hour,
        Minute,
        Second,
        FractionalSecondDigits,
    };

    Optional<Field> field {};
    String start_range {};
    StringView separator {};
    String end_range {};
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

enum class CalendarSymbol : u8 {
    DayPeriod,
    Era,
    Month,
    Weekday,
};

HourCycle hour_cycle_from_string(StringView hour_cycle);
StringView hour_cycle_to_string(HourCycle hour_cycle);
CalendarPatternStyle calendar_pattern_style_from_string(StringView style);
StringView calendar_pattern_style_to_string(CalendarPatternStyle style);
Vector<Unicode::HourCycle> get_regional_hour_cycles(StringView locale);
Optional<Unicode::HourCycle> get_default_regional_hour_cycle(StringView locale);
String combine_skeletons(StringView first, StringView second);
Optional<CalendarFormat> get_calendar_format(StringView locale, StringView calendar, CalendarFormatType type);
Vector<CalendarPattern> get_calendar_available_formats(StringView locale, StringView calendar);
Optional<Unicode::CalendarRangePattern> get_calendar_default_range_format(StringView locale, StringView calendar);
Vector<Unicode::CalendarRangePattern> get_calendar_range_formats(StringView locale, StringView calendar, StringView skeleton);
Vector<Unicode::CalendarRangePattern> get_calendar_range12_formats(StringView locale, StringView calendar, StringView skeleton);
Optional<StringView> get_calendar_era_symbol(StringView locale, StringView calendar, CalendarPatternStyle style, Unicode::Era value);
Optional<StringView> get_calendar_month_symbol(StringView locale, StringView calendar, CalendarPatternStyle style, Unicode::Month value);
Optional<StringView> get_calendar_weekday_symbol(StringView locale, StringView calendar, CalendarPatternStyle style, Unicode::Weekday value);
Optional<StringView> get_calendar_day_period_symbol(StringView locale, StringView calendar, CalendarPatternStyle style, Unicode::DayPeriod value);
Optional<StringView> get_calendar_day_period_symbol_for_hour(StringView locale, StringView calendar, CalendarPatternStyle style, u8 hour);
Optional<StringView> get_time_zone_name(StringView locale, StringView time_zone, CalendarPatternStyle style);

}
