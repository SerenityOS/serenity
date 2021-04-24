/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <Kernel/Devices/BlockDevice.h>
#include <Kernel/PhysicalAddress.h>
#include <Kernel/VM/AnonymousVMObject.h>
#include <Kernel/VM/MemoryManager.h>

namespace Kernel {

/// A Scatter-Gather List type that owns its buffers

class ScatterGatherList : public RefCounted<ScatterGatherList> {
public:
    static NonnullRefPtr<ScatterGatherList> create(AsyncBlockDeviceRequest&, NonnullRefPtrVector<PhysicalPage> allocated_pages, size_t device_block_size);
    const VMObject& vmobject() const { return m_vm_object; }
    VirtualAddress dma_region() const { return m_dma_region->vaddr(); }
    size_t scatters_count() const { return m_vm_object->physical_pages().size(); }

private:
    ScatterGatherList(AsyncBlockDeviceRequest&, NonnullRefPtrVector<PhysicalPage> allocated_pages, size_t device_block_size);
    NonnullRefPtr<AnonymousVMObject> m_vm_object;
    OwnPtr<Region> m_dma_region;
};

/// A Scatter-Gather List type that doesn't own its buffers

class ScatterGatherRefList {
    struct ScatterGatherRef {
        FlatPtr page_base;
        size_t offset;
        size_t length;
    };

public:
    static ScatterGatherRefList create_from_buffer(const u8* buffer, size_t);
    static ScatterGatherRefList create_from_physical(PhysicalAddress, size_t);

    void add_entry(FlatPtr, size_t offset, size_t size);
    [[nodiscard]] size_t length() const { return m_entries.size(); }

    void for_each_entry(Function<void(const FlatPtr, const size_t)> callback) const;

private:
    Vector<ScatterGatherRef> m_entries;
};

}
