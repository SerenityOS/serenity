/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Bus/PCI/SysFSPCI.h>
#include <Kernel/Debug.h>
#include <Kernel/Sections.h>

namespace Kernel::PCI {

UNMAP_AFTER_INIT NonnullRefPtr<PCIDeviceSysFSDirectory> PCIDeviceSysFSDirectory::create(const SysFSDirectory& parent_directory, Address address)
{
    return adopt_ref(*new (nothrow) PCIDeviceSysFSDirectory(parent_directory, address));
}

UNMAP_AFTER_INIT PCIDeviceSysFSDirectory::PCIDeviceSysFSDirectory(const SysFSDirectory& parent_directory, Address address)
    : SysFSDirectory(String::formatted("{:04x}:{:02x}:{:02x}.{}", address.domain(), address.bus(), address.device(), address.function()), parent_directory)
    , m_address(address)
{
    m_components.append(PCIDeviceAttributeSysFSComponent::create("vendor", *this, PCI::RegisterOffset::VENDOR_ID, 2));
    m_components.append(PCIDeviceAttributeSysFSComponent::create("device_id", *this, PCI::RegisterOffset::DEVICE_ID, 2));
    m_components.append(PCIDeviceAttributeSysFSComponent::create("class", *this, PCI::RegisterOffset::CLASS, 1));
    m_components.append(PCIDeviceAttributeSysFSComponent::create("subclass", *this, PCI::RegisterOffset::SUBCLASS, 1));
    m_components.append(PCIDeviceAttributeSysFSComponent::create("revision", *this, PCI::RegisterOffset::REVISION_ID, 1));
    m_components.append(PCIDeviceAttributeSysFSComponent::create("progif", *this, PCI::RegisterOffset::PROG_IF, 1));
    m_components.append(PCIDeviceAttributeSysFSComponent::create("subsystem_vendor", *this, PCI::RegisterOffset::SUBSYSTEM_VENDOR_ID, 2));
    m_components.append(PCIDeviceAttributeSysFSComponent::create("subsystem_id", *this, PCI::RegisterOffset::SUBSYSTEM_ID, 2));
}

UNMAP_AFTER_INIT void PCIBusSysFSDirectory::initialize()
{
    auto pci_directory = adopt_ref(*new (nothrow) PCIBusSysFSDirectory());
    SysFSComponentRegistry::the().register_new_bus_directory(pci_directory);
}

UNMAP_AFTER_INIT PCIBusSysFSDirectory::PCIBusSysFSDirectory()
    : SysFSDirectory("pci", SysFSComponentRegistry::the().buses_directory())
{
    PCI::enumerate([&](DeviceIdentifier const& device_identifier) {
        auto pci_device = PCI::PCIDeviceSysFSDirectory::create(*this, device_identifier.address());
        m_components.append(pci_device);
    });
}

NonnullRefPtr<PCIDeviceAttributeSysFSComponent> PCIDeviceAttributeSysFSComponent::create(String name, const PCIDeviceSysFSDirectory& device, PCI::RegisterOffset offset, size_t field_bytes_width)
{
    return adopt_ref(*new (nothrow) PCIDeviceAttributeSysFSComponent(name, device, offset, field_bytes_width));
}

PCIDeviceAttributeSysFSComponent::PCIDeviceAttributeSysFSComponent(String name, const PCIDeviceSysFSDirectory& device, PCI::RegisterOffset offset, size_t field_bytes_width)
    : SysFSComponent(name)
    , m_device(device)
    , m_offset(offset)
    , m_field_bytes_width(field_bytes_width)
{
}

KResultOr<size_t> PCIDeviceAttributeSysFSComponent::read_bytes(off_t offset, size_t count, UserOrKernelBuffer& buffer, OpenFileDescription*) const
{
    auto blob = TRY(try_to_generate_buffer());

    if ((size_t)offset >= blob->size())
        return KSuccess;

    ssize_t nread = min(static_cast<off_t>(blob->size() - offset), static_cast<off_t>(count));
    TRY(buffer.write(blob->data() + offset, nread));
    return nread;
}

KResultOr<NonnullOwnPtr<KBuffer>> PCIDeviceAttributeSysFSComponent::try_to_generate_buffer() const
{
    String value;
    switch (m_field_bytes_width) {
    case 1:
        value = String::formatted("{:#x}", PCI::read8(m_device->address(), m_offset));
        break;
    case 2:
        value = String::formatted("{:#x}", PCI::read16(m_device->address(), m_offset));
        break;
    case 4:
        value = String::formatted("{:#x}", PCI::read32(m_device->address(), m_offset));
        break;
    default:
        VERIFY_NOT_REACHED();
    }

    return KBuffer::try_create_with_bytes(value.substring_view(0).bytes());
}
}
