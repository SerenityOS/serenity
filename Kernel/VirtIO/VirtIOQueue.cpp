/*
 * Copyright (c) 2020, the SerenityOS developers.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
    m_region = MM.allocate_contiguous_kernel_region(PAGE_ROUND_UP(size_of_descriptors + size_of_driver + size_of_device), "VirtIO Queue", Region::Access::Read | Region::Access::Write);
    if (m_region) {
        // TODO: ensure alignment!!!
        u8* ptr = m_region->vaddr().as_ptr();
        memset(ptr, 0, m_region->size());
        m_descriptors = reinterpret_cast<VirtIOQueueDescriptor*>(ptr);
        m_driver = reinterpret_cast<VirtIOQueueDriver*>(ptr + size_of_descriptors);
        m_device = reinterpret_cast<VirtIOQueueDevice*>(ptr + size_of_descriptors + size_of_driver);

        enable_interrupts();
    }
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

bool VirtIOQueue::supply_buffer(const u8* buffer, u32 len, BufferType buffer_type)
{
    ASSERT(buffer && len > 0);
    ASSERT(m_free_buffers > 0);

    auto descriptor_index = m_free_head;
    m_descriptors[descriptor_index].flags = static_cast<u16>(buffer_type);
    m_descriptors[descriptor_index].address = reinterpret_cast<u64>(buffer);
    m_descriptors[descriptor_index].length = len;

    m_free_buffers--;
    m_free_head = (m_free_head + 1) % m_queue_size;

    m_driver->rings[m_driver->index % m_queue_size] = descriptor_index;

    memory_barrier();

    m_driver->index++;

    memory_barrier();

    auto device_flags = m_device->flags;
    klog() << "VirtIODevice: supplied buffer... should notify: " << device_flags;
    return device_flags & 1;
}
bool VirtIOQueue::new_data_available() const
{
    return m_device->index != m_used_tail;
}
bool VirtIOQueue::handle_interrupt()
{
    if (!new_data_available())
        return false;

    if (on_data_available)
        on_data_available();
    return true;
}

}
