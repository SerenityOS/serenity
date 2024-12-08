/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <AK/Time.h>
#include <Kernel/Arch/Delay.h>
#include <Kernel/Arch/x86_64/CMOS.h>
#include <Kernel/Arch/x86_64/RTC.h>

namespace Kernel::RTC {

static time_t s_boot_time;

void initialize()
{
    s_boot_time = now();
}

UnixDateTime boot_time()
{
    return UnixDateTime::from_seconds_since_epoch(s_boot_time);
}

static bool update_in_progress()
{
    return CMOS::read(0x0a) & 0x80;
}

static u8 bcd_to_binary(u8 bcd)
{
    return (bcd & 0x0F) + ((bcd >> 4) * 10);
}

static bool try_to_read_registers(unsigned& year, unsigned& month, unsigned& day, unsigned& hour, unsigned& minute, unsigned& second)
{
    // Note: Let's wait 0.1 seconds until we stop trying to query the RTC CMOS
    size_t time_passed_in_milliseconds = 0;
    bool update_in_progress_ended_successfully = false;
    while (time_passed_in_milliseconds < 100) {
        if (!update_in_progress()) {
            update_in_progress_ended_successfully = true;
            break;
        }
        microseconds_delay(1000);
        time_passed_in_milliseconds++;
    }

    if (!update_in_progress_ended_successfully) {
        year = 1970;
        month = 1;
        day = 1;
        hour = 0;
        minute = 0;
        second = 0;
        return false;
    }

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
    return true;
}

time_t now()
{

    auto check_registers_against_preloaded_values = [](unsigned year, unsigned month, unsigned day, unsigned hour, unsigned minute, unsigned second) {
        unsigned checked_year, checked_month, checked_day, checked_hour, checked_minute, checked_second;
        if (!try_to_read_registers(checked_year, checked_month, checked_day, checked_hour, checked_minute, checked_second))
            return false;
        return checked_year == year && checked_month == month && checked_day == day && checked_hour == hour && checked_minute == minute && checked_second == second;
    };

    unsigned year, month, day, hour, minute, second;
    bool did_read_rtc_successfully = false;
    for (size_t attempt = 0; attempt < 5; attempt++) {
        if (!try_to_read_registers(year, month, day, hour, minute, second))
            break;
        if (check_registers_against_preloaded_values(year, month, day, hour, minute, second)) {
            did_read_rtc_successfully = true;
            break;
        }
    }

    dmesgln("RTC: {} Year: {}, month: {}, day: {}, hour: {}, minute: {}, second: {}", (did_read_rtc_successfully ? "" : "(failed to read)"), year, month, day, hour, minute, second);

    time_t days = days_since_epoch(year, month, day);
    return ((days * 24 + hour) * 60 + minute) * 60 + second;
}

}
