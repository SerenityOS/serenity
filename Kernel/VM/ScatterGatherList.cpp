/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/VM/ScatterGatherList.h>

namespace Kernel {

ScatterGatherList ScatterGatherList::create_from_buffer(const u8* buffer, size_t size)
{
    VERIFY(buffer && size);
    ScatterGatherList new_list;
    auto* region = MM.find_region_from_vaddr(VirtualAddress(buffer));
    VERIFY(region);
    while (size > 0) {
        size_t offset_in_page = (VirtualAddress(buffer) - region->vaddr()).get() % PAGE_SIZE;
        size_t size_in_page = min(PAGE_SIZE - offset_in_page, size);
        VERIFY(offset_in_page + size_in_page - 1 <= PAGE_SIZE);
        new_list.add_entry(region->physical_page(region->page_index_from_address(VirtualAddress(buffer)))->paddr().get(), offset_in_page, size_in_page);
        size -= size_in_page;
        buffer += size_in_page;
    }
    return new_list;
}

ScatterGatherList ScatterGatherList::create_from_physical(PhysicalAddress paddr, size_t size)
{
    VERIFY(!paddr.is_null() && size);
    ScatterGatherList new_list;
    new_list.add_entry(paddr.page_base().get(), paddr.offset_in_page(), size);
    return new_list;
}

void ScatterGatherList::add_entry(FlatPtr addr, size_t offset, size_t size)
{
    m_entries.append({ addr, offset, size });
}

void ScatterGatherList::for_each_entry(Function<void(const FlatPtr, const size_t)> callback) const
{
    for (auto& entry : m_entries)
        callback(entry.page_base + entry.offset, entry.length);
}

}
