/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <AK/Vector.h>
#include <Kernel/Memory/PhysicalRAMPage.h>
#include <Kernel/Memory/PhysicalZone.h>

namespace Kernel::Memory {

class PhysicalRegion {
    AK_MAKE_NONCOPYABLE(PhysicalRegion);
    AK_MAKE_NONMOVABLE(PhysicalRegion);

public:
    static OwnPtr<PhysicalRegion> try_create(PhysicalAddress lower, PhysicalAddress upper)
    {
        return adopt_own_if_nonnull(new PhysicalRegion { lower, upper });
    }

    ~PhysicalRegion();

    void initialize_zones();

    PhysicalAddress lower() const { return m_lower; }
    PhysicalAddress upper() const { return m_upper; }
    size_t size() const { return m_pages; }
    bool contains(PhysicalAddress paddr) const { return paddr >= m_lower && paddr < m_upper; }

    OwnPtr<PhysicalRegion> try_take_pages_from_beginning(size_t);

    RefPtr<PhysicalRAMPage> take_free_page();
    Vector<NonnullRefPtr<PhysicalRAMPage>> take_contiguous_free_pages(size_t count);
    void return_page(PhysicalAddress);

private:
    PhysicalRegion(PhysicalAddress lower, PhysicalAddress upper);

    static constexpr size_t large_zone_size = 16 * MiB;
    static constexpr size_t small_zone_size = 1 * MiB;

    Vector<NonnullOwnPtr<PhysicalZone>> m_zones;

    size_t m_large_zones { 0 };

    PhysicalZone::List m_usable_zones;
    PhysicalZone::List m_full_zones;

    PhysicalAddress m_lower;
    PhysicalAddress m_upper;
    size_t m_pages { 0 };
};

}
