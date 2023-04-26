/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Bus/PCI/Device.h>
#include <Kernel/Bus/VirtIO/Definitions.h>
#include <Kernel/Bus/VirtIO/Queue.h>
#include <Kernel/IOWindow.h>
#include <Kernel/Interrupts/IRQHandler.h>
#include <Kernel/Memory/MemoryManager.h>

namespace Kernel::VirtIO {

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

    Configuration const* get_config(ConfigurationType cfg_type, u32 index = 0) const
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

    u8 config_read8(Configuration const&, u32);
    u16 config_read16(Configuration const&, u32);
    u32 config_read32(Configuration const&, u32);
    void config_write8(Configuration const&, u32, u8);
    void config_write16(Configuration const&, u32, u16);
    void config_write32(Configuration const&, u32, u32);
    void config_write64(Configuration const&, u32, u64);

    auto mapping_for_bar(u8) -> IOWindow&;

    u8 read_status_bits();
    void mask_status_bits(u8 status_mask);
    void set_status_bit(u8);
    u64 get_device_features();
    bool setup_queues(u16 requested_queue_count = 0);
    void finish_init();

    Queue& get_queue(u16 queue_index)
    {
        VERIFY(queue_index < m_queue_count);
        return *m_queues[queue_index];
    }

    Queue const& get_queue(u16 queue_index) const
    {
        VERIFY(queue_index < m_queue_count);
        return *m_queues[queue_index];
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
    bool accept_device_features(u64 device_features, u64 accepted_features);

    bool setup_queue(u16 queue_index);
    bool activate_queue(u16 queue_index);
    void notify_queue(u16 queue_index);

    void reset_device();

    u8 isr_status();
    virtual bool handle_irq(RegisterState const&) override;

    Vector<NonnullOwnPtr<Queue>> m_queues;
    Vector<Configuration> m_configs;
    Configuration const* m_common_cfg { nullptr }; // Cached due to high usage
    Configuration const* m_notify_cfg { nullptr }; // Cached due to high usage
    Configuration const* m_isr_cfg { nullptr };    // Cached due to high usage

    IOWindow& base_io_window();
    Array<OwnPtr<IOWindow>, 6> m_register_bases;

    StringView const m_class_name;

    u16 m_queue_count { 0 };
    bool m_use_mmio { false };
    u8 m_status { 0 };
    u64 m_accepted_features { 0 };
    bool m_did_accept_features { false };
    bool m_did_setup_queues { false };
    u32 m_notify_multiplier { 0 };
};

}
