/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/NonnullRefPtr.h>
#include <AK/RefPtr.h>
#include <Kernel/Assertions.h>
#include <Kernel/Random.h>
#include <Kernel/VM/PhysicalRegion.h>

namespace Kernel {

PhysicalRegion::PhysicalRegion(PhysicalAddress lower, PhysicalAddress upper)
    : m_lower(lower)
    , m_upper(upper)
{
}

void PhysicalRegion::expand(PhysicalAddress lower, PhysicalAddress upper)
{
    VERIFY(!m_pages);

    m_lower = lower;
    m_upper = upper;
}

unsigned PhysicalRegion::finalize_capacity()
{
    VERIFY(!m_pages);

    m_pages = (m_upper.get() - m_lower.get()) / PAGE_SIZE;
    m_bitmap.grow(m_pages, false);

    return size();
}

PhysicalRegion PhysicalRegion::take_pages_from_beginning(unsigned page_count)
{
    VERIFY(m_used == 0);
    VERIFY(page_count > 0);
    VERIFY(page_count < m_pages);
    auto taken_lower = m_lower;
    auto taken_upper = taken_lower.offset((PhysicalPtr)page_count * PAGE_SIZE);
    m_lower = m_lower.offset((PhysicalPtr)page_count * PAGE_SIZE);

    // TODO: find a more elegant way to re-init the existing region
    m_pages = 0;
    m_bitmap = {}; // FIXME: Kind of wasteful
    finalize_capacity();

    auto taken_region = create(taken_lower, taken_upper);
    taken_region.finalize_capacity();
    return taken_region;
}

NonnullRefPtrVector<PhysicalPage> PhysicalRegion::take_contiguous_free_pages(size_t count, size_t physical_alignment)
{
    VERIFY(m_pages);
    VERIFY(m_used != m_pages);

    NonnullRefPtrVector<PhysicalPage> physical_pages;
    physical_pages.ensure_capacity(count);

    auto first_contiguous_page = find_contiguous_free_pages(count, physical_alignment);

    for (size_t index = 0; index < count; index++)
        physical_pages.append(PhysicalPage::create(m_lower.offset(PAGE_SIZE * (index + first_contiguous_page))));
    return physical_pages;
}

unsigned PhysicalRegion::find_contiguous_free_pages(size_t count, size_t physical_alignment)
{
    VERIFY(count != 0);
    VERIFY(physical_alignment % PAGE_SIZE == 0);
    // search from the last page we allocated
    auto range = find_and_allocate_contiguous_range(count, physical_alignment / PAGE_SIZE);
    VERIFY(range.has_value());
    return range.value();
}

Optional<unsigned> PhysicalRegion::find_one_free_page()
{
    if (m_used == m_pages) {
        // We know we don't have any free pages, no need to check the bitmap
        // Check if we can draw one from the return queue
        if (m_recently_returned.size() > 0) {
            u8 index = get_fast_random<u8>() % m_recently_returned.size();
            Checked<PhysicalPtr> local_offset = m_recently_returned[index].get();
            local_offset -= m_lower.get();
            m_recently_returned.remove(index);
            VERIFY(!local_offset.has_overflow());
            VERIFY(local_offset.value() < (FlatPtr)(m_pages * PAGE_SIZE));
            return local_offset.value() / PAGE_SIZE;
        }
        return {};
    }
    auto free_index = m_bitmap.find_one_anywhere_unset(m_free_hint);
    if (!free_index.has_value())
        return {};

    auto page_index = free_index.value();
    m_bitmap.set(page_index, true);
    m_used++;
    m_free_hint = free_index.value() + 1; // Just a guess
    if (m_free_hint >= m_bitmap.size())
        m_free_hint = 0;
    return page_index;
}

Optional<unsigned> PhysicalRegion::find_and_allocate_contiguous_range(size_t count, unsigned alignment)
{
    VERIFY(count != 0);
    size_t found_pages_count = 0;
    // TODO: Improve how we deal with alignment != 1
    auto first_index = m_bitmap.find_longest_range_of_unset_bits(count + alignment - 1, found_pages_count);
    if (!first_index.has_value())
        return {};

    auto page = first_index.value();
    if (alignment != 1) {
        auto lower_page = m_lower.get() / PAGE_SIZE;
        page = ((lower_page + page + alignment - 1) & ~(alignment - 1)) - lower_page;
    }
    if (found_pages_count >= count) {
        m_bitmap.set_range<true>(page, count);
        m_used += count;
        m_free_hint = first_index.value() + count + 1; // Just a guess
        if (m_free_hint >= m_bitmap.size())
            m_free_hint = 0;
        return page;
    }
    return {};
}

RefPtr<PhysicalPage> PhysicalRegion::take_free_page()
{
    VERIFY(m_pages);

    auto free_index = find_one_free_page();
    if (!free_index.has_value())
        return nullptr;

    return PhysicalPage::create(m_lower.offset((PhysicalPtr)free_index.value() * PAGE_SIZE));
}

void PhysicalRegion::free_page_at(PhysicalAddress addr)
{
    VERIFY(m_pages);

    if (m_used == 0) {
        VERIFY_NOT_REACHED();
    }

    Checked<PhysicalPtr> local_offset = addr.get();
    local_offset -= m_lower.get();
    VERIFY(!local_offset.has_overflow());
    VERIFY(local_offset.value() < ((PhysicalPtr)m_pages * PAGE_SIZE));

    auto page = local_offset.value() / PAGE_SIZE;
    m_bitmap.set(page, false);
    m_free_hint = page; // We know we can find one here for sure
    m_used--;
}

void PhysicalRegion::return_page(PhysicalAddress paddr)
{
    auto returned_count = m_recently_returned.size();
    if (returned_count >= m_recently_returned.capacity()) {
        // Return queue is full, pick a random entry and free that page
        // and replace the entry with this page
        auto& entry = m_recently_returned[get_fast_random<u8>()];
        free_page_at(entry);
        entry = paddr;
    } else {
        // Still filling the return queue, just append it
        m_recently_returned.append(paddr);
    }
}

}
