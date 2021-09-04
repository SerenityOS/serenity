/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Bus/PCI/IOAccess.h>
#include <Kernel/Debug.h>
#include <Kernel/IO.h>
#include <Kernel/Sections.h>

namespace Kernel {
namespace PCI {

static Access* s_access;

inline void write8(Address address, u32 field, u8 value) { Access::the().write8_field(address, field, value); }
inline void write16(Address address, u32 field, u16 value) { Access::the().write16_field(address, field, value); }
inline void write32(Address address, u32 field, u32 value) { Access::the().write32_field(address, field, value); }
inline u8 read8(Address address, u32 field) { return Access::the().read8_field(address, field); }
inline u16 read16(Address address, u32 field) { return Access::the().read16_field(address, field); }
inline u32 read32(Address address, u32 field) { return Access::the().read32_field(address, field); }

Access& Access::the()
{
    if (s_access == nullptr) {
        VERIFY_NOT_REACHED(); // We failed to initialize the PCI subsystem, so stop here!
    }
    return *s_access;
}

bool Access::is_initialized()
{
    return (s_access != nullptr);
}

UNMAP_AFTER_INIT Access::Access()
    : m_enumerated_buses(256, false)
{
    s_access = this;
}

PhysicalID Access::get_physical_id(Address address) const
{
    for (auto physical_id : m_physical_ids) {
        if (physical_id.address().seg() == address.seg()
            && physical_id.address().bus() == address.bus()
            && physical_id.address().device() == address.device()
            && physical_id.address().function() == address.function()) {
            return physical_id;
        }
    }
    VERIFY_NOT_REACHED();
}

u8 Access::early_read8_field(Address address, u32 field)
{
    dbgln_if(PCI_DEBUG, "PCI: Early reading 8-bit field {:#08x} for {}", field, address);
    IO::out32(PCI_ADDRESS_PORT, address.io_address_for_field(field));
    return IO::in8(PCI_VALUE_PORT + (field & 3));
}

u16 Access::early_read16_field(Address address, u32 field)
{
    dbgln_if(PCI_DEBUG, "PCI: Early reading 16-bit field {:#08x} for {}", field, address);
    IO::out32(PCI_ADDRESS_PORT, address.io_address_for_field(field));
    return IO::in16(PCI_VALUE_PORT + (field & 2));
}

u32 Access::early_read32_field(Address address, u32 field)
{
    dbgln_if(PCI_DEBUG, "PCI: Early reading 32-bit field {:#08x} for {}", field, address);
    IO::out32(PCI_ADDRESS_PORT, address.io_address_for_field(field));
    return IO::in32(PCI_VALUE_PORT);
}

u16 Access::early_read_type(Address address)
{
    dbgln_if(PCI_DEBUG, "PCI: Early reading type for {}", address);
    return (early_read8_field(address, PCI_CLASS) << 8u) | early_read8_field(address, PCI_SUBCLASS);
}

UNMAP_AFTER_INIT void Access::enumerate_functions(int type, u8 bus, u8 device, u8 function, Function<void(Address, ID)>& callback, bool recursive)
{
    dbgln_if(PCI_DEBUG, "PCI: Enumerating function type={}, bus={}, device={}, function={}", type, bus, device, function);
    Address address(0, bus, device, function);
    if (type == -1 || type == early_read_type(address))
        callback(address, { early_read16_field(address, PCI_VENDOR_ID), early_read16_field(address, PCI_DEVICE_ID) });
    if (early_read_type(address) == PCI_TYPE_BRIDGE && recursive && (!m_enumerated_buses.get(early_read8_field(address, PCI_SECONDARY_BUS)))) {
        u8 secondary_bus = early_read8_field(address, PCI_SECONDARY_BUS);
        dbgln_if(PCI_DEBUG, "PCI: Found secondary bus: {}", secondary_bus);
        VERIFY(secondary_bus != bus);
        m_enumerated_buses.set(secondary_bus, true);
        enumerate_bus(type, secondary_bus, callback, recursive);
    }
}

UNMAP_AFTER_INIT void Access::enumerate_device(int type, u8 bus, u8 device, Function<void(Address, ID)>& callback, bool recursive)
{
    dbgln_if(PCI_DEBUG, "PCI: Enumerating device type={}, bus={}, device={}", type, bus, device);
    Address address(0, bus, device, 0);
    if (early_read16_field(address, PCI_VENDOR_ID) == PCI_NONE)
        return;
    enumerate_functions(type, bus, device, 0, callback, recursive);
    if (!(early_read8_field(address, PCI_HEADER_TYPE) & 0x80))
        return;
    for (u8 function = 1; function < 8; ++function) {
        Address address(0, bus, device, function);
        if (early_read16_field(address, PCI_VENDOR_ID) != PCI_NONE)
            enumerate_functions(type, bus, device, function, callback, recursive);
    }
}

UNMAP_AFTER_INIT void Access::enumerate_bus(int type, u8 bus, Function<void(Address, ID)>& callback, bool recursive)
{
    dbgln_if(PCI_DEBUG, "PCI: Enumerating bus type={}, bus={}", type, bus);
    for (u8 device = 0; device < 32; ++device)
        enumerate_device(type, bus, device, callback, recursive);
}

void Access::enumerate(Function<void(Address, ID)>& callback) const
{
    for (auto& physical_id : m_physical_ids) {
        callback(physical_id.address(), physical_id.id());
    }
}

void enumerate(Function<void(Address, ID)> callback)
{
    Access::the().enumerate(callback);
}

UNMAP_AFTER_INIT Optional<u8> get_capabilities_pointer(Address address)
{
    dbgln_if(PCI_DEBUG, "PCI: Getting capabilities pointer for {}", address);
    if (PCI::read16(address, PCI_STATUS) & (1 << 4)) {
        dbgln_if(PCI_DEBUG, "PCI: Found capabilities pointer for {}", address);
        return PCI::read8(address, PCI_CAPABILITIES_POINTER);
    }
    dbgln_if(PCI_DEBUG, "PCI: No capabilities pointer for {}", address);
    return {};
}

PhysicalID get_physical_id(Address address)
{
    return Access::the().get_physical_id(address);
}

UNMAP_AFTER_INIT Vector<Capability> get_capabilities(Address address)
{
    dbgln_if(PCI_DEBUG, "PCI: Getting capabilities for {}", address);
    auto capabilities_pointer = PCI::get_capabilities_pointer(address);
    if (!capabilities_pointer.has_value()) {
        dbgln_if(PCI_DEBUG, "PCI: No capabilities for {}", address);
        return {};
    }
    Vector<Capability> capabilities;
    auto capability_pointer = capabilities_pointer.value();
    while (capability_pointer != 0) {
        dbgln_if(PCI_DEBUG, "PCI: Reading in capability at {:#02x} for {}", capability_pointer, address);
        u16 capability_header = PCI::read16(address, capability_pointer);
        u8 capability_id = capability_header & 0xff;
        capabilities.append({ address, capability_id, capability_pointer });
        capability_pointer = capability_header >> 8;
    }
    return capabilities;
}

void raw_access(Address address, u32 field, size_t access_size, u32 value)
{
    VERIFY(access_size != 0);
    if (access_size == 1) {
        write8(address, field, value);
        return;
    }
    if (access_size == 2) {
        write16(address, field, value);
        return;
    }
    if (access_size == 4) {
        write32(address, field, value);
        return;
    }
    VERIFY_NOT_REACHED();
}

ID get_id(Address address)
{
    return { read16(address, PCI_VENDOR_ID), read16(address, PCI_DEVICE_ID) };
}

void enable_io_space(Address address)
{
    write16(address, PCI_COMMAND, read16(address, PCI_COMMAND) | (1 << 0));
}
void disable_io_space(Address address)
{
    write16(address, PCI_COMMAND, read16(address, PCI_COMMAND) & ~(1 << 0));
}

void enable_memory_space(Address address)
{
    write16(address, PCI_COMMAND, read16(address, PCI_COMMAND) | (1 << 1));
}
void disable_memory_space(Address address)
{
    write16(address, PCI_COMMAND, read16(address, PCI_COMMAND) & ~(1 << 1));
}
bool is_io_space_enabled(Address address)
{
    return (read16(address, PCI_COMMAND) & 1) != 0;
}

void enable_interrupt_line(Address address)
{
    write16(address, PCI_COMMAND, read16(address, PCI_COMMAND) & ~(1 << 10));
}

void disable_interrupt_line(Address address)
{
    write16(address, PCI_COMMAND, read16(address, PCI_COMMAND) | 1 << 10);
}

u8 get_interrupt_line(Address address)
{
    return read8(address, PCI_INTERRUPT_LINE);
}

u32 get_BAR0(Address address)
{
    return read32(address, PCI_BAR0);
}

u32 get_BAR1(Address address)
{
    return read32(address, PCI_BAR1);
}

u32 get_BAR2(Address address)
{
    return read32(address, PCI_BAR2);
}

u32 get_BAR3(Address address)
{
    return read16(address, PCI_BAR3);
}

u32 get_BAR4(Address address)
{
    return read32(address, PCI_BAR4);
}

u32 get_BAR5(Address address)
{
    return read32(address, PCI_BAR5);
}

u32 get_BAR(Address address, u8 bar)
{
    VERIFY(bar <= 5);
    switch (bar) {
    case 0:
        return get_BAR0(address);
    case 1:
        return get_BAR1(address);
    case 2:
        return get_BAR2(address);
    case 3:
        return get_BAR3(address);
    case 4:
        return get_BAR4(address);
    case 5:
        return get_BAR5(address);
    default:
        VERIFY_NOT_REACHED();
    }
}

u8 get_revision_id(Address address)
{
    return read8(address, PCI_REVISION_ID);
}

u8 get_subclass(Address address)
{
    return read8(address, PCI_SUBCLASS);
}

u8 get_class(Address address)
{
    return read8(address, PCI_CLASS);
}

u8 get_programming_interface(Address address)
{
    return read8(address, PCI_PROG_IF);
}

u16 get_subsystem_id(Address address)
{
    return read16(address, PCI_SUBSYSTEM_ID);
}

u16 get_subsystem_vendor_id(Address address)
{
    return read16(address, PCI_SUBSYSTEM_VENDOR_ID);
}

void enable_bus_mastering(Address address)
{
    auto value = read16(address, PCI_COMMAND);
    value |= (1 << 2);
    value |= (1 << 0);
    write16(address, PCI_COMMAND, value);
}

void disable_bus_mastering(Address address)
{
    auto value = read16(address, PCI_COMMAND);
    value &= ~(1 << 2);
    value |= (1 << 0);
    write16(address, PCI_COMMAND, value);
}

size_t get_BAR_space_size(Address address, u8 bar_number)
{
    // See PCI Spec 2.3, Page 222
    VERIFY(bar_number < 6);
    u8 field = (PCI_BAR0 + (bar_number << 2));
    u32 bar_reserved = read32(address, field);
    write32(address, field, 0xFFFFFFFF);
    u32 space_size = read32(address, field);
    write32(address, field, bar_reserved);
    space_size &= 0xfffffff0;
    space_size = (~space_size) + 1;
    return space_size;
}

u8 Capability::read8(u32 field) const
{
    return PCI::read8(m_address, m_ptr + field);
}

u16 Capability::read16(u32 field) const
{
    return PCI::read16(m_address, m_ptr + field);
}

u32 Capability::read32(u32 field) const
{
    return PCI::read32(m_address, m_ptr + field);
}

void Capability::write8(u32 field, u8 value)
{
    PCI::write8(m_address, m_ptr + field, value);
}

void Capability::write16(u32 field, u16 value)
{
    PCI::write16(m_address, m_ptr + field, value);
}

void Capability::write32(u32 field, u32 value)
{
    PCI::write32(m_address, m_ptr + field, value);
}

UNMAP_AFTER_INIT NonnullRefPtr<PCIDeviceSysFSDirectory> PCIDeviceSysFSDirectory::create(SysFSDirectory const& parent_directory, Address address)
{
    return adopt_ref(*new (nothrow) PCIDeviceSysFSDirectory(parent_directory, address));
}

UNMAP_AFTER_INIT PCIDeviceSysFSDirectory::PCIDeviceSysFSDirectory(SysFSDirectory const& parent_directory, Address address)
    : SysFSDirectory(String::formatted("{:04x}:{:02x}:{:02x}.{}", address.seg(), address.bus(), address.device(), address.function()), parent_directory)
    , m_address(address)
{
    m_components.append(PCIDeviceAttributeSysFSComponent::create("vendor", *this, PCI_VENDOR_ID, 2));
    m_components.append(PCIDeviceAttributeSysFSComponent::create("device_id", *this, PCI_DEVICE_ID, 2));
    m_components.append(PCIDeviceAttributeSysFSComponent::create("class", *this, PCI_CLASS, 1));
    m_components.append(PCIDeviceAttributeSysFSComponent::create("subclass", *this, PCI_SUBCLASS, 1));
    m_components.append(PCIDeviceAttributeSysFSComponent::create("revision", *this, PCI_REVISION_ID, 1));
    m_components.append(PCIDeviceAttributeSysFSComponent::create("progif", *this, PCI_PROG_IF, 1));
    m_components.append(PCIDeviceAttributeSysFSComponent::create("subsystem_vendor", *this, PCI_SUBSYSTEM_VENDOR_ID, 2));
    m_components.append(PCIDeviceAttributeSysFSComponent::create("subsystem_id", *this, PCI_SUBSYSTEM_ID, 2));
}

UNMAP_AFTER_INIT void PCIBusSysFSDirectory::initialize()
{
    auto pci_directory = adopt_ref(*new (nothrow) PCIBusSysFSDirectory());
    SysFSComponentRegistry::the().register_new_component(pci_directory);
}

UNMAP_AFTER_INIT PCIBusSysFSDirectory::PCIBusSysFSDirectory()
    : SysFSDirectory("pci", SysFSComponentRegistry::the().root_directory())
{
    PCI::enumerate([&](Address const& address, ID) {
        auto pci_device = PCI::PCIDeviceSysFSDirectory::create(*this, address);
        m_components.append(pci_device);
    });
}

NonnullRefPtr<PCIDeviceAttributeSysFSComponent> PCIDeviceAttributeSysFSComponent::create(String name, PCIDeviceSysFSDirectory const& device, size_t offset, size_t field_bytes_width)
{
    return adopt_ref(*new (nothrow) PCIDeviceAttributeSysFSComponent(name, device, offset, field_bytes_width));
}

PCIDeviceAttributeSysFSComponent::PCIDeviceAttributeSysFSComponent(String name, PCIDeviceSysFSDirectory const& device, size_t offset, size_t field_bytes_width)
    : SysFSComponent(name)
    , m_device(device)
    , m_offset(offset)
    , m_field_bytes_width(field_bytes_width)
{
}

KResultOr<size_t> PCIDeviceAttributeSysFSComponent::read_bytes(off_t offset, size_t count, UserOrKernelBuffer& buffer, FileDescription*) const
{
    auto blob = try_to_generate_buffer();
    if (!blob)
        return KResult(EFAULT);

    if ((size_t)offset >= blob->size())
        return KSuccess;

    ssize_t nread = min(static_cast<off_t>(blob->size() - offset), static_cast<off_t>(count));
    if (!buffer.write(blob->data() + offset, nread))
        return KResult(EFAULT);
    return nread;
}

OwnPtr<KBuffer> PCIDeviceAttributeSysFSComponent::try_to_generate_buffer() const
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
}
