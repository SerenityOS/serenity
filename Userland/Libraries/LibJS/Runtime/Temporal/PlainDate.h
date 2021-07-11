/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Value.h>

namespace JS::Temporal {

bool is_valid_iso_date(i32 year, i32 month, i32 day);

}
