/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/SysFS/Registry.h>
#include <Kernel/FileSystem/SysFS/Subsystems/DeviceIdentifiers/BlockDevicesDirectory.h>
#include <Kernel/Sections.h>

namespace Kernel {

static SysFSBlockDevicesDirectory* s_the { nullptr };

NonnullRefPtr<SysFSBlockDevicesDirectory> SysFSBlockDevicesDirectory::must_create(SysFSDeviceIdentifiersDirectory const& devices_directory)
{
    return adopt_ref_if_nonnull(new SysFSBlockDevicesDirectory(devices_directory)).release_nonnull();
}
SysFSBlockDevicesDirectory::SysFSBlockDevicesDirectory(SysFSDeviceIdentifiersDirectory const& devices_directory)
    : SysFSDirectory(devices_directory)
{
    s_the = this;
}

SysFSBlockDevicesDirectory& SysFSBlockDevicesDirectory::the()
{
    VERIFY(s_the);
    return *s_the;
}

}
