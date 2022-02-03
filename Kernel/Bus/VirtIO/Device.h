/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullOwnPtrVector.h>
#include <Kernel/Arch/x86/IO.h>
#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Bus/PCI/Device.h>
#include <Kernel/Bus/VirtIO/Queue.h>
#include <Kernel/Interrupts/IRQHandler.h>
#include <Kernel/Memory/MemoryManager.h>

namespace Kernel {

#define REG_DEVICE_FEATURES 0x0
#define REG_GUEST_FEATURES 0x4
#define REG_QUEUE_ADDRESS 0x8
#define REG_QUEUE_SIZE 0xc
#define REG_QUEUE_SELECT 0xe
#define REG_QUEUE_NOTIFY 0x10
#define REG_DEVICE_STATUS 0x12
#define REG_ISR_STATUS 0x13

#define DEVICE_STATUS_ACKNOWLEDGE (1 << 0)
#define DEVICE_STATUS_DRIVER (1 << 1)
#define DEVICE_STATUS_DRIVER_OK (1 << 2)
#define DEVICE_STATUS_FEATURES_OK (1 << 3)
#define DEVICE_STATUS_DEVICE_NEEDS_RESET (1 << 6)
#define DEVICE_STATUS_FAILED (1 << 7)

#define VIRTIO_F_INDIRECT_DESC ((u64)1 << 28)
#define VIRTIO_F_VERSION_1 ((u64)1 << 32)
#define VIRTIO_F_RING_PACKED ((u64)1 << 34)
#define VIRTIO_F_IN_ORDER ((u64)1 << 35)

#define VIRTIO_PCI_CAP_COMMON_CFG 1
#define VIRTIO_PCI_CAP_NOTIFY_CFG 2
#define VIRTIO_PCI_CAP_ISR_CFG 3
#define VIRTIO_PCI_CAP_DEVICE_CFG 4
#define VIRTIO_PCI_CAP_PCI_CFG 5

// virtio_pci_common_cfg
#define COMMON_CFG_DEVICE_FEATURE_SELECT 0x0
#define COMMON_CFG_DEVICE_FEATURE 0x4
#define COMMON_CFG_DRIVER_FEATURE_SELECT 0x8
#define COMMON_CFG_DRIVER_FEATURE 0xc
#define COMMON_CFG_MSIX_CONFIG 0x10
#define COMMON_CFG_NUM_QUEUES 0x12
#define COMMON_CFG_DEVICE_STATUS 0x14
#define COMMON_CFG_CONFIG_GENERATION 0x15
#define COMMON_CFG_QUEUE_SELECT 0x16
#define COMMON_CFG_QUEUE_SIZE 0x18
#define COMMON_CFG_QUEUE_MSIX_VECTOR 0x1a
#define COMMON_CFG_QUEUE_ENABLE 0x1c
#define COMMON_CFG_QUEUE_NOTIFY_OFF 0x1e
#define COMMON_CFG_QUEUE_DESC 0x20
#define COMMON_CFG_QUEUE_DRIVER 0x28
#define COMMON_CFG_QUEUE_DEVICE 0x30

#define QUEUE_INTERRUPT 0x1
#define DEVICE_CONFIG_INTERRUPT 0x2

namespace VirtIO {

enum class ConfigurationType : u8 {
    Common = 1,
    Notify = 2,
    ISR = 3,
    Device = 4,
    PCI = 5
};

struct Configuration {
    ConfigurationType cfg_type;
    u8 bar;
    u32 offset;
    u32 length;
};

void detect();

class Device
    : public PCI::Device
    , public IRQHandler {
public:
    virtual ~Device() override = default;

    virtual void initialize();

protected:
    virtual StringView class_name() const { return "VirtIO::Device"sv; }
    explicit Device(PCI::DeviceIdentifier const&);
    struct MappedMMIO {
        OwnPtr<Memory::Region> base;
        size_t size { 0 };

        template<typename T>
        T read(u32 offset) const
        {
            if (!base)
                return 0;
            VERIFY(size >= sizeof(T));
            VERIFY(offset + sizeof(T) <= size);
            return *(volatile T*)(base->vaddr().offset(offset).get());
        }

        template<typename T>
        void write(u32 offset, T value)
        {
            if (!base)
                return;
            VERIFY(size >= sizeof(T));
            VERIFY(offset + sizeof(T) <= size);
            *(volatile T*)(base->vaddr().offset(offset).get()) = value;
        }
    };

    const Configuration* get_config(ConfigurationType cfg_type, u32 index = 0) const
    {
        for (auto const& cfg : m_configs) {
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

    template<typename F>
    void read_config_atomic(F f)
    {
        if (m_common_cfg) {
            u8 generation_before, generation_after;
            do {
                generation_before = config_read8(*m_common_cfg, 0x15);
                f();
                generation_after = config_read8(*m_common_cfg, 0x15);
            } while (generation_before != generation_after);
        } else {
            f();
        }
    }

    u8 config_read8(const Configuration&, u32);
    u16 config_read16(const Configuration&, u32);
    u32 config_read32(const Configuration&, u32);
    void config_write8(const Configuration&, u32, u8);
    void config_write16(const Configuration&, u32, u16);
    void config_write32(const Configuration&, u32, u32);
    void config_write64(const Configuration&, u32, u64);

    auto mapping_for_bar(u8) -> MappedMMIO&;

    u8 read_status_bits();
    void mask_status_bits(u8 status_mask);
    void set_status_bit(u8);
    u64 get_device_features();
    bool setup_queues(u16 requested_queue_count = 0);
    void finish_init();

    Queue& get_queue(u16 queue_index)
    {
        VERIFY(queue_index < m_queue_count);
        return m_queues[queue_index];
    }

    const Queue& get_queue(u16 queue_index) const
    {
        VERIFY(queue_index < m_queue_count);
        return m_queues[queue_index];
    }

    template<typename F>
    bool negotiate_features(F f)
    {
        u64 device_features = get_device_features();
        u64 accept_features = f(device_features);
        VERIFY(!(~device_features & accept_features));
        return accept_device_features(device_features, accept_features);
    }

    static bool is_feature_set(u64 feature_set, u64 test_feature)
    {
        // features can have more than one bit
        return (feature_set & test_feature) == test_feature;
    }
    bool is_feature_accepted(u64 feature) const
    {
        VERIFY(m_did_accept_features);
        return is_feature_set(m_accepted_features, feature);
    }

    void supply_chain_and_notify(u16 queue_index, QueueChain& chain);

    virtual bool handle_device_config_change() = 0;
    virtual void handle_queue_update(u16 queue_index) = 0;

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

    bool accept_device_features(u64 device_features, u64 accepted_features);

    bool setup_queue(u16 queue_index);
    bool activate_queue(u16 queue_index);
    void notify_queue(u16 queue_index);

    void reset_device();

    u8 isr_status();
    virtual bool handle_irq(const RegisterState&) override;

    NonnullOwnPtrVector<Queue> m_queues;
    Vector<Configuration> m_configs;
    const Configuration* m_common_cfg { nullptr }; // Cached due to high usage
    const Configuration* m_notify_cfg { nullptr }; // Cached due to high usage
    const Configuration* m_isr_cfg { nullptr };    // Cached due to high usage

    IOAddress m_io_base;
    MappedMMIO m_mmio[6];

    StringView const m_class_name;

    u16 m_queue_count { 0 };
    bool m_use_mmio { false };
    u8 m_status { 0 };
    u64 m_accepted_features { 0 };
    bool m_did_accept_features { false };
    bool m_did_setup_queues { false };
    u32 m_notify_multiplier { 0 };
};
};
}
