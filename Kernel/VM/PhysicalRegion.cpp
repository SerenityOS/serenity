/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/NonnullRefPtr.h>
#include <AK/RefPtr.h>
#include <Kernel/Assertions.h>
#include <Kernel/Random.h>
#include <Kernel/VM/MemoryManager.h>
#include <Kernel/VM/PhysicalRegion.h>
#include <Kernel/VM/PhysicalZone.h>

namespace Kernel {

static constexpr u32 next_power_of_two(u32 value)
{
    value--;
    value |= value >> 1;
    value |= value >> 2;
    value |= value >> 4;
    value |= value >> 8;
    value |= value >> 16;
    value++;
    return value;
}

PhysicalRegion::~PhysicalRegion()
{
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

void PhysicalRegion::initialize_zones()
{
    size_t remaining_pages = m_pages;
    auto base_address = m_lower;

    auto make_zones = [&](size_t zone_size) {
        while (remaining_pages >= zone_size) {
            m_zones.append(make<PhysicalZone>(base_address, zone_size));
            dmesgln(" * Zone {:016x}-{:016x} ({} bytes)", base_address.get(), base_address.get() + zone_size * PAGE_SIZE - 1, zone_size * PAGE_SIZE);
            base_address = base_address.offset(zone_size * PAGE_SIZE);
            remaining_pages -= zone_size;
        }
    };

    make_zones(4096);
    make_zones(256);
}

unsigned PhysicalRegion::finalize_capacity()
{
    VERIFY(!m_pages);

    m_pages = (m_upper.get() - m_lower.get()) / PAGE_SIZE;

    return size();
}

OwnPtr<PhysicalRegion> PhysicalRegion::try_take_pages_from_beginning(unsigned page_count)
{
    VERIFY(m_used == 0);
    VERIFY(page_count > 0);
    VERIFY(page_count < m_pages);
    auto taken_lower = m_lower;
    auto taken_upper = taken_lower.offset((PhysicalPtr)page_count * PAGE_SIZE);
    m_lower = m_lower.offset((PhysicalPtr)page_count * PAGE_SIZE);

    // TODO: find a more elegant way to re-init the existing region
    m_pages = 0;
    finalize_capacity();

    auto taken_region = try_create(taken_lower, taken_upper);
    if (!taken_region)
        return {};
    taken_region->finalize_capacity();
    return taken_region;
}

NonnullRefPtrVector<PhysicalPage> PhysicalRegion::take_contiguous_free_pages(size_t count)
{
    auto rounded_page_count = next_power_of_two(count);
    auto order = __builtin_ctz(rounded_page_count);

    Optional<PhysicalAddress> page_base;
    for (auto& zone : m_zones) {
        page_base = zone.allocate_block(order);

        if (page_base.has_value())
            break;
    }

    if (!page_base.has_value())
        return {};

    NonnullRefPtrVector<PhysicalPage> physical_pages;
    physical_pages.ensure_capacity(count);

    for (size_t i = 0; i < count; ++i)
        physical_pages.append(PhysicalPage::create(page_base.value().offset(i * PAGE_SIZE)));
    return physical_pages;
}

RefPtr<PhysicalPage> PhysicalRegion::take_free_page()
{
    for (auto& zone : m_zones) {
        auto page = zone.allocate_block(0);
        if (page.has_value())
            return PhysicalPage::create(page.value());
    }

    dbgln("PhysicalRegion::take_free_page: No free physical pages");
    return nullptr;
}

void PhysicalRegion::return_page(PhysicalAddress paddr)
{
    for (auto& zone : m_zones) {
        if (zone.contains(paddr)) {
            zone.deallocate_block(paddr, 0);
            return;
        }
    }

    VERIFY_NOT_REACHED();
}

}
