/*
 * Copyright (c) 2018-2021, James Mintram <me@jamesrm.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibC/sys/arch/regs.h>

#include <AK/Platform.h>
VALIDATE_IS_AARCH64()

#include <AK/Types.h>
#include <Kernel/Arch/CPU.h>

namespace Kernel {

struct RegisterState {
    u64 x[32];     // Saved general purpose registers
    u64 spsr_el1;  // Save Processor Status Register, EL1
    u64 elr_el1;   // Exception Link Register, EL1
    u64 tpidr_el1; // EL0 thread ID
    u64 sp_el0;    // EL0 stack pointer

    FlatPtr pc;
    FlatPtr nzcv;

    FlatPtr userspace_sp() const { return sp_el0; }
    void set_userspace_sp(FlatPtr value)
    {
        sp_el0 = value;
    }
    FlatPtr ip() const { return pc; }
    void set_ip(FlatPtr value)
    {
        pc = value;
    }
    FlatPtr bp() const { return x[29]; }
    void set_bp(FlatPtr value) { x[29] = value; }
    FlatPtr flags() const { return nzcv; }
    void set_flags(FlatPtr value) { nzcv = value; }
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

inline void copy_kernel_registers_into_ptrace_registers(PtraceRegisters& ptrace_regs, RegisterState const& kernel_regs)
{
    ptrace_regs.x0 = kernel_regs.x[0];
    ptrace_regs.x1 = kernel_regs.x[1];
    ptrace_regs.x2 = kernel_regs.x[2];
    ptrace_regs.x3 = kernel_regs.x[3];
    ptrace_regs.x4 = kernel_regs.x[4];
    ptrace_regs.x5 = kernel_regs.x[5];
    ptrace_regs.x6 = kernel_regs.x[6];
    ptrace_regs.x7 = kernel_regs.x[7];
    ptrace_regs.x8 = kernel_regs.x[8];
    ptrace_regs.x9 = kernel_regs.x[9];
    ptrace_regs.x10 = kernel_regs.x[10];
    ptrace_regs.x11 = kernel_regs.x[11];
    ptrace_regs.x12 = kernel_regs.x[12];
    ptrace_regs.x13 = kernel_regs.x[13];
    ptrace_regs.x14 = kernel_regs.x[14];
    ptrace_regs.x15 = kernel_regs.x[15];
    ptrace_regs.x16 = kernel_regs.x[16];
    ptrace_regs.x17 = kernel_regs.x[17];
    ptrace_regs.x18 = kernel_regs.x[18];
    ptrace_regs.x19 = kernel_regs.x[19];
    ptrace_regs.x20 = kernel_regs.x[20];
    ptrace_regs.x21 = kernel_regs.x[21];
    ptrace_regs.x22 = kernel_regs.x[22];
    ptrace_regs.x23 = kernel_regs.x[23];
    ptrace_regs.x24 = kernel_regs.x[24];
    ptrace_regs.x25 = kernel_regs.x[25];
    ptrace_regs.x26 = kernel_regs.x[26];
    ptrace_regs.x27 = kernel_regs.x[27];
    ptrace_regs.x28 = kernel_regs.x[28];
    ptrace_regs.x29 = kernel_regs.x[29];
    ptrace_regs.x30 = kernel_regs.x[30];
    ptrace_regs.x31 = kernel_regs.x[31];

    //    // TODO: take care of flags
    //    ptrace_regs.nzcv = kernel_regs.nzcv;
}

inline void copy_ptrace_registers_into_kernel_registers(RegisterState& kernel_regs, PtraceRegisters const& ptrace_regs)
{
    kernel_regs.x[0] = ptrace_regs.x0;
    kernel_regs.x[1] = ptrace_regs.x1;
    kernel_regs.x[2] = ptrace_regs.x2;
    kernel_regs.x[3] = ptrace_regs.x3;
    kernel_regs.x[4] = ptrace_regs.x4;
    kernel_regs.x[5] = ptrace_regs.x5;
    kernel_regs.x[6] = ptrace_regs.x6;
    kernel_regs.x[7] = ptrace_regs.x7;
    kernel_regs.x[8] = ptrace_regs.x8;
    kernel_regs.x[9] = ptrace_regs.x9;
    kernel_regs.x[10] = ptrace_regs.x10;
    kernel_regs.x[11] = ptrace_regs.x11;
    kernel_regs.x[12] = ptrace_regs.x12;
    kernel_regs.x[13] = ptrace_regs.x13;
    kernel_regs.x[14] = ptrace_regs.x14;
    kernel_regs.x[15] = ptrace_regs.x15;
    kernel_regs.x[16] = ptrace_regs.x16;
    kernel_regs.x[17] = ptrace_regs.x17;
    kernel_regs.x[18] = ptrace_regs.x18;
    kernel_regs.x[19] = ptrace_regs.x19;
    kernel_regs.x[20] = ptrace_regs.x20;
    kernel_regs.x[21] = ptrace_regs.x21;
    kernel_regs.x[22] = ptrace_regs.x22;
    kernel_regs.x[23] = ptrace_regs.x23;
    kernel_regs.x[24] = ptrace_regs.x24;
    kernel_regs.x[25] = ptrace_regs.x25;
    kernel_regs.x[26] = ptrace_regs.x26;
    kernel_regs.x[27] = ptrace_regs.x27;
    kernel_regs.x[28] = ptrace_regs.x28;
    kernel_regs.x[29] = ptrace_regs.x29;
    kernel_regs.x[30] = ptrace_regs.x30;
    kernel_regs.x[31] = ptrace_regs.x31;

    //    // TODO: take care of flags
    //    // FIXME: make NZCV flags safe as they are in x86?
    //    kernel_regs.nzcv = (kernel_regs.nzcv & ~safe_nzcv_mask) | (ptrace_regs.nzcv & safe_nzcv_mask);
}

struct DebugRegisterState {
};

}
