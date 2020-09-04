/*
 * Copyright (c) 2020, The SerenityOS developers.
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

#include <AK/Bitmap.h>
#include <AK/InlineLinkedList.h>
#include <AK/ScopeGuard.h>
#include <AK/TemporaryChange.h>
#include <AK/Vector.h>
#include <AK/kmalloc.h>

//#define BUDDY_DEBUG
#define EXPAND_DEBUG

//#define ENABLE_BUDDY_ASSERTIONS

namespace Kernel {

template<size_t CHUNK_SIZE, unsigned HEAP_SCRUB_BYTE_ALLOC = 0, unsigned HEAP_SCRUB_BYTE_FREE = 0, unsigned ALLOCATION_FLAGS_BITS = 0>
class Heap {
    AK_MAKE_NONCOPYABLE(Heap);

protected:
    template<typename T>
    constexpr static bool is_power_of_two(T value)
    {
        return value && ((value & (value - 1)) == 0);
    }
    template<typename T>
    constexpr static T set_lsb(T value, size_t count)
    {
        return count > 0 ? ((set_lsb(value, count - 1) << 1) | 1) : value;
    }
    template<typename T>
    constexpr static size_t number_of_set_lsb(T value)
    {
        static_assert(is_power_of_two(value + 1));
        return value != 0 ? number_of_set_lsb(value >> 1) + 1 : 0;
    }

    typedef size_t AllocationSizeAndFlags;

    constexpr static size_t allocation_flags_shift()
    {
        return sizeof(AllocationSizeAndFlags) * 8 - ALLOCATION_FLAGS_BITS;
    }
    constexpr static size_t allocation_flags_mask()
    {
        // Creates a bitmask of the upper ALLOCATION_FLAGS_BITS bits for AllocationHeader::flags_and_allocation_size_in_chunks
        return set_lsb(0, ALLOCATION_FLAGS_BITS) << allocation_flags_shift();
    }

    static_assert(is_power_of_two(CHUNK_SIZE));
    static_assert(ALLOCATION_FLAGS_BITS < sizeof(AllocationSizeAndFlags) * 8);

    struct AllocationHeader {
        AllocationSizeAndFlags flags_and_allocation_size_in_chunks;
        u8 data[0];

        void set_allocation_size(size_t chunks)
        {
            if constexpr(ALLOCATION_FLAGS_BITS > 0) {
                // Make sure the allocation isn't too big
                ASSERT(!(chunks & allocation_flags_mask()));
            }
            flags_and_allocation_size_in_chunks = chunks;
        }
        size_t get_allocation_size() const
        {
            size_t chunks = flags_and_allocation_size_in_chunks;
            if constexpr(ALLOCATION_FLAGS_BITS > 0)
                chunks &= ~allocation_flags_mask();
            return chunks;
        }
        void set_allocation_flag_bits(size_t flags)
        {
            if constexpr(ALLOCATION_FLAGS_BITS > 0)
                flags_and_allocation_size_in_chunks |= (flags << allocation_flags_shift()) & allocation_flags_mask();
        }
        void set_allocation_flags(size_t flags)
        {
            if constexpr(ALLOCATION_FLAGS_BITS > 0)
                flags_and_allocation_size_in_chunks = (flags_and_allocation_size_in_chunks & ~allocation_flags_mask()) | (flags << allocation_flags_shift());
        }
        void clear_allocation_flags(size_t flags)
        {
            if constexpr(ALLOCATION_FLAGS_BITS > 0)
                flags_and_allocation_size_in_chunks &= ~(flags << allocation_flags_shift());
        }
        size_t get_allocation_flags() const
        {
            if constexpr(ALLOCATION_FLAGS_BITS > 0)
                return flags_and_allocation_size_in_chunks >> allocation_flags_shift();
            return 0;
        }
    };

    static size_t calculate_chunks(size_t memory_size)
    {
        return (sizeof(u8) * memory_size) / (sizeof(u8) * CHUNK_SIZE + 1);
    }

    static u8* scrub_memory(u8* memory, size_t memory_size)
    {
        return memory;
    }

public:
    Heap(u8* memory, size_t memory_size)
        : m_total_chunks(calculate_chunks(memory_size))
        , m_chunks(memory)
        , m_bitmap(Bitmap::wrap(memory + m_total_chunks * CHUNK_SIZE, m_total_chunks))
    {
        // To keep the alignment of the memory passed in, place the bitmap
        // at the end of the memory block.
        ASSERT(m_total_chunks * CHUNK_SIZE + (m_total_chunks + 7) / 8 <= memory_size);
        if constexpr (HEAP_SCRUB_BYTE_FREE != 0)
            __builtin_memset(memory, HEAP_SCRUB_BYTE_FREE, m_total_chunks * CHUNK_SIZE);
    }
    ~Heap()
    {
    }

    static size_t calculate_memory_for_bytes(size_t bytes)
    {
        size_t needed_chunks = (sizeof(AllocationHeader) + bytes + CHUNK_SIZE - 1) / CHUNK_SIZE;
        return needed_chunks * CHUNK_SIZE + (needed_chunks + 7) / 8;
    }

    void* allocate(size_t size)
    {
        ASSERT(size != 0);
        // We need space for the AllocationHeader at the head of the block.
        size_t real_size = size + sizeof(AllocationHeader);
        size_t chunks_needed = (real_size + CHUNK_SIZE - 1) / CHUNK_SIZE;

        if (chunks_needed > free_chunks())
            return nullptr;

        Optional<size_t> first_chunk;

        // Choose the right politic for allocation.
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
        a->set_allocation_size(chunks_needed);

        m_bitmap.set_range(first_chunk.value(), chunks_needed, true);

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
        auto* a = (AllocationHeader*)((((u8*)ptr) - sizeof(AllocationHeader)));
        ASSERT((u8*)a >= m_chunks && (u8*)ptr < m_chunks + m_total_chunks * CHUNK_SIZE);
        size_t allocated_chunks = a->get_allocation_size();
        ASSERT((u8*)a + allocated_chunks * CHUNK_SIZE <= m_chunks + m_total_chunks * CHUNK_SIZE);
        FlatPtr start = ((FlatPtr)a - (FlatPtr)m_chunks) / CHUNK_SIZE;

        m_bitmap.set_range(start, allocated_chunks, false);

        ASSERT(m_allocated_chunks >= allocated_chunks);
        m_allocated_chunks -= allocated_chunks;

        if constexpr (HEAP_SCRUB_BYTE_FREE != 0) {
            __builtin_memset(a, HEAP_SCRUB_BYTE_FREE, allocated_chunks * CHUNK_SIZE);
        }
    }

    template<typename MainHeap>
    void* reallocate(void* ptr, size_t new_size, MainHeap& h)
    {
        if (!ptr)
            return h.allocate(new_size);

        auto* a = (AllocationHeader*)((((u8*)ptr) - sizeof(AllocationHeader)));
        ASSERT((u8*)a >= m_chunks && (u8*)ptr < m_chunks + m_total_chunks * CHUNK_SIZE);
        size_t allocated_chunks = a->get_allocation_size();
        ASSERT((u8*)a + allocated_chunks * CHUNK_SIZE <= m_chunks + m_total_chunks * CHUNK_SIZE);

        size_t old_size = allocated_chunks * CHUNK_SIZE;

        if (old_size == new_size)
            return ptr;

        auto* new_ptr = h.allocate(new_size);
        if (new_ptr)
            __builtin_memcpy(new_ptr, ptr, min(old_size, new_size));
        h.deallocate(ptr);
        return new_ptr;
    }

    void* reallocate(void* ptr, size_t new_size)
    {
        return reallocate(ptr, new_size, *this);
    }

    bool contains(const void* ptr) const
    {
        const auto* a = (const AllocationHeader*)((((const u8*)ptr) - sizeof(AllocationHeader)));
        if ((const u8*)a < m_chunks)
            return false;
        if ((const u8*)ptr >= m_chunks + m_total_chunks * CHUNK_SIZE)
            return false;
        return true;
    }

    u8* memory() const { return m_chunks; }

    constexpr static size_t chunk_size() { return CHUNK_SIZE; }
    size_t total_chunks() const { return m_total_chunks; }
    size_t total_bytes() const { return m_total_chunks * CHUNK_SIZE; }
    size_t free_chunks() const { return m_total_chunks - m_allocated_chunks; };
    size_t free_bytes() const { return free_chunks() * CHUNK_SIZE; }
    size_t allocated_chunks() const { return m_allocated_chunks; }
    size_t allocated_bytes() const { return m_allocated_chunks * CHUNK_SIZE; }

protected:
    size_t m_total_chunks { 0 };
    size_t m_allocated_chunks { 0 };
    u8* m_chunks { nullptr };
    Bitmap m_bitmap;
};

#ifdef ENABLE_BUDDY_ASSERTIONS
#define BUDDY_ASSERT ASSERT
#else
#define BUDDY_ASSERT(c)
#endif

template<typename T>
concept IsBuddyHeap = requires(T h) { h.buddy_chunks(); };

template<size_t CHUNK_SIZE, size_t MAX_BUDDIES_PER_CHUNK, unsigned HEAP_SCRUB_BYTE_ALLOC = 0, unsigned HEAP_SCRUB_BYTE_FREE = 0>
class HeapWithBuddies: public Heap<CHUNK_SIZE, 0, 0, MAX_BUDDIES_PER_CHUNK * 2> {
    // We handle HEAP_SCRUB_BYTE_ALLOC and HEAP_SCRUB_BYTE_FREE in this class
    // instead, otherwise we'd be scrubbing areas twice when using buddies
    typedef Heap<CHUNK_SIZE, 0, 0, MAX_BUDDIES_PER_CHUNK * 2> Base;

    typedef InlineLinkedListNode<void> AvailableBuddy;

    struct AvailableChunk {
        AvailableChunk* next;
    };

    static_assert(MAX_BUDDIES_PER_CHUNK > 1);
    static_assert(Base::is_power_of_two(MAX_BUDDIES_PER_CHUNK));
    static_assert(sizeof(AvailableBuddy) <= CHUNK_SIZE / MAX_BUDDIES_PER_CHUNK);

    constexpr static size_t smallest_buddy_size()
    {
        return CHUNK_SIZE / MAX_BUDDIES_PER_CHUNK;
    }

    constexpr static size_t biggest_buddy_size()
    {
        return CHUNK_SIZE / 2;
    }

    constexpr static size_t buddy_sizes() {
        size_t n = 1;
        size_t count = 0;
        while (n < MAX_BUDDIES_PER_CHUNK) {
            n *= 2;
            count++;
        }
        return count;
    }
    static_assert(buddy_sizes() >= 1);

    static bool is_buddy_size(size_t size)
    {
        return size <= biggest_buddy_size();
    }

    template<size_t N>
    struct BuddySizes {
        // Creates a compile time array of all the buddy sizes, smallest to biggest
        constexpr BuddySizes() {
            size_t size = biggest_buddy_size();
            for (size_t i = N; i > 0; i--) {
                bytes[i - 1] = size;
                size /= 2;
            }
        }
        size_t bytes[N];
    };

    InlineLinkedList<AvailableBuddy> m_available_buddies[buddy_sizes()];
    constexpr static BuddySizes<buddy_sizes()> s_buddy_sizes = BuddySizes<buddy_sizes()>();
    static_assert(s_buddy_sizes.bytes[0] == smallest_buddy_size());
    AvailableChunk *m_available_chunks { nullptr };
    size_t m_available_chunk_count { 0 };
    size_t m_buddy_chunk_count { 0 };
    size_t m_buddy_allocation_count { 0 };
    size_t m_buddy_allocated_bytes { 0 };

    constexpr static size_t allocated_buddies_mask()
    {
        return Base::set_lsb(0, MAX_BUDDIES_PER_CHUNK) << MAX_BUDDIES_PER_CHUNK;
    }
    constexpr static size_t available_buddies_mask()
    {
        return Base::set_lsb(0, MAX_BUDDIES_PER_CHUNK);
    }

    static bool is_any_buddy_allocated(size_t flags)
    {
        return ((flags & allocated_buddies_mask()) >> MAX_BUDDIES_PER_CHUNK) != 0;
    }
    static bool is_buddy_allocated(size_t flags, size_t buddy_index)
    {
        return (flags & allocated_buddies_mask()) & (1 << (MAX_BUDDIES_PER_CHUNK + buddy_index));
    }
    static size_t buddy_allocated_flag(size_t buddy_index)
    {
        size_t flag = (1 << (MAX_BUDDIES_PER_CHUNK + buddy_index));
        BUDDY_ASSERT((flag & ~allocated_buddies_mask()) == 0);
        return flag;
    }
    static void set_buddy_allocated(size_t& flags, size_t buddy_index)
    {
        flags |= buddy_allocated_flag(buddy_index);
    }
    static void clear_buddy_allocated(size_t& flags, size_t buddy_index)
    {
        flags &= ~buddy_allocated_flag(buddy_index);
    }

    static bool is_any_buddy_available(size_t flags)
    {
        return (flags & available_buddies_mask()) != 0;
    }
    static bool is_buddy_available(size_t flags, size_t buddy_index)
    {
        return (flags & available_buddies_mask()) & (1 << (buddy_index));
    }
    static size_t buddy_available_flag(size_t buddy_index)
    {
        size_t flag = (1 << (buddy_index));
        BUDDY_ASSERT((flag & ~available_buddies_mask()) == 0);
        return flag;
    }
    static void set_buddy_available(size_t& flags, size_t buddy_index)
    {
        flags |= buddy_available_flag(buddy_index);
    }
    static void clear_buddy_available(size_t& flags, size_t buddy_index)
    {
        flags &= ~buddy_available_flag(buddy_index);
    }

    static bool is_buddy_available_or_allocated(size_t flags, size_t buddy_index)
    {
        return (flags & ((1 << (MAX_BUDDIES_PER_CHUNK + buddy_index)) | (1 << (buddy_index)))) != 0;
    }

    Base::AllocationHeader* allocation_header_for_chunk(u8* ptr)
    {
        BUDDY_ASSERT(Base::contains(ptr));
        BUDDY_ASSERT(((ptr - Base::m_chunks) % CHUNK_SIZE) == sizeof(typename Base::AllocationHeader));
        return reinterpret_cast<Base::AllocationHeader*>(ptr - sizeof(typename Base::AllocationHeader));
    }

    Base::AllocationHeader* allocation_header_for_buddy_chunk(u8* ptr, size_t& buddy_index)
    {
        // This function does *not* check if the allocation header
        // is actually valid! The assumption is that we always have one
        // because we only ever allocate chunks of size CHUNK_SIZE for
        // buddies, so there should always be a AllocationHeader preceeding
        BUDDY_ASSERT(Base::contains(ptr));
        size_t chunk_offset = (ptr - Base::m_chunks) % CHUNK_SIZE;
        BUDDY_ASSERT(chunk_offset >= sizeof(typename Base::AllocationHeader));
        buddy_index = (chunk_offset - sizeof(typename Base::AllocationHeader)) / smallest_buddy_size();
        return reinterpret_cast<Base::AllocationHeader*>(ptr - chunk_offset);
    }

    template<typename F>
    IterationDecision traverse_buddy(size_t flags, u8* ptr, size_t buddy_size, size_t first_buddy_index, F f)
    {
        BUDDY_ASSERT(buddy_size >= smallest_buddy_size());
        bool is_buddy_split = false;
        if (buddy_size > smallest_buddy_size()) {
            for (size_t index = 1; index < buddy_size / smallest_buddy_size(); index++) {
                if (is_buddy_available_or_allocated(flags, first_buddy_index + index)) {
                    is_buddy_split = true;
                    break;
                }
            }
        }
        if (is_buddy_split) {
            buddy_size /= 2;
            BUDDY_ASSERT(buddy_size >= smallest_buddy_size());
            traverse_buddy(flags, ptr, buddy_size, first_buddy_index, f);
            traverse_buddy(flags, ptr + buddy_size, buddy_size, first_buddy_index + buddy_size / smallest_buddy_size(), f);
            return IterationDecision::Continue;
        }
        return f(ptr, first_buddy_index, buddy_size);
    }

    template<typename F>
    IterationDecision for_each_buddy_in_chunk(Base::AllocationHeader* header, size_t flags, F f)
    {
        // NOTE: we intentionally keep a copy of the flags so they can be modified without affecting traversing buddies
        u8* buddy = &header->data[0];
        IterationDecision decision = traverse_buddy(flags, buddy, biggest_buddy_size(), 0, f);
        if (decision == IterationDecision::Continue)
            decision = traverse_buddy(flags, buddy + biggest_buddy_size(), biggest_buddy_size(), MAX_BUDDIES_PER_CHUNK / 2, f);
        return decision;
    }

    size_t get_chunk_total_allocated_bytes(Base::AllocationHeader* header, size_t flags)
    {
        size_t total_bytes = 0;
        for_each_buddy_in_chunk(header, flags, [&](void*, size_t, size_t buddy_size) {
            total_bytes += buddy_size;
            return IterationDecision::Continue;
        });
        return total_bytes;
    }

    static size_t get_buddy_size(size_t flags, size_t buddy_index)
    {
        size_t size = smallest_buddy_size();
        ASSERT(is_buddy_available_or_allocated(flags, buddy_index));
        for (size_t index = buddy_index + 1; index < MAX_BUDDIES_PER_CHUNK; index++) {
            if (is_buddy_available_or_allocated(flags, index))
                break;
            size += smallest_buddy_size();
        }
        return size;
    }


#ifdef BUDDY_DEBUG
    void dump_chunk_buddies(Base::AllocationHeader* header, size_t flags)
    {
        klog() << "Chunk " << &header->data[0] << " buddies:";
        auto is_available_buddy_in_list = [&](AvailableBuddy* buddy, size_t buddy_size) -> bool {
            for (size_t i = 0; i < buddy_sizes(); i++) {
                if (s_buddy_sizes.bytes[i] == buddy_size)
                    return m_available_buddies[i].contains_slow(buddy);
            }
            ASSERT_NOT_REACHED();
            return false;
        };
        size_t total_buddy_size = 0;
        for_each_buddy_in_chunk(header, flags, [&](void* ptr, size_t buddy_index, size_t buddy_size) {
            bool allocated = is_buddy_allocated(flags, buddy_index);
            bool available = is_buddy_available(flags, buddy_index);
            klog() << "  [" << buddy_index << "] at " << ptr << " size: " << buddy_size << " allocated: " << allocated << " available: " << available;
            if (!allocated && !available) {
                klog() << "  -> Buddy " << ptr << " neither allocated nor available";
            } else if (available && !is_available_buddy_in_list((AvailableBuddy*)ptr, buddy_size)) {
                klog() << "  -> Buddy " << ptr << " is missing in available list!";
                ASSERT_NOT_REACHED();
            }
            total_buddy_size += buddy_size;
            return IterationDecision::Continue;
        });
        BUDDY_ASSERT(total_buddy_size == CHUNK_SIZE);
    }
#endif

    void remove_all_available_links(Base::AllocationHeader* header, size_t& update_flags)
    {
        size_t flags = update_flags; // capture flags
        for_each_buddy_in_chunk(header, flags, [&](void* ptr, size_t buddy_index, size_t buddy_size) {
            if (is_buddy_available(flags, buddy_index)) {
#ifdef BUDDY_DEBUG
                klog() << "remove available link: buddy[" << buddy_index << "] at " << ptr << " with size " << buddy_size;
#endif
                BUDDY_ASSERT(!is_buddy_allocated(flags, buddy_index));
                for (size_t i = 0; i < buddy_sizes(); i++) {
                    if (s_buddy_sizes.bytes[i] == buddy_size) {
                        auto* buddy = (AvailableBuddy*)ptr;
                        BUDDY_ASSERT(m_available_buddies[i].contains_slow(buddy));
                        m_available_buddies[i].remove(buddy);
                        buddy->~AvailableBuddy();
                        if constexpr (HEAP_SCRUB_BYTE_FREE != 0)
                            __builtin_memset((void*)buddy, HEAP_SCRUB_BYTE_FREE, sizeof(AvailableBuddy));
                        clear_buddy_available(update_flags, buddy_index);
                        return IterationDecision::Continue;
                    }
                }
                ASSERT_NOT_REACHED();
            }
            return IterationDecision::Continue;
        });
    }

    void add_all_available_links(Base::AllocationHeader* header, size_t& update_flags)
    {
        size_t flags = update_flags; // capture flags
        for_each_buddy_in_chunk(header, update_flags, [&](void* ptr, size_t buddy_index, size_t buddy_size) {
            if (!is_buddy_allocated(flags, buddy_index)) {
                // We shouldn't already have created the available link
#ifdef BUDDY_DEBUG
                klog() << "add available link: buddy[" << buddy_index << "] at " << ptr << " with size " << buddy_size;
#endif
                BUDDY_ASSERT(!is_buddy_available(flags, buddy_index));
                for (size_t i = 0; i < buddy_sizes(); i++) {
                    if (s_buddy_sizes.bytes[i] == buddy_size) {
                        auto* buddy = (AvailableBuddy*)ptr;
                        new (buddy) AvailableBuddy();
                        m_available_buddies[i].prepend(buddy);
                        set_buddy_available(update_flags, buddy_index);
                        return IterationDecision::Continue;
                    }
                }
                ASSERT_NOT_REACHED();
            }
            return IterationDecision::Continue;
        });
    }

    Base::AllocationHeader* get_allocation_header(void* ptr)
    {
        // Similar as allocation_header_for_buddy_chunk, but gracefully
        // dismissing invalid pointers
        if (!Base::contains(ptr))
            return nullptr;
        size_t chunk_offset = ((u8*)ptr - Base::m_chunks) % CHUNK_SIZE;
        if (chunk_offset < sizeof(typename Base::AllocationHeader))
            return nullptr;
        return reinterpret_cast<Base::AllocationHeader*>((u8*)ptr - chunk_offset);
    }

    Base::AllocationHeader* is_buddy_allocation(void* ptr, size_t& buddy_index, size_t& flags)
    {
        if (!Base::contains(ptr))
            return nullptr;
        size_t chunk_offset = ((u8*)ptr - Base::m_chunks) % CHUNK_SIZE;
        if (chunk_offset < sizeof(typename Base::AllocationHeader))
            return nullptr;
        auto* header = reinterpret_cast<Base::AllocationHeader*>((u8*)ptr - chunk_offset);
        chunk_offset -= sizeof(typename Base::AllocationHeader);
        if (chunk_offset % smallest_buddy_size() != 0)
            return nullptr;
        buddy_index = chunk_offset / smallest_buddy_size();
        flags = header->get_allocation_flags();
        if (!is_buddy_allocated(flags, buddy_index))
            return nullptr;
        return header;
    }

    void deallocate_buddy(Base::AllocationHeader* header, size_t& flags, size_t buddy_index, size_t buddy_size)
    {
        if constexpr (HEAP_SCRUB_BYTE_FREE != 0) {
            u8* ptr = &header->data[buddy_index * smallest_buddy_size()];
            __builtin_memset(ptr, HEAP_SCRUB_BYTE_FREE, buddy_size);
        }
        clear_buddy_allocated(flags, buddy_index);
    }

    bool deallocate_buddy(void* ptr)
    {
        size_t buddy_index, flags;
        auto* header = is_buddy_allocation(ptr, buddy_index, flags);
        if (!header)
            return false;

        void* chunk = &header->data[0];

#ifdef BUDDY_DEBUG
        klog() << "Deallocating buddy[" << buddy_index << "] " << ptr;
        dump_chunk_buddies(header, flags);
#endif

        size_t buddy_size = get_buddy_size(flags, buddy_index);
        ASSERT(m_buddy_allocated_bytes >= buddy_size);
        m_buddy_allocated_bytes -= buddy_size;
        ASSERT(m_buddy_allocation_count > 0);
        m_buddy_allocation_count--;

        // We remove all available links first. This makes it easier
        // because we don't need as sophisticated logic to merge buddies.
        // Instead we'll just traverse the buddies again afterwards and
        // add the links again.
        remove_all_available_links(header, flags);
        
        deallocate_buddy(header, flags, buddy_index, buddy_size);

        if (is_any_buddy_allocated(flags)) {
            // Some buddies are still in use, add back any available links
            add_all_available_links(header, flags);
            header->set_allocation_flags(flags);
            
#ifdef BUDDY_DEBUG
            klog() << "Deallocated buddy[" << buddy_index << "] " << ptr;
            dump_chunk_buddies(header, flags);
#endif
        } else {
            // All buddies in this chunk are free. Remove all available
            // buddy links and deallocate this chunk

#ifdef BUDDY_DEBUG
            klog() << "All buddies freed and removed, free chunk " << chunk;
            dump_chunk_buddies(header, flags);
#endif
            // Since we cache chunks, we need to clear the allocation flags
            header->set_allocation_flags(0);
            return_chunk_to_cache(chunk);
            ASSERT(m_buddy_chunk_count > 0);
            m_buddy_chunk_count--;
        }
        return true;
    }

    void mark_buddy_allocated(void* ptr, size_t buddy_size)
    {
        size_t buddy_index;
        auto* header = allocation_header_for_buddy_chunk((u8*)ptr, buddy_index);
#ifdef BUDDY_DEBUG
        klog() << "Mark buddy[" << buddy_index << "] " << ptr << " as allocated";
#endif
        header->set_allocation_flag_bits(buddy_allocated_flag(buddy_index));
        if constexpr (HEAP_SCRUB_BYTE_ALLOC != 0)
            __builtin_memset(ptr, HEAP_SCRUB_BYTE_ALLOC, buddy_size);
        m_buddy_allocated_bytes += buddy_size;
        m_buddy_allocation_count++;
    }

    void* make_removed_buddy_unavailable(AvailableBuddy* buddy)
    {
        buddy->~AvailableBuddy();
        if constexpr (HEAP_SCRUB_BYTE_FREE != 0)
            __builtin_memset((void*)buddy, HEAP_SCRUB_BYTE_FREE, sizeof(AvailableBuddy));

        size_t buddy_index;
        auto* header = allocation_header_for_buddy_chunk((u8*)buddy, buddy_index);
        header->clear_allocation_flags(buddy_available_flag(buddy_index)); // clear available flag
        
        return (void*)buddy;
    }

    static size_t find_best_buddy_size(size_t size)
    {
        for (size_t i = 0; i < buddy_sizes(); i++) {
            if (size <= s_buddy_sizes.bytes[i])
                return s_buddy_sizes.bytes[i];
        }
        ASSERT_NOT_REACHED();
        return s_buddy_sizes.bytes[buddy_sizes() - 1];
    }

    void split_add_available_buddy(size_t& flags, u8* chunk, size_t buddy_size, size_t first_buddy_index)
    {
#ifdef BUDDY_DEBUG
        klog() << "Split chunk, add available buddy[" << first_buddy_index << "] at " << chunk;
#endif
        new (chunk) AvailableBuddy();
        AvailableBuddy* buddy = (AvailableBuddy*)chunk;
        for (size_t i = 0; i < buddy_sizes(); i++) {
            if (buddy_size == s_buddy_sizes.bytes[i]) {
                m_available_buddies[i].prepend(buddy);
                set_buddy_available(flags, first_buddy_index);
                return;
            }
        }
        ASSERT_NOT_REACHED();
    }

    void split_chunk_into_buddies(size_t& flags, u8* chunk, size_t chunk_size, size_t requested_size, size_t first_buddy_index, size_t first_available_index)
    {
        size_t buddy_size = chunk_size / 2;
        BUDDY_ASSERT(buddy_size >= smallest_buddy_size());
        size_t buddy_index_right = first_buddy_index + buddy_size / smallest_buddy_size();
        if (requested_size < buddy_size && buddy_size > smallest_buddy_size()) {
            split_chunk_into_buddies(flags, chunk, buddy_size, requested_size, first_buddy_index, first_available_index);
            // Don't split the right hand buddy further
            if (buddy_index_right >= first_available_index)
                split_add_available_buddy(flags, chunk + buddy_size, buddy_size, buddy_index_right);
        } else {
            // Add both buddies
            if (first_buddy_index >= first_available_index)
                split_add_available_buddy(flags, chunk, buddy_size, first_buddy_index);
            if (buddy_index_right >= first_available_index)
                split_add_available_buddy(flags, chunk + buddy_size, buddy_size, buddy_index_right);
        }
    }

    void convert_chunk_to_buddy(Base::AllocationHeader* header, u8* chunk, size_t requested_buddy_size, bool preserve_data)
    {
        // Now split the chunk until we reach the requested buddy size,
        // but only split the least amount of buddies neceesary so that
        // we only end up with at least one (actually two) of the requested size
        size_t flags = header->get_allocation_flags();
        split_chunk_into_buddies(flags, chunk, CHUNK_SIZE, requested_buddy_size, 0, preserve_data ? requested_buddy_size / smallest_buddy_size() : 0);
        header->set_allocation_flags(flags);
    }

    bool create_buddy_chunk(size_t requested_buddy_size)
    {
        // This allocates a full chunk and splits it into available buddies
        u8* chunk = (u8*)get_or_allocate_chunk();
        if (!chunk) {
#ifdef BUDDY_DEBUG
            klog() << "Failed to create buddy chunk for buddy size: " << requested_buddy_size;
#endif
            return false;
        }

#ifdef BUDDY_DEBUG
        klog() << "Created buddy chunk at " << chunk;
#endif
        m_buddy_chunk_count++;
        auto* header = allocation_header_for_chunk(chunk);
        convert_chunk_to_buddy(header, chunk, requested_buddy_size, false);
        return true;
    }

    size_t remove_available_buddy(size_t& flags, u8* ptr, size_t buddy_index)
    {
        BUDDY_ASSERT(is_buddy_available(flags, buddy_index));
        BUDDY_ASSERT(!is_buddy_allocated(flags, buddy_index));
        size_t buddy_size = get_buddy_size(flags, buddy_index);
        for (size_t i = 0; i < buddy_sizes(); i++) {
            if (s_buddy_sizes.bytes[i] == buddy_size) {
                auto* buddy = (AvailableBuddy*)ptr;
                BUDDY_ASSERT(m_available_buddies[i].contains_slow(buddy));
                m_available_buddies[i].remove(buddy);
                buddy->~AvailableBuddy();
                if constexpr (HEAP_SCRUB_BYTE_FREE != 0)
                    __builtin_memset((void*)buddy, HEAP_SCRUB_BYTE_FREE, sizeof(AvailableBuddy));
                clear_buddy_available(flags, buddy_index);
                return buddy_size;
            }
        }
        ASSERT_NOT_REACHED();
        return 0;
    }

    bool grow_buddy(Base::AllocationHeader* header, size_t buddy_index, size_t requested_buddy_size)
    {
        size_t flags = header->get_allocation_flags();
        BUDDY_ASSERT(is_buddy_allocated(flags, buddy_index));
        BUDDY_ASSERT(requested_buddy_size >= smallest_buddy_size());
        BUDDY_ASSERT(requested_buddy_size % smallest_buddy_size() == 0);
        size_t need = (requested_buddy_size / smallest_buddy_size()) - 1;
        size_t have = 0;
        for (size_t index = buddy_index + 1; index < CHUNK_SIZE / smallest_buddy_size() && need > 0; index++) {
            if (is_buddy_allocated(flags, index))
                break;
            have++;
        }
        if (have != need)
            return false;
        u8* ptr = &header->data[buddy_index * smallest_buddy_size()];
        for (size_t index = buddy_index + 1; index < CHUNK_SIZE / smallest_buddy_size() && need > 0; ) {
            ptr += smallest_buddy_size();
            if (!is_buddy_available(flags, index)) {
                index++;
                continue;
            }
            // We need to make this buddy unavailable
#ifdef BUDDY_DEBUG
            klog() << "Growing buddy[" << buddy_index << "] " << &header->data[buddy_index * smallest_buddy_size()] << ", make buddy[" << index << "] unavailable";
#endif
            size_t buddy_size = remove_available_buddy(flags, ptr, index);
            index += buddy_size / smallest_buddy_size();
        }
        return true;
    }

    void* allocate_buddy(size_t size)
    {
        BUDDY_ASSERT(is_buddy_size(size));
        size_t buddy_size = find_best_buddy_size(size);
        do {
try_again:
            for (size_t i = 0; i < buddy_sizes(); i++) {
                if (s_buddy_sizes.bytes[i] >= buddy_size) {
                    if (AvailableBuddy* buddy = m_available_buddies[i].remove_head()) {
                        void* ptr = make_removed_buddy_unavailable(buddy);
                        size_t buddy_index;
                        auto* header = allocation_header_for_buddy_chunk((u8*)ptr, buddy_index);
                        if (s_buddy_sizes.bytes[i] > buddy_size) {
                            // We can split this buddy further
                            size_t flags = header->get_allocation_flags();
#ifdef BUDDY_DEBUG
                            klog() << "Found buddy at " << ptr << " with size " << s_buddy_sizes.bytes[i] << ", split to size " << buddy_size;
#endif
                            split_chunk_into_buddies(flags, (u8*)ptr, s_buddy_sizes.bytes[i], buddy_size, buddy_index, 0);
                            header->set_allocation_flags(flags);
                            goto try_again;
                        }
                        mark_buddy_allocated(ptr, buddy_size);
                        return ptr;
                    }
                }
            }
        } while (create_buddy_chunk(buddy_size));
        // Could not allocate any buddy chunk
        return nullptr;
    }

    void* pull_cached_chunk()
    {
        if (m_available_chunks) {
            auto* available = m_available_chunks;
            m_available_chunks = available->next;
            m_available_chunk_count--;
            if constexpr (HEAP_SCRUB_BYTE_FREE != 0) {
                // Mark the allocated data as free here! That's because at this
                // point no buddies have been allocated
                // The rest of the chunk should already be marked as free
                __builtin_memset(available, HEAP_SCRUB_BYTE_FREE, sizeof(AvailableChunk));
            }
            return (void*)available;
        }
        return nullptr;
    }
    void* get_or_allocate_chunk()
    {
        if (void* ptr = pull_cached_chunk())
            return ptr;
#ifdef BUDDY_DEBUG
        klog() << "Cached chunks are empty, allocate new buddy chunk";
#endif
        void* ptr = Base::allocate(CHUNK_SIZE);
        if (!ptr)
            return nullptr;

        // Mark the allocated data as free here! That's because at this
        // point no buddies have been allocated
        if constexpr (HEAP_SCRUB_BYTE_FREE != 0)
            __builtin_memset(ptr, HEAP_SCRUB_BYTE_FREE, CHUNK_SIZE);
        return ptr;
    }

    void return_chunk_to_cache(void* chunk)
    {
        auto* available = (AvailableChunk*)chunk;
        available->next = m_available_chunks;
        m_available_chunks = available;
        m_available_chunk_count++;

        if constexpr (HEAP_SCRUB_BYTE_FREE != 0)
            __builtin_memset((void*)(available + 1), HEAP_SCRUB_BYTE_FREE, CHUNK_SIZE - sizeof(AvailableChunk));
    }

    bool purge_chunk_cache()
    {
        if (!m_available_chunks)
            return false;
        auto* available = m_available_chunks;
        while (available) {
            auto* next = available->next;
            if constexpr (HEAP_SCRUB_BYTE_FREE != 0) {
                // The rest of the chunk should already be marked as free
                __builtin_memset(available, HEAP_SCRUB_BYTE_FREE, sizeof(AvailableChunk));
            }
            Base::deallocate((void*)available);
            available = next;
        }
        m_available_chunks = nullptr;
        m_available_chunk_count = 0;
        return true;
    }

public:
    HeapWithBuddies(u8* memory, size_t memory_size)
        : Base(memory, memory_size)
    {
        if constexpr (HEAP_SCRUB_BYTE_FREE != 0)
            __builtin_memset(memory, HEAP_SCRUB_BYTE_FREE, Base::m_total_chunks * CHUNK_SIZE);
    }

    void* allocate(size_t size)
    {
        if (is_buddy_size(size)) {
            void* ptr = allocate_buddy(size);
#ifdef BUDDY_DEBUG
            klog() << "Allocated buddy for size " << size << ": " << ptr;
#endif
            return ptr;
        }
        if (size <= CHUNK_SIZE) {
#if 0
{
	klog() << "REALLOC TEST -->";
	void* ptr = allocate_buddy(5);
	klog() << "   ptr = " << ptr;
	void* ptr_new = reallocate(ptr, 11);
	klog() << "   ptr_new = " << ptr_new;
	if (ptr_new)
		deallocate(ptr_new ? ptr_new : ptr);
	klog() << "<-- REALLOC TEST";
}
#endif
            // fast path, steal a cached chunk
            if (void* ptr = pull_cached_chunk())
                return ptr;
        }
        void* ptr = Base::allocate(size);
        if (!ptr && purge_chunk_cache())
            ptr = Base::allocate(size);
        if (!ptr)
            return nullptr;
        if constexpr (HEAP_SCRUB_BYTE_ALLOC != 0) {
            auto* header = allocation_header_for_chunk((u8*)ptr);
            __builtin_memset(&header->data[0], HEAP_SCRUB_BYTE_ALLOC, header->get_allocation_size() * CHUNK_SIZE - sizeof(typename Base::AllocationHeader));
        }
        return ptr;
    }

    void deallocate(void* ptr)
    {
        if (!ptr)
            return;
        if (deallocate_buddy(ptr))
            return;
        if constexpr (HEAP_SCRUB_BYTE_FREE != 0) {
            auto* header = allocation_header_for_chunk((u8*)ptr);
            __builtin_memset(&header->data[0], HEAP_SCRUB_BYTE_FREE, header->get_allocation_size() * CHUNK_SIZE - sizeof(typename Base::AllocationHeader));
        }
        Base::deallocate(ptr);
    }

    template<typename MainHeap>
    void* reallocate(void* ptr, size_t new_size, MainHeap& h)
    {
        if (!ptr)
            return h.allocate(new_size);
klog() << "Reallocate " << ptr;
        size_t buddy_index, flags;
        auto* header = is_buddy_allocation(ptr, buddy_index, flags);
        if (!header) {
            // Not reallocating a buddy
            if (new_size > biggest_buddy_size())
                return Base::reallocate(ptr, new_size, h);

            // We can turn this into a buddy allocation
            header = get_allocation_header(ptr);
            if (!header)
                return nullptr;
            size_t new_buddy_size = find_best_buddy_size(new_size);
#if 1//#ifdef BUDDY_DEBUG
            klog() << "Reallocating chunk at " << ptr << " to buddy chunk with size " << new_buddy_size;
#endif
            convert_chunk_to_buddy(header, &header->data[0], new_buddy_size, true); //TODO: allocation count?
            return ptr;
        }

        size_t old_buddy_size = get_buddy_size(flags, buddy_index);
        if (new_size > biggest_buddy_size()) {
#ifdef BUDDY_DEBUG
            klog() << "Reallocating buddy at " << ptr << " to regular memory with size " << new_size;
#endif
            // Requested more than what a buddy can fit, move the
            // data to a regular allocation
            void *new_ptr = h.allocate(new_size);
            if (!new_ptr)
                return nullptr;

            __builtin_memcpy(new_ptr, ptr, min(old_buddy_size, new_size));
            deallocate_buddy(ptr);
            return new_ptr;
        }

        size_t new_buddy_size = find_best_buddy_size(new_size);
        if (new_buddy_size == old_buddy_size)
            return ptr;
        if (new_buddy_size < old_buddy_size) {
            // TODO: We can split this buddy and make available the other
            return ptr;
        }

        // Check if we can grow this buddy in place
#if 0
        if (grow_buddy(header, buddy_index, new_buddy_size)) {
#ifdef BUDDY_DEBUG
            klog() << "Grew buddy at " << ptr << " with size " << old_buddy_size << " to buddy with size " << new_size;
#endif
            return ptr;
        }
#endif

        // Move the data to a bigger buddy
        void* new_ptr = allocate_buddy(new_size);
        if (!new_ptr) {
            new_ptr = h.allocate(new_size);
            if (!new_ptr)
                return nullptr;
        }
#ifdef BUDDY_DEBUG
        klog() << "Move buddy at " << ptr << " with size " << old_buddy_size << " to new buddy at " << new_ptr << " with size " << new_size;
#endif
        __builtin_memcpy(new_ptr, ptr, min(old_buddy_size, new_size));
        deallocate_buddy(ptr);
        return new_ptr;
    }

    void* reallocate(void* ptr, size_t new_size)
    {
        return reallocate(ptr, new_size, *this);
    }

    size_t allocated_chunks() const { return Base::allocated_chunks() - m_available_chunk_count; }
    size_t allocated_bytes() const { return allocated_chunks() * CHUNK_SIZE; }
    size_t buddy_chunks() const { return m_buddy_chunk_count; }
    size_t buddy_allocated() const { return m_buddy_allocation_count; }
    size_t buddy_total_bytes() const { return m_buddy_chunk_count * CHUNK_SIZE; }
    size_t buddy_allocated_bytes() const { return m_buddy_allocated_bytes; }
};

#undef BUDDY_ASSERT

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

template<typename HeapType, typename ExpandHeap = DefaultExpandHeap>
class ExpandableHeap {
    AK_MAKE_NONCOPYABLE(ExpandableHeap);
    AK_MAKE_NONMOVABLE(ExpandableHeap);

public:
    typedef ExpandHeap ExpandHeapType;
    typedef HeapType SubHeapType;

    struct SubHeap {
        HeapType heap;
        SubHeap* next { nullptr };

        template<typename... Args>
        SubHeap(Args&&... args)
            : heap(forward<Args>(args)...)
        {
        }
    };

    ExpandableHeap(u8* memory, size_t memory_size, const ExpandHeapType& expand = ExpandHeapType())
        : m_heaps(memory, memory_size)
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
#ifdef EXPAND_DEBUG
            klog() << "Cannot allocate " << size << " bytes, request heap expansion";
#endif
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
                    // Since remove_memory may free subheap, we need to save the
                    // next pointer before calling it
                    auto* next_subheap = subheap->next;
#ifdef EXPAND_DEBUG
                    klog() << "Remove unused subheap " << subheap;
#endif
                    if (ExpandableHeapTraits<ExpandHeap>::remove_memory(m_expand, subheap)) {
                        auto* subheap2 = m_heaps.next;
                        auto** subheap_link = &m_heaps.next;
                        while (subheap2 != subheap) {
                            subheap_link = &subheap2->next;
                            subheap2 = subheap2->next;
                        }
                        *subheap_link = next_subheap;
                    }
                }
                return;
            }
        }
        ASSERT_NOT_REACHED();
    }

    void* reallocate(void* ptr, size_t new_size)
    {
        if (!ptr)
            return allocate(new_size);
        for (auto* subheap = &m_heaps; subheap; subheap = subheap->next) {
            if (subheap->heap.contains(ptr))
                return subheap->heap.reallocate(ptr, new_size, *this);
        }
        ASSERT_NOT_REACHED();
    }

    HeapType& add_subheap(void* memory, size_t memory_size)
    {
        ASSERT(memory_size > sizeof(SubHeap));

        // Place the SubHeap structure at the beginning of the new memory block
        memory_size -= sizeof(SubHeap);
        SubHeap* new_heap = (SubHeap*)memory;
        new (new_heap) SubHeap((u8*)(new_heap + 1), memory_size);

#ifdef EXPAND_DEBUG
        klog() << "Adding subheap " << new_heap << " with " << memory_size << " bytes";
#endif

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
    size_t total_bytes() const
    {
        size_t total = 0;
        for (auto* subheap = &m_heaps; subheap; subheap = subheap->next)
            total += subheap->heap.total_bytes();
        return total;
    }
    size_t free_chunks() const
    {
        size_t total = 0;
        for (auto* subheap = &m_heaps; subheap; subheap = subheap->next)
            total += subheap->heap.free_chunks();
        return total;
    }
    size_t free_bytes() const
    {
        size_t total = 0;
        for (auto* subheap = &m_heaps; subheap; subheap = subheap->next)
            total += subheap->heap.free_bytes();
        return total;
    }
    size_t allocated_chunks() const
    {
        size_t total = 0;
        for (auto* subheap = &m_heaps; subheap; subheap = subheap->next)
            total += subheap->heap.allocated_chunks();
        return total;
    }
    size_t allocated_bytes() const
    {
        size_t total = 0;
        for (auto* subheap = &m_heaps; subheap; subheap = subheap->next)
            total += subheap->heap.allocated_bytes();
        return total;
    }

    constexpr static bool is_buddy_heap = requires(HeapType h) { h.buddy_chunks(); };

    size_t buddy_chunks() const
    {
        size_t total = 0;
        if constexpr (is_buddy_heap) {
            for (auto* subheap = &m_heaps; subheap; subheap = subheap->next)
                total += subheap->heap.buddy_chunks();
        }
        return total;
    }
    size_t buddy_total_bytes() const
    {
        size_t total = 0;
        if constexpr (is_buddy_heap) {
            for (auto* subheap = &m_heaps; subheap; subheap = subheap->next)
                total += subheap->heap.buddy_total_bytes();
        }
        return total;
    }
    size_t buddy_allocated() const
    {
        size_t total = 0;
        if constexpr (is_buddy_heap) {
            for (auto* subheap = &m_heaps; subheap; subheap = subheap->next)
                total += subheap->heap.buddy_allocated();
        }
        return total;
    }
    size_t buddy_allocated_bytes() const
    {
        size_t total = 0;
        if constexpr (is_buddy_heap) {
            for (auto* subheap = &m_heaps; subheap; subheap = subheap->next)
                total += subheap->heap.buddy_allocated_bytes();
        }
        return total;
    }

private:
    SubHeap m_heaps;
    ExpandHeap m_expand;
    bool m_expanding { false };
};

}
