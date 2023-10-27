/*
 * Copyright (c) 2022, Gunnar Beutner <gbeutner@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Platform.h>

// FIXME: There's only a minor difference between x86 and Aarch64/RISC-V trap frames; the prev_irq member.
//        This seems to be unnecessary (see FIXME in Processor::enter_trap),
//        so investigate whether we need it and either:
//        (1) Remove the member and corresponding code from x86
//        (2) Implement prev_irq in the assembly stubs of Aarch64 and RISC-V
//        and then use the same TrapFrame on all architectures.

#if ARCH(X86_64)
#    include <Kernel/Arch/x86_64/TrapFrame.h>
#elif ARCH(AARCH64)
#    include <Kernel/Arch/aarch64/TrapFrame.h>
#elif ARCH(RISCV64)
#    include <Kernel/Arch/riscv64/TrapFrame.h>
#else
#    error "Unknown architecture"
#endif

namespace Kernel {

extern "C" void enter_trap_no_irq(TrapFrame* trap) __attribute__((used));
extern "C" void enter_trap(TrapFrame*) __attribute__((used));
extern "C" void exit_trap(TrapFrame*) __attribute__((used));

}
