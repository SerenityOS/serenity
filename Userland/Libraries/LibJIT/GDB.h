/*
 * Copyright (c) 2023, Jesús Lapastora <cyber.gsuscode@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Span.h>

namespace JIT::GDB {

void register_into_gdb(ReadonlyBytes data);
void unregister_from_gdb(ReadonlyBytes data);
}
