/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/AtomicRefCounted.h>
#include <AK/Vector.h>
#include <Kernel/Devices/BlockDevice.h>
#include <Kernel/Memory/AnonymousVMObject.h>
#include <Kernel/Memory/MemoryManager.h>
#include <Kernel/Memory/PhysicalAddress.h>

namespace Kernel::Memory {

// A Scatter-Gather List type that owns its buffers

class ScatterGatherList final : public AtomicRefCounted<ScatterGatherList> {
public:
    static ErrorOr<LockRefPtr<ScatterGatherList>> try_create(AsyncBlockDeviceRequest&, Span<NonnullRefPtr<PhysicalRAMPage>> allocated_pages, size_t device_block_size, StringView region_name);
    VMObject const& vmobject() const { return m_vm_object; }
    VirtualAddress dma_region() const { return m_dma_region->vaddr(); }
    size_t scatters_count() const { return m_vm_object->physical_pages().size(); }

private:
    ScatterGatherList(NonnullLockRefPtr<AnonymousVMObject>, NonnullOwnPtr<Region> dma_region);
    NonnullLockRefPtr<AnonymousVMObject> m_vm_object;
    NonnullOwnPtr<Region> m_dma_region;
};

}
