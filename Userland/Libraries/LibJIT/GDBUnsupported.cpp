/*
 * Copyright (c) 2023, Jesús Lapastora <cyber.gsuscode@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJIT/GDB.h>

namespace JIT::GDB {
Optional<FixedArray<u8>> build_gdb_image(ReadonlyBytes, StringView, StringView)
{
    return {};
}
}
