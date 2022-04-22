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
