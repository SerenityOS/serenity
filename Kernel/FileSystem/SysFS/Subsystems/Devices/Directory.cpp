/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/SysFS/RootDirectory.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Devices/Directory.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Devices/Graphics/Directory.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Devices/Storage/Directory.h>
#include <Kernel/Sections.h>

namespace Kernel {

UNMAP_AFTER_INIT NonnullRefPtr<SysFSDevicesDirectory> SysFSDevicesDirectory::must_create(SysFSRootDirectory const& root_directory)
{
    auto devices_directory = adopt_ref_if_nonnull(new (nothrow) SysFSDevicesDirectory(root_directory)).release_nonnull();
    MUST(devices_directory->m_child_components.with([&](auto& list) -> ErrorOr<void> {
        list.append(SysFSStorageDirectory::must_create(*devices_directory));
        list.append(SysFSGraphicsDirectory::must_create(*devices_directory));
        return {};
    }));
    return devices_directory;
}
SysFSDevicesDirectory::SysFSDevicesDirectory(SysFSRootDirectory const& root_directory)
    : SysFSDirectory(root_directory)
{
}

}
