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

#include <AK/JsonObjectSerializer.h>
#include <Kernel/Tracing/SimpleBufferBuilder.h>
#include <Kernel/Tracing/SyscallTracer.h>
#include <Kernel/Syscall.h>

SyscallTracer::SyscallTracer(Process& process)
    : ProcessTracer(process)
{
}

SyscallTracer::~SyscallTracer()
{
}

void SyscallTracer::did_syscall(u32 function, u32 arg1, u32 arg2, u32 arg3, u32 result)
{
    CallData data = { function, arg1, arg2, arg3, result };
    m_calls.enqueue(data);
}

void SyscallTracer::read_item(SimpleBufferBuilder& builder) const
{
    auto data = m_calls.first();

    JsonObjectSerializer object(builder);
    object.add("function", Syscall::to_string((Syscall::Function)data.function));
    object.add("result", data.result);
    auto args = object.add_array("args");
    args.add(data.arg1);
    args.add(data.arg2);
    args.add(data.arg3);
}
