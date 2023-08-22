/*
 * Copyright (c) 2021-2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Optional.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <AK/Time.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <LibLocale/Forward.h>
#include <LibTimeZone/TimeZone.h>

namespace Locale {

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
    Noon,
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
    ShortOffset,
    LongOffset,
    ShortGeneric,
    LongGeneric,
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
    Optional<HourCycle> hour_cycle {};

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

struct TimeZoneFormat {
    StringView symbol_ahead_sign {};
    StringView symbol_ahead_separator {};

    StringView symbol_behind_sign {};
    StringView symbol_behind_separator {};

    StringView gmt_format {};
    StringView gmt_zero_format {};
};

HourCycle hour_cycle_from_string(StringView hour_cycle);
StringView hour_cycle_to_string(HourCycle hour_cycle);

CalendarPatternStyle calendar_pattern_style_from_string(StringView style);
StringView calendar_pattern_style_to_string(CalendarPatternStyle style);

Optional<HourCycleRegion> hour_cycle_region_from_string(StringView hour_cycle_region);
Vector<HourCycle> get_regional_hour_cycles(StringView region);
Vector<HourCycle> get_locale_hour_cycles(StringView locale);
Optional<HourCycle> get_default_regional_hour_cycle(StringView locale);

Optional<MinimumDaysRegion> minimum_days_region_from_string(StringView minimum_days_region);
Optional<u8> get_regional_minimum_days(StringView region);
Optional<u8> get_locale_minimum_days(StringView locale);

Optional<FirstDayRegion> first_day_region_from_string(StringView first_day_region);
Optional<Weekday> get_regional_first_day(StringView region);
Optional<Weekday> get_locale_first_day(StringView locale);

Optional<WeekendStartRegion> weekend_start_region_from_string(StringView weekend_start_region);
Optional<Weekday> get_regional_weekend_start(StringView region);
Optional<Weekday> get_locale_weekend_start(StringView locale);

Optional<WeekendEndRegion> weekend_end_region_from_string(StringView weekend_end_region);
Optional<Weekday> get_regional_weekend_end(StringView region);
Optional<Weekday> get_locale_weekend_end(StringView locale);

String combine_skeletons(StringView first, StringView second);

Optional<CalendarFormat> get_calendar_date_format(StringView locale, StringView calendar);
Optional<CalendarFormat> get_calendar_time_format(StringView locale, StringView calendar);
Optional<CalendarFormat> get_calendar_date_time_format(StringView locale, StringView calendar);
Optional<CalendarFormat> get_calendar_format(StringView locale, StringView calendar, CalendarFormatType type);
Vector<CalendarPattern> get_calendar_available_formats(StringView locale, StringView calendar);
Optional<CalendarRangePattern> get_calendar_default_range_format(StringView locale, StringView calendar);
Vector<CalendarRangePattern> get_calendar_range_formats(StringView locale, StringView calendar, StringView skeleton);
Vector<CalendarRangePattern> get_calendar_range12_formats(StringView locale, StringView calendar, StringView skeleton);

Optional<StringView> get_calendar_era_symbol(StringView locale, StringView calendar, CalendarPatternStyle style, Era value);
Optional<StringView> get_calendar_month_symbol(StringView locale, StringView calendar, CalendarPatternStyle style, Month value);
Optional<StringView> get_calendar_weekday_symbol(StringView locale, StringView calendar, CalendarPatternStyle style, Weekday value);
Optional<StringView> get_calendar_day_period_symbol(StringView locale, StringView calendar, CalendarPatternStyle style, DayPeriod value);
Optional<StringView> get_calendar_day_period_symbol_for_hour(StringView locale, StringView calendar, CalendarPatternStyle style, u8 hour);

String format_time_zone(StringView locale, StringView time_zone, CalendarPatternStyle style, AK::UnixDateTime time);
Optional<StringView> get_time_zone_name(StringView locale, StringView time_zone, CalendarPatternStyle style, TimeZone::InDST in_dst);
Optional<TimeZoneFormat> get_time_zone_format(StringView locale);

}
