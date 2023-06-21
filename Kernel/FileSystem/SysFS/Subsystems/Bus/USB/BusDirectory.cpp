/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/SysFS/Registry.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Bus/USB/BusDirectory.h>
#include <Kernel/Library/KBufferBuilder.h>

namespace Kernel {

static SysFSUSBBusDirectory* s_sysfs_usb_bus_directory;

void SysFSUSBBusDirectory::plug(Badge<USB::Hub>, SysFSUSBDeviceInformation& new_device_info_node)
{
    MUST(m_child_components.with([&](auto& list) -> ErrorOr<void> {
        list.append(new_device_info_node);
        return {};
    }));
}
void SysFSUSBBusDirectory::unplug(Badge<USB::Hub>, SysFSUSBDeviceInformation& removed_device_info_node)
{
    MUST(m_child_components.with([&](auto& list) -> ErrorOr<void> {
        list.remove(removed_device_info_node);
        return {};
    }));
}

SysFSUSBBusDirectory& SysFSUSBBusDirectory::the()
{
    VERIFY(s_sysfs_usb_bus_directory);
    return *s_sysfs_usb_bus_directory;
}

UNMAP_AFTER_INIT SysFSUSBBusDirectory::SysFSUSBBusDirectory(SysFSBusDirectory& buses_directory)
    : SysFSDirectory(buses_directory)
{
}

UNMAP_AFTER_INIT void SysFSUSBBusDirectory::initialize()
{
    auto directory = adopt_ref(*new SysFSUSBBusDirectory(SysFSComponentRegistry::the().buses_directory()));
    SysFSComponentRegistry::the().register_new_bus_directory(directory);
    s_sysfs_usb_bus_directory = directory;
}

}
