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
    , m_bitmap(Bitmap::create())
{
}

void PhysicalRegion::expand(PhysicalAddress lower, PhysicalAddress upper)
{
    ASSERT(!m_pages);

    m_lower = lower;
    m_upper = upper;
}

unsigned PhysicalRegion::finalize_capacity()
{
    ASSERT(!m_pages);

    m_pages = (m_upper.get() - m_lower.get()) / PAGE_SIZE;
    m_bitmap.grow(m_pages, false);

    return size();
}

Vector<RefPtr<PhysicalPage>> PhysicalRegion::take_contiguous_free_pages(size_t count, bool supervisor)
{
    ASSERT(m_pages);
    ASSERT(m_used != m_pages);

    Vector<RefPtr<PhysicalPage>> physical_pages;
    physical_pages.ensure_capacity(count);

    auto first_contiguous_page = find_contiguous_free_pages(count);

    for (size_t index = 0; index < count; index++) {
        physical_pages.append(PhysicalPage::create(m_lower.offset(PAGE_SIZE * (index + first_contiguous_page)), supervisor));
    }
    return physical_pages;
}

unsigned PhysicalRegion::find_contiguous_free_pages(size_t count)
{
    ASSERT(count != 0);
    // search from the last page we allocated
    auto range = find_and_allocate_contiguous_range(count);
    ASSERT(range.has_value());
    return range.value();
}

Optional<unsigned> PhysicalRegion::find_and_allocate_contiguous_range(size_t count)
{
    ASSERT(count != 0);
    size_t found_pages_count = 0;
    auto first_index = m_bitmap.find_longest_range_of_unset_bits(count, found_pages_count);
    if (!first_index.has_value())
        return {};

    auto page = first_index.value();
    if (count == found_pages_count) {
        for (unsigned page_index = page; page_index < (page + count); page_index++) {
            m_bitmap.set(page_index, true);
        }
        m_used += count;
        m_last = page + count;
        return page;
    }
    return {};
}

RefPtr<PhysicalPage> PhysicalRegion::take_free_page(bool supervisor)
{
    ASSERT(m_pages);

    if (m_used == m_pages)
        return nullptr;

    return PhysicalPage::create(m_lower.offset(find_contiguous_free_pages(1) * PAGE_SIZE), supervisor);
}

void PhysicalRegion::return_page_at(PhysicalAddress addr)
{
    ASSERT(m_pages);

    if (m_used == 0) {
        ASSERT_NOT_REACHED();
    }

    ptrdiff_t local_offset = addr.get() - m_lower.get();
    ASSERT(local_offset >= 0);
    ASSERT((FlatPtr)local_offset < (FlatPtr)(m_pages * PAGE_SIZE));

    auto page = (FlatPtr)local_offset / PAGE_SIZE;
    if (page < m_last)
        m_last = page;

    m_bitmap.set(page, false);
    m_used--;
}

}
