/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/SysFS/RootDirectory.h>
#include <Kernel/FileSystem/SysFS/Subsystems/DeviceIdentifiers/BlockDevicesDirectory.h>
#include <Kernel/FileSystem/SysFS/Subsystems/DeviceIdentifiers/CharacterDevicesDirectory.h>
#include <Kernel/FileSystem/SysFS/Subsystems/DeviceIdentifiers/Directory.h>
#include <Kernel/Sections.h>

namespace Kernel {

static SysFSDeviceIdentifiersDirectory* s_the { nullptr };

SysFSDeviceIdentifiersDirectory& SysFSDeviceIdentifiersDirectory::the()
{
    VERIFY(s_the);
    return *s_the;
}

UNMAP_AFTER_INIT NonnullRefPtr<SysFSDeviceIdentifiersDirectory> SysFSDeviceIdentifiersDirectory::must_create(SysFSRootDirectory const& root_directory)
{
    auto devices_directory = adopt_ref_if_nonnull(new SysFSDeviceIdentifiersDirectory(root_directory)).release_nonnull();
    MUST(devices_directory->m_child_components.with([&](auto& list) -> ErrorOr<void> {
        list.append(SysFSBlockDevicesDirectory::must_create(*devices_directory));
        list.append(SysFSCharacterDevicesDirectory::must_create(*devices_directory));
        return {};
    }));
    s_the = devices_directory;
    return devices_directory;
}
SysFSDeviceIdentifiersDirectory::SysFSDeviceIdentifiersDirectory(SysFSRootDirectory const& root_directory)
    : SysFSDirectory(root_directory)
{
}

}
