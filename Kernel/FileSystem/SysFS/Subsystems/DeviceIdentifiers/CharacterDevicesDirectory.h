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
class DisplayConnector;
class SysFSCharacterDevicesDirectory final : public SysFSDirectory {
    friend class DisplayConnector;

public:
    virtual StringView name() const override { return "char"sv; }
    static NonnullRefPtr<SysFSCharacterDevicesDirectory> must_create(SysFSDeviceIdentifiersDirectory const&);

    static SysFSCharacterDevicesDirectory& the();

    ChildList& devices_list(Badge<Device>) { return m_child_components; }

private:
    explicit SysFSCharacterDevicesDirectory(SysFSDeviceIdentifiersDirectory const&);
};

}
