/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Bitmap.h>
#include <AK/Optional.h>
#include <Kernel/VM/PhysicalPage.h>

namespace Kernel {

class PhysicalRegion {
public:
    static PhysicalRegion create(PhysicalAddress lower, PhysicalAddress upper)
    {
        return { lower, upper };
    }

    void expand(PhysicalAddress lower, PhysicalAddress upper);
    unsigned finalize_capacity();

    PhysicalAddress lower() const { return m_lower; }
    PhysicalAddress upper() const { return m_upper; }
    unsigned size() const { return m_pages; }
    unsigned used() const { return m_used - m_recently_returned.size(); }
    unsigned free() const { return m_pages - m_used + m_recently_returned.size(); }
    bool contains(PhysicalAddress paddr) const { return paddr >= m_lower && paddr <= m_upper; }

    PhysicalRegion take_pages_from_beginning(unsigned);

    RefPtr<PhysicalPage> take_free_page();
    NonnullRefPtrVector<PhysicalPage> take_contiguous_free_pages(size_t count, size_t physical_alignment = PAGE_SIZE);
    void return_page(PhysicalAddress);

private:
    unsigned find_contiguous_free_pages(size_t count, size_t physical_alignment = PAGE_SIZE);
    Optional<unsigned> find_and_allocate_contiguous_range(size_t count, unsigned alignment = 1);
    Optional<unsigned> find_one_free_page();
    void free_page_at(PhysicalAddress);

    PhysicalRegion(PhysicalAddress lower, PhysicalAddress upper);

    PhysicalAddress m_lower;
    PhysicalAddress m_upper;
    unsigned m_pages { 0 };
    unsigned m_used { 0 };
    Bitmap m_bitmap;
    size_t m_free_hint { 0 };
    Vector<PhysicalAddress, 256> m_recently_returned;
};

}
