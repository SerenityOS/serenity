/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Memory/ScatterGatherList.h>

namespace Kernel::Memory {

RefPtr<ScatterGatherList> ScatterGatherList::try_create(AsyncBlockDeviceRequest& request, Span<NonnullRefPtr<PhysicalPage>> allocated_pages, size_t device_block_size)
{
    auto maybe_vm_object = AnonymousVMObject::try_create_with_physical_pages(allocated_pages);
    if (maybe_vm_object.is_error()) {
        // FIXME: Would be nice to be able to return a ErrorOr here.
        return {};
    }
    return adopt_ref_if_nonnull(new (nothrow) ScatterGatherList(maybe_vm_object.release_value(), request, device_block_size));
}

ScatterGatherList::ScatterGatherList(NonnullRefPtr<AnonymousVMObject> vm_object, AsyncBlockDeviceRequest& request, size_t device_block_size)
    : m_vm_object(move(vm_object))
{
    auto region_or_error = MM.allocate_kernel_region_with_vmobject(m_vm_object, page_round_up((request.block_count() * device_block_size)).release_value_but_fixme_should_propagate_errors(), "AHCI Scattered DMA", Region::Access::Read | Region::Access::Write, Region::Cacheable::Yes);
    if (region_or_error.is_error())
        TODO();
    m_dma_region = region_or_error.release_value();
}

}
