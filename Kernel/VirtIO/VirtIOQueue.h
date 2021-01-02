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

#pragma once

#include <Kernel/SpinLock.h>
#include <Kernel/VM/MemoryManager.h>

namespace Kernel {

enum class BufferType {
    DeviceReadable = 0,
    DeviceWritable = 1
};

class VirtIOQueue {
public:
    VirtIOQueue(u16 queue_size, u16 notify_offset);
    ~VirtIOQueue();

    bool is_null() const { return !m_region; }
    u16 notify_offset() const { return m_notify_offset; }

    void enable_interrupts();
    void disable_interrupts();

    PhysicalAddress descriptor_area() const { return to_physical(m_descriptors); }
    PhysicalAddress driver_area() const { return to_physical(m_driver); }
    PhysicalAddress device_area() const { return to_physical(m_device); }

    bool supply_buffer(const u8* buffer, u32 len, BufferType);
    bool new_data_available() const;

    bool handle_interrupt();
    Function<void()> on_data_available;

private:
    PhysicalAddress to_physical(void* ptr) const
    {
        auto offset = FlatPtr(ptr) - m_region->vaddr().get();
        return m_region->physical_page(0)->paddr().offset(offset);
    }
    struct VirtIOQueueDescriptor {
        u64 address;
        u32 length;
        u16 flags;
        u16 next;
    };

    struct VirtIOQueueDriver {
        u16 flags;
        u16 index;
        u16 rings[];
    };

    struct VirtIOQueueDeviceItem {
        u32 index;
        u32 length;
    };

    struct VirtIOQueueDevice {
        u16 flags;
        u16 index;
        VirtIOQueueDeviceItem rings[];
    };

    const u16 m_queue_size;
    const u16 m_notify_offset;
    u16 m_free_buffers;
    u16 m_free_head { 0 };
    u16 m_used_tail { 0 };

    VirtIOQueueDescriptor* m_descriptors { nullptr };
    VirtIOQueueDriver* m_driver { nullptr };
    VirtIOQueueDevice* m_device { nullptr };
    OwnPtr<Region> m_region;
    SpinLock<u8> m_lock;
};

}
