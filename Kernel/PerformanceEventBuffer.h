/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <Kernel/KBuffer.h>

namespace Kernel {

class KBufferBuilder;
struct RegisterState;

struct [[gnu::packed]] MallocPerformanceEvent {
    size_t size;
    FlatPtr ptr;
};

struct [[gnu::packed]] FreePerformanceEvent {
    size_t size;
    FlatPtr ptr;
};

struct [[gnu::packed]] MmapPerformanceEvent {
    size_t size;
    FlatPtr ptr;
    char name[64];
};

struct [[gnu::packed]] MunmapPerformanceEvent {
    size_t size;
    FlatPtr ptr;
};

struct [[gnu::packed]] ProcessCreatePerformanceEvent {
    pid_t parent_pid;
    char executable[64];
};

struct [[gnu::packed]] ProcessExecPerformanceEvent {
    char executable[64];
};

struct [[gnu::packed]] ThreadCreatePerformanceEvent {
    pid_t parent_tid;
};

struct [[gnu::packed]] ContextSwitchPerformanceEvent {
    pid_t next_pid;
    u32 next_tid;
};

struct [[gnu::packed]] KMallocPerformanceEvent {
    size_t size;
    FlatPtr ptr;
};

struct [[gnu::packed]] KFreePerformanceEvent {
    size_t size;
    FlatPtr ptr;
};

struct [[gnu::packed]] SignpostPerformanceEvent {
    FlatPtr arg1;
    FlatPtr arg2;
};

struct [[gnu::packed]] ReadPerformanceEvent {
    int fd;
    size_t size;
    size_t filename_index;
    size_t start_timestamp;
    bool success;
};

struct [[gnu::packed]] PerformanceEvent {
    u32 type { 0 };
    u8 stack_size { 0 };
    u32 pid { 0 };
    u32 tid { 0 };
    u64 timestamp;
    u32 lost_samples;
    union {
        MallocPerformanceEvent malloc;
        FreePerformanceEvent free;
        MmapPerformanceEvent mmap;
        MunmapPerformanceEvent munmap;
        ProcessCreatePerformanceEvent process_create;
        ProcessExecPerformanceEvent process_exec;
        ThreadCreatePerformanceEvent thread_create;
        ContextSwitchPerformanceEvent context_switch;
        KMallocPerformanceEvent kmalloc;
        KFreePerformanceEvent kfree;
        SignpostPerformanceEvent signpost;
        ReadPerformanceEvent read;
    } data;
    static constexpr size_t max_stack_frame_count = 64;
    FlatPtr stack[max_stack_frame_count];
};

enum class ProcessEventType {
    Create,
    Exec
};

class PerformanceEventBuffer {
public:
    static OwnPtr<PerformanceEventBuffer> try_create_with_size(size_t buffer_size);

    ErrorOr<void> append(int type, FlatPtr arg1, FlatPtr arg2, StringView arg3, Thread* current_thread = Thread::current(), FlatPtr arg4 = 0, u64 arg5 = 0, ErrorOr<FlatPtr> arg6 = 0);
    ErrorOr<void> append_with_ip_and_bp(ProcessID pid, ThreadID tid, FlatPtr eip, FlatPtr ebp,
        int type, u32 lost_samples, FlatPtr arg1, FlatPtr arg2, StringView arg3, FlatPtr arg4 = 0, u64 arg5 = {}, ErrorOr<FlatPtr> arg6 = 0);
    ErrorOr<void> append_with_ip_and_bp(ProcessID pid, ThreadID tid, const RegisterState& regs,
        int type, u32 lost_samples, FlatPtr arg1, FlatPtr arg2, StringView arg3, FlatPtr arg4 = 0, u64 arg5 = {}, ErrorOr<FlatPtr> arg6 = 0);

    void clear()
    {
        m_count = 0;
    }

    size_t capacity() const { return m_buffer->size() / sizeof(PerformanceEvent); }
    size_t count() const { return m_count; }
    const PerformanceEvent& at(size_t index) const
    {
        return const_cast<PerformanceEventBuffer&>(*this).at(index);
    }

    ErrorOr<void> to_json(KBufferBuilder&) const;

    ErrorOr<void> add_process(const Process&, ProcessEventType event_type);

    ErrorOr<FlatPtr> register_string(NonnullOwnPtr<KString>);

private:
    explicit PerformanceEventBuffer(NonnullOwnPtr<KBuffer>);

    template<typename Serializer>
    ErrorOr<void> to_json_impl(Serializer&) const;

    PerformanceEvent& at(size_t index);

    size_t m_count { 0 };
    NonnullOwnPtr<KBuffer> m_buffer;

    HashTable<NonnullOwnPtr<KString>> m_strings;
};

extern bool g_profiling_all_threads;
extern PerformanceEventBuffer* g_global_perf_events;
extern u64 g_profiling_event_mask;

}
