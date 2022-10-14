/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Checked.h>
#include <AK/Time.h>

// Make a reasonable guess as to which timespec/timeval definition to use.
// It doesn't really matter, since both are identical.
#ifdef KERNEL
#    include <Kernel/UnixTypes.h>
#else
#    include <sys/time.h>
#    include <time.h>
#endif

namespace AK {

int days_in_month(int year, unsigned month)
{
    VERIFY(month >= 1 && month <= 12);
    if (month == 2)
        return is_leap_year(year) ? 29 : 28;

    bool is_long_month = (month == 1 || month == 3 || month == 5 || month == 7 || month == 8 || month == 10 || month == 12);
    return is_long_month ? 31 : 30;
}

unsigned day_of_week(int year, unsigned month, int day)
{
    VERIFY(month >= 1 && month <= 12);
    constexpr Array seek_table = { 0, 3, 2, 5, 0, 3, 5, 1, 4, 6, 2, 4 };
    if (month < 3)
        --year;

    return (year + year / 4 - year / 100 + year / 400 + seek_table[month - 1] + day) % 7;
}

Time Time::from_ticks(clock_t ticks, time_t ticks_per_second)
{
    auto secs = ticks % ticks_per_second;

    i32 nsecs = 1'000'000'000 * (ticks - (ticks_per_second * secs)) / ticks_per_second;
    i32 extra_secs = sane_mod(nsecs, 1'000'000'000);
    return Time::from_half_sanitized(secs, extra_secs, nsecs);
}

Time Time::from_timespec(const struct timespec& ts)
{
    i32 nsecs = ts.tv_nsec;
    i32 extra_secs = sane_mod(nsecs, 1'000'000'000);
    return Time::from_half_sanitized(ts.tv_sec, extra_secs, nsecs);
}

Time Time::from_timeval(const struct timeval& tv)
{
    i32 usecs = tv.tv_usec;
    i32 extra_secs = sane_mod(usecs, 1'000'000);
    VERIFY(0 <= usecs && usecs < 1'000'000);
    return Time::from_half_sanitized(tv.tv_sec, extra_secs, usecs * 1'000);
}

i64 Time::to_truncated_seconds() const
{
    VERIFY(m_nanoseconds < 1'000'000'000);
    if (m_seconds < 0 && m_nanoseconds) {
        // Since m_seconds is negative, adding 1 can't possibly overflow
        return m_seconds + 1;
    }
    return m_seconds;
}

i64 Time::to_truncated_milliseconds() const
{
    VERIFY(m_nanoseconds < 1'000'000'000);
    Checked<i64> milliseconds((m_seconds < 0) ? m_seconds + 1 : m_seconds);
    milliseconds *= 1'000;
    milliseconds += m_nanoseconds / 1'000'000;
    if (m_seconds < 0) {
        if (m_nanoseconds % 1'000'000 != 0) {
            // Does not overflow: milliseconds <= 1'999.
            milliseconds++;
        }
        // We dropped one second previously, put it back in now that we have handled the rounding.
        milliseconds -= 1'000;
    }
    if (!milliseconds.has_overflow())
        return milliseconds.value();
    return m_seconds < 0 ? -0x8000'0000'0000'0000LL : 0x7fff'ffff'ffff'ffffLL;
}

i64 Time::to_truncated_microseconds() const
{
    VERIFY(m_nanoseconds < 1'000'000'000);
    Checked<i64> microseconds((m_seconds < 0) ? m_seconds + 1 : m_seconds);
    microseconds *= 1'000'000;
    microseconds += m_nanoseconds / 1'000;
    if (m_seconds < 0) {
        if (m_nanoseconds % 1'000 != 0) {
            // Does not overflow: microseconds <= 1'999'999.
            microseconds++;
        }
        // We dropped one second previously, put it back in now that we have handled the rounding.
        microseconds -= 1'000'000;
    }
    if (!microseconds.has_overflow())
        return microseconds.value();
    return m_seconds < 0 ? -0x8000'0000'0000'0000LL : 0x7fff'ffff'ffff'ffffLL;
}

i64 Time::to_seconds() const
{
    VERIFY(m_nanoseconds < 1'000'000'000);
    if (m_seconds >= 0 && m_nanoseconds) {
        Checked<i64> seconds(m_seconds);
        seconds++;
        return seconds.has_overflow() ? 0x7fff'ffff'ffff'ffffLL : seconds.value();
    }
    return m_seconds;
}

i64 Time::to_milliseconds() const
{
    VERIFY(m_nanoseconds < 1'000'000'000);
    Checked<i64> milliseconds((m_seconds < 0) ? m_seconds + 1 : m_seconds);
    milliseconds *= 1'000;
    milliseconds += m_nanoseconds / 1'000'000;
    if (m_seconds >= 0 && m_nanoseconds % 1'000'000 != 0)
        milliseconds++;
    if (m_seconds < 0) {
        // We dropped one second previously, put it back in now that we have handled the rounding.
        milliseconds -= 1'000;
    }
    if (!milliseconds.has_overflow())
        return milliseconds.value();
    return m_seconds < 0 ? -0x8000'0000'0000'0000LL : 0x7fff'ffff'ffff'ffffLL;
}

i64 Time::to_microseconds() const
{
    VERIFY(m_nanoseconds < 1'000'000'000);
    Checked<i64> microseconds((m_seconds < 0) ? m_seconds + 1 : m_seconds);
    microseconds *= 1'000'000;
    microseconds += m_nanoseconds / 1'000;
    if (m_seconds >= 0 && m_nanoseconds % 1'000 != 0)
        microseconds++;
    if (m_seconds < 0) {
        // We dropped one second previously, put it back in now that we have handled the rounding.
        microseconds -= 1'000'000;
    }
    if (!microseconds.has_overflow())
        return microseconds.value();
    return m_seconds < 0 ? -0x8000'0000'0000'0000LL : 0x7fff'ffff'ffff'ffffLL;
}

i64 Time::to_nanoseconds() const
{
    VERIFY(m_nanoseconds < 1'000'000'000);
    Checked<i64> nanoseconds((m_seconds < 0) ? m_seconds + 1 : m_seconds);
    nanoseconds *= 1'000'000'000;
    nanoseconds += m_nanoseconds;
    if (m_seconds < 0) {
        // We dropped one second previously, put it back in now that we have handled the rounding.
        nanoseconds -= 1'000'000'000;
    }
    if (!nanoseconds.has_overflow())
        return nanoseconds.value();
    return m_seconds < 0 ? -0x8000'0000'0000'0000LL : 0x7fff'ffff'ffff'ffffLL;
}

timespec Time::to_timespec() const
{
    VERIFY(m_nanoseconds < 1'000'000'000);
    return { static_cast<time_t>(m_seconds), static_cast<long>(m_nanoseconds) };
}

timeval Time::to_timeval() const
{
    VERIFY(m_nanoseconds < 1'000'000'000);
    // This is done because winsock defines tv_sec and tv_usec as long, and Linux64 as long int.
    using sec_type = decltype(declval<timeval>().tv_sec);
    using usec_type = decltype(declval<timeval>().tv_usec);
    return { static_cast<sec_type>(m_seconds), static_cast<usec_type>(m_nanoseconds) / 1000 };
}

Time Time::operator+(Time const& other) const
{
    VERIFY(m_nanoseconds < 1'000'000'000);
    VERIFY(other.m_nanoseconds < 1'000'000'000);

    u32 new_nsecs = m_nanoseconds + other.m_nanoseconds;
    u32 extra_secs = new_nsecs / 1'000'000'000;
    new_nsecs %= 1'000'000'000;

    i64 this_secs = m_seconds;
    i64 other_secs = other.m_seconds;
    // We would like to just add "this_secs + other_secs + extra_secs".
    // However, computing this naively may overflow even though the result is in-bounds.
    // Example in 8-bit: (-127) + (-2) + (+1) = (-128), which fits in an i8.
    // Example in 8-bit, the other way around: (-2) + (127) + (+1) = 126.
    // So we do something more sophisticated:
    if (extra_secs) {
        VERIFY(extra_secs == 1);
        if (this_secs != 0x7fff'ffff'ffff'ffff) {
            this_secs += 1;
        } else if (other_secs != 0x7fff'ffff'ffff'ffff) {
            other_secs += 1;
        } else {
            /* If *both* are INT64_MAX, then adding them will overflow in any case. */
            return Time::max();
        }
    }

    Checked<i64> new_secs { this_secs };
    new_secs += other_secs;
    if (new_secs.has_overflow()) {
        if (other_secs > 0)
            return Time::max();
        else
            return Time::min();
    }

    return Time { new_secs.value(), new_nsecs };
}

Time& Time::operator+=(Time const& other)
{
    *this = *this + other;
    return *this;
}

Time Time::operator-(Time const& other) const
{
    VERIFY(m_nanoseconds < 1'000'000'000);
    VERIFY(other.m_nanoseconds < 1'000'000'000);

    if (other.m_nanoseconds)
        return *this + Time((i64) ~(u64)other.m_seconds, 1'000'000'000 - other.m_nanoseconds);

    if (other.m_seconds != (i64)-0x8000'0000'0000'0000)
        return *this + Time(-other.m_seconds, 0);

    // Only remaining case: We want to subtract -0x8000'0000'0000'0000 seconds,
    // i.e. add a very large number.

    if (m_seconds >= 0)
        return Time::max();
    return Time { (m_seconds + 0x4000'0000'0000'0000) + 0x4000'0000'0000'0000, m_nanoseconds };
}

Time& Time::operator-=(Time const& other)
{
    *this = *this - other;
    return *this;
}

bool Time::operator<(Time const& other) const
{
    return m_seconds < other.m_seconds || (m_seconds == other.m_seconds && m_nanoseconds < other.m_nanoseconds);
}

bool Time::operator<=(Time const& other) const
{
    return m_seconds < other.m_seconds || (m_seconds == other.m_seconds && m_nanoseconds <= other.m_nanoseconds);
}

bool Time::operator>(Time const& other) const
{
    return m_seconds > other.m_seconds || (m_seconds == other.m_seconds && m_nanoseconds > other.m_nanoseconds);
}

bool Time::operator>=(Time const& other) const
{
    return m_seconds > other.m_seconds || (m_seconds == other.m_seconds && m_nanoseconds >= other.m_nanoseconds);
}

Time Time::from_half_sanitized(i64 seconds, i32 extra_seconds, u32 nanoseconds)
{
    VERIFY(nanoseconds < 1'000'000'000);

    if ((seconds <= 0 && extra_seconds > 0) || (seconds >= 0 && extra_seconds < 0)) {
        // Opposite signs mean that we can definitely add them together without fear of overflowing i64:
        seconds += extra_seconds;
        extra_seconds = 0;
    }

    // Now the only possible way to become invalid is overflowing i64 towards positive infinity:
    if (Checked<i64>::addition_would_overflow<i64, i64>(seconds, extra_seconds)) {
        if (seconds < 0) {
            return Time::min();
        } else {
            return Time::max();
        }
    }

    return Time { seconds + extra_seconds, nanoseconds };
}

#ifndef KERNEL
namespace {
static Time now_time_from_clock(clockid_t clock_id)
{
    timespec now_spec {};
    ::clock_gettime(clock_id, &now_spec);
    return Time::from_timespec(now_spec);
}
}

Time Time::now_realtime()
{
    return now_time_from_clock(CLOCK_REALTIME);
}

Time Time::now_realtime_coarse()
{
    return now_time_from_clock(CLOCK_REALTIME_COARSE);
}

Time Time::now_monotonic()
{
    return now_time_from_clock(CLOCK_MONOTONIC);
}

Time Time::now_monotonic_coarse()
{
    return now_time_from_clock(CLOCK_MONOTONIC_COARSE);
}

#endif

}
