/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Bitmap.h>
#include <AK/NonnullRefPtrVector.h>
#include <AK/Optional.h>
#include <AK/RefCounted.h>
#include <Kernel/VM/PhysicalPage.h>

namespace Kernel {

class PhysicalRegion : public RefCounted<PhysicalRegion> {
    AK_MAKE_ETERNAL

public:
    static NonnullRefPtr<PhysicalRegion> create(PhysicalAddress lower, PhysicalAddress upper);
    ~PhysicalRegion() = default;

    void expand(PhysicalAddress lower, PhysicalAddress upper);
    unsigned finalize_capacity();

    PhysicalAddress lower() const { return m_lower; }
    PhysicalAddress upper() const { return m_upper; }
    unsigned size() const { return m_pages; }
    unsigned used() const { return m_used - m_recently_returned.size(); }
    unsigned free() const { return m_pages - m_used + m_recently_returned.size(); }
    bool contains(const PhysicalPage& page) const { return page.paddr() >= m_lower && page.paddr() <= m_upper; }

    NonnullRefPtr<PhysicalRegion> take_pages_from_beginning(unsigned);

    RefPtr<PhysicalPage> take_free_page(bool supervisor);
    NonnullRefPtrVector<PhysicalPage> take_contiguous_free_pages(size_t count, bool supervisor, size_t physical_alignment = PAGE_SIZE);
    void return_page(const PhysicalPage& page);

private:
    unsigned find_contiguous_free_pages(size_t count, size_t physical_alignment = PAGE_SIZE);
    Optional<unsigned> find_and_allocate_contiguous_range(size_t count, unsigned alignment = 1);
    Optional<unsigned> find_one_free_page();
    void free_page_at(PhysicalAddress addr);

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
