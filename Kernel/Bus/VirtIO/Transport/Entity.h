/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/SetOnce.h>
#include <AK/Types.h>
#include <Kernel/Bus/VirtIO/Definitions.h>
#include <Kernel/Bus/VirtIO/Queue.h>
#include <Kernel/Library/IOWindow.h>

namespace Kernel::VirtIO {

class TransportEntity {
public:
    virtual ~TransportEntity() = default;

    virtual ErrorOr<void> locate_configurations_and_resources(Badge<VirtIO::Device>, VirtIO::Device&) = 0;
    virtual void disable_interrupts(Badge<VirtIO::Device>) = 0;
    virtual void enable_interrupts(Badge<VirtIO::Device>) = 0;

    virtual StringView determine_device_class_name() const = 0;

    void accept_device_features(Badge<VirtIO::Device>, u64 accepted_features);

    struct NotifyQueueDescriptor {
        u16 queue_index;
        u16 possible_notify_offset;
    };
    void notify_queue(Badge<VirtIO::Device>, NotifyQueueDescriptor);
    ErrorOr<void> activate_queue(Badge<VirtIO::Device>, u16 queue_index);
    ErrorOr<NonnullOwnPtr<Queue>> setup_queue(Badge<VirtIO::Device>, u16 queue_index);
    void set_status_bits(Badge<VirtIO::Device>, u8 status_bits);
    void reset_device(Badge<VirtIO::Device>);

    u8 read_status_bits();
    u8 isr_status();
    u64 get_device_features();

    ErrorOr<Configuration const*> get_config(ConfigurationType cfg_type, u32 index = 0) const
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
        return Error::from_errno(ENXIO);
    }

    u8 config_read8(Configuration const&, u32);
    u16 config_read16(Configuration const&, u32);
    u32 config_read32(Configuration const&, u32);
    void config_write8(Configuration const&, u32, u8);
    void config_write16(Configuration const&, u32, u16);
    void config_write32(Configuration const&, u32, u32);
    void config_write64(Configuration const&, u32, u64);

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

protected:
    TransportEntity() = default;

    auto mapping_for_resource_index(u8) -> IOWindow&;

    void set_status_bits(u8 status_bits);

    Vector<Configuration> m_configs;
    Configuration const* m_common_cfg { nullptr }; // Cached due to high usage
    Configuration const* m_notify_cfg { nullptr }; // Cached due to high usage
    Configuration const* m_isr_cfg { nullptr };    // Cached due to high usage

    IOWindow& base_io_window();
    Array<OwnPtr<IOWindow>, 6> m_register_bases;
    SetOnce m_use_mmio;

    u32 m_notify_multiplier { 0 };
};

};
