/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/SysFS/Subsystems/Devices/GPU/Directory.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Devices/GPU/DisplayConnector/Directory.h>
#include <Kernel/Sections.h>

namespace Kernel {

UNMAP_AFTER_INIT NonnullLockRefPtr<SysFSGPUDirectory> SysFSGPUDirectory::must_create(SysFSDevicesDirectory const& parent_directory)
{
    auto directory = adopt_lock_ref(*new (nothrow) SysFSGPUDirectory(parent_directory));
    MUST(directory->m_child_components.with([&](auto& list) -> ErrorOr<void> {
        list.append(SysFSDisplayConnectorsDirectory::must_create(*directory));
        return {};
    }));
    return directory;
}

UNMAP_AFTER_INIT SysFSGPUDirectory::SysFSGPUDirectory(SysFSDevicesDirectory const& parent_directory)
    : SysFSDirectory(parent_directory)
{
}

}
