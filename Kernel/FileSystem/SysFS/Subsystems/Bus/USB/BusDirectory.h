/*
 * Copyright (c) 2021-2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Badge.h>
#include <Kernel/Bus/USB/USBDevice.h>
#include <Kernel/Bus/USB/USBHub.h>
#include <Kernel/FileSystem/SysFS/Component.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Bus/USB/DeviceInformation.h>
#include <Kernel/Locking/Spinlock.h>

namespace Kernel {

class SysFSUSBBusDirectory final : public SysFSDirectory {
public:
    static void initialize();
    static SysFSUSBBusDirectory& the();

    virtual StringView name() const override { return "usb"sv; }

    void plug(Badge<USB::Hub>, SysFSUSBDeviceInformation&);
    void unplug(Badge<USB::Hub>, SysFSUSBDeviceInformation&);

private:
    explicit SysFSUSBBusDirectory(SysFSBusDirectory&);
    mutable Spinlock<LockRank::None> m_lock {};
};

}
