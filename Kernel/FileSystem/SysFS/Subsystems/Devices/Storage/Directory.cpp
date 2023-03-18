/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Devices/Storage/StorageDevice.h>
#include <Kernel/FileSystem/SysFS/RootDirectory.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Devices/Directory.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Devices/Storage/DeviceDirectory.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Devices/Storage/Directory.h>
#include <Kernel/Sections.h>

namespace Kernel {

static SysFSStorageDirectory* s_the { nullptr };

UNMAP_AFTER_INIT NonnullRefPtr<SysFSStorageDirectory> SysFSStorageDirectory::must_create(SysFSDevicesDirectory const& parent_directory)
{
    auto directory = adopt_ref(*new (nothrow) SysFSStorageDirectory(parent_directory));
    s_the = directory;
    return directory;
}

SysFSStorageDirectory& SysFSStorageDirectory::the()
{
    VERIFY(s_the);
    return *s_the;
}

void SysFSStorageDirectory::plug(Badge<StorageDevice>, StorageDeviceSysFSDirectory& new_device_directory)
{
    MUST(m_child_components.with([&](auto& list) -> ErrorOr<void> {
        list.append(new_device_directory);
        auto pointed_component_base_name = MUST(KString::try_create(new_device_directory.name()));
        auto pointed_component_relative_path = MUST(new_device_directory.relative_path(move(pointed_component_base_name), 0));
        return {};
    }));
}
void SysFSStorageDirectory::unplug(Badge<StorageDevice>, SysFSDirectory& removed_device_directory)
{
    MUST(m_child_components.with([&](auto& list) -> ErrorOr<void> {
        list.remove(removed_device_directory);
        return {};
    }));
}

UNMAP_AFTER_INIT SysFSStorageDirectory::SysFSStorageDirectory(SysFSDevicesDirectory const& parent_directory)
    : SysFSDirectory(parent_directory)
{
}

}
