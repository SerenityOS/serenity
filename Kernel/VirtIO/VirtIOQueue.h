/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Badge.h>
#include <Kernel/SpinLock.h>
#include <Kernel/VM/MemoryManager.h>
#include <Kernel/VM/ScatterGatherList.h>

namespace Kernel {

#define VIRTQ_DESC_F_NEXT 1
#define VIRTQ_DESC_F_INDIRECT 4

enum class BufferType {
    DeviceReadable = 0,
    DeviceWritable = 2
};

class VirtIODevice;

class VirtIOQueue {
public:
    VirtIOQueue(u16 queue_size, u16 notify_offset);
    ~VirtIOQueue();

    bool is_null() const { return !m_queue_region; }
    u16 notify_offset() const { return m_notify_offset; }

    void enable_interrupts();
    void disable_interrupts();

    PhysicalAddress descriptor_area() const { return to_physical(m_descriptors.ptr()); }
    PhysicalAddress driver_area() const { return to_physical(m_driver.ptr()); }
    PhysicalAddress device_area() const { return to_physical(m_device.ptr()); }

    bool supply_buffer(Badge<VirtIODevice>, const ScatterGatherList&, BufferType, void* token);
    bool new_data_available() const;
    bool can_write() const;
    void* get_buffer(size_t*);
    void discard_used_buffers();

private:
    void pop_buffer(u16 descriptor_index);

    PhysicalAddress to_physical(const void* ptr) const
    {
        auto offset = FlatPtr(ptr) - m_queue_region->vaddr().get();
        return m_queue_region->physical_page(0)->paddr().offset(offset);
    }
    struct [[gnu::packed]] VirtIOQueueDescriptor {
        u64 address;
        u32 length;
        u16 flags;
        u16 next;
    };

    struct [[gnu::packed]] VirtIOQueueDriver {
        u16 flags;
        u16 index;
        u16 rings[];
    };

    struct [[gnu::packed]] VirtIOQueueDeviceItem {
        u32 index;
        u32 length;
    };

    struct [[gnu::packed]] VirtIOQueueDevice {
        u16 flags;
        u16 index;
        VirtIOQueueDeviceItem rings[];
    };

    const u16 m_queue_size;
    const u16 m_notify_offset;
    u16 m_free_buffers;
    u16 m_free_head { 0 };
    u16 m_used_tail { 0 };
    u16 m_driver_index_shadow { 0 };

    OwnPtr<VirtIOQueueDescriptor> m_descriptors { nullptr };
    OwnPtr<VirtIOQueueDriver> m_driver { nullptr };
    OwnPtr<VirtIOQueueDevice> m_device { nullptr };
    Vector<void*> m_tokens;
    OwnPtr<Region> m_queue_region;
    SpinLock<u8> m_lock;
};

}
