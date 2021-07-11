/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Value.h>

namespace JS::Temporal {

bool is_valid_time(i32 hour, i32 minute, i32 second, i32 millisecond, i32 microsecond, i32 nanosecond);

}
