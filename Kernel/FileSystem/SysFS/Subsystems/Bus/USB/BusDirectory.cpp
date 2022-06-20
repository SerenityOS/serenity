/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/SysFS/Registry.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Bus/USB/BusDirectory.h>
#include <Kernel/KBufferBuilder.h>

namespace Kernel {

static SysFSUSBBusDirectory* s_procfs_usb_bus_directory;

ErrorOr<void> SysFSUSBBusDirectory::traverse_as_directory(FileSystemID fsid, Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)> callback) const
{
    SpinlockLocker lock(m_lock);
    // Note: if the parent directory is null, it means something bad happened as this should not happen for the USB directory.
    VERIFY(m_parent_directory);
    TRY(callback({ ".", { fsid, component_index() }, 0 }));
    TRY(callback({ "..", { fsid, m_parent_directory->component_index() }, 0 }));

    for (auto const& device_node : m_device_nodes) {
        InodeIdentifier identifier = { fsid, device_node.component_index() };
        TRY(callback({ device_node.name(), identifier, 0 }));
    }
    return {};
}

RefPtr<SysFSComponent> SysFSUSBBusDirectory::lookup(StringView name)
{
    SpinlockLocker lock(m_lock);
    for (auto& device_node : m_device_nodes) {
        if (device_node.name() == name) {
            return device_node;
        }
    }
    return {};
}

RefPtr<SysFSUSBDeviceInformation> SysFSUSBBusDirectory::device_node_for(USB::Device& device)
{
    RefPtr<USB::Device> checked_device = device;
    for (auto& device_node : m_device_nodes) {
        if (device_node.device().ptr() == checked_device.ptr())
            return device_node;
    }
    return {};
}

void SysFSUSBBusDirectory::plug(USB::Device& new_device)
{
    SpinlockLocker lock(m_lock);
    auto device_node = device_node_for(new_device);
    VERIFY(!device_node);
    auto sysfs_usb_device_or_error = SysFSUSBDeviceInformation::create(new_device);
    if (sysfs_usb_device_or_error.is_error()) {
        dbgln("Failed to create SysFSUSBDevice for device id {}", new_device.address());
        return;
    }

    m_device_nodes.append(sysfs_usb_device_or_error.release_value());
}

void SysFSUSBBusDirectory::unplug(USB::Device& deleted_device)
{
    SpinlockLocker lock(m_lock);
    auto device_node = device_node_for(deleted_device);
    VERIFY(device_node);
    device_node->m_list_node.remove();
}

SysFSUSBBusDirectory& SysFSUSBBusDirectory::the()
{
    VERIFY(s_procfs_usb_bus_directory);
    return *s_procfs_usb_bus_directory;
}

UNMAP_AFTER_INIT SysFSUSBBusDirectory::SysFSUSBBusDirectory(SysFSBusDirectory& buses_directory)
    : SysFSDirectory(buses_directory)
{
}

UNMAP_AFTER_INIT void SysFSUSBBusDirectory::initialize()
{
    auto directory = adopt_ref(*new SysFSUSBBusDirectory(SysFSComponentRegistry::the().buses_directory()));
    SysFSComponentRegistry::the().register_new_bus_directory(directory);
    s_procfs_usb_bus_directory = directory;
}

ErrorOr<NonnullRefPtr<SysFSUSBDeviceInformation>> SysFSUSBDeviceInformation::create(USB::Device& device)
{
    auto device_name = TRY(KString::number(device.address()));
    return adopt_nonnull_ref_or_enomem(new (nothrow) SysFSUSBDeviceInformation(move(device_name), device));
}

}
