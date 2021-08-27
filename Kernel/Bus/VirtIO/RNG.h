/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefCounted.h>
#include <Kernel/Bus/VirtIO/Device.h>
#include <Kernel/Devices/CharacterDevice.h>
#include <Kernel/Random.h>

namespace Kernel::VirtIO {

#define REQUESTQ 0

class RNG final
    : public RefCounted<RNG>
    , public VirtIO::Device {
public:
    static NonnullRefPtr<RNG> must_create(PCI::Address address);
    virtual StringView purpose() const override { return class_name(); }
    virtual ~RNG() override;

private:
    virtual StringView class_name() const override { return "VirtIOConsole"; }
    explicit RNG(PCI::Address);
    virtual bool handle_device_config_change() override;
    virtual void handle_queue_update(u16 queue_index) override;
    void request_entropy_from_host();

    OwnPtr<Memory::Region> m_entropy_buffer;
    EntropySource m_entropy_source;
};

}
