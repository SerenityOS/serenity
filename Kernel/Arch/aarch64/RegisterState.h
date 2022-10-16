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
    FlatPtr userspace_sp() const { return 0; }
    FlatPtr ip() const { return 0; }
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
