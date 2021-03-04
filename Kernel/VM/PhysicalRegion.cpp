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

#include <AK/Bitmap.h>
#include <AK/NonnullRefPtr.h>
#include <AK/RefPtr.h>
#include <AK/Vector.h>
#include <Kernel/Assertions.h>
#include <Kernel/Random.h>
#include <Kernel/VM/PhysicalPage.h>
#include <Kernel/VM/PhysicalRegion.h>

namespace Kernel {

NonnullRefPtr<PhysicalRegion> PhysicalRegion::create(PhysicalAddress lower, PhysicalAddress upper)
{
    return adopt(*new PhysicalRegion(lower, upper));
}

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

NonnullRefPtrVector<PhysicalPage> PhysicalRegion::take_contiguous_free_pages(size_t count, bool supervisor, size_t physical_alignment)
{
    VERIFY(m_pages);
    VERIFY(m_used != m_pages);

    NonnullRefPtrVector<PhysicalPage> physical_pages;
    physical_pages.ensure_capacity(count);

    auto first_contiguous_page = find_contiguous_free_pages(count, physical_alignment);

    for (size_t index = 0; index < count; index++)
        physical_pages.append(PhysicalPage::create(m_lower.offset(PAGE_SIZE * (index + first_contiguous_page)), supervisor));
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
            Checked<FlatPtr> local_offset = m_recently_returned[index].get();
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

RefPtr<PhysicalPage> PhysicalRegion::take_free_page(bool supervisor)
{
    VERIFY(m_pages);

    auto free_index = find_one_free_page();
    if (!free_index.has_value())
        return nullptr;

    return PhysicalPage::create(m_lower.offset(free_index.value() * PAGE_SIZE), supervisor);
}

void PhysicalRegion::free_page_at(PhysicalAddress addr)
{
    VERIFY(m_pages);

    if (m_used == 0) {
        VERIFY_NOT_REACHED();
    }

    Checked<FlatPtr> local_offset = addr.get();
    local_offset -= m_lower.get();
    VERIFY(!local_offset.has_overflow());
    VERIFY(local_offset.value() < (FlatPtr)(m_pages * PAGE_SIZE));

    auto page = local_offset.value() / PAGE_SIZE;
    m_bitmap.set(page, false);
    m_free_hint = page; // We know we can find one here for sure
    m_used--;
}

void PhysicalRegion::return_page(const PhysicalPage& page)
{
    auto returned_count = m_recently_returned.size();
    if (returned_count >= m_recently_returned.capacity()) {
        // Return queue is full, pick a random entry and free that page
        // and replace the entry with this page
        auto& entry = m_recently_returned[get_fast_random<u8>()];
        free_page_at(entry);
        entry = page.paddr();
    } else {
        // Still filling the return queue, just append it
        m_recently_returned.append(page.paddr());
    }
}

}
