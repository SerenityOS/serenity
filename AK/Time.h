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

// Concept to detect types which look like timespec without requiring the type.
template<typename T>
concept TimeSpecType = requires(T t)
{
    t.tv_sec;
    t.tv_nsec;
};

// FIXME: remove once Clang formats these properly.
// clang-format off
namespace AK {

constexpr bool is_leap_year(int year)
{
    return year % 4 == 0 && (year % 100 != 0 || year % 400 == 0);
}

constexpr unsigned days_in_year(int year)
{
    return 365 + is_leap_year(year);
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

// Month and day start at 1. Month must be >= 1 and <= 12.
// The return value is 0-indexed, that is 0 is Sunday, 1 is Monday, etc.
// Day may be negative or larger than the number of days
// in the given month.
constexpr unsigned day_of_week(int year, unsigned month, int day)
{
    VERIFY(month >= 1 && month <= 12);
    constexpr Array seek_table = { 0, 3, 2, 5, 0, 3, 5, 1, 4, 6, 2, 4 };
    if (month < 3)
        --year;

    return (year + year / 4 - year / 100 + year / 400 + seek_table[month - 1] + day) % 7;
}

// Month and day start at 1. Month must be >= 1 and <= 12.
// The return value is 0-indexed, that is Jan 1 is day 0.
// Day may be negative or larger than the number of days
// in the given month. If day is negative enough, the result
// can be negative.
constexpr int day_of_year(int year, unsigned month, int day)
{
    VERIFY(month >= 1 && month <= 12);

    constexpr Array seek_table = { 0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334 };
    auto day_of_year = seek_table[month - 1] + day - 1;

    if (is_leap_year(year) && month >= 3)
        ++day_of_year;

    return day_of_year;
}

// Month starts at 1. Month must be >= 1 and <= 12.
constexpr int days_in_month(int year, unsigned month)
{
    VERIFY(month >= 1 && month <= 12);
    if (month == 2)
        return is_leap_year(year) ? 29 : 28;

    bool const is_long_month = (month == 1 ||
                                month == 3 ||
                                month == 5 ||
                                month == 7 ||
                                month == 8 ||
                                month == 10 ||
                                month == 12);
    return is_long_month ? 31 : 30;
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
    constexpr Time() = default;
    constexpr Time(const Time&) = default;
    constexpr Time& operator=(const Time&) = default;
    constexpr Time(Time&& other) = default;
    constexpr Time& operator=(Time&& other) = default;

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
        numerator = numerator_64;
        // Does not overflow: Will be smaller than original value of 'numerator'.
        return dividend;
    }

public:
    constexpr static Time from_seconds(i64 seconds) { return Time(seconds, 0); }
    constexpr static Time from_nanoseconds(i64 nanoseconds)
    {
        i64 seconds = sane_mod(nanoseconds, 1'000'000'000);
        return Time(seconds, nanoseconds);
    }
    constexpr static Time from_microseconds(i64 microseconds)
    {
        i64 seconds = sane_mod(microseconds, 1'000'000);
        return Time(seconds, microseconds * 1'000);
    }
    constexpr static Time from_milliseconds(i64 milliseconds)
    {
        i64 seconds = sane_mod(milliseconds, 1'000);
        return Time(seconds, milliseconds * 1'000'000);
    }
    static Time from_timespec(const struct timespec&);
    static Time from_timeval(const struct timeval&);
    constexpr static Time min() { return Time(-0x8000'0000'0000'0000LL, 0); };
    constexpr static Time zero() { return Time(0, 0); };
    constexpr static Time max() { return Time(0x7fff'ffff'ffff'ffffLL, 999'999'999); };

    // Truncates towards zero (2.8s to 2s, -2.8s to -2s).
    i64 to_truncated_seconds() const;
    i64 to_truncated_milliseconds() const;
    i64 to_truncated_microseconds() const;
    // Rounds away from zero (2.3s to 3s, -2.3s to -3s).
    i64 to_seconds() const;
    i64 to_milliseconds() const;
    i64 to_microseconds() const;
    i64 to_nanoseconds() const;
    timespec to_timespec() const;
    // Rounds towards -inf (it was the easiest to implement).
    timeval to_timeval() const;

    constexpr bool is_zero() const { return !m_seconds && !m_nanoseconds; }

    constexpr bool operator==(const Time& other) const { return this->m_seconds == other.m_seconds && this->m_nanoseconds == other.m_nanoseconds; }
    constexpr bool operator!=(const Time& other) const { return !(*this == other); }
    Time operator+(const Time& other) const;
    Time& operator+=(const Time& other);
    Time operator-(const Time& other) const;
    Time& operator-=(const Time& other);
    bool operator<(const Time& other) const;
    bool operator<=(const Time& other) const;
    bool operator>(const Time& other) const;
    bool operator>=(const Time& other) const;

private:
    constexpr explicit Time(i64 seconds, u32 nanoseconds)
        : m_seconds(seconds)
        , m_nanoseconds(nanoseconds)
    {
    }

    static Time from_half_sanitized(i64 seconds, i32 extra_seconds, u32 nanoseconds);

    i64 m_seconds { 0 };
    u32 m_nanoseconds { 0 }; // Always less than 1'000'000'000
};

template<typename TimevalType>
inline void timeval_sub(const TimevalType& a, const TimevalType& b, TimevalType& result)
{
    result.tv_sec = a.tv_sec - b.tv_sec;
    result.tv_usec = a.tv_usec - b.tv_usec;
    if (result.tv_usec < 0) {
        --result.tv_sec;
        result.tv_usec += 1'000'000;
    }
}

template<typename TimevalType>
inline void timeval_add(const TimevalType& a, const TimevalType& b, TimevalType& result)
{
    result.tv_sec = a.tv_sec + b.tv_sec;
    result.tv_usec = a.tv_usec + b.tv_usec;
    if (result.tv_usec >= 1'000'000) {
        ++result.tv_sec;
        result.tv_usec -= 1'000'000;
    }
}

template<typename TimespecType>
inline void timespec_sub(const TimespecType& a, const TimespecType& b, TimespecType& result)
{
    result.tv_sec = a.tv_sec - b.tv_sec;
    result.tv_nsec = a.tv_nsec - b.tv_nsec;
    if (result.tv_nsec < 0) {
        --result.tv_sec;
        result.tv_nsec += 1'000'000'000;
    }
}

template<typename TimespecType>
inline void timespec_add(const TimespecType& a, const TimespecType& b, TimespecType& result)
{
    result.tv_sec = a.tv_sec + b.tv_sec;
    result.tv_nsec = a.tv_nsec + b.tv_nsec;
    if (result.tv_nsec >= 1000'000'000) {
        ++result.tv_sec;
        result.tv_nsec -= 1000'000'000;
    }
}

template<typename TimespecType, typename TimevalType>
inline void timespec_add_timeval(const TimespecType& a, const TimevalType& b, TimespecType& result)
{
    result.tv_sec = a.tv_sec + b.tv_sec;
    result.tv_nsec = a.tv_nsec + b.tv_usec * 1000;
    if (result.tv_nsec >= 1000'000'000) {
        ++result.tv_sec;
        result.tv_nsec -= 1000'000'000;
    }
}

template<typename TimevalType, typename TimespecType>
inline void timeval_to_timespec(const TimevalType& tv, TimespecType& ts)
{
    ts.tv_sec = tv.tv_sec;
    ts.tv_nsec = tv.tv_usec * 1000;
}

template<typename TimespecType, typename TimevalType>
inline void timespec_to_timeval(const TimespecType& ts, TimevalType& tv)
{
    tv.tv_sec = ts.tv_sec;
    tv.tv_usec = ts.tv_nsec / 1000;
}

template<TimeSpecType T>
inline bool operator>=(const T& a, const T& b)
{
    return a.tv_sec > b.tv_sec || (a.tv_sec == b.tv_sec && a.tv_nsec >= b.tv_nsec);
}

template<TimeSpecType T>
inline bool operator>(const T& a, const T& b)
{
    return a.tv_sec > b.tv_sec || (a.tv_sec == b.tv_sec && a.tv_nsec > b.tv_nsec);
}

template<TimeSpecType T>
inline bool operator<(const T& a, const T& b)
{
    return a.tv_sec < b.tv_sec || (a.tv_sec == b.tv_sec && a.tv_nsec < b.tv_nsec);
}

template<TimeSpecType T>
inline bool operator<=(const T& a, const T& b)

{
    return a.tv_sec < b.tv_sec || (a.tv_sec == b.tv_sec && a.tv_nsec <= b.tv_nsec);
}

template<TimeSpecType T>
inline bool operator==(const T& a, const T& b)
{
    return a.tv_sec == b.tv_sec && a.tv_nsec == b.tv_nsec;
}

template<TimeSpecType T>
inline bool operator!=(const T& a, const T& b)
{
    return a.tv_sec != b.tv_sec || a.tv_nsec != b.tv_nsec;
}

}
// clang-format on

using AK::day_of_week;
using AK::day_of_year;
using AK::days_in_month;
using AK::days_in_year;
using AK::is_leap_year;
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
