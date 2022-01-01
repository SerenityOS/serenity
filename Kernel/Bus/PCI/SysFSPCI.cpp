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
    // FIXME: Handle allocation failure gracefully
    auto device_name = MUST(KString::formatted("{:04x}:{:02x}:{:02x}.{}", address.domain(), address.bus(), address.device(), address.function()));
    return adopt_ref(*new (nothrow) PCIDeviceSysFSDirectory(move(device_name), parent_directory, address));
}

UNMAP_AFTER_INIT PCIDeviceSysFSDirectory::PCIDeviceSysFSDirectory(NonnullOwnPtr<KString> device_directory_name, const SysFSDirectory& parent_directory, Address address)
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
}

UNMAP_AFTER_INIT void PCIBusSysFSDirectory::initialize()
{
    auto pci_directory = adopt_ref(*new (nothrow) PCIBusSysFSDirectory());
    SysFSComponentRegistry::the().register_new_bus_directory(pci_directory);
}

UNMAP_AFTER_INIT PCIBusSysFSDirectory::PCIBusSysFSDirectory()
    : SysFSDirectory(SysFSComponentRegistry::the().buses_directory())
{
    PCI::enumerate([&](DeviceIdentifier const& device_identifier) {
        auto pci_device = PCI::PCIDeviceSysFSDirectory::create(*this, device_identifier.address());
        m_components.append(pci_device);
    });
}

StringView PCIDeviceAttributeSysFSComponent::name() const
{
    switch (m_offset) {
    case PCI::RegisterOffset::VENDOR_ID:
        return "vendor"sv;
    case PCI::RegisterOffset::DEVICE_ID:
        return "device_id"sv;
    case PCI::RegisterOffset::CLASS:
        return "class"sv;
    case PCI::RegisterOffset::SUBCLASS:
        return "subclass"sv;
    case PCI::RegisterOffset::REVISION_ID:
        return "revision"sv;
    case PCI::RegisterOffset::PROG_IF:
        return "progif"sv;
    case PCI::RegisterOffset::SUBSYSTEM_VENDOR_ID:
        return "subsystem_vendor"sv;
    case PCI::RegisterOffset::SUBSYSTEM_ID:
        return "subsystem_id"sv;
    default:
        VERIFY_NOT_REACHED();
    }
}

NonnullRefPtr<PCIDeviceAttributeSysFSComponent> PCIDeviceAttributeSysFSComponent::create(const PCIDeviceSysFSDirectory& device, PCI::RegisterOffset offset, size_t field_bytes_width)
{
    return adopt_ref(*new (nothrow) PCIDeviceAttributeSysFSComponent(device, offset, field_bytes_width));
}

PCIDeviceAttributeSysFSComponent::PCIDeviceAttributeSysFSComponent(const PCIDeviceSysFSDirectory& device, PCI::RegisterOffset offset, size_t field_bytes_width)
    : SysFSComponent()
    , m_device(device)
    , m_offset(offset)
    , m_field_bytes_width(field_bytes_width)
{
}

ErrorOr<size_t> PCIDeviceAttributeSysFSComponent::read_bytes(off_t offset, size_t count, UserOrKernelBuffer& buffer, OpenFileDescription*) const
{
    auto blob = TRY(try_to_generate_buffer());

    if ((size_t)offset >= blob->size())
        return 0;

    ssize_t nread = min(static_cast<off_t>(blob->size() - offset), static_cast<off_t>(count));
    TRY(buffer.write(blob->data() + offset, nread));
    return nread;
}

ErrorOr<NonnullOwnPtr<KBuffer>> PCIDeviceAttributeSysFSComponent::try_to_generate_buffer() const
{
    OwnPtr<KString> value;
    switch (m_field_bytes_width) {
    case 1:
        value = TRY(KString::formatted("{:#x}", PCI::read8(m_device->address(), m_offset)));
        break;
    case 2:
        value = TRY(KString::formatted("{:#x}", PCI::read16(m_device->address(), m_offset)));
        break;
    case 4:
        value = TRY(KString::formatted("{:#x}", PCI::read32(m_device->address(), m_offset)));
        break;
    default:
        VERIFY_NOT_REACHED();
    }

    return KBuffer::try_create_with_bytes(value->view().bytes());
}
}
