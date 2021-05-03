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
#include <Kernel/Arch/x86/SmapDisabler.h>
#include <Kernel/FileSystem/Custody.h>
#include <Kernel/KBufferBuilder.h>
#include <Kernel/PerformanceEventBuffer.h>
#include <Kernel/Process.h>

namespace Kernel {

PerformanceEventBuffer::PerformanceEventBuffer(NonnullOwnPtr<KBuffer> buffer)
    : m_buffer(move(buffer))
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

static Vector<FlatPtr, PerformanceEvent::max_stack_frame_count> raw_backtrace(FlatPtr ebp, FlatPtr eip)
{
    Vector<FlatPtr, PerformanceEvent::max_stack_frame_count> backtrace;
    backtrace.append(eip);
    FlatPtr stack_ptr_copy;
    FlatPtr stack_ptr = (FlatPtr)ebp;
    // FIXME: Figure out how to remove this SmapDisabler without breaking profile stacks.
    SmapDisabler disabler;
    while (stack_ptr) {
        void* fault_at;
        if (!safe_memcpy(&stack_ptr_copy, (void*)stack_ptr, sizeof(FlatPtr), fault_at))
            break;
        FlatPtr retaddr;
        if (!safe_memcpy(&retaddr, (void*)(stack_ptr + sizeof(FlatPtr)), sizeof(FlatPtr), fault_at))
            break;
        backtrace.append(retaddr);
        if (backtrace.size() == PerformanceEvent::max_stack_frame_count)
            break;
        stack_ptr = stack_ptr_copy;
    }
    return backtrace;
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

    auto backtrace = raw_backtrace(ebp, eip);
    event.stack_size = min(sizeof(event.stack) / sizeof(FlatPtr), static_cast<size_t>(backtrace.size()));
    memcpy(event.stack, backtrace.data(), event.stack_size * sizeof(FlatPtr));

    event.tid = Thread::current()->tid().value();
    event.timestamp = TimeManagement::the().uptime_ms();
    at(m_count++) = event;
    return KSuccess;
}

PerformanceEvent& PerformanceEventBuffer::at(size_t index)
{
    VERIFY(index < capacity());
    auto* events = reinterpret_cast<PerformanceEvent*>(m_buffer->data());
    return events[index];
}

template<typename Serializer>
bool PerformanceEventBuffer::to_json_impl(Serializer& object) const
{
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

bool PerformanceEventBuffer::to_json(KBufferBuilder& builder) const
{
    JsonObjectSerializer object(builder);

    auto processes_array = object.add_array("processes");
    for (auto& it : m_processes) {
        auto& process = *it.value;
        auto process_object = processes_array.add_object();
        process_object.add("pid", process.pid.value());
        process_object.add("executable", process.executable);

        auto regions_array = process_object.add_array("regions");
        for (auto& region : process.regions) {
            auto region_object = regions_array.add_object();
            region_object.add("name", region.name);
            region_object.add("base", region.range.base().get());
            region_object.add("size", region.range.size());
        }
    }

    processes_array.finish();

    return to_json_impl(object);
}

OwnPtr<PerformanceEventBuffer> PerformanceEventBuffer::try_create_with_size(size_t buffer_size)
{
    auto buffer = KBuffer::try_create_with_size(buffer_size, Region::Access::Read | Region::Access::Write, "Performance events", AllocationStrategy::AllocateNow);
    if (!buffer)
        return {};
    return adopt_own(*new PerformanceEventBuffer(buffer.release_nonnull()));
}

void PerformanceEventBuffer::add_process(const Process& process)
{
    // FIXME: What about threads that have died?

    ScopedSpinLock locker(process.space().get_lock());

    String executable;
    if (process.executable())
        executable = process.executable()->absolute_path();

    auto sampled_process = adopt_own(*new SampledProcess {
        .pid = process.pid().value(),
        .executable = executable,
        .threads = {},
        .regions = {},
    });
    process.for_each_thread([&](auto& thread) {
        sampled_process->threads.set(thread.tid());
        return IterationDecision::Continue;
    });

    for (auto& region : process.space().regions()) {
        sampled_process->regions.append(SampledProcess::Region {
            .name = region.name(),
            .range = region.range(),
        });
    }

    m_processes.set(process.pid(), move(sampled_process));
}

}
