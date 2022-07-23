/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/SysFS/RootDirectory.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Devices/Directory.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Devices/Storage/Directory.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Devices/Storage/Logical/Directory.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Devices/Storage/Logical/Partition/DeviceDirectory.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Devices/Storage/Logical/Partition/Directory.h>
#include <Kernel/Sections.h>
#include <Kernel/Storage/DiskPartition.h>

namespace Kernel {

static SysFSStoragePartitionDevicesDirectory* s_the { nullptr };

UNMAP_AFTER_INIT NonnullRefPtr<SysFSStoragePartitionDevicesDirectory> SysFSStoragePartitionDevicesDirectory::must_create(SysFSStorageLogicalDevicesDirectory const& parent_directory)
{
    auto directory = adopt_ref(*new (nothrow) SysFSStoragePartitionDevicesDirectory(parent_directory));
    s_the = directory;
    return directory;
}

SysFSStoragePartitionDevicesDirectory& SysFSStoragePartitionDevicesDirectory::the()
{
    VERIFY(s_the);
    return *s_the;
}

void SysFSStoragePartitionDevicesDirectory::plug(Badge<DiskPartition>, PartitionDeviceSysFSDirectory& new_device_directory)
{
    MUST(m_child_components.with([&](auto& list) -> ErrorOr<void> {
        list.append(new_device_directory);
        auto pointed_component_base_name = MUST(KString::try_create(new_device_directory.name()));
        auto pointed_component_relative_path = MUST(new_device_directory.relative_path(move(pointed_component_base_name), 0));
        return {};
    }));
}
void SysFSStoragePartitionDevicesDirectory::unplug(Badge<DiskPartition>, SysFSDirectory& removed_device_directory)
{
    MUST(m_child_components.with([&](auto& list) -> ErrorOr<void> {
        list.remove(removed_device_directory);
        return {};
    }));
}

UNMAP_AFTER_INIT SysFSStoragePartitionDevicesDirectory::SysFSStoragePartitionDevicesDirectory(SysFSStorageLogicalDevicesDirectory const& parent_directory)
    : SysFSDirectory(parent_directory)
{
}

}
