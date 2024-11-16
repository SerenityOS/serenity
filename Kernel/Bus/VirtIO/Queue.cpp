/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Atomic.h>
#include <Kernel/Bus/VirtIO/Queue.h>
#include <Kernel/Library/MiniStdLib.h>

namespace Kernel::VirtIO {

ErrorOr<NonnullOwnPtr<Queue>> Queue::try_create(u16 queue_size, u16 notify_offset)
{
    size_t size_of_descriptors = sizeof(QueueDescriptor) * queue_size;
    size_t size_of_driver = sizeof(QueueDriver) + queue_size * sizeof(u16);
    size_t size_of_device = sizeof(QueueDevice) + queue_size * sizeof(QueueDeviceItem);
    auto queue_region_size = TRY(Memory::page_round_up(size_of_descriptors + size_of_driver + size_of_device));
    OwnPtr<Memory::Region> queue_region;
    if (queue_region_size <= PAGE_SIZE)
        queue_region = TRY(MM.allocate_kernel_region(queue_region_size, "VirtIO Queue"sv, Memory::Region::Access::ReadWrite));
    else
        queue_region = TRY(MM.allocate_contiguous_kernel_region(queue_region_size, "VirtIO Queue"sv, Memory::Region::Access::ReadWrite));
    return adopt_nonnull_own_or_enomem(new (nothrow) Queue(queue_region.release_nonnull(), queue_size, notify_offset));
}

Queue::Queue(NonnullOwnPtr<Memory::Region> queue_region, u16 queue_size, u16 notify_offset)
    : m_queue_size(queue_size)
    , m_notify_offset(notify_offset)
    , m_free_buffers(queue_size)
    , m_queue_region(move(queue_region))
{
    size_t size_of_descriptors = sizeof(QueueDescriptor) * queue_size;
    size_t size_of_driver = sizeof(QueueDriver) + queue_size * sizeof(u16);
    // TODO: ensure alignment!!!
    u8* ptr = m_queue_region->vaddr().as_ptr();
    memset(ptr, 0, m_queue_region->size());
    m_descriptors = reinterpret_cast<QueueDescriptor*>(ptr);
    m_driver = reinterpret_cast<QueueDriver*>(ptr + size_of_descriptors);
    m_device = reinterpret_cast<QueueDevice*>(ptr + size_of_descriptors + size_of_driver);

    for (auto i = 0; i + 1 < queue_size; i++)
        m_descriptors[i].next = i + 1; // link all the descriptors in a line

    enable_interrupts();
}

Queue::~Queue() = default;

void Queue::enable_interrupts()
{
    SpinlockLocker lock(m_lock);
    m_driver->flags = 0;
}

void Queue::disable_interrupts()
{
    SpinlockLocker lock(m_lock);
    m_driver->flags = 1;
}

bool Queue::new_data_available() const
{
    auto const index = AK::atomic_load(&m_device->index, AK::MemoryOrder::memory_order_relaxed);
    auto const used_tail = AK::atomic_load(&m_used_tail, AK::MemoryOrder::memory_order_relaxed);
    return index != used_tail;
}

QueueChain Queue::pop_used_buffer_chain(size_t& used)
{
    VERIFY(m_lock.is_locked());
    if (!new_data_available()) {
        used = 0;
        return QueueChain(*this);
    }

    full_memory_barrier();

    // Determine used length
    used = m_device->rings[m_used_tail % m_queue_size].length;

    // Determine start, end and number of nodes in chain
    auto descriptor_index = m_device->rings[m_used_tail % m_queue_size].index;
    size_t length_of_chain = 1;
    auto last_index = descriptor_index;
    while (m_descriptors[last_index].flags & VIRTQ_DESC_F_NEXT) {
        ++length_of_chain;
        last_index = m_descriptors[last_index].next;
    }

    // We are now done with this buffer chain
    m_used_tail++;

    return QueueChain(*this, descriptor_index, last_index, length_of_chain);
}

void Queue::discard_used_buffers()
{
    VERIFY(m_lock.is_locked());
    size_t used;
    for (auto buffer = pop_used_buffer_chain(used); !buffer.is_empty(); buffer = pop_used_buffer_chain(used)) {
        buffer.release_buffer_slots_to_queue();
    }
}

void Queue::reclaim_buffer_chain(u16 chain_start_index, u16 chain_end_index, size_t length_of_chain)
{
    VERIFY(m_lock.is_locked());
    m_descriptors[chain_end_index].next = m_free_head;
    m_free_head = chain_start_index;
    m_free_buffers += length_of_chain;
}

bool Queue::has_free_slots() const
{
    auto const free_buffers = AK::atomic_load(&m_free_buffers, AK::MemoryOrder::memory_order_relaxed);
    return free_buffers > 0;
}

Optional<u16> Queue::take_free_slot()
{
    VERIFY(m_lock.is_locked());
    if (has_free_slots()) {
        auto descriptor_index = m_free_head;
        m_free_head = m_descriptors[descriptor_index].next;
        --m_free_buffers;
        return descriptor_index;
    }

    return {};
}

bool Queue::should_notify() const
{
    VERIFY(m_lock.is_locked());
    auto device_flags = m_device->flags;
    return !(device_flags & VIRTQ_USED_F_NO_NOTIFY);
}

bool QueueChain::add_buffer_to_chain(PhysicalAddress buffer_start, size_t buffer_length, BufferType buffer_type)
{
    VERIFY(m_queue.lock().is_locked());

    // Ensure that no readable pages will be inserted after a writable one, as required by the VirtIO spec
    VERIFY(buffer_type == BufferType::DeviceWritable || !m_chain_has_writable_pages);
    m_chain_has_writable_pages |= (buffer_type == BufferType::DeviceWritable);

    // Take a free slot from the queue
    auto descriptor_index = m_queue.take_free_slot();
    if (!descriptor_index.has_value())
        return false;

    if (!m_start_of_chain_index.has_value()) {
        // Set start of chain if it hasn't been set
        m_start_of_chain_index = descriptor_index.value();
    } else {
        // Link from previous element in QueueChain
        m_queue.m_descriptors[m_end_of_chain_index.value()].flags |= VIRTQ_DESC_F_NEXT;
        m_queue.m_descriptors[m_end_of_chain_index.value()].next = descriptor_index.value();
    }

    // Update end of chain
    m_end_of_chain_index = descriptor_index.value();
    ++m_chain_length;

    // Populate buffer info
    VERIFY(buffer_length <= NumericLimits<size_t>::max());
    m_queue.m_descriptors[descriptor_index.value()].address = static_cast<u64>(buffer_start.get());
    m_queue.m_descriptors[descriptor_index.value()].flags = static_cast<u16>(buffer_type);
    m_queue.m_descriptors[descriptor_index.value()].length = static_cast<u32>(buffer_length);

    return true;
}

void QueueChain::submit_to_queue()
{
    VERIFY(m_queue.lock().is_locked());
    VERIFY(m_start_of_chain_index.has_value());

    auto next_index = m_queue.m_driver_index_shadow % m_queue.m_queue_size;
    m_queue.m_driver->rings[next_index] = m_start_of_chain_index.value();
    m_queue.m_driver_index_shadow++;
    full_memory_barrier();
    m_queue.m_driver->index = m_queue.m_driver_index_shadow;

    // Reset internal chain state
    m_start_of_chain_index = m_end_of_chain_index = {};
    m_chain_has_writable_pages = false;
    m_chain_length = 0;
}

void QueueChain::release_buffer_slots_to_queue()
{
    VERIFY(m_queue.lock().is_locked());
    if (m_start_of_chain_index.has_value()) {
        // Add the currently stored chain back to the queue's free pool
        m_queue.reclaim_buffer_chain(m_start_of_chain_index.value(), m_end_of_chain_index.value(), m_chain_length);
        // Reset internal chain state
        m_start_of_chain_index = m_end_of_chain_index = {};
        m_chain_has_writable_pages = false;
        m_chain_length = 0;
    }
}

}
