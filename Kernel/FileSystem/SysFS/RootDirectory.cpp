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
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/Directory.h>
#include <Kernel/Sections.h>

namespace Kernel {

NonnullRefPtr<SysFSRootDirectory> SysFSRootDirectory::create()
{
    return adopt_ref(*new (nothrow) SysFSRootDirectory);
}

SysFSRootDirectory::SysFSRootDirectory()
    : m_buses_directory(SysFSBusDirectory::must_create(*this))
{
    auto device_identifiers_directory = SysFSDeviceIdentifiersDirectory::must_create(*this);
    auto devices_directory = SysFSDevicesDirectory::must_create(*this);
    auto global_kernel_stats_directory = SysFSGlobalKernelStatsDirectory::must_create(*this);
    MUST(m_child_components.with([&](auto& list) -> ErrorOr<void> {
        list.append(m_buses_directory);
        list.append(device_identifiers_directory);
        list.append(devices_directory);
        list.append(global_kernel_stats_directory);
        return {};
    }));
}

}
