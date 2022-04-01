/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <Kernel/Devices/BlockDevice.h>
#include <Kernel/Memory/AnonymousVMObject.h>
#include <Kernel/Memory/MemoryManager.h>
#include <Kernel/PhysicalAddress.h>

namespace Kernel::Memory {

// A Scatter-Gather List type that owns its buffers

class ScatterGatherList : public RefCounted<ScatterGatherList> {
public:
    static RefPtr<ScatterGatherList> try_create(AsyncBlockDeviceRequest&, Span<NonnullRefPtr<PhysicalPage>> allocated_pages, size_t device_block_size);
    VMObject const& vmobject() const { return m_vm_object; }
    VirtualAddress dma_region() const { return m_dma_region->vaddr(); }
    size_t scatters_count() const { return m_vm_object->physical_pages().size(); }

private:
    ScatterGatherList(NonnullRefPtr<AnonymousVMObject>, AsyncBlockDeviceRequest&, size_t device_block_size);
    NonnullRefPtr<AnonymousVMObject> m_vm_object;
    OwnPtr<Region> m_dma_region;
};

}
