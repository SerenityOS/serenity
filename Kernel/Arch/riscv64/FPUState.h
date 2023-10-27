/*
 * Copyright (c) 2023, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

#include <AK/Platform.h>
VALIDATE_IS_RISCV64()

namespace Kernel {

struct FPUState {
    u64 f[32];
    u64 fcsr;
};

}
