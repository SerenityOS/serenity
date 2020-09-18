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

#include <AK/Assertions.h>
#include <AK/Memory.h>
#include <Kernel/Heap/SlabAllocator.h>
#include <Kernel/Heap/kmalloc.h>
#include <Kernel/SpinLock.h>
#include <Kernel/VM/Region.h>

#define SANITIZE_SLABS

namespace Kernel {

template<size_t templated_slab_size>
class SlabAllocator {
public:
    SlabAllocator() { }

    void init(size_t size)
    {
        m_base = kmalloc_eternal(size);
        m_end = (u8*)m_base + size;
        FreeSlab* slabs = (FreeSlab*)m_base;
        m_slab_count = size / templated_slab_size;
        for (size_t i = 1; i < m_slab_count; ++i) {
            slabs[i].next = &slabs[i - 1];
        }
        slabs[0].next = nullptr;
        m_freelist = &slabs[m_slab_count - 1];
        m_num_allocated.store(0, AK::MemoryOrder::memory_order_release);
    }

    constexpr size_t slab_size() const { return templated_slab_size; }
    size_t slab_count() const { return m_slab_count; }

    void* alloc()
    {
        FreeSlab* free_slab;
        {
            // We want to avoid being swapped out in the middle of this
            ScopedCritical critical;
            FreeSlab* next_free;
            free_slab = m_freelist.load(AK::memory_order_consume);
            do {
                if (!free_slab)
                    return kmalloc(slab_size());
                // It's possible another processor is doing the same thing at
                // the same time, so next_free *can* be a bogus pointer. However,
                // in that case compare_exchange_strong would fail and we would
                // try again.
                next_free = free_slab->next;
            } while (!m_freelist.compare_exchange_strong(free_slab, next_free, AK::memory_order_acq_rel));

            m_num_allocated.fetch_add(1, AK::MemoryOrder::memory_order_acq_rel);
        }

#ifdef SANITIZE_SLABS
        memset(free_slab, SLAB_ALLOC_SCRUB_BYTE, slab_size());
#endif
        return free_slab;
    }

    void dealloc(void* ptr)
    {
        ASSERT(ptr);
        if (ptr < m_base || ptr >= m_end) {
            kfree(ptr);
            return;
        }
        FreeSlab* free_slab = (FreeSlab*)ptr;
#ifdef SANITIZE_SLABS
        if (slab_size() > sizeof(FreeSlab*))
            memset(free_slab->padding, SLAB_DEALLOC_SCRUB_BYTE, sizeof(FreeSlab::padding));
#endif

        // We want to avoid being swapped out in the middle of this
        ScopedCritical critical;
        FreeSlab* next_free = m_freelist.load(AK::memory_order_consume);
        do {
            free_slab->next = next_free;
        } while (!m_freelist.compare_exchange_strong(next_free, free_slab, AK::memory_order_acq_rel));

        m_num_allocated.fetch_sub(1, AK::MemoryOrder::memory_order_acq_rel);
    }

    size_t num_allocated() const { return m_num_allocated.load(AK::MemoryOrder::memory_order_consume); }
    size_t num_free() const { return m_slab_count - m_num_allocated.load(AK::MemoryOrder::memory_order_consume); }

private:
    struct FreeSlab {
        FreeSlab* next;
        char padding[templated_slab_size - sizeof(FreeSlab*)];
    };

    Atomic<FreeSlab*> m_freelist { nullptr };
    Atomic<ssize_t> m_num_allocated;
    size_t m_slab_count;
    void* m_base { nullptr };
    void* m_end { nullptr };

    static_assert(sizeof(FreeSlab) == templated_slab_size);
};

static SlabAllocator<16> s_slab_allocator_16;
static SlabAllocator<32> s_slab_allocator_32;
static SlabAllocator<64> s_slab_allocator_64;
static SlabAllocator<128> s_slab_allocator_128;

static_assert(sizeof(Region) <= s_slab_allocator_64.slab_size());

template<typename Callback>
void for_each_allocator(Callback callback)
{
    callback(s_slab_allocator_16);
    callback(s_slab_allocator_32);
    callback(s_slab_allocator_64);
    callback(s_slab_allocator_128);
}

void slab_alloc_init()
{
    s_slab_allocator_16.init(128 * KiB);
    s_slab_allocator_32.init(128 * KiB);
    s_slab_allocator_64.init(512 * KiB);
    s_slab_allocator_128.init(512 * KiB);
}

void* slab_alloc(size_t slab_size)
{
    if (slab_size <= 16)
        return s_slab_allocator_16.alloc();
    if (slab_size <= 32)
        return s_slab_allocator_32.alloc();
    if (slab_size <= 64)
        return s_slab_allocator_64.alloc();
    if (slab_size <= 128)
        return s_slab_allocator_128.alloc();
    ASSERT_NOT_REACHED();
}

void slab_dealloc(void* ptr, size_t slab_size)
{
    if (slab_size <= 16)
        return s_slab_allocator_16.dealloc(ptr);
    if (slab_size <= 32)
        return s_slab_allocator_32.dealloc(ptr);
    if (slab_size <= 64)
        return s_slab_allocator_64.dealloc(ptr);
    if (slab_size <= 128)
        return s_slab_allocator_128.dealloc(ptr);
    ASSERT_NOT_REACHED();
}

void slab_alloc_stats(Function<void(size_t slab_size, size_t allocated, size_t free)> callback)
{
    for_each_allocator([&](auto& allocator) {
        auto num_allocated = allocator.num_allocated();
        auto num_free = allocator.slab_count() - num_allocated;
        callback(allocator.slab_size(), num_allocated, num_free);
    });
}

}
