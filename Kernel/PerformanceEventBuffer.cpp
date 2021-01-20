/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
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
#include <AK/JsonObject.h>
#include <AK/JsonObjectSerializer.h>
#include <Kernel/KBufferBuilder.h>
#include <Kernel/PerformanceEventBuffer.h>
#include <Kernel/Process.h>

namespace Kernel {

PerformanceEventBuffer::PerformanceEventBuffer()
    : m_buffer(KBuffer::try_create_with_size(4 * MiB, Region::Access::Read | Region::Access::Write, "Performance events", AllocationStrategy::AllocateNow))
{
}

KResult PerformanceEventBuffer::append(int type, FlatPtr arg1, FlatPtr arg2)
{
    FlatPtr ebp;
    asm volatile("movl %%ebp, %%eax"
                 : "=a"(ebp));
    auto current_thread = Thread::current();
    auto eip = current_thread->get_register_dump_from_stack().eip;
    return append_with_eip_and_ebp(eip, ebp, type, arg1, arg2);
}

KResult PerformanceEventBuffer::append_with_eip_and_ebp(u32 eip, u32 ebp, int type, FlatPtr arg1, FlatPtr arg2)
{
    if (count() >= capacity())
        return ENOBUFS;

    PerformanceEvent event;
    event.type = type;

    switch (type) {
    case PERF_EVENT_SAMPLE:
        break;
    case PERF_EVENT_MALLOC:
        event.data.malloc.size = arg1;
        event.data.malloc.ptr = arg2;
        break;
    case PERF_EVENT_FREE:
        event.data.free.ptr = arg1;
        break;
    default:
        return EINVAL;
    }

    auto current_thread = Thread::current();
    Vector<FlatPtr> backtrace;
    {
        SmapDisabler disabler;
        backtrace = current_thread->raw_backtrace(ebp, eip);
    }
    event.stack_size = min(sizeof(event.stack) / sizeof(FlatPtr), static_cast<size_t>(backtrace.size()));
    memcpy(event.stack, backtrace.data(), event.stack_size * sizeof(FlatPtr));

    event.timestamp = TimeManagement::the().uptime_ms();
    at(m_count++) = event;
    return KSuccess;
}

PerformanceEvent& PerformanceEventBuffer::at(size_t index)
{
    ASSERT(index < capacity());
    auto* events = reinterpret_cast<PerformanceEvent*>(m_buffer->data());
    return events[index];
}

OwnPtr<KBuffer> PerformanceEventBuffer::to_json(ProcessID pid, const String& executable_path) const
{
    KBufferBuilder builder;
    if (!to_json(builder, pid, executable_path))
        return {};
    return builder.build();
}

bool PerformanceEventBuffer::to_json(KBufferBuilder& builder, ProcessID pid, const String& executable_path) const
{
    auto process = Process::from_pid(pid);
    ASSERT(process);
    ScopedSpinLock locker(process->get_lock());

    JsonObjectSerializer object(builder);
    object.add("pid", pid.value());
    object.add("executable", executable_path);

    {
        auto region_array = object.add_array("regions");
        for (const auto& region : process->regions()) {
            auto region_object = region_array.add_object();
            region_object.add("base", region.vaddr().get());
            region_object.add("size", region.size());
            region_object.add("name", region.name());
        }
        region_array.finish();
    }

    auto array = object.add_array("events");
    for (size_t i = 0; i < m_count; ++i) {
        auto& event = at(i);
        auto event_object = array.add_object();
        switch (event.type) {
        case PERF_EVENT_SAMPLE:
            event_object.add("type", "sample");
            break;
        case PERF_EVENT_MALLOC:
            event_object.add("type", "malloc");
            event_object.add("ptr", static_cast<u64>(event.data.malloc.ptr));
            event_object.add("size", static_cast<u64>(event.data.malloc.size));
            break;
        case PERF_EVENT_FREE:
            event_object.add("type", "free");
            event_object.add("ptr", static_cast<u64>(event.data.free.ptr));
            break;
        }
        event_object.add("tid", event.tid);
        event_object.add("timestamp", event.timestamp);
        auto stack_array = event_object.add_array("stack");
        for (size_t j = 0; j < event.stack_size; ++j) {
            stack_array.add(event.stack[j]);
        }
        stack_array.finish();
        event_object.finish();
    }
    array.finish();
    object.finish();
    return true;
}

}
