/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Temporal/AbstractOperations.h>
#include <LibJS/Runtime/Value.h>

namespace JS::Temporal {

struct ISOYearMonth {
    i32 year;
    u8 month;
};

ISOYearMonth balance_iso_year_month(i32 year, i32 month);

}
