/*
 * Copyright (c) 2018-2021, James Mintram <me@jamesrm.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <sys/arch/aarch64/regs.h>

#include <Kernel/Security/ExecutionMode.h>

#include <AK/Platform.h>
#include <AK/StdLibExtras.h>

VALIDATE_IS_AARCH64()

namespace Kernel {

struct alignas(16) RegisterState {
    u64 x[31];     // Saved general purpose registers
    u64 spsr_el1;  // Save Processor Status Register, EL1
    u64 elr_el1;   // Exception Link Register, EL1
    u64 sp_el0;    // EL0 stack pointer
    u64 tpidr_el0; // EL0 Software Thread ID Register
    u64 esr_el1;   // Exception Syndrome Register, EL1

    FlatPtr userspace_sp() const { return sp_el0; }
    void set_userspace_sp(FlatPtr value)
    {
        sp_el0 = value;
    }
    FlatPtr ip() const { return elr_el1; }
    void set_ip(FlatPtr value)
    {
        elr_el1 = value;
    }
    FlatPtr bp() const { return x[29]; }

    ExecutionMode previous_mode() const
    {
        return ((spsr_el1 & 0b1111) == 0) ? ExecutionMode::User : ExecutionMode::Kernel;
    }

    void set_return_reg(FlatPtr value) { x[0] = value; }
    void capture_syscall_params(FlatPtr& function, FlatPtr& arg1, FlatPtr& arg2, FlatPtr& arg3, FlatPtr& arg4) const
    {
        function = x[8];
        arg1 = x[1];
        arg2 = x[2];
        arg3 = x[3];
        arg4 = x[4];
    }
};

#define REGISTER_STATE_SIZE (36 * 8)
static_assert(AssertSize<RegisterState, REGISTER_STATE_SIZE>());

inline void copy_kernel_registers_into_ptrace_registers(PtraceRegisters& ptrace_regs, RegisterState const& kernel_regs)
{
    for (auto i = 0; i < 31; i++)
        ptrace_regs.x[i] = kernel_regs.x[i];

    ptrace_regs.sp = kernel_regs.userspace_sp();
    ptrace_regs.pc = kernel_regs.ip();
}

inline void copy_ptrace_registers_into_kernel_registers(RegisterState& kernel_regs, PtraceRegisters const& ptrace_regs)
{
    for (auto i = 0; i < 31; i++)
        kernel_regs.x[i] = ptrace_regs.x[i];

    kernel_regs.set_userspace_sp(ptrace_regs.sp);
    kernel_regs.set_ip(ptrace_regs.pc);
}

struct DebugRegisterState {
};

}
