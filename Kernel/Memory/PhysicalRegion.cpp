/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/BuiltinWrappers.h>
#include <Kernel/Library/Assertions.h>
#include <Kernel/Memory/MemoryManager.h>
#include <Kernel/Memory/PhysicalRegion.h>
#include <Kernel/Memory/PhysicalZone.h>
#include <Kernel/Security/Random.h>

namespace Kernel::Memory {

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

PhysicalRegion::~PhysicalRegion() = default;

PhysicalRegion::PhysicalRegion(PhysicalAddress lower, PhysicalAddress upper)
    : m_lower(lower)
    , m_upper(upper)
{
    m_pages = (m_upper.get() - m_lower.get()) / PAGE_SIZE;
}

void PhysicalRegion::initialize_zones()
{
    size_t remaining_pages = m_pages;
    auto base_address = m_lower;

    auto make_zones = [&](size_t zone_size) -> size_t {
        size_t pages_per_zone = zone_size / PAGE_SIZE;
        size_t zone_count = 0;
        auto first_address = base_address;
        while (remaining_pages >= pages_per_zone) {
            m_zones.append(adopt_nonnull_own_or_enomem(new (nothrow) PhysicalZone(base_address, pages_per_zone)).release_value_but_fixme_should_propagate_errors());
            base_address = base_address.offset(pages_per_zone * PAGE_SIZE);
            m_usable_zones.append(*m_zones.last());
            remaining_pages -= pages_per_zone;
            ++zone_count;
        }
        if (zone_count)
            dmesgln(" * {}x PhysicalZone ({} MiB) @ {:016x}-{:016x}", zone_count, pages_per_zone / 256, first_address.get(), base_address.get() - pages_per_zone * PAGE_SIZE - 1);
        return zone_count;
    };

    // First make 16 MiB zones (with 4096 pages each)
    m_large_zones = make_zones(large_zone_size);

    // Then divide any remaining space into 1 MiB zones (with 256 pages each)
    make_zones(small_zone_size);
}

OwnPtr<PhysicalRegion> PhysicalRegion::try_take_pages_from_beginning(size_t page_count)
{
    VERIFY(page_count > 0);
    VERIFY(page_count < m_pages);
    auto taken_lower = m_lower;
    auto taken_upper = taken_lower.offset((PhysicalPtr)page_count * PAGE_SIZE);
    m_lower = m_lower.offset((PhysicalPtr)page_count * PAGE_SIZE);
    m_pages = (m_upper.get() - m_lower.get()) / PAGE_SIZE;

    return try_create(taken_lower, taken_upper);
}

Vector<NonnullRefPtr<PhysicalRAMPage>> PhysicalRegion::take_contiguous_free_pages(size_t count)
{
    auto rounded_page_count = next_power_of_two(count);
    auto order = count_trailing_zeroes(rounded_page_count);

    Optional<PhysicalAddress> page_base;
    for (auto& zone : m_usable_zones) {
        page_base = zone.allocate_block(order);
        if (page_base.has_value()) {
            if (zone.is_empty()) {
                // We've exhausted this zone, move it to the full zones list.
                m_full_zones.append(zone);
            }
            break;
        }
    }

    if (!page_base.has_value())
        return {};

    Vector<NonnullRefPtr<PhysicalRAMPage>> physical_pages;
    physical_pages.ensure_capacity(count);

    for (size_t i = 0; i < count; ++i)
        physical_pages.append(PhysicalRAMPage::create(page_base.value().offset(i * PAGE_SIZE)));
    return physical_pages;
}

RefPtr<PhysicalRAMPage> PhysicalRegion::take_free_page()
{
    if (m_usable_zones.is_empty())
        return nullptr;

    auto& zone = *m_usable_zones.first();
    auto page = zone.allocate_block(0);
    VERIFY(page.has_value());

    if (zone.is_empty()) {
        // We've exhausted this zone, move it to the full zones list.
        m_full_zones.append(zone);
    }

    return PhysicalRAMPage::create(page.value());
}

void PhysicalRegion::return_page(PhysicalAddress paddr)
{
    auto large_zone_base = lower().get();
    auto small_zone_base = lower().get() + (m_large_zones * large_zone_size);

    size_t zone_index;
    if (paddr.get() < small_zone_base)
        zone_index = (paddr.get() - large_zone_base) / large_zone_size;
    else
        zone_index = m_large_zones + (paddr.get() - small_zone_base) / small_zone_size;

    auto& zone = m_zones[zone_index];
    VERIFY(zone->contains(paddr));
    zone->deallocate_block(paddr, 0);
    if (m_full_zones.contains(*zone))
        m_usable_zones.append(*zone);
}

}
