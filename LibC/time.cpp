#include <time.h>
#include <errno.h>
#include <assert.h>
#include <Kernel/Syscall.h>

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

int gettimeofday(struct timeval* tv, struct timezone*)
{
    int rc = syscall(SC_gettimeofday, tv);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

char* ctime(const time_t*)
{
    return const_cast<char*>("ctime() not implemented");
}

inline bool is_leap_year(unsigned year)
{
    return ((year % 4 == 0) && ((year % 100 != 0) || (year % 400) == 0));
}

static void time_to_tm(struct tm* tm, time_t t)
{
    static const unsigned seconds_per_day = 60 * 60 * 24;
    static const unsigned days_per_month[] = { 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };
    unsigned days = t / seconds_per_day;
    unsigned rem = t % seconds_per_day;
    tm->tm_sec = rem % 60;
    rem /= 60;
    tm->tm_min = rem % 60;
    tm->tm_hour = rem / 60;
    tm->tm_wday = (4 + days) % 7;
    unsigned year;
    for (year = 1970; days >= 365 + is_leap_year(year); ++year)
        days -= 365 + is_leap_year(year);
    tm->tm_year = year - 1900;
    tm->tm_yday = days;
    tm->tm_mday = 1;
    if (is_leap_year(year) && days == 59)
        ++tm->tm_mday;
    if (is_leap_year(year) && days >= 59)
        --days;
    unsigned month;
    for (month = 0; month < 11 && days >= days_per_month[month]; ++month)
        days -= days_per_month[month];
    tm->tm_mon = month;
    tm->tm_mday += days;
}

struct tm* localtime(const time_t* t)
{
    if (!t)
        return nullptr;
    static struct tm tm_buf;
    time_to_tm(&tm_buf, *t);
    return &tm_buf;
}

long timezone = 0;

void tzset()
{
    assert(false);
}

}
