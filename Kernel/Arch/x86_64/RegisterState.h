/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <sys/arch/regs.h>

#include <Kernel/Arch/CPU.h>
#include <Kernel/Arch/x86_64/ASM_wrapper.h>
#include <Kernel/Security/ExecutionMode.h>

#include <AK/Platform.h>
VALIDATE_IS_X86()

namespace Kernel {

struct [[gnu::packed]] RegisterState {
    FlatPtr rdi;
    FlatPtr rsi;
    FlatPtr rbp;
    FlatPtr rsp;
    FlatPtr rbx;
    FlatPtr rdx;
    FlatPtr rcx;
    FlatPtr rax;
    FlatPtr r8;
    FlatPtr r9;
    FlatPtr r10;
    FlatPtr r11;
    FlatPtr r12;
    FlatPtr r13;
    FlatPtr r14;
    FlatPtr r15;

    u16 exception_code;
    u16 isr_number;
    u32 padding;

    FlatPtr rip;
    FlatPtr cs;
    FlatPtr rflags;
    FlatPtr userspace_rsp;
    FlatPtr userspace_ss;

    FlatPtr userspace_sp() const
    {
        return userspace_rsp;
    }
    void set_userspace_sp(FlatPtr value) { userspace_rsp = value; }
    FlatPtr ip() const { return rip; }
    void set_ip(FlatPtr value) { rip = value; }
    void set_dx(FlatPtr value) { rdx = value; }
    FlatPtr bp() const { return rbp; }
    void set_bp(FlatPtr value) { rbp = value; }
    FlatPtr flags() const { return rflags; }
    void set_flags(FlatPtr value) { rflags = value; }
    void set_return_reg(FlatPtr value) { rax = value; }

    void capture_syscall_params(FlatPtr& function, FlatPtr& arg1, FlatPtr& arg2, FlatPtr& arg3, FlatPtr& arg4) const
    {
        // The syscall instruction clobbers rcx, so we must use a different calling convention to 32-bit.
        function = rax;
        arg1 = rdx;
        arg2 = rdi;
        arg3 = rbx;
        arg4 = rsi;
    }

    ExecutionMode previous_mode() const
    {
        return ((cs & 3) != 0) ? ExecutionMode::User : ExecutionMode::Kernel;
    }
};

#define REGISTER_STATE_SIZE (22 * 8)
static_assert(AssertSize<RegisterState, REGISTER_STATE_SIZE>());

inline void copy_kernel_registers_into_ptrace_registers(PtraceRegisters& ptrace_regs, RegisterState const& kernel_regs)
{
    ptrace_regs.rax = kernel_regs.rax;
    ptrace_regs.rcx = kernel_regs.rcx;
    ptrace_regs.rdx = kernel_regs.rdx;
    ptrace_regs.rbx = kernel_regs.rbx;
    ptrace_regs.rsp = kernel_regs.userspace_rsp;
    ptrace_regs.rbp = kernel_regs.rbp;
    ptrace_regs.rsi = kernel_regs.rsi;
    ptrace_regs.rdi = kernel_regs.rdi;
    ptrace_regs.rip = kernel_regs.rip;
    ptrace_regs.r8 = kernel_regs.r8;
    ptrace_regs.r9 = kernel_regs.r9;
    ptrace_regs.r10 = kernel_regs.r10;
    ptrace_regs.r11 = kernel_regs.r11;
    ptrace_regs.r12 = kernel_regs.r12;
    ptrace_regs.r13 = kernel_regs.r13;
    ptrace_regs.r14 = kernel_regs.r14;
    ptrace_regs.r15 = kernel_regs.r15;
    ptrace_regs.rflags = kernel_regs.rflags,
    ptrace_regs.cs = 0;
    ptrace_regs.ss = 0;
    ptrace_regs.ds = 0;
    ptrace_regs.es = 0;
    ptrace_regs.fs = 0;
    ptrace_regs.gs = 0;
}

inline void copy_ptrace_registers_into_kernel_registers(RegisterState& kernel_regs, PtraceRegisters const& ptrace_regs)
{
    kernel_regs.rax = ptrace_regs.rax;
    kernel_regs.rcx = ptrace_regs.rcx;
    kernel_regs.rdx = ptrace_regs.rdx;
    kernel_regs.rbx = ptrace_regs.rbx;
    kernel_regs.rsp = ptrace_regs.rsp;
    kernel_regs.rbp = ptrace_regs.rbp;
    kernel_regs.rsi = ptrace_regs.rsi;
    kernel_regs.rdi = ptrace_regs.rdi;
    kernel_regs.rip = ptrace_regs.rip;
    kernel_regs.r8 = ptrace_regs.r8;
    kernel_regs.r9 = ptrace_regs.r9;
    kernel_regs.r10 = ptrace_regs.r10;
    kernel_regs.r11 = ptrace_regs.r11;
    kernel_regs.r12 = ptrace_regs.r12;
    kernel_regs.r13 = ptrace_regs.r13;
    kernel_regs.r14 = ptrace_regs.r14;
    kernel_regs.r15 = ptrace_regs.r15;
    // FIXME: do we need a separate safe_rflags_mask here?
    kernel_regs.rflags = (kernel_regs.rflags & ~safe_eflags_mask) | (ptrace_regs.rflags & safe_eflags_mask);
}

struct [[gnu::packed]] DebugRegisterState {
    FlatPtr dr0;
    FlatPtr dr1;
    FlatPtr dr2;
    FlatPtr dr3;
    FlatPtr dr6;
    FlatPtr dr7;
};

inline void read_debug_registers_into(DebugRegisterState& state)
{
    state.dr0 = read_dr0();
    state.dr1 = read_dr1();
    state.dr2 = read_dr2();
    state.dr3 = read_dr3();
    state.dr6 = read_dr6();
    state.dr7 = read_dr7();
}

inline void write_debug_registers_from(DebugRegisterState const& state)
{
    write_dr0(state.dr0);
    write_dr1(state.dr1);
    write_dr2(state.dr2);
    write_dr3(state.dr3);
    write_dr6(state.dr6);
    write_dr7(state.dr7);
}

inline void clear_debug_registers()
{
    write_dr0(0);
    write_dr1(0);
    write_dr2(0);
    write_dr3(0);
    write_dr7(1 << 10); // Bit 10 is reserved and must be set to 1.
}

}
