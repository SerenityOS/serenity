/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/BuiltinWrappers.h>
#include <AK/Format.h>
#include <Kernel/Memory/MemoryManager.h>
#include <Kernel/Memory/PhysicalRAMPage.h>
#include <Kernel/Memory/PhysicalZone.h>

namespace Kernel::Memory {

PhysicalPageEntry& PhysicalZone::get_freelist_entry(ChunkIndex index) const
{
    return MM.get_physical_page_entry(m_base_address.offset(index * ZONE_CHUNK_SIZE));
}

PhysicalZone::PhysicalZone(PhysicalAddress base_address, size_t page_count)
    : m_base_address(base_address)
    , m_page_count(page_count)
    , m_used_chunks(0)
{
    size_t const chunk_count = page_count * 2;
    for (int order = max_order; order >= 0; --order) {
        auto& bucket = m_buckets[order];
        size_t block_size = 2u << order;
        size_t bitmap_size_for_order = ceil_div((size_t)(chunk_count / block_size), (size_t)2);
        bucket.order = order;
        if (bitmap_size_for_order)
            bucket.bitmap.grow(bitmap_size_for_order, false);
    }

    auto first_order = count_trailing_zeroes(page_count);
    size_t block_size = 2u << first_order;
    auto& bucket = m_buckets[first_order];
    size_t remaining_chunk_count = chunk_count;
    size_t initial_bundle_count = remaining_chunk_count / block_size;

    size_t offset = 0;
    for (size_t i = 0; i < initial_bundle_count; ++i) {
        ChunkIndex index = offset + i;
        bucket.set_buddy_bit(index, true);

        auto& freelist_entry = get_freelist_entry(index).freelist;
        freelist_entry.next_index = bucket.freelist;
        freelist_entry.prev_index = -1;
        bucket.freelist = index;

        remaining_chunk_count -= block_size;
        offset += block_size;
    }
}

Optional<PhysicalAddress> PhysicalZone::allocate_block(size_t order)
{
    size_t block_size = 2u << order;
    auto result = allocate_block_impl(order);
    if (!result.has_value())
        return {};
    m_used_chunks += block_size;
    VERIFY(!(result.value() & 1));
    return m_base_address.offset(result.value() * ZONE_CHUNK_SIZE);
}

Optional<PhysicalZone::ChunkIndex> PhysicalZone::allocate_block_impl(size_t order)
{
    if (order > max_order)
        return {};
    size_t block_size = 2u << order;
    auto& bucket = m_buckets[order];
    if (bucket.freelist == -1) {
        // The freelist for this order is empty, try to allocate a block from one order higher, and split it.
        auto buddies = allocate_block_impl(order + 1);

        if (!buddies.has_value()) {
            // Looks like we're unable to satisfy this allocation request.
            return {};
        }

        // Split the block from order+1 into two parts.
        // We keep one (in the freelist for this order) and return the other.

        ChunkIndex index = buddies.value();

        // First half goes in the freelist
        auto& freelist_entry = get_freelist_entry(index).freelist;
        freelist_entry.next_index = -1;
        freelist_entry.prev_index = -1;
        bucket.freelist = index;

        VERIFY(bucket.get_buddy_bit(index) == false);

        // Set buddy bit to 1 (one used, one unused).
        bucket.set_buddy_bit(index, true);

        // Second half is returned.
        return index + block_size;
    }

    // Freelist has at least one entry, return that.
    ChunkIndex index = bucket.freelist;

    bucket.freelist = get_freelist_entry(bucket.freelist).freelist.next_index;
    if (bucket.freelist != -1) {
        get_freelist_entry(bucket.freelist).freelist.prev_index = -1;
    }

    VERIFY(bucket.get_buddy_bit(index) == true);
    bucket.set_buddy_bit(index, false);

    return index;
}

void PhysicalZone::deallocate_block(PhysicalAddress address, size_t order)
{
    size_t block_size = 2u << order;
    ChunkIndex index = (address.get() - m_base_address.get()) / ZONE_CHUNK_SIZE;
    deallocate_block_impl(index, order);
    m_used_chunks -= block_size;
}

void PhysicalZone::deallocate_block_impl(ChunkIndex index, size_t order)
{
    size_t block_size = 2u << order;

    // Basic algorithm:
    // If the buddy block is free (buddy bit is 1 -- because this block was the only used one):
    // Then,
    //     1. Merge with buddy.
    //     2. Return the merged block to order+1.
    // Else (buddy bit is 0 -- because both blocks are used)
    //     1. Add the block to the freelist.
    //     2. Set buddy bit to 1.
    auto& bucket = m_buckets[order];

    if (bucket.get_buddy_bit(index)) {
        // Buddy is free! Merge with buddy and coalesce upwards to the next order.
        auto buddy_bit_index = bucket.buddy_bit_index(index);
        ChunkIndex buddy_base_index = (buddy_bit_index << 1) << (1 + order);

        if (index == buddy_base_index)
            remove_from_freelist(bucket, buddy_base_index + block_size);
        else
            remove_from_freelist(bucket, buddy_base_index);

        bucket.set_buddy_bit(index, false);
        deallocate_block_impl(buddy_base_index, order + 1);
    } else {
        // Buddy is in use. Add freed block to freelist and set buddy bit to 1.

        if (bucket.freelist != -1) {
            get_freelist_entry(bucket.freelist).freelist.prev_index = index;
        }

        auto& freelist_entry = get_freelist_entry(index).freelist;
        freelist_entry.next_index = bucket.freelist;
        freelist_entry.prev_index = -1;
        bucket.freelist = index;

        bucket.set_buddy_bit(index, true);
    }
}

void PhysicalZone::remove_from_freelist(BuddyBucket& bucket, ChunkIndex index)
{
    auto& freelist_entry = get_freelist_entry(index).freelist;
    VERIFY(freelist_entry.prev_index >= -1);
    VERIFY(freelist_entry.next_index >= -1);
    if (freelist_entry.prev_index != -1) {
        auto& prev_entry = get_freelist_entry(freelist_entry.prev_index).freelist;
        prev_entry.next_index = freelist_entry.next_index;
    }
    if (freelist_entry.next_index != -1) {
        auto& next_entry = get_freelist_entry(freelist_entry.next_index).freelist;
        next_entry.prev_index = freelist_entry.prev_index;
    }
    if (bucket.freelist == index)
        bucket.freelist = freelist_entry.next_index;
    freelist_entry.next_index = -1;
    freelist_entry.prev_index = -1;
}

void PhysicalZone::dump() const
{
    dbgln("(( {} used, {} available, page_count: {} ))", m_used_chunks, available(), m_page_count);
    for (size_t i = 0; i <= max_order; ++i) {
        auto const& bucket = m_buckets[i];
        dbgln("[{:2} / {:4}] ", i, (size_t)(2u << i));
        auto entry = bucket.freelist;
        while (entry != -1) {
            dbgln("  {}", entry);
            entry = get_freelist_entry(entry).freelist.next_index;
        }
    }
}

}
