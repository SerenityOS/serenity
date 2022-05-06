/*
 * Copyright (c) 2020-2022, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/NumericLimits.h>
#include <AK/StringBuilder.h>
#include <AK/Time.h>
#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/Date.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibTimeZone/TimeZone.h>
#include <time.h>

namespace JS {

Date* Date::create(GlobalObject& global_object, double date_value)
{
    return global_object.heap().allocate<Date>(global_object, date_value, *global_object.date_prototype());
}

Date::Date(double date_value, Object& prototype)
    : Object(prototype)
    , m_date_value(date_value)
{
}

String Date::iso_date_string() const
{
    int year = year_from_time(m_date_value);

    StringBuilder builder;
    if (year < 0)
        builder.appendff("-{:06}", -year);
    else if (year > 9999)
        builder.appendff("+{:06}", year);
    else
        builder.appendff("{:04}", year);
    builder.append('-');
    builder.appendff("{:02}", month_from_time(m_date_value) + 1);
    builder.append('-');
    builder.appendff("{:02}", date_from_time(m_date_value));
    builder.append('T');
    builder.appendff("{:02}", hour_from_time(m_date_value));
    builder.append(':');
    builder.appendff("{:02}", min_from_time(m_date_value));
    builder.append(':');
    builder.appendff("{:02}", sec_from_time(m_date_value));
    builder.append('.');
    builder.appendff("{:03}", ms_from_time(m_date_value));
    builder.append('Z');

    return builder.build();
}

// DayWithinYear(t), https://tc39.es/ecma262/#eqn-DayWithinYear
u16 day_within_year(double t)
{
    if (!Value(t).is_finite_number())
        return 0;

    // Day(t) - DayFromYear(YearFromTime(t))
    return static_cast<u16>(day(t) - day_from_year(year_from_time(t)));
}

// DateFromTime(t), https://tc39.es/ecma262/#sec-date-number
u8 date_from_time(double t)
{
    switch (month_from_time(t)) {
    // DayWithinYear(t) + 1𝔽 if MonthFromTime(t) = +0𝔽
    case 0:
        return day_within_year(t) + 1;
    // DayWithinYear(t) - 30𝔽 if MonthFromTime(t) = 1𝔽
    case 1:
        return day_within_year(t) - 30;
    // DayWithinYear(t) - 58𝔽 - InLeapYear(t) if MonthFromTime(t) = 2𝔽
    case 2:
        return day_within_year(t) - 58 - in_leap_year(t);
    // DayWithinYear(t) - 89𝔽 - InLeapYear(t) if MonthFromTime(t) = 3𝔽
    case 3:
        return day_within_year(t) - 89 - in_leap_year(t);
    // DayWithinYear(t) - 119𝔽 - InLeapYear(t) if MonthFromTime(t) = 4𝔽
    case 4:
        return day_within_year(t) - 119 - in_leap_year(t);
    // DayWithinYear(t) - 150𝔽 - InLeapYear(t) if MonthFromTime(t) = 5𝔽
    case 5:
        return day_within_year(t) - 150 - in_leap_year(t);
    // DayWithinYear(t) - 180𝔽 - InLeapYear(t) if MonthFromTime(t) = 6𝔽
    case 6:
        return day_within_year(t) - 180 - in_leap_year(t);
    // DayWithinYear(t) - 211𝔽 - InLeapYear(t) if MonthFromTime(t) = 7𝔽
    case 7:
        return day_within_year(t) - 211 - in_leap_year(t);
    // DayWithinYear(t) - 242𝔽 - InLeapYear(t) if MonthFromTime(t) = 8𝔽
    case 8:
        return day_within_year(t) - 242 - in_leap_year(t);
    // DayWithinYear(t) - 272𝔽 - InLeapYear(t) if MonthFromTime(t) = 9𝔽
    case 9:
        return day_within_year(t) - 272 - in_leap_year(t);
    // DayWithinYear(t) - 303𝔽 - InLeapYear(t) if MonthFromTime(t) = 10𝔽
    case 10:
        return day_within_year(t) - 303 - in_leap_year(t);
    // DayWithinYear(t) - 333𝔽 - InLeapYear(t) if MonthFromTime(t) = 11𝔽
    case 11:
        return day_within_year(t) - 333 - in_leap_year(t);
    default:
        VERIFY_NOT_REACHED();
    }
}

// DaysInYear(y), https://tc39.es/ecma262/#eqn-DaysInYear
u16 days_in_year(i32 y)
{
    // 365𝔽 if (ℝ(y) modulo 4) ≠ 0
    if (y % 4 != 0)
        return 365;
    // 366𝔽 if (ℝ(y) modulo 4) = 0 and (ℝ(y) modulo 100) ≠ 0
    if (y % 4 == 0 && y % 100 != 0)
        return 366;
    // 365𝔽 if (ℝ(y) modulo 100) = 0 and (ℝ(y) modulo 400) ≠ 0
    if (y % 100 == 0 && y % 400 != 0)
        return 365;
    // 366𝔽 if (ℝ(y) modulo 400) = 0
    if (y % 400 == 0)
        return 366;
    VERIFY_NOT_REACHED();
}

// DayFromYear(y), https://tc39.es/ecma262/#eqn-DaysFromYear
double day_from_year(i32 y)
{
    // 𝔽(365 × (ℝ(y) - 1970) + floor((ℝ(y) - 1969) / 4) - floor((ℝ(y) - 1901) / 100) + floor((ℝ(y) - 1601) / 400))
    return 365.0 * (y - 1970) + floor((y - 1969) / 4.0) - floor((y - 1901) / 100.0) + floor((y - 1601) / 400.0);
}

// TimeFromYear(y), https://tc39.es/ecma262/#eqn-TimeFromYear
double time_from_year(i32 y)
{
    // msPerDay × DayFromYear(y)
    return ms_per_day * day_from_year(y);
}

// YearFromTime(t), https://tc39.es/ecma262/#eqn-YearFromTime
i32 year_from_time(double t)
{
    // the largest integral Number y (closest to +∞) such that TimeFromYear(y) ≤ t
    if (!Value(t).is_finite_number())
        return NumericLimits<i32>::max();

    // Approximation using average number of milliseconds per year. We might have to adjust this guess afterwards.
    auto year = static_cast<i32>(t / (365.2425 * ms_per_day) + 1970);

    auto year_t = time_from_year(year);
    if (year_t > t)
        year--;
    else if (year_t + days_in_year(year) * ms_per_day <= t)
        year++;

    return year;
}

// InLeapYear(t), https://tc39.es/ecma262/#eqn-InLeapYear
bool in_leap_year(double t)
{
    // +0𝔽 if DaysInYear(YearFromTime(t)) = 365𝔽
    // 1𝔽 if DaysInYear(YearFromTime(t)) = 366𝔽
    return days_in_year(year_from_time(t)) == 366;
}

// MonthFromTime(t), https://tc39.es/ecma262/#eqn-MonthFromTime
u8 month_from_time(double t)
{
    auto in_leap_year = JS::in_leap_year(t);
    auto day_within_year = JS::day_within_year(t);

    // +0𝔽 if +0𝔽 ≤ DayWithinYear(t) < 31𝔽
    if (day_within_year < 31)
        return 0;
    // 1𝔽 if 31𝔽 ≤ DayWithinYear(t) < 59𝔽 + InLeapYear(t)
    if (31 <= day_within_year && day_within_year < 59 + in_leap_year)
        return 1;
    // 2𝔽 if 59𝔽 + InLeapYear(t) ≤ DayWithinYear(t) < 90𝔽 + InLeapYear(t)
    if (59 + in_leap_year <= day_within_year && day_within_year < 90 + in_leap_year)
        return 2;
    // 3𝔽 if 90𝔽 + InLeapYear(t) ≤ DayWithinYear(t) < 120𝔽 + InLeapYear(t)
    if (90 + in_leap_year <= day_within_year && day_within_year < 120 + in_leap_year)
        return 3;
    // 4𝔽 if 120𝔽 + InLeapYear(t) ≤ DayWithinYear(t) < 151𝔽 + InLeapYear(t)
    if (120 + in_leap_year <= day_within_year && day_within_year < 151 + in_leap_year)
        return 4;
    // 5𝔽 if 151𝔽 + InLeapYear(t) ≤ DayWithinYear(t) < 181𝔽 + InLeapYear(t)
    if (151 + in_leap_year <= day_within_year && day_within_year < 181 + in_leap_year)
        return 5;
    // 6𝔽 if 181𝔽 + InLeapYear(t) ≤ DayWithinYear(t) < 212𝔽 + InLeapYear(t)
    if (181 + in_leap_year <= day_within_year && day_within_year < 212 + in_leap_year)
        return 6;
    // 7𝔽 if 212𝔽 + InLeapYear(t) ≤ DayWithinYear(t) < 243𝔽 + InLeapYear(t)
    if (212 + in_leap_year <= day_within_year && day_within_year < 243 + in_leap_year)
        return 7;
    // 8𝔽 if 243𝔽 + InLeapYear(t) ≤ DayWithinYear(t) < 273𝔽 + InLeapYear(t)
    if (243 + in_leap_year <= day_within_year && day_within_year < 273 + in_leap_year)
        return 8;
    // 9𝔽 if 273𝔽 + InLeapYear(t) ≤ DayWithinYear(t) < 304𝔽 + InLeapYear(t)
    if (273 + in_leap_year <= day_within_year && day_within_year < 304 + in_leap_year)
        return 9;
    // 10𝔽 if 304𝔽 + InLeapYear(t) ≤ DayWithinYear(t) < 334𝔽 + InLeapYear(t)
    if (304 + in_leap_year <= day_within_year && day_within_year < 334 + in_leap_year)
        return 10;
    // 11𝔽 if 334𝔽 + InLeapYear(t) ≤ DayWithinYear(t) < 365𝔽 + InLeapYear(t)
    if (334 + in_leap_year <= day_within_year && day_within_year < 365 + in_leap_year)
        return 11;
    VERIFY_NOT_REACHED();
}

// HourFromTime(t), https://tc39.es/ecma262/#eqn-HourFromTime
u8 hour_from_time(double t)
{
    if (!Value(t).is_finite_number())
        return 0;

    // 𝔽(floor(ℝ(t / msPerHour)) modulo HoursPerDay)
    return static_cast<u8>(modulo(floor(t / ms_per_hour), hours_per_day));
}

// MinFromTime(t), https://tc39.es/ecma262/#eqn-MinFromTime
u8 min_from_time(double t)
{
    if (!Value(t).is_finite_number())
        return 0;

    // 𝔽(floor(ℝ(t / msPerMinute)) modulo MinutesPerHour)
    return static_cast<u8>(modulo(floor(t / ms_per_minute), minutes_per_hour));
}

// SecFromTime(t), https://tc39.es/ecma262/#eqn-SecFromTime
u8 sec_from_time(double t)
{
    if (!Value(t).is_finite_number())
        return 0;

    // 𝔽(floor(ℝ(t / msPerSecond)) modulo SecondsPerMinute)
    return static_cast<u8>(modulo(floor(t / ms_per_second), seconds_per_minute));
}

// msFromTime(t), https://tc39.es/ecma262/#eqn-msFromTime
u16 ms_from_time(double t)
{
    if (!Value(t).is_finite_number())
        return 0;

    // 𝔽(ℝ(t) modulo ℝ(msPerSecond))
    return static_cast<u16>(modulo(t, ms_per_second));
}

// 21.4.1.6 Week Day, https://tc39.es/ecma262/#sec-week-day
u8 week_day(double t)
{
    if (!Value(t).is_finite_number())
        return 0;

    // 𝔽(ℝ(Day(t) + 4𝔽) modulo 7)
    return static_cast<u8>(modulo(day(t) + 4, 7));
}

// 21.4.1.7 LocalTZA ( t, isUTC ), https://tc39.es/ecma262/#sec-local-time-zone-adjustment
double local_tza(double time, [[maybe_unused]] bool is_utc, Optional<StringView> time_zone_override)
{
    // The time_zone_override parameter is non-standard, but allows callers to override the system
    // time zone with a specific value without setting environment variables.
    auto time_zone = time_zone_override.value_or(TimeZone::current_time_zone());

    // When isUTC is true, LocalTZA( tUTC, true ) should return the offset of the local time zone from
    // UTC measured in milliseconds at time represented by time value tUTC. When the result is added to
    // tUTC, it should yield the corresponding Number tlocal.

    // When isUTC is false, LocalTZA( tlocal, false ) should return the offset of the local time zone from
    // UTC measured in milliseconds at local time represented by Number tlocal. When the result is subtracted
    // from tlocal, it should yield the corresponding time value tUTC.

    auto time_since_epoch = Value(time).is_finite_number() ? AK::Time::from_milliseconds(time) : AK::Time::max();
    auto maybe_offset = TimeZone::get_time_zone_offset(time_zone, time_since_epoch);

    return maybe_offset.has_value() ? static_cast<double>(maybe_offset->seconds) * 1000 : 0;
}

// 21.4.1.8 LocalTime ( t ), https://tc39.es/ecma262/#sec-localtime
double local_time(double time)
{
    // 1. Return t + LocalTZA(t, true).
    return time + local_tza(time, true);
}

// 21.4.1.9 UTC ( t ), https://tc39.es/ecma262/#sec-utc-t
double utc_time(double time)
{
    // 1. Return t - LocalTZA(t, false).
    return time - local_tza(time, false);
}

// 21.4.1.11 MakeTime ( hour, min, sec, ms ), https://tc39.es/ecma262/#sec-maketime
double make_time(double hour, double min, double sec, double ms)
{
    // 1. If hour is not finite or min is not finite or sec is not finite or ms is not finite, return NaN.
    if (!isfinite(hour) || !isfinite(min) || !isfinite(sec) || !isfinite(ms))
        return NAN;

    // 2. Let h be 𝔽(! ToIntegerOrInfinity(hour)).
    auto h = to_integer_or_infinity(hour);
    // 3. Let m be 𝔽(! ToIntegerOrInfinity(min)).
    auto m = to_integer_or_infinity(min);
    // 4. Let s be 𝔽(! ToIntegerOrInfinity(sec)).
    auto s = to_integer_or_infinity(sec);
    // 5. Let milli be 𝔽(! ToIntegerOrInfinity(ms)).
    auto milli = to_integer_or_infinity(ms);
    // 6. Let t be ((h * msPerHour + m * msPerMinute) + s * msPerSecond) + milli, performing the arithmetic according to IEEE 754-2019 rules (that is, as if using the ECMAScript operators * and +).
    // NOTE: C++ arithmetic abides by IEEE 754 rules
    auto t = ((h * ms_per_hour + m * ms_per_minute) + s * ms_per_second) + milli;
    // 7. Return t.
    return t;
}

// Day(t), https://tc39.es/ecma262/#eqn-Day
double day(double time_value)
{
    return floor(time_value / ms_per_day);
}

// TimeWithinDay(t), https://tc39.es/ecma262/#eqn-TimeWithinDay
double time_within_day(double time)
{
    // 𝔽(ℝ(t) modulo ℝ(msPerDay))
    return modulo(time, ms_per_day);
}

// 21.4.1.12 MakeDay ( year, month, date ), https://tc39.es/ecma262/#sec-makeday
double make_day(double year, double month, double date)
{
    // 1. If year is not finite or month is not finite or date is not finite, return NaN.
    if (!isfinite(year) || !isfinite(month) || !isfinite(date))
        return NAN;

    // 2. Let y be 𝔽(! ToIntegerOrInfinity(year)).
    auto y = to_integer_or_infinity(year);
    // 3. Let m be 𝔽(! ToIntegerOrInfinity(month)).
    auto m = to_integer_or_infinity(month);
    // 4. Let dt be 𝔽(! ToIntegerOrInfinity(date)).
    auto dt = to_integer_or_infinity(date);
    // 5. Let ym be y + 𝔽(floor(ℝ(m) / 12)).
    auto ym = y + floor(m / 12);
    // 6. If ym is not finite, return NaN.
    if (!isfinite(ym))
        return NAN;
    // 7. Let mn be 𝔽(ℝ(m) modulo 12).
    auto mn = modulo(m, 12);

    // 8. Find a finite time value t such that YearFromTime(t) is ym and MonthFromTime(t) is mn and DateFromTime(t) is 1𝔽; but if this is not possible (because some argument is out of range), return NaN.
    if (!AK::is_within_range<int>(ym) || !AK::is_within_range<int>(mn + 1))
        return NAN;

    // FIXME: We are avoiding AK::years_to_days_since_epoch here because it is implemented by looping over
    //        the range [1970, ym), which will spin for any time value with an extremely large year.
    auto t = time_from_year(ym) + (day_of_year(static_cast<int>(ym), static_cast<int>(mn) + 1, 1) * ms_per_day);

    // 9. Return Day(t) + dt - 1𝔽.
    return day(static_cast<double>(t)) + dt - 1;
}

// 21.4.1.13 MakeDate ( day, time ), https://tc39.es/ecma262/#sec-makedate
double make_date(double day, double time)
{
    // 1. If day is not finite or time is not finite, return NaN.
    if (!isfinite(day) || !isfinite(time))
        return NAN;

    // 2. Let tv be day × msPerDay + time.
    auto tv = day * ms_per_day + time;

    // 3. If tv is not finite, return NaN.
    if (!isfinite(tv))
        return NAN;

    // 4. Return tv.
    return tv;
}

// 21.4.1.14 TimeClip ( time ), https://tc39.es/ecma262/#sec-timeclip
double time_clip(double time)
{
    // 1. If time is not finite, return NaN.
    if (!isfinite(time))
        return NAN;

    // 2. If abs(ℝ(time)) > 8.64 × 10^15, return NaN.
    if (fabs(time) > 8.64E15)
        return NAN;

    // 3. Return 𝔽(! ToIntegerOrInfinity(time)).
    return to_integer_or_infinity(time);
}

}
