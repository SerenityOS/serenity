/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Devices/Storage/StorageDevice.h>
#include <Kernel/FileSystem/SysFS/RootDirectory.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Devices/Directory.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Devices/Graphics/Directory.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Devices/Graphics/DisplayConnector/DeviceDirectory.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Devices/Graphics/DisplayConnector/Directory.h>
#include <Kernel/Sections.h>

namespace Kernel {

static SysFSDisplayConnectorsDirectory* s_the { nullptr };

UNMAP_AFTER_INIT NonnullRefPtr<SysFSDisplayConnectorsDirectory> SysFSDisplayConnectorsDirectory::must_create(SysFSGraphicsDirectory const& parent_directory)
{
    auto directory = adopt_ref(*new (nothrow) SysFSDisplayConnectorsDirectory(parent_directory));
    s_the = directory;
    return directory;
}

SysFSDisplayConnectorsDirectory& SysFSDisplayConnectorsDirectory::the()
{
    VERIFY(s_the);
    return *s_the;
}

void SysFSDisplayConnectorsDirectory::plug(Badge<DisplayConnector>, DisplayConnectorSysFSDirectory& new_device_directory)
{
    MUST(m_child_components.with([&](auto& list) -> ErrorOr<void> {
        list.append(new_device_directory);
        auto pointed_component_base_name = MUST(KString::try_create(new_device_directory.name()));
        auto pointed_component_relative_path = MUST(new_device_directory.relative_path(move(pointed_component_base_name), 0));
        return {};
    }));
}
void SysFSDisplayConnectorsDirectory::unplug(Badge<DisplayConnector>, SysFSDirectory& removed_device_directory)
{
    MUST(m_child_components.with([&](auto& list) -> ErrorOr<void> {
        list.remove(removed_device_directory);
        return {};
    }));
}

UNMAP_AFTER_INIT SysFSDisplayConnectorsDirectory::SysFSDisplayConnectorsDirectory(SysFSGraphicsDirectory const& parent_directory)
    : SysFSDirectory(parent_directory)
{
}

}
