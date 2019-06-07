#include "RTC.h"
#include "CMOS.h"
#include <AK/Assertions.h>

namespace RTC {

static time_t s_boot_time;

void initialize()
{
    byte cmos_mode = CMOS::read(0x0b);
    cmos_mode |= 2; // 24 hour mode
    cmos_mode |= 4; // No BCD mode
    CMOS::write(0x0b, cmos_mode);

    s_boot_time = now();
}

time_t boot_time()
{
    return s_boot_time;
}

static bool update_in_progress()
{
    return CMOS::read(0x0a) & 0x80;
}

inline bool is_leap_year(unsigned year)
{
    return ((year % 4 == 0) && ((year % 100 != 0) || (year % 400) == 0));
}

static unsigned days_in_months_since_start_of_year(unsigned month, unsigned year)
{
    ASSERT(month <= 11);
    unsigned days = 0;
    switch (month) {
    case 11:
        days += 30;
        [[fallthrough]];
    case 10:
        days += 31;
        [[fallthrough]];
    case 9:
        days += 30;
        [[fallthrough]];
    case 8:
        days += 31;
        [[fallthrough]];
    case 7:
        days += 31;
        [[fallthrough]];
    case 6:
        days += 30;
        [[fallthrough]];
    case 5:
        days += 31;
        [[fallthrough]];
    case 4:
        days += 30;
        [[fallthrough]];
    case 3:
        days += 31;
        [[fallthrough]];
    case 2:
        if (is_leap_year(year))
            days += 29;
        else
            days += 28;
        [[fallthrough]];
    case 1:
        days += 31;
    }
    return days;
}

static unsigned days_in_years_since_epoch(unsigned year)
{
    unsigned days = 0;
    while (year > 1969) {
        days += 365;
        if (is_leap_year(year))
            ++days;
        --year;
    }
    return days;
}

void read_registers(unsigned& year, unsigned& month, unsigned& day, unsigned& hour, unsigned& minute, unsigned& second)
{
    while (update_in_progress())
        ;

    year = (CMOS::read(0x32) * 100) + CMOS::read(0x09);
    month = CMOS::read(0x08);
    day = CMOS::read(0x07);
    hour = CMOS::read(0x04);
    minute = CMOS::read(0x02);
    second = CMOS::read(0x00);
}

time_t now()
{
    // FIXME: We should probably do something more robust here.
    //        Perhaps read all the values twice and verify that they were identical.
    //        We don't want to be caught in the middle of an RTC register update.
    while (update_in_progress())
        ;

    unsigned year, month, day, hour, minute, second;
    read_registers(year, month, day, hour, minute, second);

    kprintf("year: %d, month: %d, day: %d\n", year, month, day);

    ASSERT(year >= 2018);

    return days_in_years_since_epoch(year - 1) * 86400
        + days_in_months_since_start_of_year(month - 1, year) * 86400
        + (day - 1) * 86400
        + hour * 3600
        + minute * 60
        + second;
}

}
