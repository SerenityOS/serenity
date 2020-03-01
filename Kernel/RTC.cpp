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

#include <AK/Assertions.h>
#include <AK/LogStream.h>
#include <Kernel/CMOS.h>
#include <Kernel/RTC.h>

namespace RTC {

static time_t s_boot_time;

void initialize()
{
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

u8 bcd_to_binary(u8 bcd)
{
    return (bcd & 0x0F) + ((bcd >> 4) * 10);
}

void read_registers(unsigned& year, unsigned& month, unsigned& day, unsigned& hour, unsigned& minute, unsigned& second)
{
    while (update_in_progress())
        ;

    u8 status_b = CMOS::read(0x0b);

    second = CMOS::read(0x00);
    minute = CMOS::read(0x02);
    hour = CMOS::read(0x04);
    day = CMOS::read(0x07);
    month = CMOS::read(0x08);
    year = CMOS::read(0x09);

    if (!(status_b & 0x04)) {
        second = bcd_to_binary(second);
        minute = bcd_to_binary(minute);
        hour = bcd_to_binary(hour & 0x70);
        day = bcd_to_binary(day);
        month = bcd_to_binary(month);
        year = bcd_to_binary(year);
    }

    if (!(status_b & 0x02) && (hour & 0x80)) {
        hour = ((hour & 0x7F) + 12) % 24;
    }

    year += 2000;
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

    klog() << "year: " << year << ", month: " << month << ", day: " << day;

    ASSERT(year >= 2018);

    return days_in_years_since_epoch(year - 1) * 86400
        + days_in_months_since_start_of_year(month - 1, year) * 86400
        + (day - 1) * 86400
        + hour * 3600
        + minute * 60
        + second;
}

}
