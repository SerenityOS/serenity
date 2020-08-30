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

#include <AK/StringBuilder.h>
#include <AK/Time.h>
#include <LibCore/DateTime.h>
#include <sys/time.h>
#include <time.h>

namespace Core {

DateTime DateTime::now()
{
    return from_timestamp(time(nullptr));
}

DateTime DateTime::create(unsigned year, unsigned month, unsigned day, unsigned hour, unsigned minute, unsigned second)
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

void DateTime::set_time(unsigned year, unsigned month, unsigned day, unsigned hour, unsigned minute, unsigned second)
{
    struct tm tm = {};
    tm.tm_sec = (int)second;
    tm.tm_min = (int)minute;
    tm.tm_hour = (int)hour;
    tm.tm_mday = (int)day;
    tm.tm_mon = (int)month - 1;
    tm.tm_year = (int)year - 1900;
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

String DateTime::to_string(const String& format) const
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
                builder.appendf("%02d", (tm.tm_year + 1900) / 100);
                break;
            case 'd':
                builder.appendf("%02d", tm.tm_mday);
                break;
            case 'D':
                builder.appendf("%02d/%02d/%02d", tm.tm_mon + 1, tm.tm_mday, (tm.tm_year + 1900) % 100);
                break;
            case 'e':
                builder.appendf("%2d", tm.tm_mday);
                break;
            case 'h':
                builder.append(mon_short_names[tm.tm_mon]);
                break;
            case 'H':
                builder.appendf("%02d", tm.tm_hour);
                break;
            case 'I':
                builder.appendf("%02d", tm.tm_hour % 12);
                break;
            case 'j':
                builder.appendf("%03d", tm.tm_yday + 1);
                break;
            case 'm':
                builder.appendf("%02d", tm.tm_mon + 1);
                break;
            case 'M':
                builder.appendf("%02d", tm.tm_min);
                break;
            case 'n':
                builder.append('\n');
                break;
            case 'p':
                builder.append(tm.tm_hour < 12 ? "a.m." : "p.m.");
                break;
            case 'r':
                builder.appendf("%02d:%02d:%02d %s", tm.tm_hour % 12, tm.tm_min, tm.tm_sec, tm.tm_hour < 12 ? "a.m." : "p.m.");
                break;
            case 'R':
                builder.appendf("%02d:%02d", tm.tm_hour, tm.tm_min);
                break;
            case 'S':
                builder.appendf("%02d", tm.tm_sec);
                break;
            case 't':
                builder.append('\t');
                break;
            case 'T':
                builder.appendf("%02d:%02d:%02d", tm.tm_hour, tm.tm_min, tm.tm_sec);
                break;
            case 'u':
                builder.appendf("%d", tm.tm_wday ? tm.tm_wday : 7);
                break;
            case 'U': {
                const int wday_of_year_beginning = (tm.tm_wday + 6 * tm.tm_yday) % 7;
                const int week_number = (tm.tm_yday + wday_of_year_beginning) / 7;
                builder.appendf("%02d", week_number);
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
                builder.appendf("%02d", week_number);
                break;
            }
            case 'w':
                builder.appendf("%d", tm.tm_wday);
                break;
            case 'W': {
                const int wday_of_year_beginning = (tm.tm_wday + 6 + 6 * tm.tm_yday) % 7;
                const int week_number = (tm.tm_yday + wday_of_year_beginning) / 7;
                builder.appendf("%02d", week_number);
                break;
            }
            case 'y':
                builder.appendf("%02d", (tm.tm_year + 1900) % 100);
                break;
            case 'Y':
                builder.appendf("%d", tm.tm_year + 1900);
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

bool DateTime::is_before(const String& other) const
{
    auto now_string = String::format("%04d%02d%02d%02d%02d%02dZ", year(), month(), weekday(), hour(), minute(), second());
    return __builtin_strcasecmp(now_string.characters(), other.characters()) < 0;
}

const LogStream& operator<<(const LogStream& stream, const DateTime& value)
{
    return stream << value.to_string();
}
}
