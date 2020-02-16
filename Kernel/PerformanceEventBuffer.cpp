/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/JsonArraySerializer.h>
#include <AK/JsonObjectSerializer.h>
#include <Kernel/KBufferBuilder.h>
#include <Kernel/PerformanceEventBuffer.h>

namespace Kernel {

PerformanceEventBuffer::PerformanceEventBuffer()
    : m_buffer(KBuffer::create_with_size(4 * MB))
{
}

KResult PerformanceEventBuffer::append(int type, uintptr_t arg1, uintptr_t arg2)
{
    if (count() >= capacity())
        return KResult(-ENOBUFS);

    PerformanceEvent event;
    event.type = type;

    switch (type) {
    case PERF_EVENT_MALLOC:
        event.data.malloc.size = arg1;
        event.data.malloc.ptr = arg2;
#ifdef VERY_DEBUG
        dbg() << "PERF_EVENT_MALLOC: " << (void*)event.data.malloc.ptr << " (" << event.data.malloc.size << ")";
#endif
        break;
    case PERF_EVENT_FREE:
        event.data.free.ptr = arg1;
#ifdef VERY_DEBUG
        dbg() << "PERF_EVENT_FREE: " << (void*)event.data.free.ptr;
#endif
        break;
    default:
        return KResult(-EINVAL);
    }

    uintptr_t ebp;
    asm volatile("movl %%ebp, %%eax"
                 : "=a"(ebp));
    //copy_from_user(&ebp, (uintptr_t*)current->get_register_dump_from_stack().ebp);
    Vector<uintptr_t> backtrace;
    {
        SmapDisabler disabler;
        backtrace = current->raw_backtrace(ebp);
    }
    event.stack_size = min(sizeof(event.stack) / sizeof(uintptr_t), static_cast<size_t>(backtrace.size()));
    memcpy(event.stack, backtrace.data(), event.stack_size * sizeof(uintptr_t));

#ifdef VERY_DEBUG
    for (size_t i = 0; i < event.stack_size; ++i)
        dbg() << "    " << (void*)event.stack[i];
#endif

    event.timestamp = g_uptime;
    at(m_count++) = event;
    return KSuccess;
}

PerformanceEvent& PerformanceEventBuffer::at(size_t index)
{
    ASSERT(index < capacity());
    auto* events = reinterpret_cast<PerformanceEvent*>(m_buffer.data());
    return events[index];
}

KBuffer PerformanceEventBuffer::to_json(pid_t pid, const String& executable_path) const
{
    KBufferBuilder builder;

    JsonObjectSerializer object(builder);
    object.add("pid", pid);
    object.add("executable", executable_path);

    auto array = object.add_array("events");
    for (size_t i = 0; i < m_count; ++i) {
        auto& event = at(i);
        auto object = array.add_object();
        switch (event.type) {
        case PERF_EVENT_MALLOC:
            object.add("type", "malloc");
            object.add("ptr", static_cast<u64>(event.data.malloc.ptr));
            object.add("size", static_cast<u64>(event.data.malloc.size));
            break;
        case PERF_EVENT_FREE:
            object.add("type", "free");
            object.add("ptr", static_cast<u64>(event.data.free.ptr));
            break;
        }
        object.add("timestamp", event.timestamp);
        auto stack_array = object.add_array("stack");
        for (size_t j = 0; j < event.stack_size; ++j) {
            stack_array.add(event.stack[j]);
        }
        stack_array.finish();
        object.finish();
    }
    array.finish();
    object.finish();
    return builder.build();
}

}
