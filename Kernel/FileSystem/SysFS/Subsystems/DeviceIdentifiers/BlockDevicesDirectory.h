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
class SysFSBlockDevicesDirectory final : public SysFSDirectory {
public:
    virtual StringView name() const override { return "block"sv; }
    static NonnullRefPtr<SysFSBlockDevicesDirectory> must_create(SysFSDeviceIdentifiersDirectory const&);
    virtual ErrorOr<void> traverse_as_directory(FileSystemID, Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)>) const override;
    virtual RefPtr<SysFSComponent> lookup(StringView name) override;

    static SysFSBlockDevicesDirectory& the();

    using DevicesList = SpinlockProtected<IntrusiveList<&SysFSDeviceComponent::m_list_node>>;
    DevicesList& devices_list(Badge<Device>) { return m_devices_list; }

private:
    explicit SysFSBlockDevicesDirectory(SysFSDeviceIdentifiersDirectory const&);

    DevicesList m_devices_list;
};

}
