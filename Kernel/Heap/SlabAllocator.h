/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/Atomic.h>
#include <AK/Function.h>
#include <AK/Types.h>

namespace Kernel {

#define SLAB_ALLOC_SCRUB_BYTE 0xab
#define SLAB_DEALLOC_SCRUB_BYTE 0xbc

void* slab_alloc(size_t slab_size);
void slab_dealloc(void*, size_t slab_size);
void slab_dealloc(void*);
void slab_alloc_init();
void slab_alloc_stats(Function<void(size_t slab_size, size_t allocated, size_t free)>);

template<size_t templated_slab_size>
class SlabAllocator {
public:
    SlabAllocator() = default;

    void init(void*, size_t);

    constexpr size_t slab_size() const { return templated_slab_size; }
    size_t slab_count() const { return m_slab_count; }

    void* alloc();

    void dealloc(void*);

    size_t num_allocated() const { return m_num_allocated; }
    size_t num_free() const { return m_slab_count - m_num_allocated; }

    void* base() const { return m_base; }
    void* end() const { return m_end; }

private:
    struct FreeSlab {
        FreeSlab* next;
        char padding[templated_slab_size - sizeof(FreeSlab*)];
    };

    Atomic<FreeSlab*> m_freelist { nullptr };
    Atomic<ssize_t, AK::MemoryOrder::memory_order_relaxed> m_num_allocated;
    size_t m_slab_count;
    void* m_base { nullptr };
    void* m_end { nullptr };

    static_assert(sizeof(FreeSlab) == templated_slab_size);
};

extern SlabAllocator<16> s_slab_allocator_16;
extern SlabAllocator<32> s_slab_allocator_32;
extern SlabAllocator<64> s_slab_allocator_64;
extern SlabAllocator<128> s_slab_allocator_128;

#define MAKE_SLAB_ALLOCATED(type)                                        \
public:                                                                  \
    void* operator new(size_t) { return slab_alloc(sizeof(type)); }      \
    void operator delete(void* ptr) { slab_dealloc(ptr, sizeof(type)); } \
                                                                         \
private:

}
