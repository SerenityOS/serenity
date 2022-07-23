/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/SysFS/RootDirectory.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Devices/Directory.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Devices/Storage/Directory.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Devices/Storage/Physical/DeviceDirectory.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Devices/Storage/Physical/Directory.h>
#include <Kernel/Sections.h>
#include <Kernel/Storage/StorageDevice.h>

namespace Kernel {

static SysFSStoragePhysicalDevicesDirectory* s_the { nullptr };

UNMAP_AFTER_INIT NonnullRefPtr<SysFSStoragePhysicalDevicesDirectory> SysFSStoragePhysicalDevicesDirectory::must_create(SysFSStorageDirectory const& parent_directory)
{
    auto directory = adopt_ref(*new (nothrow) SysFSStoragePhysicalDevicesDirectory(parent_directory));
    s_the = directory;
    return directory;
}

SysFSStoragePhysicalDevicesDirectory& SysFSStoragePhysicalDevicesDirectory::the()
{
    VERIFY(s_the);
    return *s_the;
}

void SysFSStoragePhysicalDevicesDirectory::plug(Badge<StorageDevice>, StorageDeviceSysFSDirectory& new_device_directory)
{
    MUST(m_child_components.with([&](auto& list) -> ErrorOr<void> {
        list.append(new_device_directory);
        auto pointed_component_base_name = MUST(KString::try_create(new_device_directory.name()));
        auto pointed_component_relative_path = MUST(new_device_directory.relative_path(move(pointed_component_base_name), 0));
        return {};
    }));
}
void SysFSStoragePhysicalDevicesDirectory::unplug(Badge<StorageDevice>, SysFSDirectory& removed_device_directory)
{
    MUST(m_child_components.with([&](auto& list) -> ErrorOr<void> {
        list.remove(removed_device_directory);
        return {};
    }));
}

UNMAP_AFTER_INIT SysFSStoragePhysicalDevicesDirectory::SysFSStoragePhysicalDevicesDirectory(SysFSStorageDirectory const& parent_directory)
    : SysFSDirectory(parent_directory)
{
}

}
