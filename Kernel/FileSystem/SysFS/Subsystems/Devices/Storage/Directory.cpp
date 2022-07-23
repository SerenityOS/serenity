/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/SysFS/RootDirectory.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Devices/Directory.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Devices/Storage/Directory.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Devices/Storage/Logical/Directory.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Devices/Storage/Physical/Directory.h>
#include <Kernel/Sections.h>
#include <Kernel/Storage/StorageDevice.h>

namespace Kernel {

UNMAP_AFTER_INIT NonnullRefPtr<SysFSStorageDirectory> SysFSStorageDirectory::must_create(SysFSDevicesDirectory const& parent_directory)
{
    auto directory = adopt_ref(*new (nothrow) SysFSStorageDirectory(parent_directory));
    MUST(directory->m_child_components.with([&](auto& list) -> ErrorOr<void> {
        list.append(SysFSStoragePhysicalDevicesDirectory::must_create(*directory));
        list.append(SysFSStorageLogicalDevicesDirectory::must_create(*directory));
        return {};
    }));
    return directory;
}

UNMAP_AFTER_INIT SysFSStorageDirectory::SysFSStorageDirectory(SysFSDevicesDirectory const& parent_directory)
    : SysFSDirectory(parent_directory)
{
}

}
