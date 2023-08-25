/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Jakub Berkop <jakub.berkop@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <Kernel/Library/KBuffer.h>

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

enum class FilesystemEventType : u8 {
    Open,
    Close,
    Readv,
    Read,
    Pread
};

struct [[gnu::packed]] OpenEventData {
    int dirfd;
    size_t filename_index;
    int options;
    u64 mode;
};

struct [[gnu::packed]] CloseEventData {
    int fd;
    size_t filename_index;
};

struct [[gnu::packed]] ReadvEventData {
    int fd;
    size_t filename_index;
    // struct iovec* iov; // TODO: Implement
    // int iov_count; // TODO: Implement
};

struct [[gnu::packed]] ReadEventData {
    int fd;
    size_t filename_index;
};

struct [[gnu::packed]] PreadEventData {
    int fd;
    size_t filename_index;
    FlatPtr buffer_ptr;
    size_t size;
    off_t offset;
};

// FIXME: This is a hack to make the compiler pack this struct correctly.
struct [[gnu::packed]] PackedErrorOr {
    bool is_error;
    FlatPtr value;
};

struct [[gnu::packed]] FilesystemEvent {
    FilesystemEventType type;
    u64 durationNs;
    PackedErrorOr result;

    union {
        OpenEventData open;
        CloseEventData close;
        ReadvEventData readv;
        ReadEventData read;
        PreadEventData pread;
    } data;
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
        FilesystemEvent filesystem;
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

    ErrorOr<void> append(int type, FlatPtr arg1, FlatPtr arg2, StringView arg3, Thread* current_thread = Thread::current(), FilesystemEvent filesystem_event = {});
    ErrorOr<void> append_with_ip_and_bp(ProcessID pid, ThreadID tid, FlatPtr eip, FlatPtr ebp,
        int type, u32 lost_samples, FlatPtr arg1, FlatPtr arg2, StringView arg3, FilesystemEvent filesystem_event = {});
    ErrorOr<void> append_with_ip_and_bp(ProcessID pid, ThreadID tid, RegisterState const& regs,
        int type, u32 lost_samples, FlatPtr arg1, FlatPtr arg2, StringView arg3, FilesystemEvent filesystem_event = {});

    void clear()
    {
        m_count = 0;
    }

    size_t capacity() const { return m_buffer->size() / sizeof(PerformanceEvent); }
    size_t count() const { return m_count; }
    PerformanceEvent const& at(size_t index) const
    {
        return const_cast<PerformanceEventBuffer&>(*this).at(index);
    }

    ErrorOr<void> to_json(KBufferBuilder&) const;

    ErrorOr<void> add_process(Process const&, ProcessEventType event_type);

    ErrorOr<FlatPtr> register_string(NonnullOwnPtr<KString>);

private:
    explicit PerformanceEventBuffer(NonnullOwnPtr<KBuffer>);

    template<typename Serializer>
    ErrorOr<void> to_json_impl(Serializer&) const;

    PerformanceEvent& at(size_t index);

    size_t m_count { 0 };
    NonnullOwnPtr<KBuffer> m_buffer;

    SpinlockProtected<HashMap<NonnullOwnPtr<KString>, size_t>, LockRank::None> m_strings;
};

extern bool g_profiling_all_threads;
extern PerformanceEventBuffer* g_global_perf_events;
extern u64 g_profiling_event_mask;

}
