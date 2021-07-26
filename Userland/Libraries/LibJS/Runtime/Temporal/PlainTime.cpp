/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/Temporal/PlainTime.h>
#include <LibJS/Runtime/Value.h>

namespace JS::Temporal {

// 4.5.5 IsValidTime ( hour, minute, second, millisecond, microsecond, nanosecond ), https://tc39.es/proposal-temporal/#sec-temporal-isvalidtime
bool is_valid_time(u8 hour, u8 minute, u8 second, u16 millisecond, u16 microsecond, u16 nanosecond)
{
    // 1. Assert: hour, minute, second, millisecond, microsecond, and nanosecond are integers.

    // 2. If hour < 0 or hour > 23, then
    if (hour > 23) {
        // a. Return false.
        return false;
    }

    // 3. If minute < 0 or minute > 59, then
    if (minute > 59) {
        // a. Return false.
        return false;
    }

    // 4. If second < 0 or second > 59, then
    if (second > 59) {
        // a. Return false.
        return false;
    }

    // 5. If millisecond < 0 or millisecond > 999, then
    if (millisecond > 999) {
        // a. Return false.
        return false;
    }

    // 6. If microsecond < 0 or microsecond > 999, then
    if (microsecond > 999) {
        // a. Return false.
        return false;
    }

    // 7. If nanosecond < 0 or nanosecond > 999, then
    if (nanosecond > 999) {
        // a. Return false.
        return false;
    }

    // 8. Return true.
    return true;
}

// 4.5.6 BalanceTime ( hour, minute, second, millisecond, microsecond, nanosecond ), https://tc39.es/proposal-temporal/#sec-temporal-balancetime
DaysAndTime balance_time(i64 hour, i64 minute, i64 second, i64 millisecond, i64 microsecond, i64 nanosecond)
{
    // 1. Assert: hour, minute, second, millisecond, microsecond, and nanosecond are integers.

    // 2. Set microsecond to microsecond + floor(nanosecond / 1000).
    microsecond += nanosecond / 1000;

    // 3. Set nanosecond to nanosecond modulo 1000.
    nanosecond %= 1000;

    // 4. Set millisecond to millisecond + floor(microsecond / 1000).
    millisecond += microsecond / 1000;

    // 5. Set microsecond to microsecond modulo 1000.
    microsecond %= 1000;

    // 6. Set second to second + floor(millisecond / 1000).
    second += millisecond / 1000;

    // 7. Set millisecond to millisecond modulo 1000.
    millisecond %= 1000;

    // 8. Set minute to minute + floor(second / 60).
    minute += second / 60;

    // 9. Set second to second modulo 60.
    second %= 60;

    // 10. Set hour to hour + floor(minute / 60).
    hour += minute / 60;

    // 11. Set minute to minute modulo 60.
    minute %= 60;

    // 12. Let days be floor(hour / 24).
    u8 days = hour / 24;

    // 13. Set hour to hour modulo 24.
    hour %= 24;

    // 14. Return the new Record { [[Days]]: days, [[Hour]]: hour, [[Minute]]: minute, [[Second]]: second, [[Millisecond]]: millisecond, [[Microsecond]]: microsecond, [[Nanosecond]]: nanosecond }.
    return DaysAndTime {
        .days = static_cast<i32>(days),
        .hour = static_cast<u8>(hour),
        .minute = static_cast<u8>(minute),
        .second = static_cast<u8>(second),
        .millisecond = static_cast<u16>(millisecond),
        .microsecond = static_cast<u16>(microsecond),
        .nanosecond = static_cast<u16>(nanosecond),
    };
}

}
