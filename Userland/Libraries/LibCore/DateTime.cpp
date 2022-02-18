/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <AK/StringBuilder.h>
#include <AK/Time.h>
#include <LibCore/DateTime.h>
#include <errno.h>
#include <time.h>

namespace Core {

DateTime DateTime::now()
{
    return from_timestamp(time(nullptr));
}

DateTime DateTime::create(int year, int month, int day, int hour, int minute, int second)
{
    DateTime dt;
    dt.set_time(year, month, day, hour, minute, second);
    return dt;
}

DateTime DateTime::from_timestamp(time_t timestamp)
{
    struct tm tm;
    localtime_r(&timestamp, &tm);
    DateTime dt;
    dt.m_year = tm.tm_year + 1900;
    dt.m_month = tm.tm_mon + 1;
    dt.m_day = tm.tm_mday;
    dt.m_hour = tm.tm_hour;
    dt.m_minute = tm.tm_min;
    dt.m_second = tm.tm_sec;
    dt.m_timestamp = timestamp;
    return dt;
}

unsigned DateTime::weekday() const
{
    return ::day_of_week(m_year, m_month, m_day);
}

unsigned DateTime::days_in_month() const
{
    return ::days_in_month(m_year, m_month);
}

unsigned DateTime::day_of_year() const
{
    return ::day_of_year(m_year, m_month, m_day);
}

bool DateTime::is_leap_year() const
{
    return ::is_leap_year(m_year);
}

void DateTime::set_time(int year, int month, int day, int hour, int minute, int second)
{
    struct tm tm = {};
    tm.tm_sec = second;
    tm.tm_min = minute;
    tm.tm_hour = hour;
    tm.tm_mday = day;
    tm.tm_mon = month - 1;
    tm.tm_year = year - 1900;
    tm.tm_isdst = -1;
    // mktime() doesn't read tm.tm_wday and tm.tm_yday, no need to fill them in.

    m_timestamp = mktime(&tm);

    // mktime() normalizes the components to the right ranges (Jan 32 -> Feb 1 etc), so read fields back out from tm.
    m_year = tm.tm_year + 1900;
    m_month = tm.tm_mon + 1;
    m_day = tm.tm_mday;
    m_hour = tm.tm_hour;
    m_minute = tm.tm_min;
    m_second = tm.tm_sec;
}

String DateTime::to_string(StringView format) const
{
    const char wday_short_names[7][4] = {
        "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"
    };
    const char wday_long_names[7][10] = {
        "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
    };
    const char mon_short_names[12][4] = {
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    };
    const char mon_long_names[12][10] = {
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    };

    struct tm tm;
    localtime_r(&m_timestamp, &tm);
    StringBuilder builder;
    const int format_len = format.length();

    auto format_time_zone_offset = [&](bool with_separator) {
#ifndef __FreeBSD__
        auto offset_seconds = -timezone;
#else
        auto offset_seconds = 0;
#endif
        StringView offset_sign;

        if (offset_seconds >= 0) {
            offset_sign = "+"sv;
        } else {
            offset_sign = "-"sv;
            offset_seconds *= -1;
        }

        auto offset_hours = offset_seconds / 3600;
        auto offset_minutes = (offset_seconds % 3600) / 60;
        auto separator = with_separator ? ":"sv : ""sv;

        builder.appendff("{}{:02}{}{:02}", offset_sign, offset_hours, separator, offset_minutes);
    };

    for (int i = 0; i < format_len; ++i) {
        if (format[i] != '%') {
            builder.append(format[i]);
        } else {
            if (++i == format_len)
                return String();

            switch (format[i]) {
            case 'a':
                builder.append(wday_short_names[tm.tm_wday]);
                break;
            case 'A':
                builder.append(wday_long_names[tm.tm_wday]);
                break;
            case 'b':
                builder.append(mon_short_names[tm.tm_mon]);
                break;
            case 'B':
                builder.append(mon_long_names[tm.tm_mon]);
                break;
            case 'C':
                builder.appendff("{:02}", (tm.tm_year + 1900) / 100);
                break;
            case 'd':
                builder.appendff("{:02}", tm.tm_mday);
                break;
            case 'D':
                builder.appendff("{:02}/{:02}/{:02}", tm.tm_mon + 1, tm.tm_mday, (tm.tm_year + 1900) % 100);
                break;
            case 'e':
                builder.appendff("{:2}", tm.tm_mday);
                break;
            case 'h':
                builder.append(mon_short_names[tm.tm_mon]);
                break;
            case 'H':
                builder.appendff("{:02}", tm.tm_hour);
                break;
            case 'I':
                builder.appendff("{:02}", tm.tm_hour % 12);
                break;
            case 'j':
                builder.appendff("{:03}", tm.tm_yday + 1);
                break;
            case 'm':
                builder.appendff("{:02}", tm.tm_mon + 1);
                break;
            case 'M':
                builder.appendff("{:02}", tm.tm_min);
                break;
            case 'n':
                builder.append('\n');
                break;
            case 'p':
                builder.append(tm.tm_hour < 12 ? "a.m." : "p.m.");
                break;
            case 'r':
                builder.appendff("{:02}:{:02}:{:02} {}", tm.tm_hour % 12, tm.tm_min, tm.tm_sec, tm.tm_hour < 12 ? "a.m." : "p.m.");
                break;
            case 'R':
                builder.appendff("{:02}:{:02}", tm.tm_hour, tm.tm_min);
                break;
            case 'S':
                builder.appendff("{:02}", tm.tm_sec);
                break;
            case 't':
                builder.append('\t');
                break;
            case 'T':
                builder.appendff("{:02}:{:02}:{:02}", tm.tm_hour, tm.tm_min, tm.tm_sec);
                break;
            case 'u':
                builder.appendff("{}", tm.tm_wday ? tm.tm_wday : 7);
                break;
            case 'U': {
                const int wday_of_year_beginning = (tm.tm_wday + 6 * tm.tm_yday) % 7;
                const int week_number = (tm.tm_yday + wday_of_year_beginning) / 7;
                builder.appendff("{:02}", week_number);
                break;
            }
            case 'V': {
                const int wday_of_year_beginning = (tm.tm_wday + 6 + 6 * tm.tm_yday) % 7;
                int week_number = (tm.tm_yday + wday_of_year_beginning) / 7 + 1;
                if (wday_of_year_beginning > 3) {
                    if (tm.tm_yday >= 7 - wday_of_year_beginning)
                        --week_number;
                    else {
                        const int days_of_last_year = days_in_year(tm.tm_year + 1900 - 1);
                        const int wday_of_last_year_beginning = (wday_of_year_beginning + 6 * days_of_last_year) % 7;
                        week_number = (days_of_last_year + wday_of_last_year_beginning) / 7 + 1;
                        if (wday_of_last_year_beginning > 3)
                            --week_number;
                    }
                }
                builder.appendff("{:02}", week_number);
                break;
            }
            case 'w':
                builder.appendff("{}", tm.tm_wday);
                break;
            case 'W': {
                const int wday_of_year_beginning = (tm.tm_wday + 6 + 6 * tm.tm_yday) % 7;
                const int week_number = (tm.tm_yday + wday_of_year_beginning) / 7;
                builder.appendff("{:02}", week_number);
                break;
            }
            case 'y':
                builder.appendff("{:02}", (tm.tm_year + 1900) % 100);
                break;
            case 'Y':
                builder.appendff("{}", tm.tm_year + 1900);
                break;
            case 'z':
                format_time_zone_offset(false);
                break;
            case ':':
                if (++i == format_len)
                    return String::empty();
                if (format[i] != 'z')
                    return String::empty();

                format_time_zone_offset(true);
                break;
            case 'Z':
                builder.append(tzname[0]);
                break;
            case '%':
                builder.append('%');
                break;
            default:
                return String();
            }
        }
    }

    return builder.build();
}

Optional<DateTime> DateTime::parse(StringView format, const String& string)
{
    unsigned format_pos = 0;
    unsigned string_pos = 0;
    struct tm tm = {};

    const StringView wday_short_names[7] = {
        "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"
    };
    const StringView wday_long_names[7] = {
        "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
    };
    const StringView mon_short_names[12] = {
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    };
    const StringView mon_long_names[12] = {
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    };

    auto parsing_failed = false;

    auto parse_number = [&] {
        if (string_pos >= string.length()) {
            parsing_failed = true;
            return 0;
        }

        char* end_ptr = nullptr;
        errno = 0;
        int number = strtol(string.characters() + string_pos, &end_ptr, 10);

        auto chars_parsed = end_ptr - (string.characters() + string_pos);
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
            for (auto name : wday_short_names) {
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
            for (auto name : wday_long_names) {
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
            for (auto name : mon_short_names) {
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
            for (auto name : mon_long_names) {
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
            auto ampm = string.substring_view(string_pos, 4);
            if (ampm == "p.m." && tm.tm_hour < 12) {
                tm.tm_hour += 12;
            }
            string_pos += 4;
            break;
        }
        case 'r': {
            auto ampm = string.substring_view(string_pos, 4);
            if (ampm == "p.m." && tm.tm_hour < 12) {
                tm.tm_hour += 12;
            }
            string_pos += 4;
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
    if (string_pos != string.length() || format_pos != format.length()) {
        return {};
    }

    return DateTime::from_timestamp(mktime(&tm));
}
}
