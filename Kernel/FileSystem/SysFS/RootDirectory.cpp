/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/SysFS/Registry.h>
#include <Kernel/FileSystem/SysFS/RootDirectory.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Bus/Directory.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Devices/Directory.h>
#include <Kernel/Sections.h>

namespace Kernel {

NonnullRefPtr<SysFSRootDirectory> SysFSRootDirectory::create()
{
    return adopt_ref(*new (nothrow) SysFSRootDirectory);
}

ErrorOr<void> SysFSRootDirectory::traverse_as_directory(FileSystemID fsid, Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)> callback) const
{
    MutexLocker locker(SysFSComponentRegistry::the().get_lock());
    TRY(callback({ ".", { fsid, component_index() }, 0 }));
    TRY(callback({ "..", { fsid, 0 }, 0 }));

    for (auto const& component : m_components) {
        InodeIdentifier identifier = { fsid, component.component_index() };
        TRY(callback({ component.name(), identifier, 0 }));
    }
    return {};
}

SysFSRootDirectory::SysFSRootDirectory()
{
    auto buses_directory = SysFSBusDirectory::must_create(*this);
    auto devices_directory = SysFSDevicesDirectory::must_create(*this);
    m_components.append(buses_directory);
    m_components.append(devices_directory);
    m_buses_directory = buses_directory;
}

}
