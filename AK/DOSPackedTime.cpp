/*
 * Copyright (c) 2022, Undefine <undefine@undefine.pl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/DOSPackedTime.h>

namespace AK {

Duration time_from_packed_dos(DOSPackedDate date, DOSPackedTime time)
{
    if (date.value == 0)
        return Duration();

    return Duration::from_timestamp(first_dos_year + date.year, date.month, date.day, time.hour, time.minute, time.second * 2, 0);
}

DOSPackedDate to_packed_dos_date(unsigned year, unsigned month, unsigned day)
{
    DOSPackedDate date;
    date.year = year - first_dos_year;
    date.month = month;
    date.day = day;

    return date;
}

DOSPackedTime to_packed_dos_time(unsigned hour, unsigned minute, unsigned second)
{
    DOSPackedTime time;
    time.hour = hour;
    time.minute = minute;
    time.second = second / 2;

    return time;
}

}
