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
#include <Kernel/Heap/SlabAllocator.h>
#include <Kernel/Heap/kmalloc.h>
#include <Kernel/VM/Region.h>

namespace Kernel {

template<size_t templated_slab_size>
class SlabAllocator {
public:
    SlabAllocator() {}

    void init(size_t size)
    {
        m_base = kmalloc_eternal(size);
        m_end = (u8*)m_base + size;
        FreeSlab* slabs = (FreeSlab*)m_base;
        size_t slab_count = size / templated_slab_size;
        for (size_t i = 1; i < slab_count; ++i) {
            slabs[i].next = &slabs[i - 1];
        }
        slabs[0].next = nullptr;
        m_freelist = &slabs[slab_count - 1];
        m_num_allocated = 0;
        m_num_free = slab_count;
    }

    constexpr size_t slab_size() const { return templated_slab_size; }

    void* alloc()
    {
        InterruptDisabler disabler;
        if (!m_freelist)
            return kmalloc(slab_size());
        ASSERT(m_freelist);
        void* ptr = m_freelist;
        m_freelist = m_freelist->next;
        ++m_num_allocated;
        --m_num_free;
#ifdef SANITIZE_KMALLOC
        memset(ptr, SLAB_ALLOC_SCRUB_BYTE, slab_size());
#endif
        return ptr;
    }

    void dealloc(void* ptr)
    {
        InterruptDisabler disabler;
        ASSERT(ptr);
        if (ptr < m_base || ptr >= m_end) {
            kfree(ptr);
            return;
        }
        ((FreeSlab*)ptr)->next = m_freelist;
#ifdef SANITIZE_KMALLOC
        if (slab_size() > sizeof(FreeSlab*))
            memset(((FreeSlab*)ptr)->padding, SLAB_DEALLOC_SCRUB_BYTE, sizeof(FreeSlab::padding));
#endif
        m_freelist = (FreeSlab*)ptr;
        ++m_num_allocated;
        --m_num_free;
    }

    size_t num_allocated() const { return m_num_allocated; }
    size_t num_free() const { return m_num_free; }

private:
    struct FreeSlab {
        FreeSlab* next { nullptr };
        char padding[templated_slab_size - sizeof(FreeSlab*)];
    };

    // NOTE: These are not default-initialized to prevent an init-time constructor from overwriting them
    FreeSlab* m_freelist;
    size_t m_num_allocated;
    size_t m_num_free;
    void* m_base;
    void* m_end;

    static_assert(sizeof(FreeSlab) == templated_slab_size);
};

static SlabAllocator<8> s_slab_allocator_8;
static SlabAllocator<16> s_slab_allocator_16;
static SlabAllocator<32> s_slab_allocator_32;
static SlabAllocator<48> s_slab_allocator_48;

static_assert(sizeof(Region) <= s_slab_allocator_48.slab_size());

template<typename Callback>
void for_each_allocator(Callback callback)
{
    callback(s_slab_allocator_8);
    callback(s_slab_allocator_16);
    callback(s_slab_allocator_32);
    callback(s_slab_allocator_48);
}

void slab_alloc_init()
{
    s_slab_allocator_8.init(384 * KB);
    s_slab_allocator_16.init(128 * KB);
    s_slab_allocator_32.init(128 * KB);
    s_slab_allocator_48.init(128 * KB);
}

void* slab_alloc(size_t slab_size)
{
    if (slab_size <= 8)
        return s_slab_allocator_8.alloc();
    if (slab_size <= 16)
        return s_slab_allocator_16.alloc();
    if (slab_size <= 32)
        return s_slab_allocator_32.alloc();
    if (slab_size <= 48)
        return s_slab_allocator_48.alloc();
    ASSERT_NOT_REACHED();
}

void slab_dealloc(void* ptr, size_t slab_size)
{
    if (slab_size <= 8)
        return s_slab_allocator_8.dealloc(ptr);
    if (slab_size <= 16)
        return s_slab_allocator_16.dealloc(ptr);
    if (slab_size <= 32)
        return s_slab_allocator_32.dealloc(ptr);
    if (slab_size <= 48)
        return s_slab_allocator_48.dealloc(ptr);
    ASSERT_NOT_REACHED();
}

void slab_alloc_stats(Function<void(size_t slab_size, size_t allocated, size_t free)> callback)
{
    for_each_allocator([&](auto& allocator) {
        callback(allocator.slab_size(), allocator.num_allocated(), allocator.num_free());
    });
}

}
