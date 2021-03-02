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

    OwnPtr<KBuffer> to_json(ProcessID, const String& executable_path) const;
    bool to_json(KBufferBuilder&, ProcessID, const String& executable_path) const;

    // Used by full-system profile (/proc/profile)
    bool to_json(KBufferBuilder&);

private:
    explicit PerformanceEventBuffer(NonnullOwnPtr<KBuffer>);

    template<typename Serializer>
    bool to_json_impl(Serializer&) const;

    PerformanceEvent& at(size_t index);

    size_t m_count { 0 };
    NonnullOwnPtr<KBuffer> m_buffer;
};

}
