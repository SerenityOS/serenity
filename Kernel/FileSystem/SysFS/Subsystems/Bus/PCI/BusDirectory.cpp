/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/FileSystem/SysFS/Registry.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Bus/PCI/BusDirectory.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Bus/PCI/DeviceDirectory.h>
#include <Kernel/Sections.h>

namespace Kernel {

UNMAP_AFTER_INIT void PCIBusSysFSDirectory::initialize()
{
    auto pci_directory = adopt_ref(*new (nothrow) PCIBusSysFSDirectory());
    SysFSComponentRegistry::the().register_new_bus_directory(pci_directory);
}

UNMAP_AFTER_INIT PCIBusSysFSDirectory::PCIBusSysFSDirectory()
    : SysFSDirectory(SysFSComponentRegistry::the().buses_directory())
{
    MUST(m_child_components.with([&](auto& list) -> ErrorOr<void> {
        MUST(PCI::enumerate([&](PCI::DeviceIdentifier const& device_identifier) {
            auto pci_device = PCIDeviceSysFSDirectory::create(*this, device_identifier);
            list.append(pci_device);
        }));
        return {};
    }));
}

}
