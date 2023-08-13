/*
 * Copyright (c) 2020-2023, Linus Groh <linusg@serenityos.org>
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
#include <LibJS/Runtime/Temporal/ISO8601.h>
#include <LibTimeZone/TimeZone.h>
#include <time.h>

namespace JS {

static Crypto::SignedBigInteger const s_one_billion_bigint { 1'000'000'000 };
static Crypto::SignedBigInteger const s_one_million_bigint { 1'000'000 };
static Crypto::SignedBigInteger const s_one_thousand_bigint { 1'000 };

Crypto::SignedBigInteger const ns_per_day_bigint { static_cast<i64>(ns_per_day) };

NonnullGCPtr<Date> Date::create(Realm& realm, double date_value)
{
    return realm.heap().allocate<Date>(realm, date_value, realm.intrinsics().date_prototype());
}

Date::Date(double date_value, Object& prototype)
    : Object(ConstructWithPrototypeTag::Tag, prototype)
    , m_date_value(date_value)
{
}

ErrorOr<String> Date::iso_date_string() const
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

    return builder.to_string();
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

// 21.4.1.7 GetUTCEpochNanoseconds ( year, month, day, hour, minute, second, millisecond, microsecond, nanosecond ), https://tc39.es/ecma262/#sec-getutcepochnanoseconds
Crypto::SignedBigInteger get_utc_epoch_nanoseconds(i32 year, u8 month, u8 day, u8 hour, u8 minute, u8 second, u16 millisecond, u16 microsecond, u16 nanosecond)
{
    // 1. Let date be MakeDay(𝔽(year), 𝔽(month - 1), 𝔽(day)).
    auto date = make_day(year, month - 1, day);

    // 2. Let time be MakeTime(𝔽(hour), 𝔽(minute), 𝔽(second), 𝔽(millisecond)).
    auto time = make_time(hour, minute, second, millisecond);

    // 3. Let ms be MakeDate(date, time).
    auto ms = make_date(date, time);

    // 4. Assert: ms is an integral Number.
    VERIFY(ms == trunc(ms));

    // 5. Return ℤ(ℝ(ms) × 10^6 + microsecond × 10^3 + nanosecond).
    auto result = Crypto::SignedBigInteger { ms }.multiplied_by(s_one_million_bigint);
    result = result.plus(Crypto::SignedBigInteger { static_cast<i32>(microsecond) }.multiplied_by(s_one_thousand_bigint));
    result = result.plus(Crypto::SignedBigInteger { static_cast<i32>(nanosecond) });
    return result;
}

static i64 clip_bigint_to_sane_time(Crypto::SignedBigInteger const& value)
{
    static Crypto::SignedBigInteger const min_bigint { NumericLimits<i64>::min() };
    static Crypto::SignedBigInteger const max_bigint { NumericLimits<i64>::max() };

    // The provided epoch (nano)seconds value is potentially out of range for AK::Duration and subsequently
    // get_time_zone_offset(). We can safely assume that the TZDB has no useful information that far
    // into the past and future anyway, so clamp it to the i64 range.
    if (value < min_bigint)
        return NumericLimits<i64>::min();
    if (value > max_bigint)
        return NumericLimits<i64>::max();

    // FIXME: Can we do this without string conversion?
    return value.to_base_deprecated(10).to_int<i64>().value();
}

// 21.4.1.8 GetNamedTimeZoneEpochNanoseconds ( timeZoneIdentifier, year, month, day, hour, minute, second, millisecond, microsecond, nanosecond ), https://tc39.es/ecma262/#sec-getnamedtimezoneepochnanoseconds
Vector<Crypto::SignedBigInteger> get_named_time_zone_epoch_nanoseconds(StringView time_zone_identifier, i32 year, u8 month, u8 day, u8 hour, u8 minute, u8 second, u16 millisecond, u16 microsecond, u16 nanosecond)
{
    auto local_nanoseconds = get_utc_epoch_nanoseconds(year, month, day, hour, minute, second, millisecond, microsecond, nanosecond);
    auto local_time = UnixDateTime::from_nanoseconds_since_epoch(clip_bigint_to_sane_time(local_nanoseconds));

    // FIXME: LibTimeZone does not behave exactly as the spec expects. It does not consider repeated or skipped time points.
    auto offset = TimeZone::get_time_zone_offset(time_zone_identifier, local_time);

    // Can only fail if the time zone identifier is invalid, which cannot be the case here.
    VERIFY(offset.has_value());

    return { local_nanoseconds.minus(Crypto::SignedBigInteger { offset->seconds }.multiplied_by(s_one_billion_bigint)) };
}

// 21.4.1.9 GetNamedTimeZoneOffsetNanoseconds ( timeZoneIdentifier, epochNanoseconds ), https://tc39.es/ecma262/#sec-getnamedtimezoneoffsetnanoseconds
i64 get_named_time_zone_offset_nanoseconds(StringView time_zone_identifier, Crypto::SignedBigInteger const& epoch_nanoseconds)
{
    // Only called with validated time zone identifier as argument.
    auto time_zone = TimeZone::time_zone_from_string(time_zone_identifier);
    VERIFY(time_zone.has_value());

    // Since UnixDateTime::from_seconds_since_epoch() and UnixDateTime::from_nanoseconds_since_epoch() both take an i64, converting to
    // seconds first gives us a greater range. The TZDB doesn't have sub-second offsets.
    auto seconds = epoch_nanoseconds.divided_by(s_one_billion_bigint).quotient;
    auto time = UnixDateTime::from_seconds_since_epoch(clip_bigint_to_sane_time(seconds));

    auto offset = TimeZone::get_time_zone_offset(*time_zone, time);
    VERIFY(offset.has_value());

    return offset->seconds * 1'000'000'000;
}

// 21.4.1.10 DefaultTimeZone ( ), https://tc39.es/ecma262/#sec-defaulttimezone
// 6.4.3 DefaultTimeZone ( ), https://tc39.es/ecma402/#sup-defaulttimezone
StringView default_time_zone()
{
    return TimeZone::current_time_zone();
}

// 21.4.1.11 LocalTime ( t ), https://tc39.es/ecma262/#sec-localtime
double local_time(double time)
{
    // 1. Let localTimeZone be DefaultTimeZone().
    auto local_time_zone = default_time_zone();

    double offset_nanoseconds { 0 };

    // 2. If IsTimeZoneOffsetString(localTimeZone) is true, then
    if (is_time_zone_offset_string(local_time_zone)) {
        // a. Let offsetNs be ParseTimeZoneOffsetString(localTimeZone).
        offset_nanoseconds = parse_time_zone_offset_string(local_time_zone);
    }
    // 3. Else,
    else {
        // a. Let offsetNs be GetNamedTimeZoneOffsetNanoseconds(localTimeZone, ℤ(ℝ(t) × 10^6)).
        auto time_bigint = Crypto::SignedBigInteger { time }.multiplied_by(s_one_million_bigint);
        offset_nanoseconds = get_named_time_zone_offset_nanoseconds(local_time_zone, time_bigint);
    }

    // 4. Let offsetMs be truncate(offsetNs / 10^6).
    auto offset_milliseconds = trunc(offset_nanoseconds / 1e6);

    // 5. Return t + 𝔽(offsetMs).
    return time + offset_milliseconds;
}

// 21.4.1.12 UTC ( t ), https://tc39.es/ecma262/#sec-utc-t
double utc_time(double time)
{
    // 1. Let localTimeZone be DefaultTimeZone().
    auto local_time_zone = default_time_zone();

    double offset_nanoseconds { 0 };

    // 2. If IsTimeZoneOffsetString(localTimeZone) is true, then
    if (is_time_zone_offset_string(local_time_zone)) {
        // a. Let offsetNs be ParseTimeZoneOffsetString(localTimeZone).
        offset_nanoseconds = parse_time_zone_offset_string(local_time_zone);
    }
    // 3. Else,
    else {
        // a. Let possibleInstants be GetNamedTimeZoneEpochNanoseconds(localTimeZone, ℝ(YearFromTime(t)), ℝ(MonthFromTime(t)) + 1, ℝ(DateFromTime(t)), ℝ(HourFromTime(t)), ℝ(MinFromTime(t)), ℝ(SecFromTime(t)), ℝ(msFromTime(t)), 0, 0).
        auto possible_instants = get_named_time_zone_epoch_nanoseconds(local_time_zone, year_from_time(time), month_from_time(time) + 1, date_from_time(time), hour_from_time(time), min_from_time(time), sec_from_time(time), ms_from_time(time), 0, 0);

        // b. NOTE: The following steps ensure that when t represents local time repeating multiple times at a negative time zone transition (e.g. when the daylight saving time ends or the time zone offset is decreased due to a time zone rule change) or skipped local time at a positive time zone transition (e.g. when the daylight saving time starts or the time zone offset is increased due to a time zone rule change), t is interpreted using the time zone offset before the transition.
        Crypto::SignedBigInteger disambiguated_instant;

        // c. If possibleInstants is not empty, then
        if (!possible_instants.is_empty()) {
            // i. Let disambiguatedInstant be possibleInstants[0].
            disambiguated_instant = move(possible_instants.first());
        }
        // d. Else,
        else {
            // i. NOTE: t represents a local time skipped at a positive time zone transition (e.g. due to daylight saving time starting or a time zone rule change increasing the UTC offset).
            // ii. Let possibleInstantsBefore be GetNamedTimeZoneEpochNanoseconds(localTimeZone, ℝ(YearFromTime(tBefore)), ℝ(MonthFromTime(tBefore)) + 1, ℝ(DateFromTime(tBefore)), ℝ(HourFromTime(tBefore)), ℝ(MinFromTime(tBefore)), ℝ(SecFromTime(tBefore)), ℝ(msFromTime(tBefore)), 0, 0), where tBefore is the largest integral Number < t for which possibleInstantsBefore is not empty (i.e., tBefore represents the last local time before the transition).
            // iii. Let disambiguatedInstant be the last element of possibleInstantsBefore.

            // FIXME: This branch currently cannot be reached with our implementation, because LibTimeZone does not handle skipped time points.
            //        When GetNamedTimeZoneEpochNanoseconds is updated to use a LibTimeZone API which does handle them, implement these steps.
            VERIFY_NOT_REACHED();
        }

        // e. Let offsetNs be GetNamedTimeZoneOffsetNanoseconds(localTimeZone, disambiguatedInstant).
        offset_nanoseconds = get_named_time_zone_offset_nanoseconds(local_time_zone, disambiguated_instant);
    }

    // 4. Let offsetMs be truncate(offsetNs / 10^6).
    auto offset_milliseconds = trunc(offset_nanoseconds / 1e6);

    // 5. Return t - 𝔽(offsetMs).
    return time - offset_milliseconds;
}

// 21.4.1.14 MakeTime ( hour, min, sec, ms ), https://tc39.es/ecma262/#sec-maketime
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

// 21.4.1.15 MakeDay ( year, month, date ), https://tc39.es/ecma262/#sec-makeday
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
    auto t = days_since_epoch(static_cast<int>(ym), static_cast<int>(mn) + 1, 1) * ms_per_day;

    // 9. Return Day(t) + dt - 1𝔽.
    return day(static_cast<double>(t)) + dt - 1;
}

// 21.4.1.16 MakeDate ( day, time ), https://tc39.es/ecma262/#sec-makedate
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

// 21.4.1.17 TimeClip ( time ), https://tc39.es/ecma262/#sec-timeclip
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

// 21.4.1.19.1 IsTimeZoneOffsetString ( offsetString ), https://tc39.es/ecma262/#sec-istimezoneoffsetstring
bool is_time_zone_offset_string(StringView offset_string)
{
    // 1. Let parseResult be ParseText(StringToCodePoints(offsetString), UTCOffset).
    auto parse_result = Temporal::parse_iso8601(Temporal::Production::TimeZoneNumericUTCOffset, offset_string);

    // 2. If parseResult is a List of errors, return false.
    // 3. Return true.
    return parse_result.has_value();
}

// 21.4.1.19.2 ParseTimeZoneOffsetString ( offsetString ), https://tc39.es/ecma262/#sec-parsetimezoneoffsetstring
double parse_time_zone_offset_string(StringView offset_string)
{
    // 1. Let parseResult be ParseText(StringToCodePoints(offsetString), UTCOffset).
    auto parse_result = Temporal::parse_iso8601(Temporal::Production::TimeZoneNumericUTCOffset, offset_string);

    // 2. Assert: parseResult is not a List of errors.
    VERIFY(parse_result.has_value());

    // 3. Assert: parseResult contains a TemporalSign Parse Node.
    VERIFY(parse_result->time_zone_utc_offset_sign.has_value());

    // 4. Let parsedSign be the source text matched by the TemporalSign Parse Node contained within parseResult.
    auto parsed_sign = *parse_result->time_zone_utc_offset_sign;
    i8 sign { 0 };

    // 5. If parsedSign is the single code point U+002D (HYPHEN-MINUS) or U+2212 (MINUS SIGN), then
    if (parsed_sign.is_one_of("-"sv, "\xE2\x88\x92"sv)) {
        // a. Let sign be -1.
        sign = -1;
    }
    // 6. Else,
    else {
        // a. Let sign be 1.
        sign = 1;
    }

    // 7. NOTE: Applications of StringToNumber below do not lose precision, since each of the parsed values is guaranteed to be a sufficiently short string of decimal digits.

    // 8. Assert: parseResult contains an Hour Parse Node.
    VERIFY(parse_result->time_zone_utc_offset_hour.has_value());

    // 9. Let parsedHours be the source text matched by the Hour Parse Node contained within parseResult.
    auto parsed_hours = *parse_result->time_zone_utc_offset_hour;

    // 10. Let hours be ℝ(StringToNumber(CodePointsToString(parsedHours))).
    auto hours = string_to_number(parsed_hours);

    double minutes { 0 };
    double seconds { 0 };
    double nanoseconds { 0 };

    // 11. If parseResult does not contain a MinuteSecond Parse Node, then
    if (!parse_result->time_zone_utc_offset_minute.has_value()) {
        // a. Let minutes be 0.
        minutes = 0;
    }
    // 12. Else,
    else {
        // a. Let parsedMinutes be the source text matched by the first MinuteSecond Parse Node contained within parseResult.
        auto parsed_minutes = *parse_result->time_zone_utc_offset_minute;

        // b. Let minutes be ℝ(StringToNumber(CodePointsToString(parsedMinutes))).
        minutes = string_to_number(parsed_minutes);
    }

    // 13. If parseResult does not contain two MinuteSecond Parse Nodes, then
    if (!parse_result->time_zone_utc_offset_second.has_value()) {
        // a. Let seconds be 0.
        seconds = 0;
    }
    // 14. Else,
    else {
        // a. Let parsedSeconds be the source text matched by the second secondSecond Parse Node contained within parseResult.
        auto parsed_seconds = *parse_result->time_zone_utc_offset_second;

        // b. Let seconds be ℝ(StringToNumber(CodePointsToString(parsedSeconds))).
        seconds = string_to_number(parsed_seconds);
    }

    // 15. If parseResult does not contain a TemporalDecimalFraction Parse Node, then
    if (!parse_result->time_zone_utc_offset_fraction.has_value()) {
        // a. Let nanoseconds be 0.
        nanoseconds = 0;
    }
    // 16. Else,
    else {
        // a. Let parsedFraction be the source text matched by the TemporalDecimalFraction Parse Node contained within parseResult.
        auto parsed_fraction = *parse_result->time_zone_utc_offset_fraction;

        // b. Let fraction be the string-concatenation of CodePointsToString(parsedFraction) and "000000000".
        auto fraction = DeprecatedString::formatted("{}000000000", parsed_fraction);

        // c. Let nanosecondsString be the substring of fraction from 1 to 10.
        auto nanoseconds_string = fraction.substring_view(1, 9);

        // d. Let nanoseconds be ℝ(StringToNumber(nanosecondsString)).
        nanoseconds = string_to_number(nanoseconds_string);
    }

    // 17. Return sign × (((hours × 60 + minutes) × 60 + seconds) × 10^9 + nanoseconds).
    // NOTE: Using scientific notation (1e9) ensures the result of this expression is a double,
    //       which is important - otherwise it's all integers and the result overflows!
    return sign * (((hours * 60 + minutes) * 60 + seconds) * 1e9 + nanoseconds);
}

}
