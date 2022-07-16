/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/SysFS/RootDirectory.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Devices/Directory.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Devices/Graphics/Adapter/DeviceDirectory.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Devices/Graphics/Adapter/Directory.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Devices/Graphics/Directory.h>
#include <Kernel/Sections.h>
#include <Kernel/Storage/StorageDevice.h>

namespace Kernel {

static SysFSGraphicsAdaptersDirectory* s_the { nullptr };

UNMAP_AFTER_INIT NonnullRefPtr<SysFSGraphicsAdaptersDirectory> SysFSGraphicsAdaptersDirectory::must_create(SysFSGraphicsDirectory const& parent_directory)
{
    auto directory = adopt_ref(*new (nothrow) SysFSGraphicsAdaptersDirectory(parent_directory));
    s_the = directory;
    return directory;
}

SysFSGraphicsAdaptersDirectory& SysFSGraphicsAdaptersDirectory::the()
{
    VERIFY(s_the);
    return *s_the;
}

void SysFSGraphicsAdaptersDirectory::plug_pci_adapter(Badge<PCIGraphicsAdapter>, GraphicsAdapterSysFSDirectory& new_device_directory)
{
    MUST(m_child_components.with([&](auto& list) -> ErrorOr<void> {
        list.append(new_device_directory);
        return {};
    }));
}

void SysFSGraphicsAdaptersDirectory::unplug_pci_adapter(Badge<PCIGraphicsAdapter>, SysFSDirectory& removed_device_directory)
{
    MUST(m_child_components.with([&](auto& list) -> ErrorOr<void> {
        list.remove(removed_device_directory);
        return {};
    }));
}

void SysFSGraphicsAdaptersDirectory::plug_virtio_adapter(Badge<VirtIOGraphicsAdapter>, GraphicsAdapterSysFSDirectory& new_device_directory)
{
    MUST(m_child_components.with([&](auto& list) -> ErrorOr<void> {
        list.append(new_device_directory);
        return {};
    }));
}

void SysFSGraphicsAdaptersDirectory::unplug_virtio_adapter(Badge<VirtIOGraphicsAdapter>, SysFSDirectory& removed_device_directory)
{
    MUST(m_child_components.with([&](auto& list) -> ErrorOr<void> {
        list.remove(removed_device_directory);
        return {};
    }));
}

UNMAP_AFTER_INIT SysFSGraphicsAdaptersDirectory::SysFSGraphicsAdaptersDirectory(SysFSGraphicsDirectory const& parent_directory)
    : SysFSDirectory(parent_directory)
{
}

}
