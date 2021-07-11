/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/Temporal/Calendar.h>
#include <LibJS/Runtime/Value.h>

namespace JS::Temporal {

// 12.1.30 IsISOLeapYear ( year ), https://tc39.es/proposal-temporal/#sec-temporal-isisoleapyear
bool is_iso_leap_year(i32 year)
{
    // 1. Assert: year is an integer.

    // 2. If year modulo 4 ≠ 0, return false.
    if (year % 4 != 0)
        return false;

    // 3. If year modulo 400 = 0, return true.
    if (year % 400 == 0)
        return true;

    // 4. If year modulo 100 = 0, return false.
    if (year % 100 == 0)
        return false;

    // 5. Return true.
    return true;
}

// 12.1.32 ISODaysInMonth ( year, month ), https://tc39.es/proposal-temporal/#sec-temporal-isodaysinmonth
i32 iso_days_in_month(i32 year, i32 month)
{
    // 1. Assert: year is an integer.

    // 2. Assert: month is an integer, month ≥ 1, and month ≤ 12.
    VERIFY(month >= 1 && month <= 12);

    // 3. If month is 1, 3, 5, 7, 8, 10, or 12, return 31.
    if (month == 1 || month == 3 || month == 5 || month == 7 || month == 8 || month == 10 || month == 12)
        return 31;

    // 4. If month is 4, 6, 9, or 11, return 30.
    if (month == 4 || month == 6 || month == 9 || month == 11)
        return 30;

    // 5. If ! IsISOLeapYear(year) is true, return 29.
    if (is_iso_leap_year(year))
        return 29;

    // 6. Return 28.
    return 28;
}

}
