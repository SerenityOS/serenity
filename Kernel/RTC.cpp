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
    switch (month) {
    case 11: return 30;
    case 10: return 31;
    case 9: return 30;
    case 8: return 31;
    case 7: return 31;
    case 6: return 30;
    case 5: return 31;
    case 4: return 30;
    case 3: return 31;
    case 2:
        if (is_leap_year(year))
            return 29;
        return 28;
    case 1: return 31;
    default: return 0;
    }
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

time_t now()
{
    // FIXME: We should probably do something more robust here.
    //        Perhaps read all the values twice and verify that they were identical.
    //        We don't want to be caught in the middle of an RTC register update.
    while (update_in_progress())
        ;

    unsigned year = (CMOS::read(0x32) * 100) + CMOS::read(0x09);
    unsigned month = CMOS::read(0x08);
    unsigned day = CMOS::read(0x07);
    unsigned hour = CMOS::read(0x04);
    unsigned minute = CMOS::read(0x02);
    unsigned second = CMOS::read(0x00);

    ASSERT(year >= 2018);

    return days_in_years_since_epoch(year - 1) * 86400
         + days_in_months_since_start_of_year(month - 1, year) * 86400
         + day * 86400
         + hour * 3600
         + minute * 60
         + second;
}

}

