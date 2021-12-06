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
    TrapFrame(const TrapFrame&) = delete;
    TrapFrame(TrapFrame&&) = delete;
    TrapFrame& operator=(const TrapFrame&) = delete;
    TrapFrame& operator=(TrapFrame&&) = delete;
};

#if ARCH(I386)
#    define TRAP_FRAME_SIZE (3 * 4)
#else
#    define TRAP_FRAME_SIZE (3 * 8)
#endif

static_assert(AssertSize<TrapFrame, TRAP_FRAME_SIZE>());

extern "C" void enter_trap_no_irq(TrapFrame* trap) __attribute__((used));
extern "C" void enter_trap(TrapFrame*) __attribute__((used));
extern "C" void exit_trap(TrapFrame*) __attribute__((used));

}
