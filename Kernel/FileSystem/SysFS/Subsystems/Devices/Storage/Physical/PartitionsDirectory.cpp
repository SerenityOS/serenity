/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Debug.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Devices/Storage/Physical/DeviceDirectory.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Devices/Storage/Physical/PartitionDeviceSymbolicLink.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Devices/Storage/Physical/PartitionsDirectory.h>
#include <Kernel/Storage/StorageDevice.h>

namespace Kernel {

NonnullRefPtr<StorageDevicePartitionsSysFSDirectory> StorageDevicePartitionsSysFSDirectory::must_create(StorageDeviceSysFSDirectory const& parent_directory)
{
    return adopt_ref(*new (nothrow) StorageDevicePartitionsSysFSDirectory(parent_directory));
}

void StorageDevicePartitionsSysFSDirectory::plug(Badge<StorageDevice>, PartitionDeviceSymbolicLinkSysFSComponent& sysfs_partition_device_symbolic_link)
{
    MUST(m_child_components.with([&](auto& list) -> ErrorOr<void> {
        list.append(sysfs_partition_device_symbolic_link);
        return {};
    }));
}

void StorageDevicePartitionsSysFSDirectory::clear(Badge<StorageDevice>)
{
    MUST(m_child_components.with([&](auto& list) -> ErrorOr<void> {
        list.clear();
        return {};
    }));
}

StorageDevicePartitionsSysFSDirectory::StorageDevicePartitionsSysFSDirectory(StorageDeviceSysFSDirectory const& parent_directory)
    : SysFSDirectory(parent_directory)
{
}

}
