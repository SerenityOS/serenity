/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

#include <AK/Platform.h>

namespace Kernel {

struct TrapFrame {
    u64 x[31];     // Saved general purpose registers
    u64 spsr_el1;  // Save Processor Status Register, EL1
    u64 elr_el1;   // Exception Link Reigster, EL1
    u64 tpidr_el1; // EL0 thread ID
    u64 sp_el0;    // EL0 stack pointer
};

}
