/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Bitmap.h>
#include <AK/ScopeGuard.h>
#include <AK/TemporaryChange.h>
#include <AK/Vector.h>
#include <AK/kmalloc.h>

namespace Kernel {

template<size_t CHUNK_SIZE, unsigned HEAP_SCRUB_BYTE_ALLOC = 0, unsigned HEAP_SCRUB_BYTE_FREE = 0>
class Heap {
    AK_MAKE_NONCOPYABLE(Heap);

    struct AllocationHeader {
        size_t allocation_size_in_chunks;
#if ARCH(X86_64)
        // FIXME: Get rid of this somehow
        size_t alignment_dummy;
#endif
        u8 data[0];
    };

    static_assert(CHUNK_SIZE >= sizeof(AllocationHeader));

    ALWAYS_INLINE AllocationHeader* allocation_header(void* ptr)
    {
        return (AllocationHeader*)((((u8*)ptr) - sizeof(AllocationHeader)));
    }
    ALWAYS_INLINE const AllocationHeader* allocation_header(const void* ptr) const
    {
        return (const AllocationHeader*)((((const u8*)ptr) - sizeof(AllocationHeader)));
    }

    static size_t calculate_chunks(size_t memory_size)
    {
        return (sizeof(u8) * memory_size) / (sizeof(u8) * CHUNK_SIZE + 1);
    }

public:
    Heap(u8* memory, size_t memory_size)
        : m_total_chunks(calculate_chunks(memory_size))
        , m_chunks(memory)
        , m_bitmap(memory + m_total_chunks * CHUNK_SIZE, m_total_chunks)
    {
        // To keep the alignment of the memory passed in, place the bitmap
        // at the end of the memory block.
        VERIFY(m_total_chunks * CHUNK_SIZE + (m_total_chunks + 7) / 8 <= memory_size);
    }
    ~Heap() = default;

    static size_t calculate_memory_for_bytes(size_t bytes)
    {
        size_t needed_chunks = (sizeof(AllocationHeader) + bytes + CHUNK_SIZE - 1) / CHUNK_SIZE;
        return needed_chunks * CHUNK_SIZE + (needed_chunks + 7) / 8;
    }

    void* allocate(size_t size)
    {
        // We need space for the AllocationHeader at the head of the block.
        size_t real_size = size + sizeof(AllocationHeader);
        size_t chunks_needed = (real_size + CHUNK_SIZE - 1) / CHUNK_SIZE;

        if (chunks_needed > free_chunks())
            return nullptr;

        Optional<size_t> first_chunk;

        // Choose the right policy for allocation.
        constexpr u32 best_fit_threshold = 128;
        if (chunks_needed < best_fit_threshold) {
            first_chunk = m_bitmap.find_first_fit(chunks_needed);
        } else {
            first_chunk = m_bitmap.find_best_fit(chunks_needed);
        }

        if (!first_chunk.has_value())
            return nullptr;

        auto* a = (AllocationHeader*)(m_chunks + (first_chunk.value() * CHUNK_SIZE));
        u8* ptr = a->data;
        a->allocation_size_in_chunks = chunks_needed;

        m_bitmap.set_range_and_verify_that_all_bits_flip(first_chunk.value(), chunks_needed, true);

        m_allocated_chunks += chunks_needed;
        if constexpr (HEAP_SCRUB_BYTE_ALLOC != 0) {
            __builtin_memset(ptr, HEAP_SCRUB_BYTE_ALLOC, (chunks_needed * CHUNK_SIZE) - sizeof(AllocationHeader));
        }
        return ptr;
    }

    void deallocate(void* ptr)
    {
        if (!ptr)
            return;
        auto* a = allocation_header(ptr);
        VERIFY((u8*)a >= m_chunks && (u8*)ptr < m_chunks + m_total_chunks * CHUNK_SIZE);
        FlatPtr start = ((FlatPtr)a - (FlatPtr)m_chunks) / CHUNK_SIZE;

        // First, verify that the start of the allocation at `ptr` is actually allocated.
        VERIFY(m_bitmap.get(start));

        VERIFY((u8*)a + a->allocation_size_in_chunks * CHUNK_SIZE <= m_chunks + m_total_chunks * CHUNK_SIZE);
        m_bitmap.set_range_and_verify_that_all_bits_flip(start, a->allocation_size_in_chunks, false);

        VERIFY(m_allocated_chunks >= a->allocation_size_in_chunks);
        m_allocated_chunks -= a->allocation_size_in_chunks;

        if constexpr (HEAP_SCRUB_BYTE_FREE != 0) {
            __builtin_memset(a, HEAP_SCRUB_BYTE_FREE, a->allocation_size_in_chunks * CHUNK_SIZE);
        }
    }

    bool contains(const void* ptr) const
    {
        const auto* a = allocation_header(ptr);
        if ((const u8*)a < m_chunks)
            return false;
        if ((const u8*)ptr >= m_chunks + m_total_chunks * CHUNK_SIZE)
            return false;
        return true;
    }

    u8* memory() const { return m_chunks; }

    size_t total_chunks() const { return m_total_chunks; }
    size_t total_bytes() const { return m_total_chunks * CHUNK_SIZE; }
    size_t free_chunks() const { return m_total_chunks - m_allocated_chunks; };
    size_t free_bytes() const { return free_chunks() * CHUNK_SIZE; }
    size_t allocated_chunks() const { return m_allocated_chunks; }
    size_t allocated_bytes() const { return m_allocated_chunks * CHUNK_SIZE; }

private:
    size_t m_total_chunks { 0 };
    size_t m_allocated_chunks { 0 };
    u8* m_chunks { nullptr };
    Bitmap m_bitmap;
};

}
