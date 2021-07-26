/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Value.h>

namespace JS::Temporal {

bool is_valid_time(u8 hour, u8 minute, u8 second, u16 millisecond, u16 microsecond, u16 nanosecond);

}
