/*
 * Copyright (c) 2024, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/EnumBits.h>
#include <AK/Types.h>

namespace Kernel {

enum class UnshareType {
    ScopedProcessList = 1,
    VFSRootContext = 2,
    HostnameContext = 3,
};

enum class UnshareEnterFlags : u32 {
    None = 0,
    CurrentProgram = 1 << 0,
    AfterExec = 1 << 1,
};

AK_ENUM_BITWISE_OPERATORS(UnshareEnterFlags);

}
