/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/FileSystem/SysFS/Component.h>
#include <Kernel/FileSystem/SysFS/Subsystems/DeviceIdentifiers/DeviceComponent.h>
#include <Kernel/FileSystem/SysFS/Subsystems/DeviceIdentifiers/Directory.h>

namespace Kernel {

class Device;
class StorageDevice;
class SysFSBlockDevicesDirectory final : public SysFSDirectory {
    friend class Device;
    friend class StorageDevice;

public:
    virtual StringView name() const override { return "block"sv; }
    static NonnullRefPtr<SysFSBlockDevicesDirectory> must_create(SysFSDeviceIdentifiersDirectory const&);

    static SysFSBlockDevicesDirectory& the();

    ChildList& devices_list(Badge<Device>) { return m_child_components; }

private:
    explicit SysFSBlockDevicesDirectory(SysFSDeviceIdentifiersDirectory const&);
};

}
