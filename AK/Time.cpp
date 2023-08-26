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
#    include <AK/CharacterTypes.h>
#    include <AK/DateConstants.h>
#    include <errno.h>
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

#ifndef KERNEL
Optional<struct tm> convert_formatted_string_to_timespec(StringView input, StringView format, size_t& string_pos)
{
    [[maybe_unused]] size_t format_pos = 0;
    auto tm_or_void = convert_formatted_string_to_timespec(input, format, string_pos, format_pos);
    if (!tm_or_void.has_value())
        return {};
    return tm_or_void.release_value();
}

Optional<struct tm> convert_formatted_string_to_timespec_restrictively(StringView input, StringView format)
{
    size_t string_pos = 0;
    size_t format_pos = 0;
    auto tm_or_void = convert_formatted_string_to_timespec(input, format, string_pos, format_pos);
    if (!tm_or_void.has_value())
        return {};
    if (string_pos != input.length() || format_pos != format.length())
        return {};
    return tm_or_void.release_value();
}

Optional<struct tm> convert_formatted_string_to_timespec(StringView string, StringView format, size_t& string_pos, size_t& format_pos)
{
    struct tm tm;
    tm.tm_isdst = -1;

    auto parsing_failed = false;
    auto tm_represents_utc_time = false;

    auto parse_number = [&] {
        if (string_pos >= string.length()) {
            parsing_failed = true;
            return 0;
        }

        char* end_ptr = nullptr;
        errno = 0;
        int number = strtol(string.characters_without_null_termination() + string_pos, &end_ptr, 10);

        auto chars_parsed = end_ptr - (string.characters_without_null_termination() + string_pos);
        if (chars_parsed == 0 || errno != 0)
            parsing_failed = true;
        else
            string_pos += chars_parsed;
        return number;
    };

    auto consume = [&](char x) {
        if (string_pos >= string.length()) {
            parsing_failed = true;
            return;
        }
        if (string[string_pos] != x)
            parsing_failed = true;
        else
            string_pos++;
    };

    while (format_pos < format.length() && string_pos < string.length()) {
        if (format[format_pos] != '%') {
            if (format[format_pos] != string[string_pos]) {
                return {};
            }
            format_pos++;
            string_pos++;
            continue;
        }

        format_pos++;
        if (format_pos == format.length()) {
            return {};
        }
        switch (format[format_pos]) {
        case 'a': {
            auto wday = 0;
            for (auto name : short_day_names) {
                if (string.substring_view(string_pos).starts_with(name, AK::CaseSensitivity::CaseInsensitive)) {
                    string_pos += name.length();
                    tm.tm_wday = wday;
                    break;
                }
                ++wday;
            }
            if (wday == 7)
                return {};
            break;
        }
        case 'A': {
            auto wday = 0;
            for (auto name : long_day_names) {
                if (string.substring_view(string_pos).starts_with(name, AK::CaseSensitivity::CaseInsensitive)) {
                    string_pos += name.length();
                    tm.tm_wday = wday;
                    break;
                }
                ++wday;
            }
            if (wday == 7)
                return {};
            break;
        }
        case 'h':
        case 'b': {
            auto mon = 0;
            for (auto name : short_month_names) {
                if (string.substring_view(string_pos).starts_with(name, AK::CaseSensitivity::CaseInsensitive)) {
                    string_pos += name.length();
                    tm.tm_mon = mon;
                    break;
                }
                ++mon;
            }
            if (mon == 12)
                return {};
            break;
        }
        case 'B': {
            auto mon = 0;
            for (auto name : long_month_names) {
                if (string.substring_view(string_pos).starts_with(name, AK::CaseSensitivity::CaseInsensitive)) {
                    string_pos += name.length();
                    tm.tm_mon = mon;
                    break;
                }
                ++mon;
            }
            if (mon == 12)
                return {};
            break;
        }
        case 'C': {
            int num = parse_number();
            tm.tm_year = (num - 19) * 100;
            break;
        }
        case 'd': {
            tm.tm_mday = parse_number();
            break;
        }
        case 'D': {
            int mon = parse_number();
            consume('/');
            int day = parse_number();
            consume('/');
            int year = parse_number();
            tm.tm_mon = mon + 1;
            tm.tm_mday = day;
            tm.tm_year = (year + 1900) % 100;
            break;
        }
        case 'e': {
            tm.tm_mday = parse_number();
            break;
        }
        case 'H': {
            tm.tm_hour = parse_number();
            break;
        }
        case 'I': {
            int num = parse_number();
            tm.tm_hour = num % 12;
            break;
        }
        case 'j': {
            // a little trickery here... we can get mktime() to figure out mon and mday using out of range values.
            // yday is not used so setting it is pointless.
            tm.tm_mday = parse_number();
            tm.tm_mon = 0;
            mktime(&tm);
            break;
        }
        case 'm': {
            int num = parse_number();
            tm.tm_mon = num - 1;
            break;
        }
        case 'M': {
            tm.tm_min = parse_number();
            break;
        }
        case 'n':
        case 't':
            while (is_ascii_blank(string[string_pos])) {
                string_pos++;
            }
            break;
        case 'p': {
            auto ampm = string.substring_view(string_pos, 2);
            if (ampm == "PM" && tm.tm_hour < 12) {
                tm.tm_hour += 12;
            }
            string_pos += 2;
            break;
        }
        case 'r': {
            auto ampm = string.substring_view(string_pos, 2);
            if (ampm == "PM" && tm.tm_hour < 12) {
                tm.tm_hour += 12;
            }
            string_pos += 2;
            break;
        }
        case 'R': {
            tm.tm_hour = parse_number();
            consume(':');
            tm.tm_min = parse_number();
            break;
        }
        case 'S':
            tm.tm_sec = parse_number();
            break;
        case 'T':
            tm.tm_hour = parse_number();
            consume(':');
            tm.tm_min = parse_number();
            consume(':');
            tm.tm_sec = parse_number();
            break;
        case 'w':
            tm.tm_wday = parse_number();
            break;
        case 'y': {
            int year = parse_number();
            tm.tm_year = year <= 99 && year > 69 ? 1900 + year : 2000 + year;
            break;
        }
        case 'Y': {
            int year = parse_number();
            tm.tm_year = year - 1900;
            break;
        }
        case 'z': {
            tm_represents_utc_time = true;
            if (string[string_pos] == 'Z') {
                // UTC time
                string_pos++;
                break;
            }
            int sign;

            if (string[string_pos] == '+')
                sign = -1;
            else if (string[string_pos] == '-')
                sign = +1;
            else
                return {};

            string_pos++;

            auto hours = parse_number();
            int minutes;
            if (string_pos < string.length() && string[string_pos] == ':') {
                string_pos++;
                minutes = parse_number();
            } else {
                minutes = hours % 100;
                hours = hours / 100;
            }

            tm.tm_hour += sign * hours;
            tm.tm_min += sign * minutes;
            break;
        }
        case '%':
            if (string[string_pos] != '%') {
                return {};
            }
            string_pos += 1;
            break;
        default:
            parsing_failed = true;
            break;
        }

        if (parsing_failed) {
            return {};
        }

        format_pos++;
    }

    // If an explicit timezone was present, the time in tm was shifted to UTC.
    // Convert it to local time, since that is what `mktime` expects.
    if (tm_represents_utc_time) {
        auto utc_time = timegm(&tm);
        localtime_r(&utc_time, &tm);
    }

    return tm;
}
#endif

Duration Duration::from_ticks(clock_t ticks, time_t ticks_per_second)
{
    auto secs = ticks % ticks_per_second;

    i32 nsecs = 1'000'000'000 * (ticks - (ticks_per_second * secs)) / ticks_per_second;
    i32 extra_secs = sane_mod(nsecs, 1'000'000'000);
    return Duration::from_half_sanitized(secs, extra_secs, nsecs);
}

Duration Duration::from_timespec(const struct timespec& ts)
{
    i32 nsecs = ts.tv_nsec;
    i32 extra_secs = sane_mod(nsecs, 1'000'000'000);
    return Duration::from_half_sanitized(ts.tv_sec, extra_secs, nsecs);
}

Duration Duration::from_timeval(const struct timeval& tv)
{
    i32 usecs = tv.tv_usec;
    i32 extra_secs = sane_mod(usecs, 1'000'000);
    VERIFY(0 <= usecs && usecs < 1'000'000);
    return Duration::from_half_sanitized(tv.tv_sec, extra_secs, usecs * 1'000);
}

i64 Duration::to_truncated_seconds() const
{
    VERIFY(m_nanoseconds < 1'000'000'000);
    if (m_seconds < 0 && m_nanoseconds) {
        // Since m_seconds is negative, adding 1 can't possibly overflow
        return m_seconds + 1;
    }
    return m_seconds;
}

i64 Duration::to_truncated_milliseconds() const
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

i64 Duration::to_truncated_microseconds() const
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

i64 Duration::to_seconds() const
{
    VERIFY(m_nanoseconds < 1'000'000'000);
    if (m_seconds >= 0 && m_nanoseconds) {
        Checked<i64> seconds(m_seconds);
        seconds++;
        return seconds.has_overflow() ? 0x7fff'ffff'ffff'ffffLL : seconds.value();
    }
    return m_seconds;
}

i64 Duration::to_milliseconds() const
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

i64 Duration::to_microseconds() const
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

i64 Duration::to_nanoseconds() const
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

timespec Duration::to_timespec() const
{
    VERIFY(m_nanoseconds < 1'000'000'000);
    return { static_cast<time_t>(m_seconds), static_cast<long>(m_nanoseconds) };
}

timeval Duration::to_timeval() const
{
    VERIFY(m_nanoseconds < 1'000'000'000);
    // This is done because winsock defines tv_sec and tv_usec as long, and Linux64 as long int.
    using sec_type = decltype(declval<timeval>().tv_sec);
    using usec_type = decltype(declval<timeval>().tv_usec);
    return { static_cast<sec_type>(m_seconds), static_cast<usec_type>(m_nanoseconds) / 1000 };
}

Duration Duration::from_half_sanitized(i64 seconds, i32 extra_seconds, u32 nanoseconds)
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
            return Duration::min();
        } else {
            return Duration::max();
        }
    }

    return Duration { seconds + extra_seconds, nanoseconds };
}

#ifndef KERNEL
namespace {
static Duration now_time_from_clock(clockid_t clock_id)
{
    timespec now_spec {};
    ::clock_gettime(clock_id, &now_spec);
    return Duration::from_timespec(now_spec);
}
}

MonotonicTime MonotonicTime::now()
{
    return MonotonicTime { now_time_from_clock(CLOCK_MONOTONIC) };
}

MonotonicTime MonotonicTime::now_coarse()
{
    return MonotonicTime { now_time_from_clock(CLOCK_MONOTONIC_COARSE) };
}

UnixDateTime UnixDateTime::now()
{
    return UnixDateTime { now_time_from_clock(CLOCK_REALTIME) };
}

UnixDateTime UnixDateTime::now_coarse()
{
    return UnixDateTime { now_time_from_clock(CLOCK_REALTIME_COARSE) };
}

#endif

}
