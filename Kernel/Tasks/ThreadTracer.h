/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Optional.h>
#include <AK/OwnPtr.h>
#include <Kernel/Arch/RegisterState.h>
#include <Kernel/Forward.h>
#include <sys/arch/regs.h>

namespace Kernel {

class ThreadTracer {
public:
    static ErrorOr<NonnullOwnPtr<ThreadTracer>> try_create(ProcessID tracer) { return adopt_nonnull_own_or_enomem(new (nothrow) ThreadTracer(tracer)); }

    ProcessID tracer_pid() const { return m_tracer_pid; }
    bool has_pending_signal(u32 signal) const { return (m_pending_signals & (1 << (signal - 1))) != 0; }
    void set_signal(u32 signal) { m_pending_signals |= (1 << (signal - 1)); }
    void unset_signal(u32 signal) { m_pending_signals &= ~(1 << (signal - 1)); }

    bool is_tracing_syscalls() const { return m_trace_syscalls; }
    void set_trace_syscalls(bool val) { m_trace_syscalls = val; }

    void set_regs(RegisterState const& regs);
    void set_regs(PtraceRegisters const& regs) { m_regs = regs; }
    bool has_regs() const { return m_regs.has_value(); }
    PtraceRegisters const& regs() const
    {
        VERIFY(m_regs.has_value());
        return m_regs.value();
    }

private:
    explicit ThreadTracer(ProcessID);

    ProcessID m_tracer_pid { -1 };

    // This is a bitmap for signals that are sent from the tracer to the tracee
    // TODO: Since we do not currently support sending signals
    //       to the tracee via PT_CONTINUE, this bitmap is always zeroed
    u32 m_pending_signals { 0 };

    bool m_trace_syscalls { false };
    Optional<PtraceRegisters> m_regs;
};

}
