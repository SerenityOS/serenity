/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefCounted.h>
#include <Kernel/Bus/VirtIO/VirtIO.h>
#include <Kernel/Devices/CharacterDevice.h>
#include <Kernel/Random.h>

namespace Kernel {

#define REQUESTQ 0

class VirtIORNG final
    : public RefCounted<VirtIORNG>
    , public VirtIODevice {
public:
    virtual StringView purpose() const override { return class_name(); }

    VirtIORNG(PCI::Address);
    virtual ~VirtIORNG() override;

private:
    virtual StringView class_name() const override { return "VirtIOConsole"; }
    virtual bool handle_device_config_change() override;
    virtual void handle_queue_update(u16 queue_index) override;
    void request_entropy_from_host();

    OwnPtr<Memory::Region> m_entropy_buffer;
    EntropySource m_entropy_source;
};

}
