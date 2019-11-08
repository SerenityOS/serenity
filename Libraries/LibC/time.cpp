#include <Kernel/Syscall.h>
#include <assert.h>
#include <errno.h>
#include <sys/time.h>
#include <sys/times.h>
#include <time.h>
#include <string.h>

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
    int rc = syscall(SC_gettimeofday, tv);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

char* ctime(const time_t*)
{
    return const_cast<char*>("ctime() not implemented");
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
    days += tm->tm_yday;
    return days * __seconds_per_day + seconds;
}

struct tm* localtime(const time_t* t)
{
    if (!t)
        return nullptr;
    static struct tm tm_buf;
    time_to_tm(&tm_buf, *t);
    return &tm_buf;
}

struct tm* gmtime(const time_t* t)
{
    // FIXME: This is obviously not correct. What about timezones bro?
    return localtime(t);
}

char* asctime(const struct tm*)
{
    ASSERT_NOT_REACHED();
}

size_t strftime(char* destination, size_t, const char*, const struct tm*)
{
    // FIXME: Stubbed function to make nasm work. Should be properly implemented
    strcpy(destination, "strftime_unimplemented");
    return strlen("strftime_unimplemented");
}

long timezone;
long altzone;
char* tzname[2];
int daylight;

void tzset()
{
    ASSERT_NOT_REACHED();
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

}
