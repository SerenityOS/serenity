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
ErrorOr<void> SysFSCharacterDevicesDirectory::traverse_as_directory(FileSystemID fsid, Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)> callback) const
{
    VERIFY(m_parent_directory);
    TRY(callback({ "."sv, { fsid, component_index() }, 0 }));
    TRY(callback({ ".."sv, { fsid, m_parent_directory->component_index() }, 0 }));

    return m_devices_list.with([&](auto& list) -> ErrorOr<void> {
        for (auto& exposed_device : list) {
            VERIFY(!exposed_device.is_block_device());
            TRY(callback({ exposed_device.name(), { fsid, exposed_device.component_index() }, 0 }));
        }
        return {};
    });
}

SysFSCharacterDevicesDirectory& SysFSCharacterDevicesDirectory::the()
{
    VERIFY(s_the);
    return *s_the;
}

RefPtr<SysFSComponent> SysFSCharacterDevicesDirectory::lookup(StringView name)
{
    return m_devices_list.with([&](auto& list) -> RefPtr<SysFSComponent> {
        for (auto& exposed_device : list) {
            VERIFY(!exposed_device.is_block_device());
            if (exposed_device.name() == name)
                return exposed_device;
        }
        return nullptr;
    });
}

}
