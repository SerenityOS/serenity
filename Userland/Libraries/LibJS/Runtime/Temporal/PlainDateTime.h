/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/BigInt.h>

namespace JS::Temporal {

BigInt* get_epoch_from_iso_parts(GlobalObject&, i32 year, i32 month, i32 day, i32 hour, i32 minute, i32 second, i32 millisecond, i32 microsecond, i32 nanosecond);
bool iso_date_time_within_limits(GlobalObject&, i32 year, i32 month, i32 day, i32 hour, i32 minute, i32 second, i32 millisecond, i32 microsecond, i32 nanosecond);

}
