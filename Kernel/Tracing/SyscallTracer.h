/*
 * Copyright (c) 2020, Sergey Bugaev <bugaevc@serenityos.org>
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

#include <AK/CircularQueue.h>
#include <Kernel/Tracing/ProcessTracer.h>

class SyscallTracer : public ProcessTracer {
public:
    static NonnullRefPtr<SyscallTracer> create(Process& process) { return adopt(*new SyscallTracer(process)); }
    virtual ~SyscallTracer() override;

    virtual bool have_more_items() const override { return !m_calls.is_empty(); }
    virtual void read_item(SimpleBufferBuilder&) const override;
    virtual void dequeue_item() override { m_calls.dequeue(); }

    void did_syscall(u32 function, u32 arg1, u32 arg2, u32 arg3, u32 result);

    virtual bool is_syscall_tracer() const override { return true; }

private:
    virtual const char* class_name() const override { return "SyscallTracer"; }
    explicit SyscallTracer(Process&);

    struct CallData {
        u32 function;
        u32 arg1;
        u32 arg2;
        u32 arg3;
        u32 result;
    };

    CircularQueue<CallData, 200> m_calls;
};
