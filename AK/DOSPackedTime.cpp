/*
 * Copyright (c) 2022, Undefine <undefine@undefine.pl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/DOSPackedTime.h>

namespace AK {

UnixDateTime time_from_packed_dos(DOSPackedDate date, DOSPackedTime time)
{
    if (date.value == 0)
        return UnixDateTime::from_unix_time_parts(first_dos_year, 1, 1, 0, 0, 0, 0);

    return UnixDateTime::from_unix_time_parts(first_dos_year + date.year, date.month, date.day, time.hour, time.minute, time.second * 2, 0);
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
