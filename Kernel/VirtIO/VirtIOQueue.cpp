/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/StdLib.h>
#include <Kernel/VirtIO/VirtIOQueue.h>

namespace Kernel {

VirtIOQueue::VirtIOQueue(u16 queue_size, u16 notify_offset)
    : m_queue_size(queue_size)
    , m_notify_offset(notify_offset)
    , m_free_buffers(queue_size)
{
    size_t size_of_descriptors = sizeof(VirtIOQueueDescriptor) * queue_size;
    size_t size_of_driver = sizeof(VirtIOQueueDriver) + queue_size * sizeof(u16);
    size_t size_of_device = sizeof(VirtIOQueueDevice) + queue_size * sizeof(VirtIOQueueDeviceItem);
    m_queue_region = MM.allocate_contiguous_kernel_region(page_round_up(size_of_descriptors + size_of_driver + size_of_device), "VirtIO Queue", Region::Access::Read | Region::Access::Write);
    VERIFY(m_queue_region);
    // TODO: ensure alignment!!!
    u8* ptr = m_queue_region->vaddr().as_ptr();
    memset(ptr, 0, m_queue_region->size());
    m_descriptors = reinterpret_cast<VirtIOQueueDescriptor*>(ptr);
    m_driver = reinterpret_cast<VirtIOQueueDriver*>(ptr + size_of_descriptors);
    m_device = reinterpret_cast<VirtIOQueueDevice*>(ptr + size_of_descriptors + size_of_driver);
    m_tokens.resize(queue_size);

    for (auto i = 0; i < queue_size; i++) {
        m_descriptors[i].next = (i + 1) % queue_size; // link all of the descriptors in a circle
    }

    enable_interrupts();
}

VirtIOQueue::~VirtIOQueue()
{
}

void VirtIOQueue::enable_interrupts()
{
    m_driver->flags = 0;
}

void VirtIOQueue::disable_interrupts()
{
    m_driver->flags = 1;
}

bool VirtIOQueue::supply_buffer(Badge<VirtIODevice>, const ScatterGatherList& scatter_list, BufferType buffer_type, void* token)
{
    VERIFY(scatter_list.length() && scatter_list.length() <= m_free_buffers);
    m_free_buffers -= scatter_list.length();

    auto descriptor_index = m_free_head;
    auto last_index = descriptor_index;
    scatter_list.for_each_entry([&](auto paddr, auto size) {
        m_descriptors[descriptor_index].flags = static_cast<u16>(buffer_type) | VIRTQ_DESC_F_NEXT;
        m_descriptors[descriptor_index].address = static_cast<u64>(paddr);
        m_descriptors[descriptor_index].length = static_cast<u32>(size);
        last_index = descriptor_index;
        descriptor_index = m_descriptors[descriptor_index].next; // ensure we place the buffer in chain order
    });
    m_descriptors[last_index].flags &= ~(VIRTQ_DESC_F_NEXT); // last descriptor in chain doesn't have a next descriptor

    m_driver->rings[m_driver_index_shadow % m_queue_size] = m_free_head; // m_driver_index_shadow is used to prevent accesses to index before the rings are updated
    m_tokens[m_free_head] = token;
    m_free_head = descriptor_index;

    full_memory_barrier();

    m_driver_index_shadow++;
    m_driver->index++;

    full_memory_barrier();

    auto device_flags = m_device->flags;
    return !(device_flags & 1); // if bit 1 is enabled the device disabled interrupts
}

bool VirtIOQueue::new_data_available() const
{
    return m_device->index != m_used_tail;
}

void* VirtIOQueue::get_buffer(size_t* size)
{
    if (!new_data_available()) {
        *size = 0;
        return nullptr;
    }

    full_memory_barrier();

    auto descriptor_index = m_device->rings[m_used_tail % m_queue_size].index;
    *size = m_device->rings[m_used_tail % m_queue_size].length;

    m_used_tail++;

    auto token = m_tokens[descriptor_index];
    pop_buffer(descriptor_index);
    return token;
}

void VirtIOQueue::discard_used_buffers()
{
    size_t size;
    while (!get_buffer(&size)) {
    }
}

void VirtIOQueue::pop_buffer(u16 descriptor_index)
{
    m_tokens[descriptor_index] = nullptr;

    auto i = descriptor_index;
    while (m_descriptors[i].flags & VIRTQ_DESC_F_NEXT) {
        m_free_buffers++;
        i = m_descriptors[i].next;
    }
    m_free_buffers++; // the last descriptor in the chain doesn't have the NEXT flag

    m_descriptors[i].next = m_free_head; // empend the popped descriptors to the free chain
    m_free_head = descriptor_index;
}

bool VirtIOQueue::can_write() const
{
    return m_free_buffers > 0;
}

}
