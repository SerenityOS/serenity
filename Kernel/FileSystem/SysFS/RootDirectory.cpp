/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/SysFS/Registry.h>
#include <Kernel/FileSystem/SysFS/RootDirectory.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Bus/Directory.h>
#include <Kernel/FileSystem/SysFS/Subsystems/DeviceIdentifiers/Directory.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Devices/Directory.h>
#include <Kernel/Sections.h>

namespace Kernel {

NonnullLockRefPtr<SysFSRootDirectory> SysFSRootDirectory::create()
{
    return adopt_lock_ref(*new (nothrow) SysFSRootDirectory);
}

SysFSRootDirectory::SysFSRootDirectory()
{
    auto buses_directory = SysFSBusDirectory::must_create(*this);
    auto device_identifiers_directory = SysFSDeviceIdentifiersDirectory::must_create(*this);
    auto devices_directory = SysFSDevicesDirectory::must_create(*this);
    MUST(m_child_components.with([&](auto& list) -> ErrorOr<void> {
        list.append(buses_directory);
        list.append(device_identifiers_directory);
        list.append(devices_directory);
        return {};
    }));
    m_buses_directory = buses_directory;
}

}
