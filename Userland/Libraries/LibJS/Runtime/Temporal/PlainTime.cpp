/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/Temporal/PlainTime.h>
#include <LibJS/Runtime/Value.h>

namespace JS::Temporal {

// 4.5.5 IsValidTime ( hour, minute, second, millisecond, microsecond, nanosecond ), https://tc39.es/proposal-temporal/#sec-temporal-isvalidtime
bool is_valid_time(i32 hour, i32 minute, i32 second, i32 millisecond, i32 microsecond, i32 nanosecond)
{
    // 1. Assert: hour, minute, second, millisecond, microsecond, and nanosecond are integers.

    // 2. If hour < 0 or hour > 23, then
    if (hour < 0 || hour > 23) {
        // a. Return false.
        return false;
    }

    // 3. If minute < 0 or minute > 59, then
    if (minute < 0 || minute > 59) {
        // a. Return false.
        return false;
    }

    // 4. If second < 0 or second > 59, then
    if (second < 0 || second > 59) {
        // a. Return false.
        return false;
    }

    // 5. If millisecond < 0 or millisecond > 999, then
    if (millisecond < 0 || millisecond > 999) {
        // a. Return false.
        return false;
    }

    // 6. If microsecond < 0 or microsecond > 999, then
    if (microsecond < 0 || microsecond > 999) {
        // a. Return false.
        return false;
    }

    // 7. If nanosecond < 0 or nanosecond > 999, then
    if (nanosecond < 0 || nanosecond > 999) {
        // a. Return false.
        return false;
    }

    // 8. Return true.
    return true;
}

}
