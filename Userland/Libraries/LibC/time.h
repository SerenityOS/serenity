/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/API/POSIX/time.h>

__BEGIN_DECLS

struct tm {
    int tm_sec;   /* Seconds (0-60) */
    int tm_min;   /* Minutes (0-59) */
    int tm_hour;  /* Hours (0-23) */
    int tm_mday;  /* Day of the month (1-31) */
    int tm_mon;   /* Month (0-11) */
    int tm_year;  /* Year - 1900 */
    int tm_wday;  /* Day of the week (0-6, Sunday = 0) */
    int tm_yday;  /* Day in the year (0-365, 1 Jan = 0) */
    int tm_isdst; /* Daylight saving time */
};

extern long timezone; /* The difference in seconds between UTC and local time */
extern long altzone;
extern char* tzname[2];
extern int daylight;

typedef uint32_t clock_t;
typedef int64_t time_t;

struct tm* localtime(const time_t*);
struct tm* gmtime(const time_t*);
time_t mktime(struct tm*);
time_t timegm(struct tm*);
time_t time(time_t*);
char* ctime(const time_t*);
char* ctime_r(const time_t* tm, char* buf);
void tzset(void);
char* asctime(const struct tm*);
char* asctime_r(const struct tm*, char* buf);

clock_t clock(void);

int clock_gettime(clockid_t, struct timespec*);
int clock_settime(clockid_t, struct timespec*);
int clock_nanosleep(clockid_t, int flags, const struct timespec* requested_sleep, struct timespec* remaining_sleep);
int clock_getres(clockid_t, struct timespec* result);
int nanosleep(const struct timespec* requested_sleep, struct timespec* remaining_sleep);
struct tm* gmtime_r(const time_t* timep, struct tm* result);
struct tm* localtime_r(const time_t* timep, struct tm* result);

double difftime(time_t, time_t);
size_t strftime(char* s, size_t max, const char* format, const struct tm*) __attribute__((format(strftime, 3, 0)));

__END_DECLS
