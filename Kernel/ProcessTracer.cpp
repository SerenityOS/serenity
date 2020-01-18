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

#include <AK/kstdio.h>
#include <Kernel/ProcessTracer.h>

ProcessTracer::ProcessTracer(pid_t pid)
    : m_pid(pid)
{
}

ProcessTracer::~ProcessTracer()
{
}

void ProcessTracer::did_syscall(u32 function, u32 arg1, u32 arg2, u32 arg3, u32 result)
{
    CallData data = { function, arg1, arg2, arg3, result };
    m_calls.enqueue(data);
}

int ProcessTracer::read(FileDescription&, u8* buffer, int buffer_size)
{
    if (m_calls.is_empty())
        return 0;
    auto data = m_calls.dequeue();
    // FIXME: This should not be an assertion.
    ASSERT(buffer_size == sizeof(data));
    memcpy(buffer, &data, sizeof(data));
    return sizeof(data);
}

String ProcessTracer::absolute_path(const FileDescription&) const
{
    return String::format("tracer:%d", m_pid);
}
