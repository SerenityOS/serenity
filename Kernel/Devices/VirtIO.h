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

#include <AK/NonnullOwnPtrVector.h>
#include <Kernel/IO.h>
#include <Kernel/Interrupts/IRQHandler.h>
#include <Kernel/PCI/Access.h>
#include <Kernel/PCI/Device.h>
#include <Kernel/SpinLock.h>
#include <Kernel/VM/MemoryManager.h>

namespace Kernel {

class VirtIO {
public:
    static void detect();
};

class VirtIOQueue {
public:
    VirtIOQueue(u16 queue_size);
    ~VirtIOQueue();

    bool is_null() const { return !m_region; }

    void enable_interrupts();
    void disable_interrupts();

    PhysicalAddress descriptor_area() const { return to_physical(m_descriptors); }
    PhysicalAddress driver_area() const { return to_physical(m_driver); }
    PhysicalAddress device_area() const { return to_physical(m_device); }

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
    VirtIOQueueDescriptor* m_descriptors { nullptr };
    VirtIOQueueDriver* m_driver { nullptr };
    VirtIOQueueDevice* m_device { nullptr };
    OwnPtr<Region> m_region;
    SpinLock<u8> m_lock;
};

class VirtIODevice: public PCI::Device {
public:
    VirtIODevice(PCI::Address, u8 irq, const char*);
    virtual ~VirtIODevice() override;

protected:
    const char* const m_class_name;

    struct MappedMMIO
    {
        OwnPtr<Region> base;
        size_t size { 0 };

        template<typename T>
        T read(u32 offset) const
        {
            if (!base)
                return 0;
            ASSERT(size >= sizeof(T));
            ASSERT(offset + sizeof(T) <= size);
            return *(volatile T*)(base->vaddr().offset(offset).get());
        }

        template<typename T>
        void write(u32 offset, T value)
        {
            if (!base)
                return;
            ASSERT(size >= sizeof(T));
            ASSERT(offset + sizeof(T) <= size);
            *(volatile T*)(base->vaddr().offset(offset).get()) = value;
        }
    };

    struct Configuration
    {
        u8 cfg_type;
        u8 bar;
        u32 offset;
        u32 length;
    };

    const Configuration* get_config(u8 cfg_type, u32 index = 0) const
    {
        for (const auto& cfg : m_config) {
            if (cfg.cfg_type != cfg_type)
                continue;
            if (index > 0) {
                index--;
                continue;
            }
            return &cfg;
        }
        return nullptr;
    }
    const Configuration* get_common_config(u32 index = 0) const;
    const Configuration* get_device_config(u32 index = 0) const;

    template<typename F>
    void read_config_atomic(F f)
    {
        if (m_common_cfg) {
            u8 generation_before, generation_after;
            do {
                generation_before = config_read8(m_common_cfg, 0x15);
                f();
                generation_after = config_read8(m_common_cfg, 0x15);
            } while (generation_before != generation_after);
        } else {
            f();
        }
    }

    u8 config_read8(const Configuration*, u32);
    u16 config_read16(const Configuration*, u32);
    u32 config_read32(const Configuration*, u32);
    void config_write8(const Configuration*, u32, u8);
    void config_write16(const Configuration*, u32, u16);
    void config_write32(const Configuration*, u32, u32);
    void config_write64(const Configuration*, u32, u64);

    u8 read_status_bits();
    void clear_status_bit(u8);
    void set_status_bit(u8);
    u32 get_device_features();
    bool finish_init();

    static bool is_feature_set(u32 feature_set, u32 test_feature)
    {
        // features can have more than one bit
        return (feature_set & test_feature) == test_feature;
    }

    VirtIOQueue* get_queue(u16 queue_index)
    {
        return &m_queues[queue_index];
    }

    template<typename F>
    bool negotiate_features(F f)
    {
        u32 device_features = get_device_features();
        u32 accept_features = f(device_features);
        ASSERT(!(~device_features & accept_features));
        return accept_device_features(device_features, accept_features);
    }

    bool is_feature_accepted(u32 feature) const
    {
        ASSERT(m_did_accept_features);
        return is_feature_set(m_accepted_features, feature);
    }

    void reset_device();

    auto mapping_for_bar(u8) -> MappedMMIO&;

private:
    template<typename T>
    void out(u16 address, T value)
    {
        m_io_base.offset(address).out(value);
    }

    template<typename T>
    T in(u16 address)
    {
        return m_io_base.offset(address).in<T>();
    }

    bool accept_device_features(u32 device_features, u32 accepted_features);
    bool setup_queues();
    bool setup_queue(u16 queue_index);

    NonnullOwnPtrVector<VirtIOQueue> m_queues;
    Vector<Configuration> m_config;
    const Configuration* m_common_cfg { nullptr };

    IOAddress m_io_base;
    MappedMMIO m_mmio[6];
    u16 m_queue_count { 0 };
    bool m_use_mmio { false };
    u8 m_status { 0 };
    u8 m_accepted_features { 0 };
    bool m_did_accept_features { false };
};

}
