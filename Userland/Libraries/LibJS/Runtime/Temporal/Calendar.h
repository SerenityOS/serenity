/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Value.h>

namespace JS::Temporal {

bool is_iso_leap_year(i32 year);
i32 iso_days_in_month(i32 year, i32 month);

}
