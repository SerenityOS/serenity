#pragma once

#include <sys/cdefs.h>
#include <sys/types.h>

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

extern long timezone;
extern long altzone;
extern char* tzname[2];
extern int daylight;

typedef uint32_t clock_t;
typedef uint32_t time_t;

struct tm* localtime(const time_t*);
struct tm* gmtime(const time_t*);
time_t mktime(struct tm*);
time_t time(time_t*);
char* ctime(const time_t*);
void tzset();
char* asctime(const struct tm*);

#define CLOCKS_PER_SEC 1000
clock_t clock();

struct timespec {
    time_t tv_sec;
    long tv_nsec;
};

typedef int clockid_t;

#define CLOCK_MONOTONIC 1
#define TIMER_ABSTIME 99

int clock_gettime(clockid_t, struct timespec*);
int clock_nanosleep(clockid_t, int flags, const struct timespec* requested_sleep, struct timespec* remaining_sleep);
int clock_getres(clockid_t, struct timespec* result);
struct tm* gmtime_r(const time_t* timep, struct tm* result);
struct tm* localtime_r(const time_t* timep, struct tm* result);

double difftime(time_t, time_t);
size_t strftime(char* s, size_t max, const char* format, const struct tm*);

#define difftime(t1, t0) (double)(t1 - t0)

__END_DECLS
