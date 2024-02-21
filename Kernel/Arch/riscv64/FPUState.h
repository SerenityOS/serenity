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

// This struct will get pushed on the stack by the signal handling code.
// Therefore, it has to be aligned to a 16-byte boundary.
struct [[gnu::aligned(16)]] FPUState {
    // FIXME: Add support for the Q extension.
    u64 f[32];
    u64 fcsr;
};

}
