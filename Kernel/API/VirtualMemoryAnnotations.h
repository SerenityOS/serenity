/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/EnumBits.h>
#include <AK/Types.h>

namespace Kernel {

enum class VirtualMemoryRangeFlags : u32 {
    None = 0,
    SyscallCode = 1 << 0,
    Immutable = 1 << 1,
};

AK_ENUM_BITWISE_OPERATORS(VirtualMemoryRangeFlags);

}
