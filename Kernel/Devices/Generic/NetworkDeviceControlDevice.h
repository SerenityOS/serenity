/*
 * Copyright (c) 2024, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/Devices/CharacterDevice.h>

namespace Kernel {

class NetworkDeviceControlDevice final : public CharacterDevice {
    friend class DeviceManagement;

public:
    static NonnullLockRefPtr<NetworkDeviceControlDevice> must_create();
    virtual ~NetworkDeviceControlDevice() override;

private:
    NetworkDeviceControlDevice();

    // ^CharacterDevice
    virtual ErrorOr<void> ioctl(OpenFileDescription&, unsigned request, Userspace<void*> arg) override;
    virtual ErrorOr<size_t> read(OpenFileDescription&, u64, UserOrKernelBuffer&, size_t) override;
    virtual ErrorOr<size_t> write(OpenFileDescription&, u64, UserOrKernelBuffer const&, size_t) override { return Error::from_errno(ENOTSUP); }
    virtual bool can_read(OpenFileDescription const&, u64) const override;
    virtual bool can_write(OpenFileDescription const&, u64) const override { return false; }
    virtual StringView class_name() const override { return "NetworkDeviceControlDevice"sv; }
};

}
