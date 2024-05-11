/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Jakub Berkop <jakub.berkop@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonArraySerializer.h>
#include <AK/JsonObjectSerializer.h>
#include <AK/ScopeGuard.h>
#include <AK/StackUnwinder.h>
#include <Kernel/Arch/RegisterState.h>
#include <Kernel/Arch/SafeMem.h>
#include <Kernel/FileSystem/Custody.h>
#include <Kernel/Library/KBufferBuilder.h>
#include <Kernel/Tasks/PerformanceEventBuffer.h>
#include <Kernel/Tasks/Process.h>
#include <Kernel/Time/TimeManagement.h>

namespace Kernel {

PerformanceEventBuffer::PerformanceEventBuffer(NonnullOwnPtr<KBuffer> buffer)
    : m_buffer(move(buffer))
{
}

NEVER_INLINE ErrorOr<void> PerformanceEventBuffer::append(int type, FlatPtr arg1, FlatPtr arg2, StringView arg3, Thread* current_thread, FilesystemEvent filesystem_event)
{
    FlatPtr base_pointer = (FlatPtr)__builtin_frame_address(0);
    return append_with_ip_and_bp(current_thread->pid(), current_thread->tid(), 0, base_pointer, type, 0, arg1, arg2, arg3, filesystem_event);
}

static Vector<FlatPtr, PerformanceEvent::max_stack_frame_count> raw_backtrace(FlatPtr frame_pointer, FlatPtr pc)
{
    Vector<FlatPtr, PerformanceEvent::max_stack_frame_count> backtrace;
    if (pc != 0)
        backtrace.unchecked_append(pc);

    // NOTE: The stack should always have kernel frames first, followed by userspace frames.
    //       If a userspace frame points back into kernel memory, something is afoot.
    bool is_walking_userspace_stack = false;

    MUST(AK::unwind_stack_from_frame_pointer(
        frame_pointer,
        [&is_walking_userspace_stack](FlatPtr address) -> ErrorOr<FlatPtr> {
            if (!Memory::is_user_address(VirtualAddress { address })) {
                if (is_walking_userspace_stack) {
                    dbgln("SHENANIGANS! Userspace stack points back into kernel memory");
                    return EFAULT;
                }
            } else {
                is_walking_userspace_stack = true;
            }

            FlatPtr value;

            if (Memory::is_user_range(VirtualAddress { address }, sizeof(FlatPtr))) {
                TRY(copy_from_user(&value, bit_cast<FlatPtr*>(address)));
            } else {
                void* fault_at;
                if (!safe_memcpy(&value, bit_cast<FlatPtr*>(address), sizeof(FlatPtr), fault_at))
                    return EFAULT;
            }

            return value;
        },
        [&backtrace](AK::StackFrame stack_frame) -> ErrorOr<IterationDecision> {
            backtrace.unchecked_append(stack_frame.return_address);
            if (backtrace.size() >= PerformanceEvent::max_stack_frame_count)
                return IterationDecision::Break;

            return IterationDecision::Continue;
        }));

    return backtrace;
}

ErrorOr<void> PerformanceEventBuffer::append_with_ip_and_bp(ProcessID pid, ThreadID tid, RegisterState const& regs,
    int type, u32 lost_samples, FlatPtr arg1, FlatPtr arg2, StringView arg3, FilesystemEvent filesystem_event)
{
    return append_with_ip_and_bp(pid, tid, regs.ip(), regs.bp(), type, lost_samples, arg1, arg2, arg3, filesystem_event);
}

ErrorOr<void> PerformanceEventBuffer::append_with_ip_and_bp(ProcessID pid, ThreadID tid,
    FlatPtr ip, FlatPtr bp, int type, u32 lost_samples, FlatPtr arg1, FlatPtr arg2, StringView arg3, FilesystemEvent filesystem_event)
{
    if (count() >= capacity())
        return ENOBUFS;

    if ((g_profiling_event_mask & type) == 0)
        return EINVAL;

    auto* current_thread = Thread::current();
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
    case PERF_EVENT_FILESYSTEM:
        event.data.filesystem = filesystem_event;
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
        auto strings_object = TRY(object.add_array("strings"sv));
        Vector<KString*> strings_sorted_by_index;

        TRY(m_strings.with([&](auto& strings) -> ErrorOr<void> {
            TRY(strings_sorted_by_index.try_resize(strings.size()));

            for (auto& entry : strings) {
                strings_sorted_by_index[entry.value] = const_cast<Kernel::KString*>(entry.key.ptr());
            }

            for (size_t i = 0; i < strings.size(); i++) {
                TRY(strings_object.add(strings_sorted_by_index[i]->view()));
            }

            return {};
        }));

        TRY(strings_object.finish());
    }

    auto current_process_credentials = Process::current().credentials();
    bool show_kernel_addresses = current_process_credentials->is_superuser();
    auto array = TRY(object.add_array("events"sv));
    bool seen_first_sample = false;
    for (size_t i = 0; i < m_count; ++i) {
        auto const& event = at(i);

        if (!show_kernel_addresses) {
            if (event.type == PERF_EVENT_KMALLOC || event.type == PERF_EVENT_KFREE)
                continue;
        }

        auto event_object = TRY(array.add_object());
        switch (event.type) {
        case PERF_EVENT_SAMPLE:
            TRY(event_object.add("type"sv, "sample"));
            break;
        case PERF_EVENT_MALLOC:
            TRY(event_object.add("type"sv, "malloc"));
            TRY(event_object.add("ptr"sv, static_cast<u64>(event.data.malloc.ptr)));
            TRY(event_object.add("size"sv, static_cast<u64>(event.data.malloc.size)));
            break;
        case PERF_EVENT_FREE:
            TRY(event_object.add("type"sv, "free"));
            TRY(event_object.add("ptr"sv, static_cast<u64>(event.data.free.ptr)));
            break;
        case PERF_EVENT_MMAP:
            TRY(event_object.add("type"sv, "mmap"));
            TRY(event_object.add("ptr"sv, static_cast<u64>(event.data.mmap.ptr)));
            TRY(event_object.add("size"sv, static_cast<u64>(event.data.mmap.size)));
            TRY(event_object.add("name"sv, event.data.mmap.name));
            break;
        case PERF_EVENT_MUNMAP:
            TRY(event_object.add("type"sv, "munmap"));
            TRY(event_object.add("ptr"sv, static_cast<u64>(event.data.munmap.ptr)));
            TRY(event_object.add("size"sv, static_cast<u64>(event.data.munmap.size)));
            break;
        case PERF_EVENT_PROCESS_CREATE:
            TRY(event_object.add("type"sv, "process_create"));
            TRY(event_object.add("parent_pid"sv, static_cast<u64>(event.data.process_create.parent_pid)));
            TRY(event_object.add("executable"sv, event.data.process_create.executable));
            break;
        case PERF_EVENT_PROCESS_EXEC:
            TRY(event_object.add("type"sv, "process_exec"));
            TRY(event_object.add("executable"sv, event.data.process_exec.executable));
            break;
        case PERF_EVENT_PROCESS_EXIT:
            TRY(event_object.add("type"sv, "process_exit"));
            break;
        case PERF_EVENT_THREAD_CREATE:
            TRY(event_object.add("type"sv, "thread_create"));
            TRY(event_object.add("parent_tid"sv, static_cast<u64>(event.data.thread_create.parent_tid)));
            break;
        case PERF_EVENT_THREAD_EXIT:
            TRY(event_object.add("type"sv, "thread_exit"));
            break;
        case PERF_EVENT_CONTEXT_SWITCH:
            TRY(event_object.add("type"sv, "context_switch"));
            TRY(event_object.add("next_pid"sv, static_cast<u64>(event.data.context_switch.next_pid)));
            TRY(event_object.add("next_tid"sv, static_cast<u64>(event.data.context_switch.next_tid)));
            break;
        case PERF_EVENT_KMALLOC:
            TRY(event_object.add("type"sv, "kmalloc"));
            TRY(event_object.add("ptr"sv, static_cast<u64>(event.data.kmalloc.ptr)));
            TRY(event_object.add("size"sv, static_cast<u64>(event.data.kmalloc.size)));
            break;
        case PERF_EVENT_KFREE:
            TRY(event_object.add("type"sv, "kfree"));
            TRY(event_object.add("ptr"sv, static_cast<u64>(event.data.kfree.ptr)));
            TRY(event_object.add("size"sv, static_cast<u64>(event.data.kfree.size)));
            break;
        case PERF_EVENT_PAGE_FAULT:
            TRY(event_object.add("type"sv, "page_fault"));
            break;
        case PERF_EVENT_SYSCALL:
            TRY(event_object.add("type"sv, "syscall"));
            break;
        case PERF_EVENT_SIGNPOST:
            TRY(event_object.add("type"sv, "signpost"sv));
            TRY(event_object.add("arg1"sv, event.data.signpost.arg1));
            TRY(event_object.add("arg2"sv, event.data.signpost.arg2));
            break;
        case PERF_EVENT_FILESYSTEM:
            TRY(event_object.add("type"sv, "filesystem"sv));
            TRY(event_object.add("durationNs"sv, event.data.filesystem.durationNs));
            switch (event.data.filesystem.type) {
            case FilesystemEventType::Open: {
                auto const& open = event.data.filesystem.data.open;
                TRY(event_object.add("fs_event_type"sv, "open"sv));
                TRY(event_object.add("dirfd"sv, open.dirfd));
                TRY(event_object.add("filename_index"sv, open.filename_index));
                TRY(event_object.add("options"sv, open.options));
                TRY(event_object.add("mode"sv, open.mode));
                break;
            }
            case FilesystemEventType::Close: {
                auto const& close = event.data.filesystem.data.close;
                TRY(event_object.add("fs_event_type"sv, "close"sv));
                TRY(event_object.add("fd"sv, close.fd));
                TRY(event_object.add("filename_index"sv, close.filename_index));
                break;
            }
            case FilesystemEventType::Readv: {
                auto const& readv = event.data.filesystem.data.readv;
                TRY(event_object.add("fs_event_type"sv, "readv"sv));
                TRY(event_object.add("fd"sv, readv.fd));
                TRY(event_object.add("filename_index"sv, readv.filename_index));
                break;
            }
            case FilesystemEventType::Read: {
                auto const& read = event.data.filesystem.data.read;
                TRY(event_object.add("fs_event_type"sv, "read"sv));
                TRY(event_object.add("fd"sv, read.fd));
                TRY(event_object.add("filename_index"sv, read.filename_index));
                break;
            }
            case FilesystemEventType::Pread: {
                auto const& pread = event.data.filesystem.data.pread;
                TRY(event_object.add("fs_event_type"sv, "pread"sv));
                TRY(event_object.add("fd"sv, pread.fd));
                TRY(event_object.add("filename_index"sv, pread.filename_index));
                TRY(event_object.add("buffer_ptr"sv, pread.buffer_ptr));
                TRY(event_object.add("size"sv, pread.size));
                TRY(event_object.add("offset"sv, pread.offset));
                break;
            }
            }
            break;
        }
        TRY(event_object.add("pid"sv, event.pid));
        TRY(event_object.add("tid"sv, event.tid));
        TRY(event_object.add("timestamp"sv, event.timestamp));
        TRY(event_object.add("lost_samples"sv, seen_first_sample ? event.lost_samples : 0));
        if (event.type == PERF_EVENT_SAMPLE)
            seen_first_sample = true;
        auto stack_array = TRY(event_object.add_array("stack"sv));
        for (size_t j = 0; j < event.stack_size; ++j) {
            auto address = event.stack[j];
            if (!show_kernel_addresses && !Memory::is_user_address(VirtualAddress { address }))
                address = 0xdeadc0de;
            TRY(stack_array.add(address));
        }
        TRY(stack_array.finish());
        TRY(event_object.finish());
    }
    TRY(array.finish());
    TRY(object.finish());
    return {};
}

ErrorOr<void> PerformanceEventBuffer::to_json(KBufferBuilder& builder) const
{
    auto object = TRY(JsonObjectSerializer<>::try_create(builder));
    return to_json_impl(object);
}

OwnPtr<PerformanceEventBuffer> PerformanceEventBuffer::try_create_with_size(size_t buffer_size)
{
    auto buffer_or_error = KBuffer::try_create_with_size("Performance events"sv, buffer_size, Memory::Region::Access::ReadWrite, AllocationStrategy::AllocateNow);
    if (buffer_or_error.is_error())
        return {};
    return adopt_own_if_nonnull(new (nothrow) PerformanceEventBuffer(buffer_or_error.release_value()));
}

ErrorOr<void> PerformanceEventBuffer::add_process(Process const& process, ProcessEventType event_type)
{
    OwnPtr<KString> executable;
    if (process.executable()) {
        executable = TRY(process.executable()->try_serialize_absolute_path());
    } else {
        executable = TRY(process.name().with([&](auto& process_name) {
            return KString::formatted("<{}>", process_name.representable_view());
        }));
    }

    TRY(append_with_ip_and_bp(process.pid(), 0, 0, 0,
        event_type == ProcessEventType::Create ? PERF_EVENT_PROCESS_CREATE : PERF_EVENT_PROCESS_EXEC,
        0, process.pid().value(), 0, executable->view()));

    ErrorOr<void> result;
    process.for_each_thread([&](auto& thread) {
        result = append_with_ip_and_bp(process.pid(), thread.tid().value(),
            0, 0, PERF_EVENT_THREAD_CREATE, 0, 0, 0, {});
        return result.is_error() ? IterationDecision::Break : IterationDecision::Continue;
    });
    TRY(result);

    return process.address_space().with([&](auto& space) -> ErrorOr<void> {
        for (auto const& region : space->region_tree().regions()) {
            TRY(append_with_ip_and_bp(process.pid(), 0,
                0, 0, PERF_EVENT_MMAP, 0, region.range().base().get(), region.range().size(), region.name()));
        }
        return {};
    });
}

ErrorOr<FlatPtr> PerformanceEventBuffer::register_string(NonnullOwnPtr<KString> string)
{
    return m_strings.with([&](auto& m_strings) -> ErrorOr<FlatPtr> {
        auto it = m_strings.find(string);
        if (it != m_strings.end()) {
            return it->value;
        }

        auto new_index = m_strings.size();
        TRY(m_strings.try_set(move(string), move(new_index)));
        return new_index;
    });
}

}
