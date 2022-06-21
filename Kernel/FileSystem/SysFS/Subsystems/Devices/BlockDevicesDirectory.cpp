/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/SysFS/Registry.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Devices/BlockDevicesDirectory.h>
#include <Kernel/Sections.h>

namespace Kernel {

NonnullRefPtr<SysFSBlockDevicesDirectory> SysFSBlockDevicesDirectory::must_create(SysFSDevicesDirectory const& devices_directory)
{
    return adopt_ref_if_nonnull(new SysFSBlockDevicesDirectory(devices_directory)).release_nonnull();
}
SysFSBlockDevicesDirectory::SysFSBlockDevicesDirectory(SysFSDevicesDirectory const& devices_directory)
    : SysFSDirectory(devices_directory)
{
}

ErrorOr<void> SysFSBlockDevicesDirectory::traverse_as_directory(FileSystemID fsid, Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)> callback) const
{
    VERIFY(m_parent_directory);
    TRY(callback({ ".", { fsid, component_index() }, 0 }));
    TRY(callback({ "..", { fsid, m_parent_directory->component_index() }, 0 }));

    return SysFSComponentRegistry::the().devices_list().with_exclusive([&](auto& list) -> ErrorOr<void> {
        for (auto& exposed_device : list) {
            if (!exposed_device.is_block_device())
                continue;
            TRY(callback({ exposed_device.name(), { fsid, exposed_device.component_index() }, 0 }));
        }
        return {};
    });
}

RefPtr<SysFSComponent> SysFSBlockDevicesDirectory::lookup(StringView name)
{
    return SysFSComponentRegistry::the().devices_list().with_exclusive([&](auto& list) -> RefPtr<SysFSComponent> {
        for (auto& exposed_device : list) {
            if (!exposed_device.is_block_device())
                continue;
            if (exposed_device.name() == name)
                return exposed_device;
        }
        return nullptr;
    });
}

}
