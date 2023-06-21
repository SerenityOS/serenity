/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/AtomicRefCounted.h>
#include <Kernel/Bus/VirtIO/Device.h>
#include <Kernel/Devices/CharacterDevice.h>
#include <Kernel/Security/Random.h>

namespace Kernel::VirtIO {

#define REQUESTQ 0

class RNG final
    : public AtomicRefCounted<RNG>
    , public VirtIO::Device {
public:
    static NonnullLockRefPtr<RNG> must_create(PCI::DeviceIdentifier const&);
    virtual StringView purpose() const override { return class_name(); }
    virtual StringView device_name() const override { return class_name(); }
    virtual ~RNG() override = default;

    virtual ErrorOr<void> initialize_virtio_resources() override;

private:
    virtual StringView class_name() const override { return "VirtIORNG"sv; }
    explicit RNG(PCI::DeviceIdentifier const&);
    virtual bool handle_device_config_change() override;
    virtual void handle_queue_update(u16 queue_index) override;
    void request_entropy_from_host();

    OwnPtr<Memory::Region> m_entropy_buffer;
    EntropySource m_entropy_source;
};

}
