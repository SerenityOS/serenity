/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/Platform.h>
#include <AK/Types.h>

// Kernel and Userspace pull in the definitions from different places.
// Avoid trying to figure out which one.
struct timeval;
struct timespec;

namespace AK {

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
int day_of_year(int year, unsigned month, int day);

// Month starts at 1. Month must be >= 1 and <= 12.
int days_in_month(int year, unsigned month);

inline bool is_leap_year(int year)
{
    return year % 4 == 0 && (year % 100 != 0 || year % 400 == 0);
}

inline unsigned days_in_year(int year)
{
    return 365 + is_leap_year(year);
}

inline int years_to_days_since_epoch(int year)
{
    int days = 0;
    for (int current_year = 1970; current_year < year; ++current_year)
        days += days_in_year(current_year);
    for (int current_year = year; current_year < 1970; ++current_year)
        days -= days_in_year(current_year);
    return days;
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
    Time(const Time&) = default;

    static Time from_seconds(i64 seconds) { return Time(seconds, 0); }
    static Time from_nanoseconds(i32 nanoseconds);
    static Time from_timespec(const struct timespec&);
    static Time from_timeval(const struct timeval&);
    static Time min() { return Time(-0x8000'0000'0000'0000LL, 0); };
    static Time zero() { return Time(0, 0); };
    static Time max() { return Time(0x7fff'ffff'ffff'ffffLL, 999'999'999); };

    // Truncates "2.8 seconds" to 2 seconds.
    // Truncates "-2.8 seconds" to -2 seconds.
    i64 to_truncated_seconds() const;
    timespec to_timespec() const;
    timeval to_timeval() const;

    bool operator==(const Time& other) const { return this->m_seconds == other.m_seconds && this->m_nanoseconds == other.m_nanoseconds; }
    bool operator!=(const Time& other) const { return !(*this == other); }
    Time operator+(const Time& other) const;
    Time operator-(const Time& other) const;
    bool operator<(const Time& other) const;
    bool operator<=(const Time& other) const;
    bool operator>(const Time& other) const;
    bool operator>=(const Time& other) const;

private:
    explicit Time(i64 seconds, u32 nanoseconds)
        : m_seconds(seconds)
        , m_nanoseconds(nanoseconds)
    {
    }

    static Time from_half_sanitized(i64 seconds, i32 extra_seconds, u32 nanoseconds);

    i64 m_seconds;
    u32 m_nanoseconds; // Always less than 1'000'000'000
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

template<typename TimespecType>
inline bool operator>=(const TimespecType& a, const TimespecType& b)
{
    return a.tv_sec > b.tv_sec || (a.tv_sec == b.tv_sec && a.tv_nsec >= b.tv_nsec);
}

template<typename TimespecType>
inline bool operator>(const TimespecType& a, const TimespecType& b)
{
    return a.tv_sec > b.tv_sec || (a.tv_sec == b.tv_sec && a.tv_nsec > b.tv_nsec);
}

template<typename TimespecType>
inline bool operator<(const TimespecType& a, const TimespecType& b)
{
    return a.tv_sec < b.tv_sec || (a.tv_sec == b.tv_sec && a.tv_nsec < b.tv_nsec);
}

template<typename TimespecType>
inline bool operator<=(const TimespecType& a, const TimespecType& b)
{
    return a.tv_sec < b.tv_sec || (a.tv_sec == b.tv_sec && a.tv_nsec <= b.tv_nsec);
}

template<typename TimespecType>
inline bool operator==(const TimespecType& a, const TimespecType& b)
{
    return a.tv_sec == b.tv_sec && a.tv_nsec == b.tv_nsec;
}

template<typename TimespecType>
inline bool operator!=(const TimespecType& a, const TimespecType& b)
{
    return a.tv_sec != b.tv_sec || a.tv_nsec != b.tv_nsec;
}

}

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
