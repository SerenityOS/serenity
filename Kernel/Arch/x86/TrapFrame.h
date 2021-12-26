/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

#include <Kernel/Arch/x86/InterruptDisabler.h>
#include <Kernel/Arch/x86/Processor.h>
#include <Kernel/Arch/x86/RegisterState.h>

namespace Kernel {

struct TrapFrame {
    u32 prev_irq_level;
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

static_assert(TRAP_FRAME_SIZE == sizeof(TrapFrame));

extern "C" void enter_trap_no_irq(TrapFrame* trap);
extern "C" void enter_trap(TrapFrame*) __attribute__((used));
extern "C" void exit_trap(TrapFrame*) __attribute__((used));

}
