/*
 * Copyright (c) 2018-2021, James Mintram <me@jamesrm.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibC/sys/arch/i386/regs.h>

#include <AK/Platform.h>
VALIDATE_IS_AARCH64()

namespace Kernel {

struct RegisterState {
    u64 x[31];     // Saved general purpose registers
    u64 spsr_el1;  // Save Processor Status Register, EL1
    u64 elr_el1;   // Exception Link Register, EL1
    u64 tpidr_el1; // EL0 thread ID
    u64 sp_el0;    // EL0 stack pointer

    FlatPtr userspace_sp() const { return 0; }
    void set_userspace_sp(FlatPtr value)
    {
        (void)value;
        TODO_AARCH64();
    }
    FlatPtr ip() const { return 0; }
    void set_ip(FlatPtr value)
    {
        (void)value;
        TODO_AARCH64();
    }
    FlatPtr bp() const { TODO_AARCH64(); }
};

inline void copy_kernel_registers_into_ptrace_registers(PtraceRegisters& ptrace_regs, RegisterState const& kernel_regs)
{
    (void)ptrace_regs;
    (void)kernel_regs;
    TODO_AARCH64();
}

struct DebugRegisterState {
};

}
