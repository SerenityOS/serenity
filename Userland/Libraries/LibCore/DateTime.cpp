/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <AK/DateConstants.h>
#include <AK/String.h>
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

ErrorOr<String> DateTime::to_string(StringView format) const
{
    struct tm tm;
    localtime_r(&m_timestamp, &tm);
    StringBuilder builder;
    int const format_len = format.length();

    auto format_time_zone_offset = [&](bool with_separator) -> ErrorOr<void> {
        struct tm gmt_tm;
        gmtime_r(&m_timestamp, &gmt_tm);

        gmt_tm.tm_isdst = -1;
        auto gmt_timestamp = mktime(&gmt_tm);

        auto offset_seconds = static_cast<time_t>(difftime(m_timestamp, gmt_timestamp));
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

        TRY(builder.try_appendff("{}{:02}{}{:02}", offset_sign, offset_hours, separator, offset_minutes));
        return {};
    };

    for (int i = 0; i < format_len; ++i) {
        if (format[i] != '%') {
            TRY(builder.try_append(format[i]));
        } else {
            if (++i == format_len)
                return String {};

            switch (format[i]) {
            case 'a':
                TRY(builder.try_append(short_day_names[tm.tm_wday]));
                break;
            case 'A':
                TRY(builder.try_append(long_day_names[tm.tm_wday]));
                break;
            case 'b':
                TRY(builder.try_append(short_month_names[tm.tm_mon]));
                break;
            case 'B':
                TRY(builder.try_append(long_month_names[tm.tm_mon]));
                break;
            case 'C':
                TRY(builder.try_appendff("{:02}", (tm.tm_year + 1900) / 100));
                break;
            case 'd':
                TRY(builder.try_appendff("{:02}", tm.tm_mday));
                break;
            case 'D':
                TRY(builder.try_appendff("{:02}/{:02}/{:02}", tm.tm_mon + 1, tm.tm_mday, (tm.tm_year + 1900) % 100));
                break;
            case 'e':
                TRY(builder.try_appendff("{:2}", tm.tm_mday));
                break;
            case 'h':
                TRY(builder.try_append(short_month_names[tm.tm_mon]));
                break;
            case 'H':
                TRY(builder.try_appendff("{:02}", tm.tm_hour));
                break;
            case 'I': {
                int display_hour = tm.tm_hour % 12;
                if (display_hour == 0)
                    display_hour = 12;
                TRY(builder.try_appendff("{:02}", display_hour));
                break;
            }
            case 'j':
                TRY(builder.try_appendff("{:03}", tm.tm_yday + 1));
                break;
            case 'l': {
                int display_hour = tm.tm_hour % 12;
                if (display_hour == 0)
                    display_hour = 12;
                TRY(builder.try_appendff("{:2}", display_hour));
                break;
            }
            case 'm':
                TRY(builder.try_appendff("{:02}", tm.tm_mon + 1));
                break;
            case 'M':
                TRY(builder.try_appendff("{:02}", tm.tm_min));
                break;
            case 'n':
                TRY(builder.try_append('\n'));
                break;
            case 'p':
                TRY(builder.try_append(tm.tm_hour < 12 ? "AM"sv : "PM"sv));
                break;
            case 'r': {
                int display_hour = tm.tm_hour % 12;
                if (display_hour == 0)
                    display_hour = 12;
                TRY(builder.try_appendff("{:02}:{:02}:{:02} {}", display_hour, tm.tm_min, tm.tm_sec, tm.tm_hour < 12 ? "AM" : "PM"));
                break;
            }
            case 'R':
                TRY(builder.try_appendff("{:02}:{:02}", tm.tm_hour, tm.tm_min));
                break;
            case 'S':
                TRY(builder.try_appendff("{:02}", tm.tm_sec));
                break;
            case 't':
                TRY(builder.try_append('\t'));
                break;
            case 'T':
                TRY(builder.try_appendff("{:02}:{:02}:{:02}", tm.tm_hour, tm.tm_min, tm.tm_sec));
                break;
            case 'u':
                TRY(builder.try_appendff("{}", tm.tm_wday ? tm.tm_wday : 7));
                break;
            case 'U': {
                int const wday_of_year_beginning = (tm.tm_wday + 6 * tm.tm_yday) % 7;
                int const week_number = (tm.tm_yday + wday_of_year_beginning) / 7;
                TRY(builder.try_appendff("{:02}", week_number));
                break;
            }
            case 'V': {
                int const wday_of_year_beginning = (tm.tm_wday + 6 + 6 * tm.tm_yday) % 7;
                int week_number = (tm.tm_yday + wday_of_year_beginning) / 7 + 1;
                if (wday_of_year_beginning > 3) {
                    if (tm.tm_yday >= 7 - wday_of_year_beginning)
                        --week_number;
                    else {
                        int const days_of_last_year = days_in_year(tm.tm_year + 1900 - 1);
                        int const wday_of_last_year_beginning = (wday_of_year_beginning + 6 * days_of_last_year) % 7;
                        week_number = (days_of_last_year + wday_of_last_year_beginning) / 7 + 1;
                        if (wday_of_last_year_beginning > 3)
                            --week_number;
                    }
                }
                TRY(builder.try_appendff("{:02}", week_number));
                break;
            }
            case 'w':
                TRY(builder.try_appendff("{}", tm.tm_wday));
                break;
            case 'W': {
                int const wday_of_year_beginning = (tm.tm_wday + 6 + 6 * tm.tm_yday) % 7;
                int const week_number = (tm.tm_yday + wday_of_year_beginning) / 7;
                TRY(builder.try_appendff("{:02}", week_number));
                break;
            }
            case 'y':
                TRY(builder.try_appendff("{:02}", (tm.tm_year + 1900) % 100));
                break;
            case 'Y':
                TRY(builder.try_appendff("{}", tm.tm_year + 1900));
                break;
            case 'z':
                TRY(format_time_zone_offset(false));
                break;
            case ':':
                if (++i == format_len) {
                    TRY(builder.try_append("%:"sv));
                    break;
                }
                if (format[i] != 'z') {
                    TRY(builder.try_append("%:"sv));
                    TRY(builder.try_append(format[i]));
                    break;
                }
                TRY(format_time_zone_offset(true));
                break;
            case 'Z': {
                auto const* timezone_name = tzname[tm.tm_isdst == 0 ? 0 : 1];
                TRY(builder.try_append({ timezone_name, strlen(timezone_name) }));
                break;
            }
            case '%':
                TRY(builder.try_append('%'));
                break;
            default:
                TRY(builder.try_append('%'));
                TRY(builder.try_append(format[i]));
                break;
            }
        }
    }

    return builder.to_string();
}

DeprecatedString DateTime::to_deprecated_string(StringView format) const
{
    return MUST(to_string(format)).to_deprecated_string();
}

Optional<DateTime> DateTime::parse(StringView format, DeprecatedString const& string)
{
    auto tm_or_void = AK::convert_formatted_string_to_timespec_restrictively(string, format);
    if (!tm_or_void.has_value())
        return {};
    struct tm tm = tm_or_void.value();
    return DateTime::from_timestamp(mktime(&tm));
}

}
