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
#include <Kernel/Assertions.h>
#include <Kernel/VM/PhysicalPage.h>
#include <Kernel/VM/PhysicalRegion.h>

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

RefPtr<PhysicalPage> PhysicalRegion::take_free_page(bool supervisor)
{
    ASSERT(m_pages);

    if (m_used == m_pages)
        return nullptr;

    // search from the last page we allocated
    for (unsigned page = m_last; page < m_pages; page++) {
        if (!m_bitmap.get(page)) {
            m_bitmap.set(page, true);
            m_used++;
            m_last = page + 1;
            return PhysicalPage::create(m_lower.offset(page * PAGE_SIZE), supervisor);
        }
    }

    // wrap back around to the start in case we missed something
    for (unsigned page = 0; page < m_last; page++) {
        if (!m_bitmap.get(page)) {
            m_bitmap.set(page, true);
            m_used++;
            m_last = page + 1;
            return PhysicalPage::create(m_lower.offset(page * PAGE_SIZE), supervisor);
        }
    }

    ASSERT_NOT_REACHED();

    return nullptr;
}

void PhysicalRegion::return_page_at(PhysicalAddress addr)
{
    ASSERT(m_pages);

    if (m_used == 0) {
        ASSERT_NOT_REACHED();
    }

    int local_offset = addr.get() - m_lower.get();
    ASSERT(local_offset >= 0);
    ASSERT((u32)local_offset < (u32)(m_pages * PAGE_SIZE));

    auto page = (unsigned)local_offset / PAGE_SIZE;
    if (page < m_last)
        m_last = page;

    m_bitmap.set(page, false);
    m_used--;
}
