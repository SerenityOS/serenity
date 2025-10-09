/*
 * Copyright (c) 2023, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

#include <AK/Platform.h>
VALIDATE_IS_RISCV64()

namespace Kernel {

static constexpr size_t MAX_SUPPORTED_VLENB = 1024 / 8;

// This struct will get pushed on the stack by the signal handling code.
// Therefore, it has to be aligned to a 16-byte boundary.
struct [[gnu::aligned(16)]] FPUState {
    // FIXME: Add support for the Q extension.
    u64 f[32];
    u64 fcsr;

    u64 vstart;
    u64 vcsr;
    u64 vl;
    u64 vtype;

    // 32 v* registers of vlenb bytes each.
    u8 v[MAX_SUPPORTED_VLENB * 32];
};

}
