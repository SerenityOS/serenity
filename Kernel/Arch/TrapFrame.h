/*
 * Copyright (c) 2022, Gunnar Beutner <gbeutner@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Platform.h>
#include <AK/StdLibExtraDetails.h>

namespace Kernel {

struct RegisterState;

struct TrapFrame {
    TrapFrame* next_trap;
    RegisterState* regs; // must be last

    TrapFrame() = delete;
    TrapFrame(TrapFrame const&) = delete;
    TrapFrame(TrapFrame&&) = delete;
    TrapFrame& operator=(TrapFrame const&) = delete;
    TrapFrame& operator=(TrapFrame&&) = delete;
};

#define TRAP_FRAME_SIZE (2 * 8)

static_assert(AssertSize<TrapFrame, TRAP_FRAME_SIZE>());

extern "C" void enter_trap_no_irq(TrapFrame* trap) __attribute__((used));
extern "C" void enter_trap(TrapFrame*) __attribute__((used));
extern "C" void exit_trap(TrapFrame*) __attribute__((used));
extern "C" void exit_trap_exception(TrapFrame*) __attribute__((used));

}
