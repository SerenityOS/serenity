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
class SysFSCharacterDevicesDirectory final : public SysFSDirectory {
public:
    virtual StringView name() const override { return "char"sv; }
    static NonnullRefPtr<SysFSCharacterDevicesDirectory> must_create(SysFSDeviceIdentifiersDirectory const&);
    virtual ErrorOr<void> traverse_as_directory(FileSystemID, Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)>) const override;
    virtual RefPtr<SysFSComponent> lookup(StringView name) override;

    static SysFSCharacterDevicesDirectory& the();

    using DevicesList = SpinlockProtected<IntrusiveList<&SysFSDeviceComponent::m_list_node>>;
    DevicesList& devices_list(Badge<Device>) { return m_devices_list; }

private:
    explicit SysFSCharacterDevicesDirectory(SysFSDeviceIdentifiersDirectory const&);

    DevicesList m_devices_list;
};

}
