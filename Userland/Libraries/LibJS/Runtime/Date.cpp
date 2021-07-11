/*
 * Copyright (c) 2020, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringBuilder.h>
#include <LibCore/DateTime.h>
#include <LibJS/Heap/Heap.h>
#include <LibJS/Runtime/Date.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <sys/time.h>
#include <time.h>

namespace JS {

Date* Date::create(GlobalObject& global_object, Core::DateTime datetime, i16 milliseconds, bool is_invalid)
{
    return global_object.heap().allocate<Date>(global_object, datetime, milliseconds, is_invalid, *global_object.date_prototype());
}

Date::Date(Core::DateTime datetime, i16 milliseconds, bool is_invalid, Object& prototype)
    : Object(prototype)
    , m_datetime(datetime)
    , m_milliseconds(milliseconds)
    , m_is_invalid(is_invalid)
{
}

Date::~Date()
{
}

tm Date::to_utc_tm() const
{
    time_t timestamp = m_datetime.timestamp();
    struct tm tm;
    gmtime_r(&timestamp, &tm);
    return tm;
}

int Date::utc_date() const
{
    return to_utc_tm().tm_mday;
}

int Date::utc_day() const
{
    return to_utc_tm().tm_wday;
}

int Date::utc_full_year() const
{
    return to_utc_tm().tm_year + 1900;
}

int Date::utc_hours() const
{
    return to_utc_tm().tm_hour;
}

int Date::utc_minutes() const
{
    return to_utc_tm().tm_min;
}

int Date::utc_month() const
{
    return to_utc_tm().tm_mon;
}

int Date::utc_seconds() const
{
    return to_utc_tm().tm_sec;
}

String Date::gmt_date_string() const
{
    // Mon, 18 Dec 1995 17:28:35 GMT
    // FIXME: Note that we're totally cheating with the timezone part here..
    return datetime().to_string("%a, %e %b %Y %T GMT");
}

String Date::iso_date_string() const
{
    auto tm = to_utc_tm();
    int year = tm.tm_year + 1900;
    int month = tm.tm_mon + 1;

    StringBuilder builder;
    if (year < 0)
        builder.appendff("-{:06}", -year);
    else if (year > 9999)
        builder.appendff("+{:06}", year);
    else
        builder.appendff("{:04}", year);
    builder.append('-');
    builder.appendff("{:02}", month);
    builder.append('-');
    builder.appendff("{:02}", tm.tm_mday);
    builder.append('T');
    builder.appendff("{:02}", tm.tm_hour);
    builder.append(':');
    builder.appendff("{:02}", tm.tm_min);
    builder.append(':');
    builder.appendff("{:02}", tm.tm_sec);
    builder.append('.');
    builder.appendff("{:03}", m_milliseconds);
    builder.append('Z');

    return builder.build();
}

// https://tc39.es/ecma262/#eqn-msPerSecond
static constexpr double MS_PER_SECOND = 1000;
// https://tc39.es/ecma262/#eqn-msPerMinute
static constexpr double MS_PER_MINUTE = 60000;
// https://tc39.es/ecma262/#eqn-msPerHour
static constexpr double MS_PER_HOUR = 3600000;
// https://tc39.es/ecma262/#eqn-msPerDay
static constexpr double MS_PER_DAY = 86400000;

// 21.4.1.11 MakeTime ( hour, min, sec, ms ), https://tc39.es/ecma262/#sec-maketime
Value make_time(GlobalObject& global_object, Value hour, Value min, Value sec, Value ms)
{
    // 1. If hour is not finite or min is not finite or sec is not finite or ms is not finite, return NaN.
    if (!hour.is_finite_number() || !min.is_finite_number() || !sec.is_finite_number() || !ms.is_finite_number())
        return js_nan();

    // 2. Let h be 𝔽(! ToIntegerOrInfinity(hour)).
    auto h = hour.to_integer_or_infinity(global_object);
    // 3. Let m be 𝔽(! ToIntegerOrInfinity(min)).
    auto m = min.to_integer_or_infinity(global_object);
    // 4. Let s be 𝔽(! ToIntegerOrInfinity(sec)).
    auto s = sec.to_integer_or_infinity(global_object);
    // 5. Let milli be 𝔽(! ToIntegerOrInfinity(ms)).
    auto milli = ms.to_integer_or_infinity(global_object);
    // 6. Let t be ((h * msPerHour + m * msPerMinute) + s * msPerSecond) + milli, performing the arithmetic according to IEEE 754-2019 rules (that is, as if using the ECMAScript operators * and +).
    // NOTE: C++ arithmetic abides by IEEE 754 rules
    auto t = ((h * MS_PER_HOUR + m * MS_PER_MINUTE) + s * MS_PER_SECOND) + milli;
    // 7. Return t.
    return Value(t);
}

// https://tc39.es/ecma262/#eqn-Day
static inline double day(double time_value)
{
    return floor(time_value / MS_PER_DAY);
}

// 21.4.1.12 MakeDay ( year, month, date ), https://tc39.es/ecma262/#sec-makeday
Value make_day(GlobalObject& global_object, Value year, Value month, Value date)
{
    // 1. If year is not finite or month is not finite or date is not finite, return NaN.
    if (!year.is_finite_number() || !month.is_finite_number() || !date.is_finite_number())
        return js_nan();

    // 2. Let y be 𝔽(! ToIntegerOrInfinity(year)).
    auto y = year.to_integer_or_infinity(global_object);
    // 3. Let m be 𝔽(! ToIntegerOrInfinity(month)).
    auto m = month.to_integer_or_infinity(global_object);
    // 4. Let dt be 𝔽(! ToIntegerOrInfinity(date)).
    auto dt = date.to_integer_or_infinity(global_object);
    // 5. Let ym be y + 𝔽(floor(ℝ(m) / 12)).
    auto ym = Value(y + floor(m / 12));
    // 6. If ym is not finite, return NaN.
    if (!ym.is_finite_number())
        return js_nan();
    // 7. Let mn be 𝔽(ℝ(m) modulo 12).
    // NOTE: This calculation has no side-effects and is unused, so we omit it

    // 8. Find a finite time value t such that YearFromTime(t) is ym and MonthFromTime(t) is mn and DateFromTime(t) is 1𝔽; but if this is not possible (because some argument is out of range), return NaN.
    auto t = Core::DateTime::create(y, m + 1, 0).timestamp() * 1000;
    // 9. Return Day(t) + dt - 1𝔽.
    return Value(day(t) + dt - 1);
}

// 21.4.1.13 MakeDate ( day, time ), https://tc39.es/ecma262/#sec-makedate
Value make_date(Value day, Value time)
{
    // 1. If day is not finite or time is not finite, return NaN.
    if (!day.is_finite_number() || !time.is_finite_number())
        return js_nan();

    // 2. Let tv be day × msPerDay + time.
    auto tv = Value(day.as_double() * MS_PER_DAY + time.as_double());

    // 3. If tv is not finite, return NaN.
    if (!tv.is_finite_number())
        return js_nan();

    // 4. Return tv.
    return tv;
}

}
