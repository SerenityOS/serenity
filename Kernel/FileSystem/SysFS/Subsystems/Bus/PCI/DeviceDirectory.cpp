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

UNMAP_AFTER_INIT NonnullRefPtr<PCIDeviceSysFSDirectory> PCIDeviceSysFSDirectory::create(SysFSDirectory const& parent_directory, PCI::Address address)
{
    // FIXME: Handle allocation failure gracefully
    auto device_name = MUST(KString::formatted("{:04x}:{:02x}:{:02x}.{}", address.domain(), address.bus(), address.device(), address.function()));
    return adopt_ref(*new (nothrow) PCIDeviceSysFSDirectory(move(device_name), parent_directory, address));
}

UNMAP_AFTER_INIT PCIDeviceSysFSDirectory::PCIDeviceSysFSDirectory(NonnullOwnPtr<KString> device_directory_name, SysFSDirectory const& parent_directory, PCI::Address address)
    : SysFSDirectory(parent_directory)
    , m_address(address)
    , m_device_directory_name(move(device_directory_name))
{
    m_components.append(PCIDeviceAttributeSysFSComponent::create(*this, PCI::RegisterOffset::VENDOR_ID, 2));
    m_components.append(PCIDeviceAttributeSysFSComponent::create(*this, PCI::RegisterOffset::DEVICE_ID, 2));
    m_components.append(PCIDeviceAttributeSysFSComponent::create(*this, PCI::RegisterOffset::CLASS, 1));
    m_components.append(PCIDeviceAttributeSysFSComponent::create(*this, PCI::RegisterOffset::SUBCLASS, 1));
    m_components.append(PCIDeviceAttributeSysFSComponent::create(*this, PCI::RegisterOffset::REVISION_ID, 1));
    m_components.append(PCIDeviceAttributeSysFSComponent::create(*this, PCI::RegisterOffset::PROG_IF, 1));
    m_components.append(PCIDeviceAttributeSysFSComponent::create(*this, PCI::RegisterOffset::SUBSYSTEM_VENDOR_ID, 2));
    m_components.append(PCIDeviceAttributeSysFSComponent::create(*this, PCI::RegisterOffset::SUBSYSTEM_ID, 2));

    m_components.append(PCIDeviceAttributeSysFSComponent::create(*this, PCI::RegisterOffset::BAR0, 4));
    m_components.append(PCIDeviceAttributeSysFSComponent::create(*this, PCI::RegisterOffset::BAR1, 4));
    m_components.append(PCIDeviceAttributeSysFSComponent::create(*this, PCI::RegisterOffset::BAR2, 4));
    m_components.append(PCIDeviceAttributeSysFSComponent::create(*this, PCI::RegisterOffset::BAR3, 4));
    m_components.append(PCIDeviceAttributeSysFSComponent::create(*this, PCI::RegisterOffset::BAR4, 4));
    m_components.append(PCIDeviceAttributeSysFSComponent::create(*this, PCI::RegisterOffset::BAR5, 4));
}

}
