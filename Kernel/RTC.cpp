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
#include <AK/Time.h>
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

static u8 bcd_to_binary(u8 bcd)
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

    bool is_pm = hour & 0x80;

    if (!(status_b & 0x04)) {
        second = bcd_to_binary(second);
        minute = bcd_to_binary(minute);
        hour = bcd_to_binary(hour & 0x7F);
        day = bcd_to_binary(day);
        month = bcd_to_binary(month);
        year = bcd_to_binary(year);
    }

    if (!(status_b & 0x02)) {
        // In the 12 hour clock, midnight and noon are 12, not 0. Map it to 0.
        hour %= 12;
        if (is_pm)
            hour += 12;
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

    klog() << "RTC: Year: " << year << ", month: " << month << ", day: " << day << ", hour: " << hour << ", minute: " << minute << ", second: " << second;

    ASSERT(year >= 2018);

    time_t days_since_epoch = years_to_days_since_epoch(year) + day_of_year(year, month, day);
    return ((days_since_epoch * 24 + hour) * 60 + minute) * 60 + second;
}

}
