/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/NonnullOwnPtr.h>
#include <AK/Optional.h>
#include <Kernel/UnixTypes.h>
#include <LibC/sys/arch/i386/regs.h>

namespace Kernel {

class ThreadTracer {
public:
    static NonnullOwnPtr<ThreadTracer> create(ProcessID tracer) { return make<ThreadTracer>(tracer); }

    ProcessID tracer_pid() const { return m_tracer_pid; }
    bool has_pending_signal(u32 signal) const { return m_pending_signals & (1 << (signal - 1)); }
    void set_signal(u32 signal) { m_pending_signals |= (1 << (signal - 1)); }
    void unset_signal(u32 signal) { m_pending_signals &= ~(1 << (signal - 1)); }

    bool is_tracing_syscalls() const { return m_trace_syscalls; }
    void set_trace_syscalls(bool val) { m_trace_syscalls = val; }

    void set_regs(const RegisterState& regs);
    void set_regs(const PtraceRegisters& regs) { m_regs = regs; }
    bool has_regs() const { return m_regs.has_value(); }
    const PtraceRegisters& regs() const
    {
        ASSERT(m_regs.has_value());
        return m_regs.value();
    }

    explicit ThreadTracer(ProcessID);

private:
    ProcessID m_tracer_pid { -1 };

    // This is a bitmap for signals that are sent from the tracer to the tracee
    // TODO: Since we do not currently support sending signals
    //       to the tracee via PT_CONTINUE, this bitmap is always zeroed
    u32 m_pending_signals { 0 };

    bool m_trace_syscalls { false };
    Optional<PtraceRegisters> m_regs;
};

}
