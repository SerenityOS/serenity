/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Memory/ScatterGatherList.h>

namespace Kernel::Memory {

ErrorOr<LockRefPtr<ScatterGatherList>> ScatterGatherList::try_create(AsyncBlockDeviceRequest& request, Span<NonnullRefPtr<PhysicalRAMPage>> allocated_pages, size_t device_block_size, StringView region_name)
{
    auto vm_object = TRY(AnonymousVMObject::try_create_with_physical_pages(allocated_pages));
    auto size = TRY(page_round_up((request.block_count() * device_block_size)));
    auto region = TRY(MM.allocate_kernel_region_with_vmobject(vm_object, size, region_name, Region::Access::Read | Region::Access::Write, MemoryType::Normal));

    return adopt_lock_ref_if_nonnull(new (nothrow) ScatterGatherList(vm_object, move(region)));
}

ScatterGatherList::ScatterGatherList(NonnullLockRefPtr<AnonymousVMObject> vm_object, NonnullOwnPtr<Region> dma_region)
    : m_vm_object(move(vm_object))
    , m_dma_region(move(dma_region))
{
}

}
