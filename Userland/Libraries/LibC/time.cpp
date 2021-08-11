/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <AK/Time.h>
#include <Kernel/API/TimePage.h>
#include <assert.h>
#include <errno.h>
#include <stdio.h>
#include <string.h>
#include <sys/time.h>
#include <sys/times.h>
#include <syscall.h>
#include <time.h>
#include <utime.h>

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

int adjtime(const struct timeval* delta, struct timeval* old_delta)
{
    int rc = syscall(SC_adjtime, delta, old_delta);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int gettimeofday(struct timeval* __restrict__ tv, void* __restrict__)
{
    if (!tv) {
        errno = EFAULT;
        return -1;
    }

    struct timespec ts = {};
    if (clock_gettime(CLOCK_REALTIME_COARSE, &ts) < 0)
        return -1;

    TIMESPEC_TO_TIMEVAL(tv, &ts);
    return 0;
}

int settimeofday(struct timeval* __restrict__ tv, void* __restrict__)
{
    if (!tv) {
        errno = EFAULT;
        return -1;
    }

    timespec ts;
    TIMEVAL_TO_TIMESPEC(tv, &ts);
    return clock_settime(CLOCK_REALTIME, &ts);
}

int utimes(const char* pathname, const struct timeval times[2])
{
    if (!times) {
        return utime(pathname, nullptr);
    }
    // FIXME: implement support for tv_usec in the utime (or a new) syscall
    utimbuf buf = { times[0].tv_sec, times[1].tv_sec };
    return utime(pathname, &buf);
}

char* ctime(const time_t* t)
{
    return asctime(localtime(t));
}

char* ctime_r(const time_t* t, char* buf)
{
    struct tm tm_buf;
    return asctime_r(localtime_r(t, &tm_buf), buf);
}

static const int __seconds_per_day = 60 * 60 * 24;

static void time_to_tm(struct tm* tm, time_t t)
{
    int year = 1970;
    for (; t >= days_in_year(year) * __seconds_per_day; ++year)
        t -= days_in_year(year) * __seconds_per_day;
    for (; t < 0; --year)
        t += days_in_year(year - 1) * __seconds_per_day;
    tm->tm_year = year - 1900;

    VERIFY(t >= 0);
    int days = t / __seconds_per_day;
    tm->tm_yday = days;
    int remaining = t % __seconds_per_day;
    tm->tm_sec = remaining % 60;
    remaining /= 60;
    tm->tm_min = remaining % 60;
    tm->tm_hour = remaining / 60;

    int month;
    for (month = 1; month < 12 && days >= days_in_month(year, month); ++month)
        days -= days_in_month(year, month);

    tm->tm_mday = days + 1;
    tm->tm_wday = day_of_week(year, month, tm->tm_mday);
    tm->tm_mon = month - 1;
}

static time_t tm_to_time(struct tm* tm, long timezone_adjust_seconds)
{
    // "The original values of the tm_wday and tm_yday components of the structure are ignored,
    // and the original values of the other components are not restricted to the ranges described in <time.h>.
    // [...]
    // Upon successful completion, the values of the tm_wday and tm_yday components of the structure shall be set appropriately,
    // and the other components are set to represent the specified time since the Epoch,
    // but with their values forced to the ranges indicated in the <time.h> entry;
    // the final value of tm_mday shall not be set until tm_mon and tm_year are determined."

    // FIXME: Handle tm_isdst eventually.

    tm->tm_year += tm->tm_mon / 12;
    tm->tm_mon %= 12;
    if (tm->tm_mon < 0) {
        tm->tm_year--;
        tm->tm_mon += 12;
    }

    tm->tm_yday = day_of_year(1900 + tm->tm_year, tm->tm_mon + 1, tm->tm_mday);
    time_t days_since_epoch = years_to_days_since_epoch(1900 + tm->tm_year) + tm->tm_yday;
    auto timestamp = ((days_since_epoch * 24 + tm->tm_hour) * 60 + tm->tm_min) * 60 + tm->tm_sec + timezone_adjust_seconds;
    time_to_tm(tm, timestamp);
    return timestamp;
}

time_t mktime(struct tm* tm)
{
    return tm_to_time(tm, timezone);
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

time_t timegm(struct tm* tm)
{
    return tm_to_time(tm, 0);
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

char* asctime(const struct tm* tm)
{
    static char buffer[69];
    return asctime_r(tm, buffer);
}

char* asctime_r(const struct tm* tm, char* buffer)
{
    // Spec states buffer must be at least 26 bytes.
    constexpr size_t assumed_len = 26;
    size_t filled_size = strftime(buffer, assumed_len, "%a %b %e %T %Y\n", tm);

    // Verify that the buffer was large enough.
    VERIFY(filled_size != 0);

    return buffer;
}

//FIXME: Some formats are not supported.
size_t strftime(char* destination, size_t max_size, const char* format, const struct tm* tm)
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

    StringBuilder builder { max_size };

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
                builder.appendff("{:02}", (tm->tm_year + 1900) / 100);
                break;
            case 'd':
                builder.appendff("{:02}", tm->tm_mday);
                break;
            case 'D':
                builder.appendff("{:02}/{:02}/{:02}", tm->tm_mon + 1, tm->tm_mday, (tm->tm_year + 1900) % 100);
                break;
            case 'e':
                builder.appendff("{:2}", tm->tm_mday);
                break;
            case 'h':
                builder.append(mon_short_names[tm->tm_mon]);
                break;
            case 'H':
                builder.appendff("{:02}", tm->tm_hour);
                break;
            case 'I':
                builder.appendff("{:02}", tm->tm_hour % 12);
                break;
            case 'j':
                builder.appendff("{:03}", tm->tm_yday + 1);
                break;
            case 'm':
                builder.appendff("{:02}", tm->tm_mon + 1);
                break;
            case 'M':
                builder.appendff("{:02}", tm->tm_min);
                break;
            case 'n':
                builder.append('\n');
                break;
            case 'p':
                builder.append(tm->tm_hour < 12 ? "a.m." : "p.m.");
                break;
            case 'r':
                builder.appendff("{:02}:{:02}:{:02} {}", tm->tm_hour % 12, tm->tm_min, tm->tm_sec, tm->tm_hour < 12 ? "a.m." : "p.m.");
                break;
            case 'R':
                builder.appendff("{:02}:{:02}", tm->tm_hour, tm->tm_min);
                break;
            case 'S':
                builder.appendff("{:02}", tm->tm_sec);
                break;
            case 't':
                builder.append('\t');
                break;
            case 'T':
                builder.appendff("{:02}:{:02}:{:02}", tm->tm_hour, tm->tm_min, tm->tm_sec);
                break;
            case 'u':
                builder.appendff("{}", tm->tm_wday ? tm->tm_wday : 7);
                break;
            case 'U': {
                const int wday_of_year_beginning = (tm->tm_wday + 6 * tm->tm_yday) % 7;
                const int week_number = (tm->tm_yday + wday_of_year_beginning) / 7;
                builder.appendff("{:02}", week_number);
                break;
            }
            case 'V': {
                const int wday_of_year_beginning = (tm->tm_wday + 6 + 6 * tm->tm_yday) % 7;
                int week_number = (tm->tm_yday + wday_of_year_beginning) / 7 + 1;
                if (wday_of_year_beginning > 3) {
                    if (tm->tm_yday >= 7 - wday_of_year_beginning)
                        --week_number;
                    else {
                        const int days_of_last_year = days_in_year(tm->tm_year + 1900 - 1);
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
                builder.appendff("{}", tm->tm_wday);
                break;
            case 'W': {
                const int wday_of_year_beginning = (tm->tm_wday + 6 + 6 * tm->tm_yday) % 7;
                const int week_number = (tm->tm_yday + wday_of_year_beginning) / 7;
                builder.appendff("{:02}", week_number);
                break;
            }
            case 'y':
                builder.appendff("{:02}", (tm->tm_year + 1900) % 100);
                break;
            case 'Y':
                builder.appendff("{}", tm->tm_year + 1900);
                break;
            case '%':
                builder.append('%');
                break;
            default:
                return 0;
            }
        }
        if (builder.length() + 1 > max_size)
            return 0;
    }

    auto str = builder.build();
    bool fits = str.copy_characters_to_buffer(destination, max_size);
    return fits ? str.length() : 0;
}

long timezone;
long altzone;
char* tzname[2];
int daylight;

constexpr const char* __utc = "UTC";

void tzset()
{
    // FIXME: Here we pretend we are in UTC+0.
    timezone = 0;
    daylight = 0;
    tzname[0] = const_cast<char*>(__utc);
    tzname[1] = const_cast<char*>(__utc);
}

clock_t clock()
{
    struct tms tms;
    times(&tms);
    return tms.tms_utime + tms.tms_stime;
}

static Kernel::TimePage* get_kernel_time_page()
{
    static Kernel::TimePage* s_kernel_time_page;
    // FIXME: Thread safety
    if (!s_kernel_time_page) {
        auto rc = syscall(SC_map_time_page);
        if ((int)rc < 0 && (int)rc > -EMAXERRNO) {
            errno = -(int)rc;
            return nullptr;
        }
        s_kernel_time_page = (Kernel::TimePage*)rc;
    }
    return s_kernel_time_page;
}

int clock_gettime(clockid_t clock_id, struct timespec* ts)
{
    if (Kernel::time_page_supports(clock_id)) {
        if (!ts) {
            errno = EFAULT;
            return -1;
        }

        if (auto* kernel_time_page = get_kernel_time_page()) {
            u32 update_iteration;
            do {
                update_iteration = AK::atomic_load(&kernel_time_page->update1, AK::memory_order_acquire);
                *ts = kernel_time_page->clocks[clock_id];
            } while (update_iteration != AK::atomic_load(&kernel_time_page->update2, AK::memory_order_acquire));
            return 0;
        }
    }

    int rc = syscall(SC_clock_gettime, clock_id, ts);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int clock_settime(clockid_t clock_id, struct timespec* ts)
{
    int rc = syscall(SC_clock_settime, clock_id, ts);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int clock_nanosleep(clockid_t clock_id, int flags, const struct timespec* requested_sleep, struct timespec* remaining_sleep)
{
    Syscall::SC_clock_nanosleep_params params { clock_id, flags, requested_sleep, remaining_sleep };
    int rc = syscall(SC_clock_nanosleep, &params);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int nanosleep(const struct timespec* requested_sleep, struct timespec* remaining_sleep)
{
    return clock_nanosleep(CLOCK_REALTIME, 0, requested_sleep, remaining_sleep);
}

int clock_getres(clockid_t, struct timespec*)
{
    dbgln("FIXME: Implement clock_getres()");
    auto rc = -ENOSYS;
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

double difftime(time_t t1, time_t t0)
{
    return (double)(t1 - t0);
}
}
