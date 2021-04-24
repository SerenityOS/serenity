/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Devices/CharacterDevice.h>
#include <Kernel/Random.h>
#include <Kernel/VirtIO/VirtIO.h>

namespace Kernel {

#define REQUESTQ 0

class VirtIORNG final : public CharacterDevice
    , public VirtIODevice {
public:
    virtual const char* class_name() const override { return m_class_name.characters(); }

    virtual bool can_read(const FileDescription&, size_t) const override { return false; }
    virtual KResultOr<size_t> read(FileDescription&, u64, UserOrKernelBuffer&, size_t) override { return 0; }
    virtual bool can_write(const FileDescription&, size_t) const override { return false; }
    virtual KResultOr<size_t> write(FileDescription&, u64, const UserOrKernelBuffer&, size_t) override { return 0; }

    virtual mode_t required_mode() const override { return 0666; }
    virtual String device_name() const override { return "hwrng"; }

    VirtIORNG(PCI::Address);
    virtual ~VirtIORNG() override;

private:
    virtual bool handle_device_config_change() override;
    virtual void handle_queue_update(u16 queue_index) override;
    void request_entropy_from_host();

    OwnPtr<Region> m_entropy_buffer;
    EntropySource m_entropy_source;
};

}
