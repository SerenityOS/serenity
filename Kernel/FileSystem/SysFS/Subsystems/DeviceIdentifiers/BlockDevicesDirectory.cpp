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

ErrorOr<void> SysFSBlockDevicesDirectory::traverse_as_directory(FileSystemID fsid, Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)> callback) const
{
    VERIFY(m_parent_directory);
    TRY(callback({ "."sv, { fsid, component_index() }, 0 }));
    TRY(callback({ ".."sv, { fsid, m_parent_directory->component_index() }, 0 }));

    return m_devices_list.with([&](auto& list) -> ErrorOr<void> {
        for (auto& exposed_device : list) {
            VERIFY(exposed_device.is_block_device());
            TRY(callback({ exposed_device.name(), { fsid, exposed_device.component_index() }, 0 }));
        }
        return {};
    });
}

RefPtr<SysFSComponent> SysFSBlockDevicesDirectory::lookup(StringView name)
{
    return m_devices_list.with([&](auto& list) -> RefPtr<SysFSComponent> {
        for (auto& exposed_device : list) {
            VERIFY(exposed_device.is_block_device());
            if (exposed_device.name() == name)
                return exposed_device;
        }
        return nullptr;
    });
}

}
