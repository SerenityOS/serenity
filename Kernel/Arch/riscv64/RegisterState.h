/*
 * Copyright (c) 2023, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <sys/arch/riscv64/regs.h>

#include <Kernel/Arch/riscv64/CSR.h>
#include <Kernel/Security/ExecutionMode.h>

#include <AK/Platform.h>
#include <AK/StdLibExtras.h>

VALIDATE_IS_RISCV64()

namespace Kernel {

struct alignas(16) RegisterState {
    u64 x[31];

    RISCV64::CSR::SSTATUS sstatus;
    u64 sepc;
    RISCV64::CSR::SCAUSE scause;
    u64 stval;

    // x86_64 uses its additional RegisterState member "userspace_rsp" here, which is also invalid if no privilege mode change happened.
    // On RISC-V, we only have one sp member, and regardless of the previous privilege mode, we always use this member here.
    FlatPtr userspace_sp() const { return x[1]; }
    void set_userspace_sp(FlatPtr value) { x[1] = value; }

    FlatPtr ip() const { return sepc; }
    void set_ip(FlatPtr value) { sepc = value; }

    FlatPtr bp() const { return x[7]; }
    void set_bp(FlatPtr value) { x[7] = value; }

    ExecutionMode previous_mode() const
    {
        switch (sstatus.SPP) {
        case RISCV64::CSR::SSTATUS::PrivilegeMode::User:
            return ExecutionMode::User;
        case RISCV64::CSR::SSTATUS::PrivilegeMode::Supervisor:
            return ExecutionMode::Kernel;
        default:
            VERIFY_NOT_REACHED();
        }
    }

    void set_return_reg(FlatPtr value) { x[9] = value; }
    void capture_syscall_params(FlatPtr& function, FlatPtr& arg1, FlatPtr& arg2, FlatPtr& arg3, FlatPtr& arg4) const
    {
        function = x[16];
        arg1 = x[9];
        arg2 = x[10];
        arg3 = x[11];
        arg4 = x[12];
    }
};

#define REGISTER_STATE_SIZE (36 * 8)
static_assert(AssertSize<RegisterState, REGISTER_STATE_SIZE>());

inline void copy_kernel_registers_into_ptrace_registers(PtraceRegisters& ptrace_regs, RegisterState const& kernel_regs)
{
    for (auto i = 0; i < 31; i++)
        ptrace_regs.x[i] = kernel_regs.x[i];

    ptrace_regs.pc = kernel_regs.ip();
}

inline void copy_ptrace_registers_into_kernel_registers(RegisterState& kernel_regs, PtraceRegisters const& ptrace_regs)
{
    for (auto i = 0; i < 31; i++)
        kernel_regs.x[i] = ptrace_regs.x[i];

    kernel_regs.set_ip(ptrace_regs.pc);
}

struct DebugRegisterState {
};

}
