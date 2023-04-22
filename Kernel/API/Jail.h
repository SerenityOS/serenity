/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/EnumBits.h>
#include <AK/Types.h>

enum class JailIsolationFlags : u32 {
    None = 0,
    PIDIsolation = 1 << 0,
    FileSystemUnveilIsolation = 1 << 1,
};

AK_ENUM_BITWISE_OPERATORS(JailIsolationFlags);

enum class JailConfigureRequest : u32 {
    Invalid = 0,
    UnveilPath = 1,
    LockUnveil = 2,
    SetCleanOnLastDetach = 3,
    UnsetCleanOnLastDetach = 4,
};
