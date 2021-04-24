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

}
