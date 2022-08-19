/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Debug.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Bus/PCI/DeviceAttribute.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Bus/PCI/DeviceDirectory.h>
#include <Kernel/Sections.h>

namespace Kernel {

UNMAP_AFTER_INIT NonnullLockRefPtr<PCIDeviceSysFSDirectory> PCIDeviceSysFSDirectory::create(SysFSDirectory const& parent_directory, PCI::Address address)
{
    // FIXME: Handle allocation failure gracefully
    auto device_name = MUST(KString::formatted("{:04x}:{:02x}:{:02x}.{}", address.domain(), address.bus(), address.device(), address.function()));
    auto directory = adopt_lock_ref(*new (nothrow) PCIDeviceSysFSDirectory(move(device_name), parent_directory, address));
    MUST(directory->m_child_components.with([&](auto& list) -> ErrorOr<void> {
        list.append(PCIDeviceAttributeSysFSComponent::create(*directory, PCI::RegisterOffset::VENDOR_ID, 2));
        list.append(PCIDeviceAttributeSysFSComponent::create(*directory, PCI::RegisterOffset::DEVICE_ID, 2));
        list.append(PCIDeviceAttributeSysFSComponent::create(*directory, PCI::RegisterOffset::CLASS, 1));
        list.append(PCIDeviceAttributeSysFSComponent::create(*directory, PCI::RegisterOffset::SUBCLASS, 1));
        list.append(PCIDeviceAttributeSysFSComponent::create(*directory, PCI::RegisterOffset::REVISION_ID, 1));
        list.append(PCIDeviceAttributeSysFSComponent::create(*directory, PCI::RegisterOffset::PROG_IF, 1));
        list.append(PCIDeviceAttributeSysFSComponent::create(*directory, PCI::RegisterOffset::SUBSYSTEM_VENDOR_ID, 2));
        list.append(PCIDeviceAttributeSysFSComponent::create(*directory, PCI::RegisterOffset::SUBSYSTEM_ID, 2));

        list.append(PCIDeviceAttributeSysFSComponent::create(*directory, PCI::RegisterOffset::BAR0, 4));
        list.append(PCIDeviceAttributeSysFSComponent::create(*directory, PCI::RegisterOffset::BAR1, 4));
        list.append(PCIDeviceAttributeSysFSComponent::create(*directory, PCI::RegisterOffset::BAR2, 4));
        list.append(PCIDeviceAttributeSysFSComponent::create(*directory, PCI::RegisterOffset::BAR3, 4));
        list.append(PCIDeviceAttributeSysFSComponent::create(*directory, PCI::RegisterOffset::BAR4, 4));
        list.append(PCIDeviceAttributeSysFSComponent::create(*directory, PCI::RegisterOffset::BAR5, 4));
        return {};
    }));
    return directory;
}

UNMAP_AFTER_INIT PCIDeviceSysFSDirectory::PCIDeviceSysFSDirectory(NonnullOwnPtr<KString> device_directory_name, SysFSDirectory const& parent_directory, PCI::Address address)
    : SysFSDirectory(parent_directory)
    , m_address(address)
    , m_device_directory_name(move(device_directory_name))
{
}

}
