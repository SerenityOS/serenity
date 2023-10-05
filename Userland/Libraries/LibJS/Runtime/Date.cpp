/*
 * Copyright (c) 2020-2023, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2022-2023, Tim Flynn <trflynn89@serenityos.org>
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

// 21.4.1.3 Day ( t ), https://tc39.es/ecma262/#sec-day
double day(double time_value)
{
    // 1. Return 𝔽(floor(ℝ(t / msPerDay))).
    return floor(time_value / ms_per_day);
}

// 21.4.1.4 TimeWithinDay ( t ), https://tc39.es/ecma262/#sec-timewithinday
double time_within_day(double time)
{
    // 1. Return 𝔽(ℝ(t) modulo ℝ(msPerDay)).
    return modulo(time, ms_per_day);
}

// 21.4.1.5 DaysInYear ( y ), https://tc39.es/ecma262/#sec-daysinyear
u16 days_in_year(i32 y)
{
    // 1. Let ry be ℝ(y).
    auto ry = static_cast<double>(y);

    // 2. If (ry modulo 400) = 0, return 366𝔽.
    if (modulo(ry, 400.0) == 0)
        return 366;

    // 3. If (ry modulo 100) = 0, return 365𝔽.
    if (modulo(ry, 100.0) == 0)
        return 365;

    // 4. If (ry modulo 4) = 0, return 366𝔽.
    if (modulo(ry, 4.0) == 0)
        return 366;

    // 5. Return 365𝔽.
    return 365;
}

// 21.4.1.6 DayFromYear ( y ), https://tc39.es/ecma262/#sec-dayfromyear
double day_from_year(i32 y)
{
    // 1. Let ry be ℝ(y).
    auto ry = static_cast<double>(y);

    // 2. NOTE: In the following steps, each _numYearsN_ is the number of years divisible by N that occur between the
    //    epoch and the start of year y. (The number is negative if y is before the epoch.)

    // 3. Let numYears1 be (ry - 1970).
    auto num_years_1 = ry - 1970;

    // 4. Let numYears4 be floor((ry - 1969) / 4).
    auto num_years_4 = floor((ry - 1969) / 4.0);

    // 5. Let numYears100 be floor((ry - 1901) / 100).
    auto num_years_100 = floor((ry - 1901) / 100.0);

    // 6. Let numYears400 be floor((ry - 1601) / 400).
    auto num_years_400 = floor((ry - 1601) / 400.0);

    // 7. Return 𝔽(365 × numYears1 + numYears4 - numYears100 + numYears400).
    return 365.0 * num_years_1 + num_years_4 - num_years_100 + num_years_400;
}

// 21.4.1.7 TimeFromYear ( y ), https://tc39.es/ecma262/#sec-timefromyear
double time_from_year(i32 y)
{
    // 1. Return msPerDay × DayFromYear(y).
    return ms_per_day * day_from_year(y);
}

// 21.4.1.8 YearFromTime ( t ), https://tc39.es/ecma262/#sec-yearfromtime
i32 year_from_time(double t)
{
    // 1. Return the largest integral Number y (closest to +∞) such that TimeFromYear(y) ≤ t.
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

// 21.4.1.9 DayWithinYear ( t ), https://tc39.es/ecma262/#sec-daywithinyear
u16 day_within_year(double t)
{
    if (!Value(t).is_finite_number())
        return 0;

    // 1. Return Day(t) - DayFromYear(YearFromTime(t)).
    return static_cast<u16>(day(t) - day_from_year(year_from_time(t)));
}

// 21.4.1.10 InLeapYear ( t ), https://tc39.es/ecma262/#sec-inleapyear
bool in_leap_year(double t)
{
    // 1. If DaysInYear(YearFromTime(t)) is 366𝔽, return 1𝔽; else return +0𝔽.
    return days_in_year(year_from_time(t)) == 366;
}

// 21.4.1.11 MonthFromTime ( t ), https://tc39.es/ecma262/#sec-monthfromtime
u8 month_from_time(double t)
{
    // 1. Let inLeapYear be InLeapYear(t).
    auto in_leap_year = static_cast<unsigned>(JS::in_leap_year(t));

    // 2. Let dayWithinYear be DayWithinYear(t).
    auto day_within_year = JS::day_within_year(t);

    // 3. If dayWithinYear < 31𝔽, return +0𝔽.
    if (day_within_year < 31)
        return 0;

    // 4. If dayWithinYear < 59𝔽 + inLeapYear, return 1𝔽.
    if (day_within_year < (59 + in_leap_year))
        return 1;

    // 5. If dayWithinYear < 90𝔽 + inLeapYear, return 2𝔽.
    if (day_within_year < (90 + in_leap_year))
        return 2;

    // 6. If dayWithinYear < 120𝔽 + inLeapYear, return 3𝔽.
    if (day_within_year < (120 + in_leap_year))
        return 3;

    // 7. If dayWithinYear < 151𝔽 + inLeapYear, return 4𝔽.
    if (day_within_year < (151 + in_leap_year))
        return 4;

    // 8. If dayWithinYear < 181𝔽 + inLeapYear, return 5𝔽.
    if (day_within_year < (181 + in_leap_year))
        return 5;

    // 9. If dayWithinYear < 212𝔽 + inLeapYear, return 6𝔽.
    if (day_within_year < (212 + in_leap_year))
        return 6;

    // 10. If dayWithinYear < 243𝔽 + inLeapYear, return 7𝔽.
    if (day_within_year < (243 + in_leap_year))
        return 7;

    // 11. If dayWithinYear < 273𝔽 + inLeapYear, return 8𝔽.
    if (day_within_year < (273 + in_leap_year))
        return 8;

    // 12. If dayWithinYear < 304𝔽 + inLeapYear, return 9𝔽.
    if (day_within_year < (304 + in_leap_year))
        return 9;

    // 13. If dayWithinYear < 334𝔽 + inLeapYear, return 10𝔽.
    if (day_within_year < (334 + in_leap_year))
        return 10;

    // 14. Assert: dayWithinYear < 365𝔽 + inLeapYear.
    VERIFY(day_within_year < (365 + in_leap_year));

    // 15. Return 11𝔽.
    return 11;
}

// 21.4.1.12 DateFromTime ( t ), https://tc39.es/ecma262/#sec-datefromtime
u8 date_from_time(double t)
{
    // 1. Let inLeapYear be InLeapYear(t).
    auto in_leap_year = static_cast<unsigned>(JS::in_leap_year(t));

    // 2. Let dayWithinYear be DayWithinYear(t).
    auto day_within_year = JS::day_within_year(t);

    // 3. Let month be MonthFromTime(t).
    auto month = month_from_time(t);

    // 4. If month is +0𝔽, return dayWithinYear + 1𝔽.
    if (month == 0)
        return day_within_year + 1;

    // 5. If month is 1𝔽, return dayWithinYear - 30𝔽.
    if (month == 1)
        return day_within_year - 30;

    // 6. If month is 2𝔽, return dayWithinYear - 58𝔽 - inLeapYear.
    if (month == 2)
        return day_within_year - 58 - in_leap_year;

    // 7. If month is 3𝔽, return dayWithinYear - 89𝔽 - inLeapYear.
    if (month == 3)
        return day_within_year - 89 - in_leap_year;

    // 8. If month is 4𝔽, return dayWithinYear - 119𝔽 - inLeapYear.
    if (month == 4)
        return day_within_year - 119 - in_leap_year;

    // 9. If month is 5𝔽, return dayWithinYear - 150𝔽 - inLeapYear.
    if (month == 5)
        return day_within_year - 150 - in_leap_year;

    // 10. If month is 6𝔽, return dayWithinYear - 180𝔽 - inLeapYear.
    if (month == 6)
        return day_within_year - 180 - in_leap_year;

    // 11. If month is 7𝔽, return dayWithinYear - 211𝔽 - inLeapYear.
    if (month == 7)
        return day_within_year - 211 - in_leap_year;

    // 12. If month is 8𝔽, return dayWithinYear - 242𝔽 - inLeapYear.
    if (month == 8)
        return day_within_year - 242 - in_leap_year;

    // 13. If month is 9𝔽, return dayWithinYear - 272𝔽 - inLeapYear.
    if (month == 9)
        return day_within_year - 272 - in_leap_year;

    // 14. If month is 10𝔽, return dayWithinYear - 303𝔽 - inLeapYear.
    if (month == 10)
        return day_within_year - 303 - in_leap_year;

    // 15. Assert: month is 11𝔽.
    VERIFY(month == 11);

    // 16. Return dayWithinYear - 333𝔽 - inLeapYear.
    return day_within_year - 333 - in_leap_year;
}

// 21.4.1.13 WeekDay ( t ), https://tc39.es/ecma262/#sec-weekday
u8 week_day(double t)
{
    if (!Value(t).is_finite_number())
        return 0;

    // 1. Return 𝔽(ℝ(Day(t) + 4𝔽) modulo 7).
    return static_cast<u8>(modulo(day(t) + 4, 7));
}

// 21.4.1.14 HourFromTime ( t ), https://tc39.es/ecma262/#sec-hourfromtime
u8 hour_from_time(double t)
{
    if (!Value(t).is_finite_number())
        return 0;

    // 1. Return 𝔽(floor(ℝ(t / msPerHour)) modulo HoursPerDay).
    return static_cast<u8>(modulo(floor(t / ms_per_hour), hours_per_day));
}

// 21.4.1.15 MinFromTime ( t ), https://tc39.es/ecma262/#sec-minfromtime
u8 min_from_time(double t)
{
    if (!Value(t).is_finite_number())
        return 0;

    // 1. Return 𝔽(floor(ℝ(t / msPerMinute)) modulo MinutesPerHour).
    return static_cast<u8>(modulo(floor(t / ms_per_minute), minutes_per_hour));
}

// 21.4.1.16 SecFromTime ( t ), https://tc39.es/ecma262/#sec-secfromtime
u8 sec_from_time(double t)
{
    if (!Value(t).is_finite_number())
        return 0;

    // 1. Return 𝔽(floor(ℝ(t / msPerSecond)) modulo SecondsPerMinute).
    return static_cast<u8>(modulo(floor(t / ms_per_second), seconds_per_minute));
}

// 21.4.1.17 msFromTime ( t ), https://tc39.es/ecma262/#sec-msfromtime
u16 ms_from_time(double t)
{
    if (!Value(t).is_finite_number())
        return 0;

    // 1. Return 𝔽(ℝ(t) modulo ℝ(msPerSecond)).
    return static_cast<u16>(modulo(t, ms_per_second));
}

// 21.4.1.18 GetUTCEpochNanoseconds ( year, month, day, hour, minute, second, millisecond, microsecond, nanosecond ), https://tc39.es/ecma262/#sec-getutcepochnanoseconds
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

// 21.4.1.20 GetNamedTimeZoneEpochNanoseconds ( timeZoneIdentifier, year, month, day, hour, minute, second, millisecond, microsecond, nanosecond ), https://tc39.es/ecma262/#sec-getnamedtimezoneepochnanoseconds
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

// 21.4.1.21 GetNamedTimeZoneOffsetNanoseconds ( timeZoneIdentifier, epochNanoseconds ), https://tc39.es/ecma262/#sec-getnamedtimezoneoffsetnanoseconds
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

// 21.4.1.23 AvailableNamedTimeZoneIdentifiers ( ), https://tc39.es/ecma262/#sec-time-zone-identifier-record
Vector<TimeZoneIdentifier> available_named_time_zone_identifiers()
{
    // 1. If the implementation does not include local political rules for any time zones, then
    //     a. Return « the Time Zone Identifier Record { [[Identifier]]: "UTC", [[PrimaryIdentifier]]: "UTC" } ».
    // NOTE: This step is not applicable as LibTimeZone will always return at least UTC, even if the TZDB is disabled.

    // 2. Let identifiers be the List of unique available named time zone identifiers.
    auto identifiers = TimeZone::all_time_zones();

    // 3. Sort identifiers into the same order as if an Array of the same values had been sorted using %Array.prototype.sort% with undefined as comparefn.
    // NOTE: LibTimeZone provides the identifiers already sorted.

    // 4. Let result be a new empty List.
    Vector<TimeZoneIdentifier> result;
    result.ensure_capacity(identifiers.size());

    bool found_utc = false;

    // 5. For each element identifier of identifiers, do
    for (auto identifier : identifiers) {
        // a. Let primary be identifier.
        auto primary = identifier.name;

        // b. If identifier is a non-primary time zone identifier in this implementation and identifier is not "UTC", then
        if (identifier.is_link == TimeZone::IsLink::Yes && identifier.name != "UTC"sv) {
            // i. Set primary to the primary time zone identifier associated with identifier.
            // ii. NOTE: An implementation may need to resolve identifier iteratively to obtain the primary time zone identifier.
            primary = TimeZone::canonicalize_time_zone(identifier.name).value();
        }

        // c. Let record be the Time Zone Identifier Record { [[Identifier]]: identifier, [[PrimaryIdentifier]]: primary }.
        TimeZoneIdentifier record { .identifier = identifier.name, .primary_identifier = primary };

        // d. Append record to result.
        result.unchecked_append(record);

        if (!found_utc && identifier.name == "UTC"sv && primary == "UTC"sv)
            found_utc = true;
    }

    // 6. Assert: result contains a Time Zone Identifier Record r such that r.[[Identifier]] is "UTC" and r.[[PrimaryIdentifier]] is "UTC".
    VERIFY(found_utc);

    // 7. Return result.
    return result;
}

// 21.4.1.24 SystemTimeZoneIdentifier ( ), https://tc39.es/ecma262/#sec-systemtimezoneidentifier
StringView system_time_zone_identifier()
{
    return TimeZone::current_time_zone();
}

// 21.4.1.25 LocalTime ( t ), https://tc39.es/ecma262/#sec-localtime
double local_time(double time)
{
    // 1. Let systemTimeZoneIdentifier be SystemTimeZoneIdentifier().
    auto system_time_zone_identifier = JS::system_time_zone_identifier();

    double offset_nanoseconds { 0 };

    // 2. If IsTimeZoneOffsetString(systemTimeZoneIdentifier) is true, then
    if (is_time_zone_offset_string(system_time_zone_identifier)) {
        // a. Let offsetNs be ParseTimeZoneOffsetString(systemTimeZoneIdentifier).
        offset_nanoseconds = parse_time_zone_offset_string(system_time_zone_identifier);
    }
    // 3. Else,
    else {
        // a. Let offsetNs be GetNamedTimeZoneOffsetNanoseconds(systemTimeZoneIdentifier, ℤ(ℝ(t) × 10^6)).
        auto time_bigint = Crypto::SignedBigInteger { time }.multiplied_by(s_one_million_bigint);
        offset_nanoseconds = get_named_time_zone_offset_nanoseconds(system_time_zone_identifier, time_bigint);
    }

    // 4. Let offsetMs be truncate(offsetNs / 10^6).
    auto offset_milliseconds = trunc(offset_nanoseconds / 1e6);

    // 5. Return t + 𝔽(offsetMs).
    return time + offset_milliseconds;
}

// 21.4.1.26 UTC ( t ), https://tc39.es/ecma262/#sec-utc-t
double utc_time(double time)
{
    // 1. Let systemTimeZoneIdentifier be SystemTimeZoneIdentifier().
    auto system_time_zone_identifier = JS::system_time_zone_identifier();

    double offset_nanoseconds { 0 };

    // 2. If IsTimeZoneOffsetString(systemTimeZoneIdentifier) is true, then
    if (is_time_zone_offset_string(system_time_zone_identifier)) {
        // a. Let offsetNs be ParseTimeZoneOffsetString(systemTimeZoneIdentifier).
        offset_nanoseconds = parse_time_zone_offset_string(system_time_zone_identifier);
    }
    // 3. Else,
    else {
        // a. Let possibleInstants be GetNamedTimeZoneEpochNanoseconds(systemTimeZoneIdentifier, ℝ(YearFromTime(t)), ℝ(MonthFromTime(t)) + 1, ℝ(DateFromTime(t)), ℝ(HourFromTime(t)), ℝ(MinFromTime(t)), ℝ(SecFromTime(t)), ℝ(msFromTime(t)), 0, 0).
        auto possible_instants = get_named_time_zone_epoch_nanoseconds(system_time_zone_identifier, year_from_time(time), month_from_time(time) + 1, date_from_time(time), hour_from_time(time), min_from_time(time), sec_from_time(time), ms_from_time(time), 0, 0);

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
            // ii. Let possibleInstantsBefore be GetNamedTimeZoneEpochNanoseconds(systemTimeZoneIdentifier, ℝ(YearFromTime(tBefore)), ℝ(MonthFromTime(tBefore)) + 1, ℝ(DateFromTime(tBefore)), ℝ(HourFromTime(tBefore)), ℝ(MinFromTime(tBefore)), ℝ(SecFromTime(tBefore)), ℝ(msFromTime(tBefore)), 0, 0), where tBefore is the largest integral Number < t for which possibleInstantsBefore is not empty (i.e., tBefore represents the last local time before the transition).
            // iii. Let disambiguatedInstant be the last element of possibleInstantsBefore.

            // FIXME: This branch currently cannot be reached with our implementation, because LibTimeZone does not handle skipped time points.
            //        When GetNamedTimeZoneEpochNanoseconds is updated to use a LibTimeZone API which does handle them, implement these steps.
            VERIFY_NOT_REACHED();
        }

        // e. Let offsetNs be GetNamedTimeZoneOffsetNanoseconds(systemTimeZoneIdentifier, disambiguatedInstant).
        offset_nanoseconds = get_named_time_zone_offset_nanoseconds(system_time_zone_identifier, disambiguated_instant);
    }

    // 4. Let offsetMs be truncate(offsetNs / 10^6).
    auto offset_milliseconds = trunc(offset_nanoseconds / 1e6);

    // 5. Return t - 𝔽(offsetMs).
    return time - offset_milliseconds;
}

// 21.4.1.27 MakeTime ( hour, min, sec, ms ), https://tc39.es/ecma262/#sec-maketime
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

// 21.4.1.28 MakeDay ( year, month, date ), https://tc39.es/ecma262/#sec-makeday
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

// 21.4.1.29 MakeDate ( day, time ), https://tc39.es/ecma262/#sec-makedate
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

// 21.4.1.31 TimeClip ( time ), https://tc39.es/ecma262/#sec-timeclip
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

// 21.4.1.33.1 IsTimeZoneOffsetString ( offsetString ), https://tc39.es/ecma262/#sec-istimezoneoffsetstring
bool is_time_zone_offset_string(StringView offset_string)
{
    // 1. Let parseResult be ParseText(StringToCodePoints(offsetString), UTCOffset).
    auto parse_result = Temporal::parse_iso8601(Temporal::Production::TimeZoneNumericUTCOffset, offset_string);

    // 2. If parseResult is a List of errors, return false.
    // 3. Return true.
    return parse_result.has_value();
}

// 21.4.1.33.2 ParseTimeZoneOffsetString ( offsetString ), https://tc39.es/ecma262/#sec-parsetimezoneoffsetstring
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
