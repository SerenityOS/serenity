#include "RTC.h"
#include "CMOS.h"
#include <AK/Assertions.h>

namespace RTC {

static time_t s_bootTime;

void initialize()
{    
    byte cmosMode = CMOS::read(0x0b);
    cmosMode |= 2; // 24 hour mode
    cmosMode |= 4; // No BCD mode
    CMOS::write(0x0b, cmosMode);

    s_bootTime = now();
}

time_t bootTime()
{
    return s_bootTime;
}

static bool updateInProgress()
{
    return CMOS::read(0x0a) & 0x80;
}

inline bool isLeapYear(unsigned year)
{
    return ((year % 4 == 0) && ((year % 100 != 0) || (year % 400) == 0));
}

static unsigned daysInMonthsSinceStartOfYear(unsigned month, unsigned year)
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
        if (isLeapYear(year))
            return 29;
        return 28;
    case 1: return 31;
    default: return 0;
    }
}

static unsigned daysInYearsSinceEpoch(unsigned year)
{
    unsigned days = 0;
    while (year > 1969) {
        days += 365;
        if (isLeapYear(year))
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
    while (updateInProgress())
        ;

    unsigned year = (CMOS::read(0x32) * 100) + CMOS::read(0x09);
    unsigned month = CMOS::read(0x08);
    unsigned day = CMOS::read(0x07);
    unsigned hour = CMOS::read(0x04);
    unsigned minute = CMOS::read(0x02);
    unsigned second = CMOS::read(0x00);

    ASSERT(year >= 2018);

    return daysInYearsSinceEpoch(year - 1) * 86400
         + daysInMonthsSinceStartOfYear(month - 1, year) * 86400
         + day * 86400
         + hour * 3600
         + minute * 60
         + second;
}

}

