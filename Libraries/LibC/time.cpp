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

#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <Kernel/KernelInfoPage.h>
#include <Kernel/Syscall.h>
#include <assert.h>
#include <errno.h>
#include <stdio.h>
#include <string.h>
#include <sys/time.h>
#include <sys/times.h>
#include <time.h>

extern "C" {

time_t time(time_t* tloc)
{
    struct timeval tv;
    struct timezone tz;
    if (gettimeofday(&tv, &tz) < 0)
        return (time_t)-1;
    if (tloc)
        *tloc = tv.tv_sec;
    return tv.tv_sec;
}

int gettimeofday(struct timeval* __restrict__ tv, void* __restrict__)
{
    static volatile KernelInfoPage* kernel_info;
    if (!kernel_info)
        kernel_info = (volatile KernelInfoPage*)syscall(SC_get_kernel_info_page);

    for (;;) {
        auto serial = kernel_info->serial;
        *tv = const_cast<struct timeval&>(kernel_info->now);
        if (serial == kernel_info->serial)
            break;
    }
    return 0;
}

char* ctime(const time_t* t)
{
    return asctime(localtime(t));
}

static inline bool __is_leap_year(int year)
{
    return ((year % 4 == 0) && ((year % 100 != 0) || (year % 400) == 0));
}

static const int __days_per_month[] = { 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };
static const int __seconds_per_day = 60 * 60 * 24;

static void time_to_tm(struct tm* tm, time_t t)
{
    int days = t / __seconds_per_day;
    int remaining = t % __seconds_per_day;
    tm->tm_sec = remaining % 60;
    remaining /= 60;
    tm->tm_min = remaining % 60;
    tm->tm_hour = remaining / 60;
    tm->tm_wday = (4 + days) % 7;
    int year;
    for (year = 1970; days >= 365 + __is_leap_year(year); ++year)
        days -= 365 + __is_leap_year(year);
    tm->tm_year = year - 1900;
    tm->tm_yday = days;
    tm->tm_mday = 1;
    if (__is_leap_year(year) && days == 59)
        ++tm->tm_mday;
    if (__is_leap_year(year) && days >= 59)
        --days;
    int month;
    for (month = 0; month < 11 && days >= __days_per_month[month]; ++month)
        days -= __days_per_month[month];
    tm->tm_mon = month;
    tm->tm_mday += days;
}

time_t mktime(struct tm* tm)
{
    int days = 0;
    int seconds = tm->tm_hour * 3600 + tm->tm_min * 60 + tm->tm_sec;
    for (int year = 70; year < tm->tm_year; ++year)
        days += 365 + __is_leap_year(1900 + year);

    tm->tm_yday = tm->tm_mday - 1;
    for (int month = 0; month < tm->tm_mon; ++month)
        tm->tm_yday += __days_per_month[month];
    if (tm->tm_mon > 1 && __is_leap_year(1900 + tm->tm_year))
        ++tm->tm_yday;

    days += tm->tm_yday;
    return days * __seconds_per_day + seconds + timezone;
}

struct tm* localtime(const time_t* t)
{
    static struct tm tm_buf;
    return localtime_r(t, &tm_buf);
}

struct tm* localtime_r(const time_t* t, struct tm* tm)
{
    if (!t)
        return nullptr;
    time_to_tm(tm, (*t) - timezone);
    return tm;
}

struct tm* gmtime(const time_t* t)
{
    static struct tm tm_buf;
    return gmtime_r(t, &tm_buf);
}

struct tm* gmtime_r(const time_t* t, struct tm* tm)
{
    if (!t)
        return nullptr;
    time_to_tm(tm, *t);
    return tm;
}

static char wday_short_names[7][4] = {
    "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"
};

static char wday_long_names[7][10] = {
    "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
};

static char mon_short_names[12][4] = {
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
};

static char mon_long_names[12][10] = {
    "January", "February", "March", "April", "May", "June",
    "July", "Auguest", "September", "October", "November", "December"
};

char* asctime(const struct tm* tm)
{
    constexpr int maxLength = 69;
    StringBuilder builder { maxLength };
    builder.appendf("%.3s %.3s %2d %02d:%02d:%02d %4d\n", wday_short_names[tm->tm_wday],
        mon_short_names[tm->tm_mon], tm->tm_mday, tm->tm_hour, tm->tm_min, tm->tm_sec, 1900 + tm->tm_year);

    static char result[maxLength];
    strncpy(result, builder.build().characters(), sizeof result);
    return result;
}

//FIXME: Some formats are not supported.
size_t strftime(char* destination, size_t max_size, const char* format, const struct tm* tm)
{
    StringBuilder builder { max_size - 1 };

    const int format_len = strlen(format);
    for (int i = 0; i < format_len; ++i) {
        if (format[i] != '%') {
            builder.append(format[i]);
        } else {
            if (++i >= format_len)
                return 0;

            switch (format[i]) {
            case 'a':
                builder.append(wday_short_names[tm->tm_wday]);
                break;
            case 'A':
                builder.append(wday_long_names[tm->tm_wday]);
                break;
            case 'b':
                builder.append(mon_short_names[tm->tm_mon]);
                break;
            case 'B':
                builder.append(mon_long_names[tm->tm_mon]);
                break;
            case 'C':
                builder.appendf("%02d", (tm->tm_year + 1900) / 100);
                break;
            case 'd':
                builder.appendf("%02d", tm->tm_mday);
                break;
            case 'D':
                builder.appendf("%02d/%02d/%02d", tm->tm_mon + 1, tm->tm_mday, (tm->tm_year + 1900) % 100);
                break;
            case 'e':
                builder.appendf("%2d", tm->tm_mday);
                break;
            case 'h':
                builder.append(mon_short_names[tm->tm_mon]);
                break;
            case 'H':
                builder.appendf("%02d", tm->tm_hour);
                break;
            case 'I':
                builder.appendf("%02d", tm->tm_hour % 12);
                break;
            case 'j':
                builder.appendf("%03d", tm->tm_yday + 1);
                break;
            case 'm':
                builder.appendf("%02d", tm->tm_mon + 1);
                break;
            case 'M':
                builder.appendf("%02d", tm->tm_min);
                break;
            case 'n':
                builder.append('\n');
                break;
            case 'p':
                builder.append(tm->tm_hour < 12 ? "a.m." : "p.m.");
                break;
            case 'r':
                builder.appendf("%02d:%02d:%02d %s", tm->tm_hour % 12, tm->tm_min, tm->tm_sec, tm->tm_hour < 12 ? "a.m." : "p.m.");
                break;
            case 'R':
                builder.appendf("%02d:%02d", tm->tm_hour, tm->tm_min);
                break;
            case 'S':
                builder.appendf("%02d", tm->tm_sec);
                break;
            case 't':
                builder.append('\t');
                break;
            case 'T':
                builder.appendf("%02d:%02d:%02d", tm->tm_hour, tm->tm_min, tm->tm_sec);
                break;
            case 'u':
                builder.appendf("%d", tm->tm_wday ? tm->tm_wday : 7);
                break;
            case 'U': {
                const int wday_of_year_beginning = (tm->tm_wday + 6 * tm->tm_yday) % 7;
                const int week_number = (tm->tm_yday + wday_of_year_beginning) / 7;
                builder.appendf("%02d", week_number);
                break;
            }
            case 'V': {
                const int wday_of_year_beginning = (tm->tm_wday + 6 + 6 * tm->tm_yday) % 7;
                int week_number = (tm->tm_yday + wday_of_year_beginning) / 7 + 1;
                if (wday_of_year_beginning > 3) {
                    if (tm->tm_yday >= 7 - wday_of_year_beginning)
                        --week_number;
                    else {
                        const int days_of_last_year = 365 + __is_leap_year(tm->tm_year + 1900);
                        const int wday_of_last_year_beginning = (wday_of_year_beginning + 6 * days_of_last_year) % 7;
                        week_number = (days_of_last_year + wday_of_last_year_beginning) / 7 + 1;
                        if (wday_of_year_beginning > 3)
                            --week_number;
                    }
                }
                builder.appendf("%02d", week_number);
                break;
            }
            case 'w':
                builder.appendf("%d", tm->tm_wday);
                break;
            case 'W': {
                const int wday_of_year_beginning = (tm->tm_wday + 6 + 6 * tm->tm_yday) % 7;
                const int week_number = (tm->tm_yday + wday_of_year_beginning) / 7;
                builder.appendf("%02d", week_number);
                break;
            }
            case 'y':
                builder.appendf("%02d", (tm->tm_year + 1900) % 100);
                break;
            case 'Y':
                builder.appendf("%d", tm->tm_year + 1900);
                break;
            case '%':
                builder.append('%');
                break;
            default:
                return 0;
            }
        }
        if (builder.length() > max_size - 1)
                return 0;
    }

    strcpy(destination, builder.build().characters());
    return builder.length();
}

long timezone = 0;
long altzone;
char* tzname[2];
int daylight;

void tzset()
{
    //FIXME: Here we prepend we are in UTC+0.
    timezone = 0;
}

clock_t clock()
{
    struct tms tms;
    times(&tms);
    return tms.tms_utime + tms.tms_stime;
}

int clock_gettime(clockid_t clock_id, struct timespec* ts)
{
    int rc = syscall(SC_clock_gettime, clock_id, ts);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int clock_nanosleep(clockid_t clock_id, int flags, const struct timespec* requested_sleep, struct timespec* remaining_sleep)
{
    Syscall::SC_clock_nanosleep_params params { clock_id, flags, requested_sleep, remaining_sleep };
    int rc = syscall(SC_clock_nanosleep, &params);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int clock_getres(clockid_t, struct timespec*)
{
    ASSERT_NOT_REACHED();
}
}
