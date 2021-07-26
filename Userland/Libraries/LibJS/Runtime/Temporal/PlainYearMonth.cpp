/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/Temporal/PlainYearMonth.h>

namespace JS::Temporal {

// 9.5.5 BalanceISOYearMonth ( year, month ), https://tc39.es/proposal-temporal/#sec-temporal-balanceisoyearmonth
ISOYearMonth balance_iso_year_month(i32 year, i32 month)
{
    // 1. Assert: year and month are integers.

    // 2. Set year to year + floor((month - 1) / 12).
    year += (month - 1) / 12;

    // 3. Set month to (month − 1) modulo 12 + 1.
    month = (month - 1) % 12 + 1;

    // 4. Return the new Record { [[Year]]: year, [[Month]]: month }.
    return ISOYearMonth { .year = year, .month = static_cast<u8>(month) };
}

}
