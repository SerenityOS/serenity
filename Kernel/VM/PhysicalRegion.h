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
    ~PhysicalRegion() { }

    void expand(PhysicalAddress lower, PhysicalAddress upper);
    unsigned finalize_capacity();

    PhysicalAddress lower() const { return m_lower; }
    PhysicalAddress upper() const { return m_upper; }
    unsigned size() const { return m_pages; }
    unsigned used() const { return m_used - m_recently_returned.size(); }
    unsigned free() const { return m_pages - m_used + m_recently_returned.size(); }
    bool contains(const PhysicalPage& page) const { return page.paddr() >= m_lower && page.paddr() <= m_upper; }

    RefPtr<PhysicalPage> take_free_page(bool supervisor);
    NonnullRefPtrVector<PhysicalPage> take_contiguous_free_pages(size_t count, bool supervisor);
    void return_page(const PhysicalPage& page);

private:
    unsigned find_contiguous_free_pages(size_t count);
    Optional<unsigned> find_and_allocate_contiguous_range(size_t count);
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
