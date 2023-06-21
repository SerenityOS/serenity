/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/SysFS/Registry.h>
#include <Kernel/FileSystem/SysFS/Subsystems/DeviceIdentifiers/CharacterDevicesDirectory.h>
#include <Kernel/Sections.h>

namespace Kernel {

static SysFSCharacterDevicesDirectory* s_the { nullptr };

NonnullRefPtr<SysFSCharacterDevicesDirectory> SysFSCharacterDevicesDirectory::must_create(SysFSDeviceIdentifiersDirectory const& devices_directory)
{
    return adopt_ref_if_nonnull(new SysFSCharacterDevicesDirectory(devices_directory)).release_nonnull();
}
SysFSCharacterDevicesDirectory::SysFSCharacterDevicesDirectory(SysFSDeviceIdentifiersDirectory const& devices_directory)
    : SysFSDirectory(devices_directory)
{
    s_the = this;
}

SysFSCharacterDevicesDirectory& SysFSCharacterDevicesDirectory::the()
{
    VERIFY(s_the);
    return *s_the;
}

}
