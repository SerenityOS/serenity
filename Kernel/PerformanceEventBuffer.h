/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/KBuffer.h>
#include <Kernel/KResult.h>

namespace Kernel {

class KBufferBuilder;

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

struct [[gnu::packed]] PerformanceEvent {
    u16 type { 0 };
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

    KResult append(int type, FlatPtr arg1, FlatPtr arg2, const StringView& arg3, Thread* current_thread = Thread::current());
    KResult append_with_eip_and_ebp(ProcessID pid, ThreadID tid, u32 eip, u32 ebp,
        int type, u32 lost_samples, FlatPtr arg1, FlatPtr arg2, const StringView& arg3);

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

    bool to_json(KBufferBuilder&) const;

    void add_process(const Process&, ProcessEventType event_type);

private:
    explicit PerformanceEventBuffer(NonnullOwnPtr<KBuffer>);

    template<typename Serializer>
    bool to_json_impl(Serializer&) const;

    PerformanceEvent& at(size_t index);

    size_t m_count { 0 };
    NonnullOwnPtr<KBuffer> m_buffer;
};

extern bool g_profiling_all_threads;
extern PerformanceEventBuffer* g_global_perf_events;
extern u64 g_profiling_event_mask;

}
