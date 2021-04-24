/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/VM/ScatterGatherList.h>

namespace Kernel {

NonnullRefPtr<ScatterGatherList> ScatterGatherList::create(AsyncBlockDeviceRequest& request, NonnullRefPtrVector<PhysicalPage> allocated_pages, size_t device_block_size)
{
    return adopt_ref(*new ScatterGatherList(request, allocated_pages, device_block_size));
}

ScatterGatherList::ScatterGatherList(AsyncBlockDeviceRequest& request, NonnullRefPtrVector<PhysicalPage> allocated_pages, size_t device_block_size)
    : m_vm_object(AnonymousVMObject::create_with_physical_pages(allocated_pages))
{
    m_dma_region = MM.allocate_kernel_region_with_vmobject(m_vm_object, page_round_up((request.block_count() * device_block_size)), "AHCI Scattered DMA", Region::Access::Read | Region::Access::Write, Region::Cacheable::Yes);
}

ScatterGatherRefList ScatterGatherRefList::create_from_buffer(const u8* buffer, size_t size)
{
    VERIFY(buffer && size);
    ScatterGatherRefList new_list;
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

ScatterGatherRefList ScatterGatherRefList::create_from_physical(PhysicalAddress paddr, size_t size)
{
    VERIFY(!paddr.is_null() && size);
    ScatterGatherRefList new_list;
    new_list.add_entry(paddr.page_base().get(), paddr.offset_in_page(), size);
    return new_list;
}

void ScatterGatherRefList::add_entry(FlatPtr addr, size_t offset, size_t size)
{
    m_entries.append({ addr, offset, size });
}

void ScatterGatherRefList::for_each_entry(Function<void(const FlatPtr, const size_t)> callback) const
{
    for (auto& entry : m_entries)
        callback(entry.page_base + entry.offset, entry.length);
}

}
