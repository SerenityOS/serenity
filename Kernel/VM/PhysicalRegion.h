/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Bitmap.h>
#include <AK/NonnullOwnPtrVector.h>
#include <AK/Optional.h>
#include <AK/OwnPtr.h>
#include <Kernel/VM/PhysicalPage.h>

namespace Kernel {

class PhysicalZone;

class PhysicalRegion {
public:
    static OwnPtr<PhysicalRegion> try_create(PhysicalAddress lower, PhysicalAddress upper)
    {
        return adopt_own_if_nonnull(new PhysicalRegion { lower, upper });
    }

    ~PhysicalRegion();

    void initialize_zones();

    PhysicalAddress lower() const { return m_lower; }
    PhysicalAddress upper() const { return m_upper; }
    unsigned size() const { return m_pages; }
    bool contains(PhysicalAddress paddr) const { return paddr >= m_lower && paddr <= m_upper; }

    OwnPtr<PhysicalRegion> try_take_pages_from_beginning(unsigned);

    RefPtr<PhysicalPage> take_free_page();
    NonnullRefPtrVector<PhysicalPage> take_contiguous_free_pages(size_t count);
    void return_page(PhysicalAddress);

private:
    PhysicalRegion(PhysicalAddress lower, PhysicalAddress upper);

    NonnullOwnPtrVector<PhysicalZone> m_zones;

    PhysicalAddress m_lower;
    PhysicalAddress m_upper;
    unsigned m_pages { 0 };
};

}
