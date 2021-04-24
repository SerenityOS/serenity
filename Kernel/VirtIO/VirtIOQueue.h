/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/SpinLock.h>
#include <Kernel/VM/MemoryManager.h>
#include <Kernel/VM/ScatterGatherList.h>

namespace Kernel {

#define VIRTQ_DESC_F_NEXT 1
#define VIRTQ_DESC_F_INDIRECT 4

#define VIRTQ_AVAIL_F_NO_INTERRUPT 1
#define VIRTQ_USED_F_NO_NOTIFY 1

enum class BufferType {
    DeviceReadable = 0,
    DeviceWritable = 2
};

class VirtIODevice;
class VirtIOQueueChain;

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

    bool new_data_available() const;
    bool has_free_slots() const;
    Optional<u16> take_free_slot();
    VirtIOQueueChain pop_used_buffer_chain(size_t& used);
    void discard_used_buffers();

    SpinLock<u8>& lock() { return m_lock; }

    bool should_notify() const;

private:
    void reclaim_buffer_chain(u16 chain_start_index, u16 chain_end_index, size_t length_of_chain);

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
    OwnPtr<Region> m_queue_region;
    SpinLock<u8> m_lock;

    friend class VirtIOQueueChain;
};

class VirtIOQueueChain {
public:
    VirtIOQueueChain(VirtIOQueue& queue)
        : m_queue(queue)
    {
    }

    VirtIOQueueChain(VirtIOQueue& queue, u16 start_index, u16 end_index, size_t chain_length)
        : m_queue(queue)
        , m_start_of_chain_index(start_index)
        , m_end_of_chain_index(end_index)
        , m_chain_length(chain_length)
    {
    }

    VirtIOQueueChain(VirtIOQueueChain&& other)
        : m_queue(other.m_queue)
        , m_start_of_chain_index(other.m_start_of_chain_index)
        , m_end_of_chain_index(other.m_end_of_chain_index)
        , m_chain_length(other.m_chain_length)
        , m_chain_has_writable_pages(other.m_chain_has_writable_pages)
    {
        other.m_start_of_chain_index = {};
        other.m_end_of_chain_index = {};
        other.m_chain_length = 0;
        other.m_chain_has_writable_pages = false;
    }

    VirtIOQueueChain& operator=(VirtIOQueueChain&& other)
    {
        VERIFY(&m_queue == &other.m_queue);
        ensure_chain_is_empty();
        m_start_of_chain_index = other.m_start_of_chain_index;
        m_end_of_chain_index = other.m_end_of_chain_index;
        m_chain_length = other.m_chain_length;
        m_chain_has_writable_pages = other.m_chain_has_writable_pages;
        other.m_start_of_chain_index = {};
        other.m_end_of_chain_index = {};
        other.m_chain_length = 0;
        other.m_chain_has_writable_pages = false;
        return *this;
    }

    ~VirtIOQueueChain()
    {
        ensure_chain_is_empty();
    }

    [[nodiscard]] VirtIOQueue& queue() const { return m_queue; }
    [[nodiscard]] bool is_empty() const { return m_chain_length == 0; }
    [[nodiscard]] size_t length() const { return m_chain_length; }
    bool add_buffer_to_chain(PhysicalAddress buffer_start, size_t buffer_length, BufferType buffer_type);
    void submit_to_queue();
    void release_buffer_slots_to_queue();

    void for_each(Function<void(PhysicalAddress, size_t)> callback)
    {
        VERIFY(m_queue.lock().is_locked());
        if (!m_start_of_chain_index.has_value())
            return;
        auto index = m_start_of_chain_index.value();
        for (size_t i = 0; i < m_chain_length; ++i) {
            auto addr = m_queue.m_descriptors[index].address;
            auto length = m_queue.m_descriptors[index].length;
            callback(PhysicalAddress(addr), length);
            index = m_queue.m_descriptors[index].next;
        }
    }

private:
    void ensure_chain_is_empty() const
    {
        VERIFY(!m_start_of_chain_index.has_value());
        VERIFY(!m_end_of_chain_index.has_value());
        VERIFY(m_chain_length == 0);
    }

    VirtIOQueue& m_queue;
    Optional<u16> m_start_of_chain_index {};
    Optional<u16> m_end_of_chain_index {};
    size_t m_chain_length {};
    bool m_chain_has_writable_pages { false };
};

}
