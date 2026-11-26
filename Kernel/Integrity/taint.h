/*
 * Copyright (c) 2026, bogdan-glitchm <github.34373732+copyright@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <AK/EnumBits.h>
#include <AK/Atomic.h>

enum class TaintFlags : u8 {
    None = 0,  // None
    Oops = 1    // Oops Occured
};

AK_ENUM_BITWISE_OPERATORS(TaintFlags)

namespace Kernel::Integrity {
    inline constinit Atomic<u64> taint = (u64)TaintFlags::None;
}
