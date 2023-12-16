/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Atomic.h>
#include <AK/DateConstants.h>
#include <AK/StringBuilder.h>
#include <AK/Time.h>
#include <Kernel/API/TimePage.h>
#include <LibTimeZone/TimeZone.h>
#include <assert.h>
#include <bits/pthread_cancel.h>
#include <errno.h>
#include <fcntl.h>
#include <limits.h>
#include <stdio.h>
#include <string.h>
#include <sys/time.h>
#include <sys/times.h>
#include <syscall.h>
#include <time.h>
#include <utime.h>

extern "C" {

static constexpr char const* __utc = "UTC";
static StringView __tzname { __utc, __builtin_strlen(__utc) };
static char __tzname_standard[TZNAME_MAX];
static char __tzname_daylight[TZNAME_MAX];

long timezone = 0;
long altzone = 0;
char* tzname[2] = { const_cast<char*>(__utc), const_cast<char*>(__utc) };
int daylight = 0;

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

int utimes(char const* pathname, struct timeval const times[2])
{
    if (!times)
        return utime(pathname, nullptr);
    // FIXME: implement support for tv_usec in the utime (or a new) syscall
    utimbuf buf = { times[0].tv_sec, times[1].tv_sec };
    return utime(pathname, &buf);
}

// Not in POSIX, originated in BSD but also supported on Linux.
// https://man.netbsd.org/NetBSD-6.0/lutimes.2
int lutimes(char const* pathname, struct timeval const times[2])
{
    if (!times)
        return utimensat(AT_FDCWD, pathname, nullptr, AT_SYMLINK_NOFOLLOW);
    timespec ts[2];
    TIMEVAL_TO_TIMESPEC(&times[0], &ts[0]);
    TIMEVAL_TO_TIMESPEC(&times[1], &ts[1]);
    return utimensat(AT_FDCWD, pathname, ts, AT_SYMLINK_NOFOLLOW);
}

// Not in POSIX, originated in BSD but also supported on Linux.
// https://man.netbsd.org/NetBSD-6.0/futimes.2
int futimes(int fd, struct timeval const times[2])
{
    if (!times)
        return utimensat(fd, nullptr, nullptr, 0);
    timespec ts[2];
    TIMEVAL_TO_TIMESPEC(&times[0], &ts[0]);
    TIMEVAL_TO_TIMESPEC(&times[1], &ts[1]);
    return utimensat(fd, nullptr, ts, 0);
}

char* ctime(time_t const* t)
{
    return asctime(localtime(t));
}

char* ctime_r(time_t const* t, char* buf)
{
    struct tm tm_buf;
    return asctime_r(localtime_r(t, &tm_buf), buf);
}

static int const __seconds_per_day = 60 * 60 * 24;

static bool is_valid_time(time_t timestamp)
{
    // Note: these correspond to the number of seconds from epoch to the dates "Jan 1 00:00:00 -2147483648" and "Dec 31 23:59:59 2147483647",
    // respectively, which are the smallest and biggest representable dates without overflowing tm->tm_year, if it is an int.
    constexpr time_t smallest_possible_time = -67768040609740800;
    constexpr time_t biggest_possible_time = 67768036191676799;

    return (timestamp >= smallest_possible_time) && (timestamp <= biggest_possible_time);
}

static struct tm* time_to_tm(struct tm* tm, time_t t, StringView time_zone)
{
    if (!is_valid_time(t)) {
        errno = EOVERFLOW;
        return nullptr;
    }

    if (auto offset = TimeZone::get_time_zone_offset(time_zone, AK::UnixDateTime::from_seconds_since_epoch(t)); offset.has_value()) {
        tm->tm_isdst = offset->in_dst == TimeZone::InDST::Yes;
        t += offset->seconds;
    }

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

    return tm;
}

static time_t tm_to_time(struct tm* tm, StringView time_zone)
{
    // "The original values of the tm_wday and tm_yday components of the structure are ignored,
    // and the original values of the other components are not restricted to the ranges described in <time.h>.
    // [...]
    // Upon successful completion, the values of the tm_wday and tm_yday components of the structure shall be set appropriately,
    // and the other components are set to represent the specified time since the Epoch,
    // but with their values forced to the ranges indicated in the <time.h> entry;
    // the final value of tm_mday shall not be set until tm_mon and tm_year are determined."

    tm->tm_year += tm->tm_mon / 12;
    tm->tm_mon %= 12;
    if (tm->tm_mon < 0) {
        tm->tm_year--;
        tm->tm_mon += 12;
    }

    tm->tm_yday = day_of_year(1900 + tm->tm_year, tm->tm_mon + 1, tm->tm_mday);
    time_t days_since_epoch = years_to_days_since_epoch(1900 + tm->tm_year) + tm->tm_yday;
    auto timestamp = ((days_since_epoch * 24 + tm->tm_hour) * 60 + tm->tm_min) * 60 + tm->tm_sec;

    if (tm->tm_isdst < 0) {
        if (auto offset = TimeZone::get_time_zone_offset(time_zone, AK::UnixDateTime::from_seconds_since_epoch(timestamp)); offset.has_value())
            timestamp -= offset->seconds;
    } else {
        auto index = tm->tm_isdst == 0 ? 0 : 1;

        if (auto offsets = TimeZone::get_named_time_zone_offsets(time_zone, AK::UnixDateTime::from_seconds_since_epoch(timestamp)); offsets.has_value())
            timestamp -= offsets->at(index).seconds;
    }

    if (!is_valid_time(timestamp)) {
        errno = EOVERFLOW;
        return -1;
    }

    return timestamp;
}

time_t mktime(struct tm* tm)
{
    tzset();
    return tm_to_time(tm, __tzname);
}

struct tm* localtime(time_t const* t)
{
    tzset();

    static struct tm tm_buf;
    return localtime_r(t, &tm_buf);
}

struct tm* localtime_r(time_t const* t, struct tm* tm)
{
    if (!t)
        return nullptr;

    return time_to_tm(tm, *t, __tzname);
}

time_t timegm(struct tm* tm)
{
    tm->tm_isdst = 0;
    return tm_to_time(tm, { __utc, __builtin_strlen(__utc) });
}

struct tm* gmtime(time_t const* t)
{
    static struct tm tm_buf;
    return gmtime_r(t, &tm_buf);
}

struct tm* gmtime_r(time_t const* t, struct tm* tm)
{
    if (!t)
        return nullptr;
    return time_to_tm(tm, *t, { __utc, __builtin_strlen(__utc) });
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

    // If the buffer was not large enough, set EOVERFLOW and return null.
    if (filled_size == 0) {
        errno = EOVERFLOW;
        return nullptr;
    }

    return buffer;
}

// FIXME: Some formats are not supported.
size_t strftime(char* destination, size_t max_size, char const* format, const struct tm* tm)
{
    tzset();

    StringBuilder builder { max_size };

    int const format_len = strlen(format);
    for (int i = 0; i < format_len; ++i) {
        if (format[i] != '%') {
            builder.append(format[i]);
        } else {
            if (++i >= format_len)
                return 0;

            switch (format[i]) {
            case 'a':
                builder.append(short_day_names[tm->tm_wday]);
                break;
            case 'A':
                builder.append(long_day_names[tm->tm_wday]);
                break;
            case 'b':
                builder.append(short_month_names[tm->tm_mon]);
                break;
            case 'B':
                builder.append(long_month_names[tm->tm_mon]);
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
                builder.append(short_month_names[tm->tm_mon]);
                break;
            case 'H':
                builder.appendff("{:02}", tm->tm_hour);
                break;
            case 'I': {
                int display_hour = tm->tm_hour % 12;
                if (display_hour == 0)
                    display_hour = 12;
                builder.appendff("{:02}", display_hour);
                break;
            }
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
                builder.append(tm->tm_hour < 12 ? "AM"sv : "PM"sv);
                break;
            case 'r': {
                int display_hour = tm->tm_hour % 12;
                if (display_hour == 0)
                    display_hour = 12;
                builder.appendff("{:02}:{:02}:{:02} {}", display_hour, tm->tm_min, tm->tm_sec, tm->tm_hour < 12 ? "AM" : "PM");
                break;
            }
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
                int const wday_of_year_beginning = (tm->tm_wday + 6 * tm->tm_yday) % 7;
                int const week_number = (tm->tm_yday + wday_of_year_beginning) / 7;
                builder.appendff("{:02}", week_number);
                break;
            }
            case 'V': {
                int const wday_of_year_beginning = (tm->tm_wday + 6 + 6 * tm->tm_yday) % 7;
                int week_number = (tm->tm_yday + wday_of_year_beginning) / 7 + 1;
                if (wday_of_year_beginning > 3) {
                    if (tm->tm_yday >= 7 - wday_of_year_beginning)
                        --week_number;
                    else {
                        int const days_of_last_year = days_in_year(tm->tm_year + 1900 - 1);
                        int const wday_of_last_year_beginning = (wday_of_year_beginning + 6 * days_of_last_year) % 7;
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
                int const wday_of_year_beginning = (tm->tm_wday + 6 + 6 * tm->tm_yday) % 7;
                int const week_number = (tm->tm_yday + wday_of_year_beginning) / 7;
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

    auto str = builder.to_byte_string();
    bool fits = str.copy_characters_to_buffer(destination, max_size);
    return fits ? str.length() : 0;
}

void tzset()
{
    __tzname = TimeZone::current_time_zone();

    auto set_default_values = []() {
        timezone = 0;
        altzone = 0;
        daylight = 0;
        __tzname = StringView { __utc, __builtin_strlen(__utc) };
        tzname[0] = const_cast<char*>(__utc);
        tzname[1] = const_cast<char*>(__utc);
    };

    if (auto offsets = TimeZone::get_named_time_zone_offsets(__tzname, AK::UnixDateTime::now()); offsets.has_value()) {
        if (!offsets->at(0).name.copy_characters_to_buffer(__tzname_standard, TZNAME_MAX))
            return set_default_values();
        if (!offsets->at(1).name.copy_characters_to_buffer(__tzname_daylight, TZNAME_MAX))
            return set_default_values();

        // timezone and altzone are seconds west of UTC, i.e. the offsets are negated.
        timezone = -offsets->at(0).seconds;
        altzone = -offsets->at(1).seconds;
        daylight = timezone != altzone;
        tzname[0] = __tzname_standard;
        tzname[1] = __tzname_daylight;
    } else {
        set_default_values();
    }
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
    __pthread_maybe_cancel();

    Syscall::SC_clock_nanosleep_params params { clock_id, flags, requested_sleep, remaining_sleep };
    int rc = syscall(SC_clock_nanosleep, &params);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int nanosleep(const struct timespec* requested_sleep, struct timespec* remaining_sleep)
{
    return clock_nanosleep(CLOCK_REALTIME, 0, requested_sleep, remaining_sleep);
}

int clock_getres(clockid_t clock_id, struct timespec* result)
{
    Syscall::SC_clock_getres_params params { clock_id, result };
    int rc = syscall(SC_clock_getres, &params);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

double difftime(time_t t1, time_t t0)
{
    return (double)(t1 - t0);
}
}
