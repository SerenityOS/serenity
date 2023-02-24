/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/RegisterState.h>
#include <Kernel/Tasks/ThreadTracer.h>

namespace Kernel {

ThreadTracer::ThreadTracer(ProcessID tracer_pid)
    : m_tracer_pid(tracer_pid)
{
}

void ThreadTracer::set_regs(RegisterState const& regs)
{
    PtraceRegisters r {};
    copy_kernel_registers_into_ptrace_registers(r, regs);
    m_regs = r;
}

}
