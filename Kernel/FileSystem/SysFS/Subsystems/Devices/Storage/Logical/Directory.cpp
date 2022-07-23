/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/SysFS/RootDirectory.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Devices/Directory.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Devices/Storage/Directory.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Devices/Storage/Logical/Directory.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Devices/Storage/Logical/Partition/Directory.h>
#include <Kernel/Sections.h>
#include <Kernel/Storage/StorageDevice.h>

namespace Kernel {

UNMAP_AFTER_INIT NonnullRefPtr<SysFSStorageLogicalDevicesDirectory> SysFSStorageLogicalDevicesDirectory::must_create(SysFSStorageDirectory const& parent_directory)
{
    auto directory = adopt_ref(*new (nothrow) SysFSStorageLogicalDevicesDirectory(parent_directory));
    MUST(directory->m_child_components.with([&](auto& list) -> ErrorOr<void> {
        list.append(SysFSStoragePartitionDevicesDirectory::must_create(*directory));
        return {};
    }));
    return directory;
}

UNMAP_AFTER_INIT SysFSStorageLogicalDevicesDirectory::SysFSStorageLogicalDevicesDirectory(SysFSStorageDirectory const& parent_directory)
    : SysFSDirectory(parent_directory)
{
}

}
