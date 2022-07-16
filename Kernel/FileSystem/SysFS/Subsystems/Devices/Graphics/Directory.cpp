/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/SysFS/Subsystems/Devices/Graphics/Directory.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Devices/Graphics/DisplayConnector/Directory.h>
#include <Kernel/Sections.h>

namespace Kernel {

UNMAP_AFTER_INIT NonnullRefPtr<SysFSGraphicsDirectory> SysFSGraphicsDirectory::must_create(SysFSDevicesDirectory const& parent_directory)
{
    auto directory = adopt_ref(*new (nothrow) SysFSGraphicsDirectory(parent_directory));
    MUST(directory->m_child_components.with([&](auto& list) -> ErrorOr<void> {
        list.append(SysFSDisplayConnectorsDirectory::must_create(*directory));
        return {};
    }));
    return directory;
}

UNMAP_AFTER_INIT SysFSGraphicsDirectory::SysFSGraphicsDirectory(SysFSDevicesDirectory const& parent_directory)
    : SysFSDirectory(parent_directory)
{
}

}
