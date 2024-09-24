/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Devices/CharacterDevice.h>
#include <Kernel/FileSystem/SysFS/Subsystems/DeviceIdentifiers/CharacterDevicesDirectory.h>

namespace Kernel {

CharacterDevice::CharacterDevice(MajorAllocation::CharacterDeviceFamily character_device_family, MinorNumber minor)
    : Device(MajorAllocation::character_device_family_to_major_number(character_device_family), minor)
{
}

CharacterDevice::~CharacterDevice() = default;

void CharacterDevice::after_inserting_add_symlink_to_device_identifier_directory()
{
    VERIFY(m_symlink_sysfs_component);
    SysFSCharacterDevicesDirectory::the().devices_list({}).with([&](auto& list) -> void {
        list.append(*m_symlink_sysfs_component);
    });
}

void CharacterDevice::before_will_be_destroyed_remove_symlink_from_device_identifier_directory()
{
    VERIFY(m_symlink_sysfs_component);
    SysFSCharacterDevicesDirectory::the().devices_list({}).with([&](auto& list) -> void {
        list.remove(*m_symlink_sysfs_component);
    });
}

// FIXME: This method will be eventually removed after all nodes in /sys/dev/char/ are symlinks
void CharacterDevice::after_inserting_add_to_device_identifier_directory()
{
    VERIFY(m_sysfs_component);
    SysFSCharacterDevicesDirectory::the().devices_list({}).with([&](auto& list) -> void {
        list.append(*m_sysfs_component);
    });
}

// FIXME: This method will be eventually removed after all nodes in /sys/dev/char/ are symlinks
void CharacterDevice::before_will_be_destroyed_remove_from_device_identifier_directory()
{
    VERIFY(m_sysfs_component);
    SysFSCharacterDevicesDirectory::the().devices_list({}).with([&](auto& list) -> void {
        list.remove(*m_sysfs_component);
    });
}

}
