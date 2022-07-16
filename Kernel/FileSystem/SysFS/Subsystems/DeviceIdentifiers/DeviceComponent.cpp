/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Devices/Device.h>
#include <Kernel/FileSystem/SysFS/Subsystems/DeviceIdentifiers/BlockDevicesDirectory.h>
#include <Kernel/FileSystem/SysFS/Subsystems/DeviceIdentifiers/CharacterDevicesDirectory.h>
#include <Kernel/FileSystem/SysFS/Subsystems/DeviceIdentifiers/DeviceComponent.h>
#include <Kernel/Sections.h>

namespace Kernel {

NonnullRefPtr<SysFSDeviceComponent> SysFSDeviceComponent::must_create(Device const& device)
{
    // FIXME: Handle allocation failure gracefully
    auto device_name = MUST(KString::formatted("{}:{}", device.major(), device.minor()));
    RefPtr<SysFSDirectory> sysfs_corresponding_devices_directory;
    if (device.is_block_device())
        sysfs_corresponding_devices_directory = SysFSBlockDevicesDirectory::the();
    else
        sysfs_corresponding_devices_directory = SysFSCharacterDevicesDirectory::the();
    return adopt_ref_if_nonnull(new SysFSDeviceComponent(*sysfs_corresponding_devices_directory, move(device_name), device)).release_nonnull();
}
SysFSDeviceComponent::SysFSDeviceComponent(SysFSDirectory const& parent_directory, NonnullOwnPtr<KString> major_minor_formatted_device_name, Device const& device)
    : SysFSComponent(parent_directory)
    , m_block_device(device.is_block_device())
    , m_major_minor_formatted_device_name(move(major_minor_formatted_device_name))
{
    VERIFY(device.is_block_device() || device.is_character_device());
}

}
