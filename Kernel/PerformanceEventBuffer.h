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

struct [[gnu::packed]] MallocPerformanceEvent
{
    size_t size;
    FlatPtr ptr;
};

struct [[gnu::packed]] FreePerformanceEvent
{
    size_t size;
    FlatPtr ptr;
};

struct [[gnu::packed]] PerformanceEvent
{
    u8 type { 0 };
    u8 stack_size { 0 };
    u64 timestamp;
    union {
        MallocPerformanceEvent malloc;
        FreePerformanceEvent free;
    } data;
    FlatPtr stack[32];
};

class PerformanceEventBuffer {
public:
    PerformanceEventBuffer();

    KResult append(int type, FlatPtr arg1, FlatPtr arg2);

    size_t capacity() const { return m_buffer.size() / sizeof(PerformanceEvent); }
    size_t count() const { return m_count; }
    const PerformanceEvent& at(size_t index) const
    {
        return const_cast<PerformanceEventBuffer&>(*this).at(index);
    }

    KBuffer to_json(ProcessID, const String& executable_path) const;

private:
    PerformanceEvent& at(size_t index);

    size_t m_count { 0 };
    KBuffer m_buffer;
};

}
