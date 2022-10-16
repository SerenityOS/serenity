/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <AK/Assertions.h>
#include <AK/Platform.h>
#include <AK/Types.h>

// Kernel and Userspace pull in the definitions from different places.
// Avoid trying to figure out which one.
struct timeval;
struct timespec;

#if defined(AK_OS_WINDOWS)
#    include <time.h>
#endif

namespace AK {

// Concept to detect types which look like timespec without requiring the type.
template<typename T>
concept TimeSpecType = requires(T t) {
                           t.tv_sec;
                           t.tv_nsec;
                       };

constexpr bool is_leap_year(int year)
{
    return year % 4 == 0 && (year % 100 != 0 || year % 400 == 0);
}

// Month and day start at 1. Month must be >= 1 and <= 12.
// The return value is 0-indexed, that is 0 is Sunday, 1 is Monday, etc.
// Day may be negative or larger than the number of days
// in the given month.
unsigned day_of_week(int year, unsigned month, int day);

// Month and day start at 1. Month must be >= 1 and <= 12.
// The return value is 0-indexed, that is Jan 1 is day 0.
// Day may be negative or larger than the number of days
// in the given month. If day is negative enough, the result
// can be negative.
constexpr int day_of_year(int year, unsigned month, int day)
{
    VERIFY(month >= 1 && month <= 12);

    constexpr Array seek_table = { 0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334 };
    int day_of_year = seek_table[month - 1] + day - 1;

    if (is_leap_year(year) && month >= 3)
        day_of_year++;

    return day_of_year;
}

// Month starts at 1. Month must be >= 1 and <= 12.
int days_in_month(int year, unsigned month);

constexpr int days_in_year(int year)
{
    return 365 + (is_leap_year(year) ? 1 : 0);
}

constexpr int years_to_days_since_epoch(int year)
{
    int days = 0;
    for (int current_year = 1970; current_year < year; ++current_year)
        days += days_in_year(current_year);
    for (int current_year = year; current_year < 1970; ++current_year)
        days -= days_in_year(current_year);
    return days;
}

constexpr int days_since_epoch(int year, int month, int day)
{
    return years_to_days_since_epoch(year) + day_of_year(year, month, day);
}

constexpr i64 seconds_since_epoch_to_year(i64 seconds)
{
    constexpr double seconds_per_year = 60.0 * 60.0 * 24.0 * 365.2425;

    // NOTE: We are not using floor() from <math.h> to avoid LibC / DynamicLoader dependency issues.
    auto round_down = [](double value) -> i64 {
        auto as_i64 = static_cast<i64>(value);

        if ((value == as_i64) || (as_i64 >= 0))
            return as_i64;
        return as_i64 - 1;
    };

    auto years_since_epoch = static_cast<double>(seconds) / seconds_per_year;
    return 1970 + round_down(years_since_epoch);
}

/*
 * Represents a time amount in a "safe" way.
 * Minimum: 0 seconds, 0 nanoseconds
 * Maximum: 2**63-1 seconds, 999'999'999 nanoseconds
 * If any operation (e.g. 'from_timeval' or operator-) would over- or underflow, the closest legal value is returned instead.
 * Inputs (e.g. to 'from_timespec') are allowed to be in non-normal form (e.g. "1 second, 2'012'345'678 nanoseconds" or "1 second, -2 microseconds").
 * Outputs (e.g. from 'to_timeval') are always in normal form.
 */
class Time {
public:
    Time() = default;
    Time(Time const&) = default;
    Time& operator=(Time const&) = default;

    Time(Time&& other)
        : m_seconds(exchange(other.m_seconds, 0))
        , m_nanoseconds(exchange(other.m_nanoseconds, 0))
    {
    }
    Time& operator=(Time&& other)
    {
        if (this != &other) {
            m_seconds = exchange(other.m_seconds, 0);
            m_nanoseconds = exchange(other.m_nanoseconds, 0);
        }
        return *this;
    }

private:
    // This must be part of the header in order to make the various 'from_*' functions constexpr.
    // However, sane_mod can only deal with a limited range of values for 'denominator', so this can't be made public.
    ALWAYS_INLINE static constexpr i64 sane_mod(i64& numerator, i64 denominator)
    {
        VERIFY(2 <= denominator && denominator <= 1'000'000'000);
        // '%' in C/C++ does not work in the obvious way:
        // For example, -9 % 7 is -2, not +5.
        // However, we want a representation like "(-2)*7 + (+5)".
        i64 dividend = numerator / denominator;
        numerator %= denominator;
        if (numerator < 0) {
            // Does not overflow: different signs.
            numerator += denominator;
            // Does not underflow: denominator >= 2.
            dividend -= 1;
        }
        return dividend;
    }
    ALWAYS_INLINE static constexpr i32 sane_mod(i32& numerator, i32 denominator)
    {
        i64 numerator_64 = numerator;
        i64 dividend = sane_mod(numerator_64, denominator);
        // Does not underflow: numerator can only become smaller.
        numerator = static_cast<i32>(numerator_64);
        // Does not overflow: Will be smaller than original value of 'numerator'.
        return static_cast<i32>(dividend);
    }

public:
    [[nodiscard]] constexpr static Time from_timestamp(i32 year, u8 month, u8 day, u8 hour, u8 minute, u8 second, u16 millisecond)
    {
        constexpr auto milliseconds_per_day = 86'400'000;
        constexpr auto milliseconds_per_hour = 3'600'000;
        constexpr auto milliseconds_per_minute = 60'000;
        constexpr auto milliseconds_per_second = 1'000;

        i64 milliseconds_since_epoch = days_since_epoch(year, month, day);
        milliseconds_since_epoch *= milliseconds_per_day;

        milliseconds_since_epoch += hour * milliseconds_per_hour;
        milliseconds_since_epoch += minute * milliseconds_per_minute;
        milliseconds_since_epoch += second * milliseconds_per_second;
        milliseconds_since_epoch += millisecond;

        return from_milliseconds(milliseconds_since_epoch);
    }

    [[nodiscard]] constexpr static Time from_seconds(i64 seconds) { return Time(seconds, 0); }
    [[nodiscard]] constexpr static Time from_nanoseconds(i64 nanoseconds)
    {
        i64 seconds = sane_mod(nanoseconds, 1'000'000'000);
        return Time(seconds, nanoseconds);
    }
    [[nodiscard]] constexpr static Time from_microseconds(i64 microseconds)
    {
        i64 seconds = sane_mod(microseconds, 1'000'000);
        return Time(seconds, microseconds * 1'000);
    }
    [[nodiscard]] constexpr static Time from_milliseconds(i64 milliseconds)
    {
        i64 seconds = sane_mod(milliseconds, 1'000);
        return Time(seconds, milliseconds * 1'000'000);
    }
    [[nodiscard]] static Time from_ticks(clock_t, time_t);
    [[nodiscard]] static Time from_timespec(const struct timespec&);
    [[nodiscard]] static Time from_timeval(const struct timeval&);
    // We don't pull in <stdint.h> for the pretty min/max definitions because this file is also included in the Kernel
    [[nodiscard]] constexpr static Time min() { return Time(-__INT64_MAX__ - 1LL, 0); };
    [[nodiscard]] constexpr static Time zero() { return Time(0, 0); };
    [[nodiscard]] constexpr static Time max() { return Time(__INT64_MAX__, 999'999'999); };

#ifndef KERNEL
    [[nodiscard]] static Time now_realtime();
    [[nodiscard]] static Time now_realtime_coarse();
    [[nodiscard]] static Time now_monotonic();
    [[nodiscard]] static Time now_monotonic_coarse();
#endif

    // Truncates towards zero (2.8s to 2s, -2.8s to -2s).
    [[nodiscard]] i64 to_truncated_seconds() const;
    [[nodiscard]] i64 to_truncated_milliseconds() const;
    [[nodiscard]] i64 to_truncated_microseconds() const;
    // Rounds away from zero (2.3s to 3s, -2.3s to -3s).
    [[nodiscard]] i64 to_seconds() const;
    [[nodiscard]] i64 to_milliseconds() const;
    [[nodiscard]] i64 to_microseconds() const;
    [[nodiscard]] i64 to_nanoseconds() const;
    [[nodiscard]] timespec to_timespec() const;
    // Rounds towards -inf (it was the easiest to implement).
    [[nodiscard]] timeval to_timeval() const;

    [[nodiscard]] bool is_zero() const { return (m_seconds == 0) && (m_nanoseconds == 0); }
    [[nodiscard]] bool is_negative() const { return m_seconds < 0; }

    bool operator==(Time const& other) const { return this->m_seconds == other.m_seconds && this->m_nanoseconds == other.m_nanoseconds; }
    Time operator+(Time const& other) const;
    Time& operator+=(Time const& other);
    Time operator-(Time const& other) const;
    Time& operator-=(Time const& other);
    bool operator<(Time const& other) const;
    bool operator<=(Time const& other) const;
    bool operator>(Time const& other) const;
    bool operator>=(Time const& other) const;

private:
    constexpr explicit Time(i64 seconds, u32 nanoseconds)
        : m_seconds(seconds)
        , m_nanoseconds(nanoseconds)
    {
    }

    [[nodiscard]] static Time from_half_sanitized(i64 seconds, i32 extra_seconds, u32 nanoseconds);

    i64 m_seconds { 0 };
    u32 m_nanoseconds { 0 }; // Always less than 1'000'000'000
};

template<typename TimevalType>
inline void timeval_sub(TimevalType const& a, TimevalType const& b, TimevalType& result)
{
    result.tv_sec = a.tv_sec - b.tv_sec;
    result.tv_usec = a.tv_usec - b.tv_usec;
    if (result.tv_usec < 0) {
        --result.tv_sec;
        result.tv_usec += 1'000'000;
    }
}

template<typename TimevalType>
inline void timeval_add(TimevalType const& a, TimevalType const& b, TimevalType& result)
{
    result.tv_sec = a.tv_sec + b.tv_sec;
    result.tv_usec = a.tv_usec + b.tv_usec;
    if (result.tv_usec >= 1'000'000) {
        ++result.tv_sec;
        result.tv_usec -= 1'000'000;
    }
}

template<typename TimespecType>
inline void timespec_sub(TimespecType const& a, TimespecType const& b, TimespecType& result)
{
    result.tv_sec = a.tv_sec - b.tv_sec;
    result.tv_nsec = a.tv_nsec - b.tv_nsec;
    if (result.tv_nsec < 0) {
        --result.tv_sec;
        result.tv_nsec += 1'000'000'000;
    }
}

template<typename TimespecType>
inline void timespec_add(TimespecType const& a, TimespecType const& b, TimespecType& result)
{
    result.tv_sec = a.tv_sec + b.tv_sec;
    result.tv_nsec = a.tv_nsec + b.tv_nsec;
    if (result.tv_nsec >= 1000'000'000) {
        ++result.tv_sec;
        result.tv_nsec -= 1000'000'000;
    }
}

template<typename TimespecType, typename TimevalType>
inline void timespec_add_timeval(TimespecType const& a, TimevalType const& b, TimespecType& result)
{
    result.tv_sec = a.tv_sec + b.tv_sec;
    result.tv_nsec = a.tv_nsec + b.tv_usec * 1000;
    if (result.tv_nsec >= 1000'000'000) {
        ++result.tv_sec;
        result.tv_nsec -= 1000'000'000;
    }
}

template<typename TimevalType, typename TimespecType>
inline void timeval_to_timespec(TimevalType const& tv, TimespecType& ts)
{
    ts.tv_sec = tv.tv_sec;
    ts.tv_nsec = tv.tv_usec * 1000;
}

template<typename TimespecType, typename TimevalType>
inline void timespec_to_timeval(TimespecType const& ts, TimevalType& tv)
{
    tv.tv_sec = ts.tv_sec;
    tv.tv_usec = ts.tv_nsec / 1000;
}

template<TimeSpecType T>
inline bool operator>=(T const& a, T const& b)
{
    return a.tv_sec > b.tv_sec || (a.tv_sec == b.tv_sec && a.tv_nsec >= b.tv_nsec);
}

template<TimeSpecType T>
inline bool operator>(T const& a, T const& b)
{
    return a.tv_sec > b.tv_sec || (a.tv_sec == b.tv_sec && a.tv_nsec > b.tv_nsec);
}

template<TimeSpecType T>
inline bool operator<(T const& a, T const& b)
{
    return a.tv_sec < b.tv_sec || (a.tv_sec == b.tv_sec && a.tv_nsec < b.tv_nsec);
}

template<TimeSpecType T>
inline bool operator<=(T const& a, T const& b)

{
    return a.tv_sec < b.tv_sec || (a.tv_sec == b.tv_sec && a.tv_nsec <= b.tv_nsec);
}

template<TimeSpecType T>
inline bool operator==(T const& a, T const& b)
{
    return a.tv_sec == b.tv_sec && a.tv_nsec == b.tv_nsec;
}

template<TimeSpecType T>
inline bool operator!=(T const& a, T const& b)
{
    return a.tv_sec != b.tv_sec || a.tv_nsec != b.tv_nsec;
}

}

#if USING_AK_GLOBALLY
using AK::day_of_week;
using AK::day_of_year;
using AK::days_in_month;
using AK::days_in_year;
using AK::days_since_epoch;
using AK::is_leap_year;
using AK::seconds_since_epoch_to_year;
using AK::Time;
using AK::timespec_add;
using AK::timespec_add_timeval;
using AK::timespec_sub;
using AK::timespec_to_timeval;
using AK::timeval_add;
using AK::timeval_sub;
using AK::timeval_to_timespec;
using AK::years_to_days_since_epoch;
using AK::operator<=;
using AK::operator<;
using AK::operator>;
using AK::operator>=;
using AK::operator==;
using AK::operator!=;
#endif
