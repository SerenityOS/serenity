/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Devices/CharacterDevice.h>

namespace Kernel {

class DeviceControlDevice final : public CharacterDevice {
    friend class DeviceManagement;

public:
    static NonnullRefPtr<DeviceControlDevice> must_create();
    virtual ~DeviceControlDevice() override;

private:
    DeviceControlDevice();

    // ^CharacterDevice
    virtual ErrorOr<void> ioctl(OpenFileDescription&, unsigned request, Userspace<void*> arg) override;
    virtual ErrorOr<size_t> read(OpenFileDescription&, u64, UserOrKernelBuffer&, size_t) override;
    virtual ErrorOr<size_t> write(OpenFileDescription&, u64, const UserOrKernelBuffer&, size_t) override { return Error::from_errno(ENOTSUP); }
    virtual bool can_read(const OpenFileDescription&, u64) const override;
    virtual bool can_write(const OpenFileDescription&, u64) const override { return false; }
    virtual StringView class_name() const override { return "DeviceControlDevice"sv; }
};

}
