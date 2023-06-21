/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Bitmap.h>
#include <AK/IntrusiveList.h>

namespace Kernel::Memory {

// A PhysicalZone is an allocator that manages a sub-area of a PhysicalRegion.
// Its total size is always a power of two.
// You allocate chunks at a time. One chunk is PAGE_SIZE/2, and the minimum allocation size is 2 chunks.
// The allocator uses a buddy block scheme internally.

class PhysicalZone {
    AK_MAKE_NONCOPYABLE(PhysicalZone);
    AK_MAKE_NONMOVABLE(PhysicalZone);

public:
    static constexpr size_t ZONE_CHUNK_SIZE = PAGE_SIZE / 2;
    using ChunkIndex = i16;

    PhysicalZone(PhysicalAddress base, size_t page_count);

    Optional<PhysicalAddress> allocate_block(size_t order);
    void deallocate_block(PhysicalAddress, size_t order);

    void dump() const;
    size_t available() const { return m_page_count - (m_used_chunks / 2); }

    bool is_empty() const { return available() == 0; }

    PhysicalAddress base() const { return m_base_address; }
    bool contains(PhysicalAddress paddr) const
    {
        return paddr >= m_base_address && paddr < m_base_address.offset(m_page_count * PAGE_SIZE);
    }

private:
    Optional<ChunkIndex> allocate_block_impl(size_t order);
    void deallocate_block_impl(ChunkIndex, size_t order);

    struct BuddyBucket {
        bool get_buddy_bit(ChunkIndex index) const
        {
            return bitmap.get(buddy_bit_index(index));
        }

        void set_buddy_bit(ChunkIndex index, bool value)
        {
            bitmap.set(buddy_bit_index(index), value);
        }

        size_t buddy_bit_index(ChunkIndex index) const
        {
            // NOTE: We cut the index in half since one chunk is half a page.
            return (index >> 1) >> (1 + order);
        }

        // This bucket's index in the m_buckets array. (Redundant data kept here for convenience.)
        size_t order { 0 };

        // This is the start of the freelist for this buddy size.
        // It's an index into the global PhysicalPageEntry array (offset by this PhysicalRegion's base.)
        // A value of -1 indicates an empty freelist.
        ChunkIndex freelist { -1 };

        // Bitmap with 1 bit per buddy pair.
        // 0 == Both blocks either free or used.
        // 1 == One block free, one block used.
        Bitmap bitmap;
    };

    static constexpr size_t max_order = 12;
    BuddyBucket m_buckets[max_order + 1];

    PhysicalPageEntry& get_freelist_entry(ChunkIndex) const;
    void remove_from_freelist(BuddyBucket&, ChunkIndex);

    PhysicalAddress m_base_address { 0 };
    size_t m_page_count { 0 };
    size_t m_used_chunks { 0 };

    IntrusiveListNode<PhysicalZone> m_list_node;

public:
    using List = IntrusiveList<&PhysicalZone::m_list_node>;
};

}
