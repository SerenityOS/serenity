/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonArraySerializer.h>
#include <AK/JsonObjectSerializer.h>
#include <AK/ScopeGuard.h>
#include <Kernel/Arch/RegisterState.h>
#include <Kernel/Arch/SmapDisabler.h>
#include <Kernel/FileSystem/Custody.h>
#include <Kernel/KBufferBuilder.h>
#include <Kernel/PerformanceEventBuffer.h>
#include <Kernel/Process.h>

namespace Kernel {

PerformanceEventBuffer::PerformanceEventBuffer(NonnullOwnPtr<KBuffer> buffer)
    : m_buffer(move(buffer))
{
}

NEVER_INLINE ErrorOr<void> PerformanceEventBuffer::append(int type, FlatPtr arg1, FlatPtr arg2, StringView arg3, Thread* current_thread)
{
    FlatPtr ebp;
    asm volatile("movl %%ebp, %%eax"
                 : "=a"(ebp));
    return append_with_ip_and_bp(current_thread->pid(), current_thread->tid(), 0, ebp, type, 0, arg1, arg2, arg3);
}

static Vector<FlatPtr, PerformanceEvent::max_stack_frame_count> raw_backtrace(FlatPtr bp, FlatPtr ip)
{
    Vector<FlatPtr, PerformanceEvent::max_stack_frame_count> backtrace;
    if (ip != 0)
        backtrace.append(ip);
    FlatPtr stack_ptr_copy;
    FlatPtr stack_ptr = bp;
    // FIXME: Figure out how to remove this SmapDisabler without breaking profile stacks.
    SmapDisabler disabler;
    while (stack_ptr) {
        void* fault_at;
        if (!safe_memcpy(&stack_ptr_copy, (void*)stack_ptr, sizeof(FlatPtr), fault_at))
            break;
        FlatPtr retaddr;
        if (!safe_memcpy(&retaddr, (void*)(stack_ptr + sizeof(FlatPtr)), sizeof(FlatPtr), fault_at))
            break;
        if (retaddr == 0)
            break;
        backtrace.append(retaddr);
        if (backtrace.size() == PerformanceEvent::max_stack_frame_count)
            break;
        stack_ptr = stack_ptr_copy;
    }
    return backtrace;
}

ErrorOr<void> PerformanceEventBuffer::append_with_ip_and_bp(ProcessID pid, ThreadID tid, const RegisterState& regs,
    int type, u32 lost_samples, FlatPtr arg1, FlatPtr arg2, StringView arg3)
{
    return append_with_ip_and_bp(pid, tid, regs.ip(), regs.bp(), type, lost_samples, arg1, arg2, arg3);
}

ErrorOr<void> PerformanceEventBuffer::append_with_ip_and_bp(ProcessID pid, ThreadID tid,
    FlatPtr ip, FlatPtr bp, int type, u32 lost_samples, FlatPtr arg1, FlatPtr arg2, StringView arg3)
{
    if (count() >= capacity())
        return ENOBUFS;

    if ((g_profiling_event_mask & type) == 0)
        return EINVAL;

    auto current_thread = Thread::current();
    u32 enter_count = 0;
    if (current_thread)
        enter_count = current_thread->enter_profiler();
    ScopeGuard leave_profiler([&] {
        if (current_thread)
            current_thread->leave_profiler();
    });
    if (enter_count > 0)
        return EINVAL;

    PerformanceEvent event;
    event.type = type;
    event.lost_samples = lost_samples;

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
    case PERF_EVENT_MMAP:
        event.data.mmap.ptr = arg1;
        event.data.mmap.size = arg2;
        memset(event.data.mmap.name, 0, sizeof(event.data.mmap.name));
        if (!arg3.is_empty())
            memcpy(event.data.mmap.name, arg3.characters_without_null_termination(), min(arg3.length(), sizeof(event.data.mmap.name) - 1));
        break;
    case PERF_EVENT_MUNMAP:
        event.data.munmap.ptr = arg1;
        event.data.munmap.size = arg2;
        break;
    case PERF_EVENT_PROCESS_CREATE:
        event.data.process_create.parent_pid = arg1;
        memset(event.data.process_create.executable, 0, sizeof(event.data.process_create.executable));
        if (!arg3.is_empty()) {
            memcpy(event.data.process_create.executable, arg3.characters_without_null_termination(),
                min(arg3.length(), sizeof(event.data.process_create.executable) - 1));
        }
        break;
    case PERF_EVENT_PROCESS_EXEC:
        memset(event.data.process_exec.executable, 0, sizeof(event.data.process_exec.executable));
        if (!arg3.is_empty()) {
            memcpy(event.data.process_exec.executable, arg3.characters_without_null_termination(),
                min(arg3.length(), sizeof(event.data.process_exec.executable) - 1));
        }
        break;
    case PERF_EVENT_PROCESS_EXIT:
        break;
    case PERF_EVENT_THREAD_CREATE:
        event.data.thread_create.parent_tid = arg1;
        break;
    case PERF_EVENT_THREAD_EXIT:
        break;
    case PERF_EVENT_CONTEXT_SWITCH:
        event.data.context_switch.next_pid = arg1;
        event.data.context_switch.next_tid = arg2;
        break;
    case PERF_EVENT_KMALLOC:
        event.data.kmalloc.size = arg1;
        event.data.kmalloc.ptr = arg2;
        break;
    case PERF_EVENT_KFREE:
        event.data.kfree.size = arg1;
        event.data.kfree.ptr = arg2;
        break;
    case PERF_EVENT_PAGE_FAULT:
        break;
    case PERF_EVENT_SYSCALL:
        break;
    case PERF_EVENT_SIGNPOST:
        event.data.signpost.arg1 = arg1;
        event.data.signpost.arg2 = arg2;
        break;
    default:
        return EINVAL;
    }

    auto backtrace = raw_backtrace(bp, ip);
    event.stack_size = min(sizeof(event.stack) / sizeof(FlatPtr), static_cast<size_t>(backtrace.size()));
    memcpy(event.stack, backtrace.data(), event.stack_size * sizeof(FlatPtr));

    event.pid = pid.value();
    event.tid = tid.value();
    event.timestamp = TimeManagement::the().uptime_ms();
    at(m_count++) = event;
    return {};
}

PerformanceEvent& PerformanceEventBuffer::at(size_t index)
{
    VERIFY(index < capacity());
    auto* events = reinterpret_cast<PerformanceEvent*>(m_buffer->data());
    return events[index];
}

template<typename Serializer>
ErrorOr<void> PerformanceEventBuffer::to_json_impl(Serializer& object) const
{
    {
        auto strings = object.add_array("strings");
        for (auto& it : m_strings) {
            strings.add(it->view());
        }
    }

    auto array = object.add_array("events");
    bool seen_first_sample = false;
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
        case PERF_EVENT_MMAP:
            event_object.add("type", "mmap");
            event_object.add("ptr", static_cast<u64>(event.data.mmap.ptr));
            event_object.add("size", static_cast<u64>(event.data.mmap.size));
            event_object.add("name", event.data.mmap.name);
            break;
        case PERF_EVENT_MUNMAP:
            event_object.add("type", "munmap");
            event_object.add("ptr", static_cast<u64>(event.data.munmap.ptr));
            event_object.add("size", static_cast<u64>(event.data.munmap.size));
            break;
        case PERF_EVENT_PROCESS_CREATE:
            event_object.add("type", "process_create");
            event_object.add("parent_pid", static_cast<u64>(event.data.process_create.parent_pid));
            event_object.add("executable", event.data.process_create.executable);
            break;
        case PERF_EVENT_PROCESS_EXEC:
            event_object.add("type", "process_exec");
            event_object.add("executable", event.data.process_exec.executable);
            break;
        case PERF_EVENT_PROCESS_EXIT:
            event_object.add("type", "process_exit");
            break;
        case PERF_EVENT_THREAD_CREATE:
            event_object.add("type", "thread_create");
            event_object.add("parent_tid", static_cast<u64>(event.data.thread_create.parent_tid));
            break;
        case PERF_EVENT_THREAD_EXIT:
            event_object.add("type", "thread_exit");
            break;
        case PERF_EVENT_CONTEXT_SWITCH:
            event_object.add("type", "context_switch");
            event_object.add("next_pid", static_cast<u64>(event.data.context_switch.next_pid));
            event_object.add("next_tid", static_cast<u64>(event.data.context_switch.next_tid));
            break;
        case PERF_EVENT_KMALLOC:
            event_object.add("type", "kmalloc");
            event_object.add("ptr", static_cast<u64>(event.data.kmalloc.ptr));
            event_object.add("size", static_cast<u64>(event.data.kmalloc.size));
            break;
        case PERF_EVENT_KFREE:
            event_object.add("type", "kfree");
            event_object.add("ptr", static_cast<u64>(event.data.kfree.ptr));
            event_object.add("size", static_cast<u64>(event.data.kfree.size));
            break;
        case PERF_EVENT_PAGE_FAULT:
            event_object.add("type", "page_fault");
            break;
        case PERF_EVENT_SYSCALL:
            event_object.add("type", "syscall");
            break;
        case PERF_EVENT_SIGNPOST:
            event_object.add("type"sv, "signpost"sv);
            event_object.add("arg1"sv, event.data.signpost.arg1);
            event_object.add("arg2"sv, event.data.signpost.arg2);
            break;
        }
        event_object.add("pid", event.pid);
        event_object.add("tid", event.tid);
        event_object.add("timestamp", event.timestamp);
        event_object.add("lost_samples", seen_first_sample ? event.lost_samples : 0);
        if (event.type == PERF_EVENT_SAMPLE)
            seen_first_sample = true;
        auto stack_array = event_object.add_array("stack");
        for (size_t j = 0; j < event.stack_size; ++j) {
            stack_array.add(event.stack[j]);
        }
        stack_array.finish();
        event_object.finish();
    }
    array.finish();
    object.finish();
    return {};
}

ErrorOr<void> PerformanceEventBuffer::to_json(KBufferBuilder& builder) const
{
    JsonObjectSerializer object(builder);
    return to_json_impl(object);
}

OwnPtr<PerformanceEventBuffer> PerformanceEventBuffer::try_create_with_size(size_t buffer_size)
{
    auto buffer_or_error = KBuffer::try_create_with_size(buffer_size, Memory::Region::Access::ReadWrite, "Performance events", AllocationStrategy::AllocateNow);
    if (buffer_or_error.is_error())
        return {};
    return adopt_own_if_nonnull(new (nothrow) PerformanceEventBuffer(buffer_or_error.release_value()));
}

void PerformanceEventBuffer::add_process(const Process& process, ProcessEventType event_type)
{
    SpinlockLocker locker(process.address_space().get_lock());

    String executable;
    if (process.executable())
        executable = process.executable()->absolute_path();
    else
        executable = String::formatted("<{}>", process.name());

    [[maybe_unused]] auto rc = append_with_ip_and_bp(process.pid(), 0, 0, 0,
        event_type == ProcessEventType::Create ? PERF_EVENT_PROCESS_CREATE : PERF_EVENT_PROCESS_EXEC,
        0, process.pid().value(), 0, executable);

    process.for_each_thread([&](auto& thread) {
        [[maybe_unused]] auto rc = append_with_ip_and_bp(process.pid(), thread.tid().value(),
            0, 0, PERF_EVENT_THREAD_CREATE, 0, 0, 0, nullptr);
    });

    for (auto& region : process.address_space().regions()) {
        [[maybe_unused]] auto rc = append_with_ip_and_bp(process.pid(), 0,
            0, 0, PERF_EVENT_MMAP, 0, region->range().base().get(), region->range().size(), region->name());
    }
}

ErrorOr<FlatPtr> PerformanceEventBuffer::register_string(NonnullOwnPtr<KString> string)
{
    FlatPtr string_id = m_strings.size();
    TRY(m_strings.try_set(move(string)));
    return string_id;
}

}
