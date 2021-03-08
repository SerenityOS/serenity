/*
 * Copyright (c) 2020, The SerenityOS developers.
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

#include <AK/Assertions.h>
#include <AK/Checked.h>
#include <AK/LogStream.h>
#include <AK/Time.h>

// Make a reasonable guess as to which timespec/timeval definition to use.
// It doesn't really matter, since both are identical.
#ifdef KERNEL
#    include <Kernel/UnixTypes.h>
#else
#    include <sys/time.h>
#endif

namespace AK {

int day_of_year(int year, unsigned month, int day)
{
    VERIFY(month >= 1 && month <= 12);

    static const int seek_table[] = { 0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334 };
    int day_of_year = seek_table[month - 1] + day - 1;

    if (is_leap_year(year) && month >= 3)
        day_of_year++;

    return day_of_year;
}

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
    static const int seek_table[] = { 0, 3, 2, 5, 0, 3, 5, 1, 4, 6, 2, 4 };
    if (month < 3)
        --year;

    return (year + year / 4 - year / 100 + year / 400 + seek_table[month - 1] + day) % 7;
}

ALWAYS_INLINE static i32 sane_mod(i32& numerator, i32 denominator)
{
    VERIFY(2 <= denominator && denominator <= 1'000'000'000);
    // '%' in C/C++ does not work in the obvious way:
    // For example, -9 % 7 is -2, not +5.
    // However, we want a representation like "(-2)*7 + (+5)".
    i32 dividend = numerator / denominator;
    numerator %= denominator;
    if (numerator < 0) {
        // Does not overflow: different signs.
        numerator += denominator;
        // Does not underflow: denominator >= 2.
        dividend -= 1;
    }
    return dividend;
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
        Checked<i64> seconds(m_seconds);
        seconds++;
        return seconds.has_overflow() ? 0x7fff'ffff'ffff'ffffLL : seconds.value();
    }
    return m_seconds;
}
i64 Time::to_truncated_milliseconds() const
{
    VERIFY(m_nanoseconds < 1'000'000'000);
    Checked<i64> milliseconds(m_seconds);
    milliseconds *= 1'000;
    if (!milliseconds.has_overflow()) {
        u32 add_ms = (u32)(m_nanoseconds / 1'000'000);
        if (add_ms) {
            milliseconds += add_ms;
            if (m_seconds < 0 && m_nanoseconds % 1'000'000 != 0)
                milliseconds++;
            if (!milliseconds.has_overflow())
                return milliseconds.value();
        } else {
            return milliseconds.value();
        }
    }
    return m_seconds < 0 ? -0x8000'0000'0000'0000LL : 0x7fff'ffff'ffff'ffffLL;
}
i64 Time::to_truncated_microseconds() const
{
    VERIFY(m_nanoseconds < 1'000'000'000);
    Checked<i64> microseconds(m_seconds);
    microseconds *= 1'000'000;
    if (!microseconds.has_overflow()) {
        u32 add_us = (u32)(m_nanoseconds / 1'000);
        if (add_us) {
            microseconds += add_us;
            if (m_seconds < 0 && m_nanoseconds % 1'000 != 0)
                microseconds++;
            if (!microseconds.has_overflow())
                return microseconds.value();
        } else {
            return microseconds.value();
        }
    }
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
    Checked<i64> milliseconds(m_seconds);
    milliseconds *= 1'000;
    if (!milliseconds.has_overflow()) {
        u32 add_ms = (u32)(m_nanoseconds / 1'000'000);
        if (add_ms) {
            milliseconds += add_ms;
            if (!milliseconds.has_overflow()) {
                if (m_seconds >= 0 && m_nanoseconds % 1'000'000 != 0) {
                    milliseconds++;
                    if (!milliseconds.has_overflow())
                        return milliseconds.value();
                } else {
                    return milliseconds.value();
                }
            }
        } else {
            return milliseconds.value();
        }
    }
    return m_seconds < 0 ? -0x8000'0000'0000'0000LL : 0x7fff'ffff'ffff'ffffLL;
}
i64 Time::to_microseconds() const
{
    VERIFY(m_nanoseconds < 1'000'000'000);
    Checked<i64> microseconds(m_seconds);
    microseconds *= 1'000'000;
    if (!microseconds.has_overflow()) {
        u32 add_us = (u32)(m_nanoseconds / 1'000);
        if (add_us) {
            microseconds += add_us;
            if (!microseconds.has_overflow()) {
                if (m_seconds >= 0 && m_nanoseconds % 1'000 != 0) {
                    microseconds++;
                    if (!microseconds.has_overflow())
                        return microseconds.value();
                } else {
                    return microseconds.value();
                }
            }
        } else {
            return microseconds.value();
        }
    }
    return m_seconds < 0 ? -0x8000'0000'0000'0000LL : 0x7fff'ffff'ffff'ffffLL;
}
i64 Time::to_nanoseconds() const
{
    VERIFY(m_nanoseconds < 1'000'000'000);
    Checked<i64> nanoseconds(m_seconds);
    nanoseconds *= 1'000'000'000;
    if (!nanoseconds.has_overflow()) {
        if (m_nanoseconds) {
            nanoseconds += m_nanoseconds;
            if (!nanoseconds.has_overflow())
                return nanoseconds.value();
        }
        return nanoseconds.value();
    }
    return m_seconds < 0 ? -0x8000'0000'0000'0000LL : 0x7fff'ffff'ffff'ffffLL;
}
timespec Time::to_timespec() const
{
    VERIFY(m_nanoseconds < 1'000'000'000);
    return { static_cast<i64>(m_seconds), static_cast<i32>(m_nanoseconds) };
}
timeval Time::to_timeval() const
{
    VERIFY(m_nanoseconds < 1'000'000'000);
    return { static_cast<i64>(m_seconds), static_cast<i32>(m_nanoseconds) / 1000 };
}

Time Time::operator+(const Time& other) const
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
        extra_secs = 0;
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

Time& Time::operator+=(const Time& other)
{
    *this = *this + other;
    return *this;
}

Time Time::operator-(const Time& other) const
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

Time& Time::operator-=(const Time& other)
{
    *this = *this - other;
    return *this;
}

bool Time::operator<(const Time& other) const
{
    return m_seconds < other.m_seconds || (m_seconds == other.m_seconds && m_nanoseconds < other.m_nanoseconds);
}
bool Time::operator<=(const Time& other) const
{
    return m_seconds < other.m_seconds || (m_seconds == other.m_seconds && m_nanoseconds <= other.m_nanoseconds);
}
bool Time::operator>(const Time& other) const
{
    return m_seconds > other.m_seconds || (m_seconds == other.m_seconds && m_nanoseconds > other.m_nanoseconds);
}
bool Time::operator>=(const Time& other) const
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

}
