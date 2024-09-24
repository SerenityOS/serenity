/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <Kernel/Memory/MemoryManager.h>
#include <Kernel/Memory/VMObject.h>

namespace Kernel::Memory {

static Singleton<SpinlockProtected<VMObject::AllInstancesList, LockRank::None>> s_all_instances;

SpinlockProtected<VMObject::AllInstancesList, LockRank::None>& VMObject::all_instances()
{
    return s_all_instances;
}

ErrorOr<FixedArray<RefPtr<PhysicalRAMPage>>> VMObject::try_clone_physical_pages() const
{
    return m_physical_pages.clone();
}

ErrorOr<FixedArray<RefPtr<PhysicalRAMPage>>> VMObject::try_create_physical_pages(size_t size)
{
    return FixedArray<RefPtr<PhysicalRAMPage>>::create(ceil_div(size, static_cast<size_t>(PAGE_SIZE)));
}

VMObject::VMObject(FixedArray<RefPtr<PhysicalRAMPage>>&& new_physical_pages)
    : m_physical_pages(move(new_physical_pages))
{
    all_instances().with([&](auto& list) { list.append(*this); });
}

VMObject::~VMObject()
{
    VERIFY(m_regions.is_empty());
}

void VMObject::remap_regions()
{
    for_each_region([](Region& region) {
        region.remap();
    });
}

bool VMObject::remap_regions_one_page(size_t page_index, NonnullRefPtr<PhysicalRAMPage> page)
{
    bool success = true;
    for_each_region([&](Region& region) {
        if (!region.remap_vmobject_page(page_index, *page))
            success = false;
    });
    return success;
}

}
