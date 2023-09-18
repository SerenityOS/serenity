/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

#include <AK/Platform.h>
VALIDATE_IS_X86()

namespace Kernel {

struct RegisterState;

struct TrapFrame {
    FlatPtr prev_irq_level;
    TrapFrame* next_trap;
    RegisterState* regs; // must be last

    TrapFrame() = delete;
    TrapFrame(TrapFrame const&) = delete;
    TrapFrame(TrapFrame&&) = delete;
    TrapFrame& operator=(TrapFrame const&) = delete;
    TrapFrame& operator=(TrapFrame&&) = delete;
};

#define TRAP_FRAME_SIZE (3 * 8)

static_assert(AssertSize<TrapFrame, TRAP_FRAME_SIZE>());

}
