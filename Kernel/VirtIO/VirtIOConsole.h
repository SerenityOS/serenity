/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Devices/CharacterDevice.h>
#include <Kernel/VirtIO/VirtIO.h>

namespace Kernel {

#define VIRTIO_CONSOLE_PCI_DEVICE_ID 0x1003

#define VIRTIO_CONSOLE_F_SIZE (1 << 0)
#define VIRTIO_CONSOLE_F_MULTIPORT (1 << 1)
#define VIRTIO_CONSOLE_F_EMERG_WRITE (1 << 2)

#define RECEIVEQ 0
#define TRANSMITQ 1

class VirtIOConsole final : public CharacterDevice
    , public VirtIODevice {
public:
    VirtIOConsole(PCI::Address);
    virtual ~VirtIOConsole() override;

private:
    virtual const char* class_name() const override { return m_class_name.characters(); }

    virtual bool can_read(const FileDescription&, size_t) const override;
    virtual KResultOr<size_t> read(FileDescription&, u64, UserOrKernelBuffer&, size_t) override;
    virtual bool can_write(const FileDescription&, size_t) const override;
    virtual KResultOr<size_t> write(FileDescription&, u64, const UserOrKernelBuffer&, size_t) override;

    virtual mode_t required_mode() const override { return 0666; }

    virtual bool handle_device_config_change() override;
    virtual String device_name() const override { return String::formatted("hvc{}", minor()); }
    virtual void handle_queue_update(u16 queue_index) override;

    OwnPtr<Region> m_receive_region;
    OwnPtr<Region> m_transmit_region;

    static unsigned next_device_id;
};

}
