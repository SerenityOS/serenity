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

template<typename ExpandHeap>
struct ExpandableHeapTraits {
    static bool add_memory(ExpandHeap& expand, size_t allocation_request)
    {
        return expand.add_memory(allocation_request);
    }

    static bool remove_memory(ExpandHeap& expand, void* memory)
    {
        return expand.remove_memory(memory);
    }
};

struct DefaultExpandHeap {
    bool add_memory(size_t)
    {
        // Requires explicit implementation
        return false;
    }

    bool remove_memory(void*)
    {
        return false;
    }
};

template<size_t CHUNK_SIZE, unsigned HEAP_SCRUB_BYTE_ALLOC = 0, unsigned HEAP_SCRUB_BYTE_FREE = 0, typename ExpandHeap = DefaultExpandHeap>
class ExpandableHeap {
    AK_MAKE_NONCOPYABLE(ExpandableHeap);
    AK_MAKE_NONMOVABLE(ExpandableHeap);

public:
    using ExpandHeapType = ExpandHeap;
    using HeapType = Heap<CHUNK_SIZE, HEAP_SCRUB_BYTE_ALLOC, HEAP_SCRUB_BYTE_FREE>;

    struct SubHeap {
        HeapType heap;
        SubHeap* next { nullptr };
        size_t memory_size { 0 };

        template<typename... Args>
        SubHeap(size_t memory_size, Args&&... args)
            : heap(forward<Args>(args)...)
            , memory_size(memory_size)
        {
        }
    };

    ExpandableHeap(u8* memory, size_t memory_size, const ExpandHeapType& expand = ExpandHeapType())
        : m_heaps(memory_size, memory, memory_size)
        , m_expand(expand)
    {
    }
    ~ExpandableHeap()
    {
        // We don't own the main heap, only remove memory that we added previously
        SubHeap* next;
        for (auto* heap = m_heaps.next; heap; heap = next) {
            next = heap->next;

            heap->~SubHeap();
            ExpandableHeapTraits<ExpandHeap>::remove_memory(m_expand, (void*)heap);
        }
    }

    static size_t calculate_memory_for_bytes(size_t bytes)
    {
        return sizeof(SubHeap) + HeapType::calculate_memory_for_bytes(bytes);
    }

    bool expand_memory(size_t size)
    {
        if (m_expanding)
            return false;

        // Allocating more memory itself may trigger allocations and deallocations
        // on this heap. We need to prevent recursive expansion. We also disable
        // removing memory while trying to expand the heap.
        TemporaryChange change(m_expanding, true);
        return ExpandableHeapTraits<ExpandHeap>::add_memory(m_expand, size);
    }

    void* allocate(size_t size)
    {
        int attempt = 0;
        do {
            for (auto* subheap = &m_heaps; subheap; subheap = subheap->next) {
                if (void* ptr = subheap->heap.allocate(size))
                    return ptr;
            }

            // We need to loop because we won't know how much memory was added.
            // Even though we make a best guess how much memory needs to be added,
            // it doesn't guarantee that enough will be available after adding it.
            // This is especially true for the kmalloc heap, where adding memory
            // requires several other objects to be allocated just to be able to
            // expand the heap.

            // To avoid an infinite expansion loop, limit to two attempts
            if (attempt++ >= 2)
                break;
        } while (expand_memory(size));
        return nullptr;
    }

    void deallocate(void* ptr)
    {
        if (!ptr)
            return;
        for (auto* subheap = &m_heaps; subheap; subheap = subheap->next) {
            if (subheap->heap.contains(ptr)) {
                subheap->heap.deallocate(ptr);
                if (subheap->heap.allocated_chunks() == 0 && subheap != &m_heaps && !m_expanding) {
                    // remove_memory expects the memory to be unused and
                    // may deallocate the memory. We need to therefore first
                    // unlink the subheap and destroy it. If remove_memory
                    // ends up not not removing the memory, we'll initialize
                    // a new subheap and re-add it.
                    // We need to remove the subheap before calling remove_memory
                    // because it's possible that remove_memory itself could
                    // cause a memory allocation that we don't want to end up
                    // potentially being made in the subheap we're about to remove.
                    {
                        auto* subheap2 = m_heaps.next;
                        auto** subheap_link = &m_heaps.next;
                        while (subheap2 != subheap) {
                            subheap_link = &subheap2->next;
                            subheap2 = subheap2->next;
                        }
                        *subheap_link = subheap->next;
                    }

                    auto memory_size = subheap->memory_size;
                    subheap->~SubHeap();

                    if (!ExpandableHeapTraits<ExpandHeap>::remove_memory(m_expand, subheap)) {
                        // Removal of the subheap was rejected, add it back in and
                        // re-initialize with a clean subheap.
                        add_subheap(subheap, memory_size);
                    }
                }
                return;
            }
        }
        VERIFY_NOT_REACHED();
    }

    HeapType& add_subheap(void* memory, size_t memory_size)
    {
        VERIFY(memory_size > sizeof(SubHeap));

        // Place the SubHeap structure at the beginning of the new memory block
        memory_size -= sizeof(SubHeap);
        SubHeap* new_heap = (SubHeap*)memory;
        new (new_heap) SubHeap(memory_size, (u8*)(new_heap + 1), memory_size);

        // Add the subheap to the list (but leave the main heap where it is)
        SubHeap* next_heap = m_heaps.next;
        SubHeap** next_heap_link = &m_heaps.next;
        while (next_heap) {
            if (new_heap->heap.memory() < next_heap->heap.memory())
                break;
            next_heap_link = &next_heap->next;
            next_heap = next_heap->next;
        }
        new_heap->next = *next_heap_link;
        *next_heap_link = new_heap;
        return new_heap->heap;
    }

    bool contains(const void* ptr) const
    {
        for (auto* subheap = &m_heaps; subheap; subheap = subheap->next) {
            if (subheap->heap.contains(ptr))
                return true;
        }
        return false;
    }

    size_t total_chunks() const
    {
        size_t total = 0;
        for (auto* subheap = &m_heaps; subheap; subheap = subheap->next)
            total += subheap->heap.total_chunks();
        return total;
    }
    size_t total_bytes() const { return total_chunks() * CHUNK_SIZE; }
    size_t free_chunks() const
    {
        size_t total = 0;
        for (auto* subheap = &m_heaps; subheap; subheap = subheap->next)
            total += subheap->heap.free_chunks();
        return total;
    }
    size_t free_bytes() const { return free_chunks() * CHUNK_SIZE; }
    size_t allocated_chunks() const
    {
        size_t total = 0;
        for (auto* subheap = &m_heaps; subheap; subheap = subheap->next)
            total += subheap->heap.allocated_chunks();
        return total;
    }
    size_t allocated_bytes() const { return allocated_chunks() * CHUNK_SIZE; }

private:
    SubHeap m_heaps;
    ExpandHeap m_expand;
    bool m_expanding { false };
};

}
