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

struct [[gnu::packed]] PerformanceEvent {
    u8 type { 0 };
    u8 stack_size { 0 };
    u32 tid { 0 };
    u64 timestamp;
    union {
        MallocPerformanceEvent malloc;
        FreePerformanceEvent free;
    } data;
    static constexpr size_t max_stack_frame_count = 32;
    FlatPtr stack[max_stack_frame_count];
};

class PerformanceEventBuffer {
public:
    static OwnPtr<PerformanceEventBuffer> try_create_with_size(size_t buffer_size);

    KResult append(int type, FlatPtr arg1, FlatPtr arg2);
    KResult append_with_eip_and_ebp(u32 eip, u32 ebp, int type, FlatPtr arg1, FlatPtr arg2);

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

    void add_process(const Process&);

private:
    explicit PerformanceEventBuffer(NonnullOwnPtr<KBuffer>);

    struct SampledProcess {
        ProcessID pid;
        String executable;
        HashTable<ThreadID> threads;

        struct Region {
            String name;
            Range range;
        };
        Vector<Region> regions;
    };

    template<typename Serializer>
    bool to_json_impl(Serializer&) const;

    PerformanceEvent& at(size_t index);

    size_t m_count { 0 };
    NonnullOwnPtr<KBuffer> m_buffer;

    HashMap<ProcessID, NonnullOwnPtr<SampledProcess>> m_processes;
};

}
