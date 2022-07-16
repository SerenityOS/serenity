/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/SysFS/Subsystems/Devices/Graphics/Adapter/DeviceDirectory.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Devices/Graphics/Adapter/Directory.h>
#include <Kernel/Graphics/PCIGraphicsAdapter.h>

namespace Kernel {

PCIGraphicsAdapter::PCIGraphicsAdapter(PCI::DeviceIdentifier const& pci_device_identifier)
    : PCI::Device(pci_device_identifier.address())
{
}

void PCIGraphicsAdapter::after_inserting()
{
    auto sysfs_graphics_adapter_directory = GraphicsAdapterSysFSDirectory::create(SysFSGraphicsAdaptersDirectory::the(), pci_address(), adapter_id());
    m_sysfs_directory = sysfs_graphics_adapter_directory;
    SysFSGraphicsAdaptersDirectory::the().plug_pci_adapter({}, *sysfs_graphics_adapter_directory);
}

}
