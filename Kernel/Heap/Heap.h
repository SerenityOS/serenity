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
#include <Kernel/Security/AddressSanitizer.h>

namespace Kernel {

enum class CallerWillInitializeMemory {
    No,
    Yes,
};

template<size_t CHUNK_SIZE, unsigned HEAP_SCRUB_BYTE_ALLOC = 0, unsigned HEAP_SCRUB_BYTE_FREE = 0>
class Heap {
    AK_MAKE_NONCOPYABLE(Heap);

    struct AllocationHeader {
        size_t allocation_size_in_chunks;
#if ARCH(X86_64) || ARCH(AARCH64)
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
    ALWAYS_INLINE AllocationHeader const* allocation_header(void const* ptr) const
    {
        return (AllocationHeader const*)((((u8 const*)ptr) - sizeof(AllocationHeader)));
    }

    static size_t calculate_chunks(size_t memory_size)
    {
        return (sizeof(u8) * memory_size) / (sizeof(u8) * CHUNK_SIZE + 1);
    }

public:
    static constexpr size_t AllocationHeaderSize = sizeof(AllocationHeader);

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

    void* allocate(size_t size, size_t alignment, [[maybe_unused]] CallerWillInitializeMemory caller_will_initialize_memory)
    {
        // The minimum possible alignment is CHUNK_SIZE, since we only track chunks here, nothing smaller.
        if (alignment < CHUNK_SIZE)
            alignment = CHUNK_SIZE;

        // We need space for the AllocationHeader at the head of the block.
        size_t real_size = size + sizeof(AllocationHeader);
        size_t chunks_needed = (real_size + CHUNK_SIZE - 1) / CHUNK_SIZE;
        size_t chunk_alignment = (alignment + CHUNK_SIZE - 1) / CHUNK_SIZE;

        if (chunks_needed > free_chunks())
            return nullptr;

        Optional<size_t> first_chunk;

        // Choose the right policy for allocation.
        // FIXME: These should utilize the alignment directly instead of trying to allocate `size + alignment`.
        constexpr u32 best_fit_threshold = 128;
        if (chunks_needed < best_fit_threshold) {
            first_chunk = m_bitmap.find_first_fit(chunks_needed + chunk_alignment);
        } else {
            first_chunk = m_bitmap.find_best_fit(chunks_needed + chunk_alignment);
        }

        if (!first_chunk.has_value())
            return nullptr;

        auto* a = (AllocationHeader*)(m_chunks + (first_chunk.value() * CHUNK_SIZE));

        // Align the starting address and verify that we haven't gone outside the calculated free area.
        a = (AllocationHeader*)((FlatPtr)a + alignment - (FlatPtr)a->data % alignment);
        auto aligned_first_chunk = ((FlatPtr)a - (FlatPtr)m_chunks) / CHUNK_SIZE;
        VERIFY(first_chunk.value() <= aligned_first_chunk);
        VERIFY(aligned_first_chunk + chunks_needed <= first_chunk.value() + chunks_needed + chunk_alignment);

#ifdef HAS_ADDRESS_SANITIZER
        AddressSanitizer::mark_region((FlatPtr)a, real_size, (chunks_needed * CHUNK_SIZE), AddressSanitizer::ShadowType::Malloc);
#endif

        u8* ptr = a->data;
        a->allocation_size_in_chunks = chunks_needed;

        m_bitmap.set_range_and_verify_that_all_bits_flip(aligned_first_chunk, chunks_needed, true);

        m_allocated_chunks += chunks_needed;
#ifndef HAS_ADDRESS_SANITIZER
        if (caller_will_initialize_memory == CallerWillInitializeMemory::No) {
            if constexpr (HEAP_SCRUB_BYTE_ALLOC != 0) {
                __builtin_memset(ptr, HEAP_SCRUB_BYTE_ALLOC, (chunks_needed * CHUNK_SIZE) - sizeof(AllocationHeader));
            }
        }
#endif

        VERIFY((FlatPtr)ptr % alignment == 0);
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

#ifdef HAS_ADDRESS_SANITIZER
        AddressSanitizer::fill_shadow((FlatPtr)a, a->allocation_size_in_chunks * CHUNK_SIZE, AddressSanitizer::ShadowType::Free);
#else
        if constexpr (HEAP_SCRUB_BYTE_FREE != 0) {
            __builtin_memset(a, HEAP_SCRUB_BYTE_FREE, a->allocation_size_in_chunks * CHUNK_SIZE);
        }
#endif
    }

    bool contains(void const* ptr) const
    {
        auto const* a = allocation_header(ptr);
        if ((u8 const*)a < m_chunks)
            return false;
        if ((u8 const*)ptr >= m_chunks + m_total_chunks * CHUNK_SIZE)
            return false;
        return true;
    }

    u8* memory() const { return m_chunks; }

    size_t total_chunks() const { return m_total_chunks; }
    size_t total_bytes() const { return m_total_chunks * CHUNK_SIZE; }
    size_t free_chunks() const { return m_total_chunks - m_allocated_chunks; }
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
