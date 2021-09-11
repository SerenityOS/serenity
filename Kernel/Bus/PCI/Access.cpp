/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteReader.h>
#include <AK/HashTable.h>
#include <Kernel/API/KResult.h>
#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Debug.h>
#include <Kernel/Firmware/ACPI/Definitions.h>
#include <Kernel/IO.h>
#include <Kernel/Memory/MemoryManager.h>
#include <Kernel/Memory/Region.h>
#include <Kernel/Sections.h>

namespace Kernel::PCI {

#define PCI_MMIO_CONFIG_SPACE_SIZE 4096
#define MEMORY_RANGE_PER_BUS (PCI_MMIO_CONFIG_SPACE_SIZE * PCI_MAX_FUNCTIONS_PER_DEVICE * PCI_MAX_DEVICES_PER_BUS)

static Access* s_access;

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

UNMAP_AFTER_INIT bool Access::initialize_for_memory_access(PhysicalAddress mcfg_table)
{
    if (Access::is_initialized())
        return false;

    InterruptDisabler disabler;
    auto* access = new Access(Access::AccessType::Memory);
    if (!access->scan_pci_domains(mcfg_table))
        return false;
    access->rescan_hardware();
    dbgln_if(PCI_DEBUG, "PCI: MMIO access initialised.");
    return true;
}

UNMAP_AFTER_INIT bool Access::scan_pci_domains(PhysicalAddress mcfg_table)
{
    auto checkup_region_or_error = MM.allocate_kernel_region(mcfg_table.page_base(), (PAGE_SIZE * 2), "PCI MCFG Checkup", Memory::Region::Access::ReadWrite);
    if (checkup_region_or_error.is_error())
        return false;
    dbgln_if(PCI_DEBUG, "PCI: Checking MCFG Table length to choose the correct mapping size");
    auto* sdt = (ACPI::Structures::SDTHeader*)checkup_region_or_error.value()->vaddr().offset(mcfg_table.offset_in_page()).as_ptr();
    u32 length = sdt->length;
    u8 revision = sdt->revision;

    dbgln("PCI: MCFG, length: {}, revision: {}", length, revision);

    auto mcfg_region_or_error = MM.allocate_kernel_region(mcfg_table.page_base(), Memory::page_round_up(length) + PAGE_SIZE, "PCI Parsing MCFG", Memory::Region::Access::ReadWrite);
    if (mcfg_region_or_error.is_error())
        return false;
    auto& mcfg = *(ACPI::Structures::MCFG*)mcfg_region_or_error.value()->vaddr().offset(mcfg_table.offset_in_page()).as_ptr();
    dbgln_if(PCI_DEBUG, "PCI: Checking MCFG @ {}, {}", VirtualAddress(&mcfg), mcfg_table);
    for (u32 index = 0; index < ((mcfg.header.length - sizeof(ACPI::Structures::MCFG)) / sizeof(ACPI::Structures::PCI_MMIO_Descriptor)); index++) {
        u8 start_bus = mcfg.descriptors[index].start_pci_bus;
        u8 end_bus = mcfg.descriptors[index].end_pci_bus;
        u32 lower_addr = mcfg.descriptors[index].base_addr;

        auto result = m_domains.set(index, { PhysicalAddress(lower_addr), start_bus, end_bus });
        VERIFY(result == AK::HashSetResult::InsertedNewEntry);
        dmesgln("PCI: New PCI domain @ {}, PCI buses ({}-{})", PhysicalAddress { lower_addr }, start_bus, end_bus);
    }
    VERIFY(m_domains.contains(0));
    dmesgln("PCI: MMIO domain: {}", m_domains.size());
    return true;
}

UNMAP_AFTER_INIT bool Access::initialize_for_io_access()
{
    if (Access::is_initialized()) {
        return false;
    }
    auto* access = new Access(Access::AccessType::IO);
    access->rescan_hardware();
    dbgln_if(PCI_DEBUG, "PCI: IO access initialised.");
    return true;
}

UNMAP_AFTER_INIT Access::Access(AccessType access_type)
    : m_enumerated_buses(256, false)
    , m_access_type(access_type)
{
    if (access_type == AccessType::IO)
        dmesgln("PCI: Using I/O instructions for PCI configuration space access");
    else
        dmesgln("PCI: Using memory access for PCI configuration space accesses");
    s_access = this;
}

Optional<PhysicalAddress> Access::determine_memory_mapped_bus_base_address(u32 domain, u8 bus) const
{
    auto chosen_domain = m_domains.get(domain);
    if (!chosen_domain.has_value())
        return {};
    if (!(chosen_domain.value().start_bus() <= bus && bus <= chosen_domain.value().end_bus()))
        return {};
    return chosen_domain.value().paddr().offset(MEMORY_RANGE_PER_BUS * (bus - chosen_domain.value().start_bus()));
}

void Access::map_bus_region(u32 domain, u8 bus)
{
    VERIFY(m_access_lock.is_locked());
    if (m_mapped_bus == bus && m_mapped_bus_region)
        return;
    auto bus_base_address = determine_memory_mapped_bus_base_address(domain, bus);
    // FIXME: Find a way to propagate error from here.
    if (!bus_base_address.has_value())
        VERIFY_NOT_REACHED();
    auto region_or_error = MM.allocate_kernel_region(bus_base_address.value(), MEMORY_RANGE_PER_BUS, "PCI ECAM", Memory::Region::Access::ReadWrite);
    // FIXME: Find a way to propagate error from here.
    if (region_or_error.is_error())
        VERIFY_NOT_REACHED();
    m_mapped_bus_region = region_or_error.release_value();
    m_mapped_bus = bus;
    dbgln_if(PCI_DEBUG, "PCI: New PCI ECAM Mapped region for bus {} @ {} {}", bus, m_mapped_bus_region->vaddr(), m_mapped_bus_region->physical_page(0)->paddr());
}

VirtualAddress Access::get_device_configuration_memory_mapped_space(Address address)
{
    VERIFY(m_access_lock.is_locked());
    dbgln_if(PCI_DEBUG, "PCI: Getting device configuration space for {}", address);
    map_bus_region(address.domain(), address.bus());
    return m_mapped_bus_region->vaddr().offset(PCI_MMIO_CONFIG_SPACE_SIZE * address.function() + (PCI_MMIO_CONFIG_SPACE_SIZE * PCI_MAX_FUNCTIONS_PER_DEVICE) * address.device());
}

u8 Access::io_read8_field(Address address, u32 field)
{
    MutexLocker lock(m_access_lock);
    dbgln_if(PCI_DEBUG, "PCI: IO Reading 8-bit field {:#08x} for {}", field, address);
    IO::out32(PCI_ADDRESS_PORT, address.io_address_for_field(field));
    return IO::in8(PCI_VALUE_PORT + (field & 3));
}
u16 Access::io_read16_field(Address address, u32 field)
{
    MutexLocker lock(m_access_lock);
    dbgln_if(PCI_DEBUG, "PCI: IO Reading 16-bit field {:#08x} for {}", field, address);
    IO::out32(PCI_ADDRESS_PORT, address.io_address_for_field(field));
    return IO::in16(PCI_VALUE_PORT + (field & 2));
}
u32 Access::io_read32_field(Address address, u32 field)
{
    MutexLocker lock(m_access_lock);
    dbgln_if(PCI_DEBUG, "PCI: IO Reading 32-bit field {:#08x} for {}", field, address);
    IO::out32(PCI_ADDRESS_PORT, address.io_address_for_field(field));
    return IO::in32(PCI_VALUE_PORT);
}
void Access::io_write8_field(Address address, u32 field, u8 value)
{
    MutexLocker lock(m_access_lock);
    dbgln_if(PCI_DEBUG, "PCI: IO Writing to 8-bit field {:#08x}, value={:#02x} for {}", field, value, address);
    IO::out32(PCI_ADDRESS_PORT, address.io_address_for_field(field));
    IO::out8(PCI_VALUE_PORT + (field & 3), value);
}
void Access::io_write16_field(Address address, u32 field, u16 value)
{
    MutexLocker lock(m_access_lock);
    dbgln_if(PCI_DEBUG, "PCI: IO Writing to 16-bit field {:#08x}, value={:#02x} for {}", field, value, address);
    IO::out32(PCI_ADDRESS_PORT, address.io_address_for_field(field));
    IO::out16(PCI_VALUE_PORT + (field & 2), value);
}
void Access::io_write32_field(Address address, u32 field, u32 value)
{
    MutexLocker lock(m_access_lock);
    dbgln_if(PCI_DEBUG, "PCI: IO Writing to 32-bit field {:#08x}, value={:#02x} for {}", field, value, address);
    IO::out32(PCI_ADDRESS_PORT, address.io_address_for_field(field));
    IO::out32(PCI_VALUE_PORT, value);
}

u8 Access::memory_read8_field(Address address, u32 field)
{
    MutexLocker lock(m_access_lock);
    VERIFY(field <= 0xfff);
    dbgln_if(PCI_DEBUG, "PCI: MMIO Reading 8-bit field {:#08x} for {}", field, address);
    return *((volatile u8*)(get_device_configuration_memory_mapped_space(address).get() + (field & 0xfff)));
}
u16 Access::memory_read16_field(Address address, u32 field)
{
    MutexLocker lock(m_access_lock);
    VERIFY(field < 0xfff);
    dbgln_if(PCI_DEBUG, "PCI: MMIO Reading 16-bit field {:#08x} for {}", field, address);
    u16 data = 0;
    ByteReader::load<u16>(get_device_configuration_memory_mapped_space(address).offset(field & 0xfff).as_ptr(), data);
    return data;
}
u32 Access::memory_read32_field(Address address, u32 field)
{
    MutexLocker lock(m_access_lock);
    VERIFY(field <= 0xffc);
    dbgln_if(PCI_DEBUG, "PCI: MMIO Reading 32-bit field {:#08x} for {}", field, address);
    u32 data = 0;
    ByteReader::load<u32>(get_device_configuration_memory_mapped_space(address).offset(field & 0xfff).as_ptr(), data);
    return data;
}
void Access::memory_write8_field(Address address, u32 field, u8 value)
{
    MutexLocker lock(m_access_lock);
    VERIFY(field <= 0xfff);
    dbgln_if(PCI_DEBUG, "PCI: MMIO Writing 8-bit field {:#08x}, value={:#02x} for {}", field, value, address);
    *((volatile u8*)(get_device_configuration_memory_mapped_space(address).get() + (field & 0xfff))) = value;
}
void Access::memory_write16_field(Address address, u32 field, u16 value)
{
    MutexLocker lock(m_access_lock);
    VERIFY(field < 0xfff);
    dbgln_if(PCI_DEBUG, "PCI: MMIO Writing 16-bit field {:#08x}, value={:#02x} for {}", field, value, address);
    ByteReader::store<u16>(get_device_configuration_memory_mapped_space(address).offset(field & 0xfff).as_ptr(), value);
}
void Access::memory_write32_field(Address address, u32 field, u32 value)
{
    MutexLocker lock(m_access_lock);
    VERIFY(field <= 0xffc);
    dbgln_if(PCI_DEBUG, "PCI: MMIO Writing 32-bit field {:#08x}, value={:#02x} for {}", field, value, address);
    ByteReader::store<u32>(get_device_configuration_memory_mapped_space(address).offset(field & 0xfff).as_ptr(), value);
}

void Access::write8_field(Address address, u32 field, u8 value)
{
    switch (m_access_type) {
    case AccessType::IO:
        io_write8_field(address, field, value);
        return;
    case AccessType::Memory:
        memory_write8_field(address, field, value);
        return;
    }
    VERIFY_NOT_REACHED();
}
void Access::write16_field(Address address, u32 field, u16 value)
{
    switch (m_access_type) {
    case AccessType::IO:
        io_write16_field(address, field, value);
        return;
    case AccessType::Memory:
        memory_write16_field(address, field, value);
        return;
    }
    VERIFY_NOT_REACHED();
}
void Access::write32_field(Address address, u32 field, u32 value)
{
    switch (m_access_type) {
    case AccessType::IO:
        io_write32_field(address, field, value);
        return;
    case AccessType::Memory:
        memory_write32_field(address, field, value);
        return;
    }
    VERIFY_NOT_REACHED();
}

u8 Access::read8_field(Address address, u32 field)
{
    switch (m_access_type) {
    case AccessType::IO:
        return io_read8_field(address, field);
    case AccessType::Memory:
        return memory_read8_field(address, field);
    }
    VERIFY_NOT_REACHED();
}
u16 Access::read16_field(Address address, u32 field)
{
    switch (m_access_type) {
    case AccessType::IO:
        return io_read16_field(address, field);
    case AccessType::Memory:
        return memory_read16_field(address, field);
    }
    VERIFY_NOT_REACHED();
}
u32 Access::read32_field(Address address, u32 field)
{
    switch (m_access_type) {
    case AccessType::IO:
        return io_read32_field(address, field);
    case AccessType::Memory:
        return memory_read32_field(address, field);
    }
    VERIFY_NOT_REACHED();
}

UNMAP_AFTER_INIT void Access::rescan_hardware()
{
    MutexLocker locker(m_scan_lock);
    VERIFY(m_physical_ids.is_empty());
    if (m_access_type == AccessType::IO) {
        dbgln_if(PCI_DEBUG, "PCI: IO enumerating hardware");

        // First scan bus 0. Find any device on that bus, and if it's a PCI-to-PCI
        // bridge, recursively scan it too.
        m_enumerated_buses.set(0, true);
        enumerate_bus(-1, 0, true);

        // Handle Multiple PCI host bridges on slot 0, device 0.
        // If we happen to miss some PCI buses because they are not reachable through
        // recursive PCI-to-PCI bridges starting from bus 0, we might find them here.
        if ((read8_field(Address(), PCI_HEADER_TYPE) & 0x80) != 0) {
            for (int bus = 1; bus < 256; ++bus) {
                if (read16_field(Address(0, 0, 0, bus), PCI_VENDOR_ID) == PCI_NONE)
                    continue;
                if (read16_field(Address(0, 0, 0, bus), PCI_CLASS) != 0x6)
                    continue;
                if (m_enumerated_buses.get(bus))
                    continue;
                enumerate_bus(-1, bus, false);
                m_enumerated_buses.set(bus, true);
            }
        }
        return;
    }
    VERIFY(m_access_type == AccessType::Memory);

    for (u32 domain = 0; domain < m_domains.size(); domain++) {
        dbgln_if(PCI_DEBUG, "PCI: Scan memory mapped domain {}", domain);
        // Single PCI host controller.
        if ((read8_field(Address(domain), PCI_HEADER_TYPE) & 0x80) == 0) {
            enumerate_bus(-1, 0, true);
            return;
        }

        // Multiple PCI host controllers.
        for (u8 function = 0; function < 8; ++function) {
            if (read16_field(Address(domain, 0, 0, function), PCI_VENDOR_ID) == PCI_NONE)
                break;
            enumerate_bus(-1, function, false);
        }
    }
}

UNMAP_AFTER_INIT Optional<u8> Access::get_capabilities_pointer(Address address)
{
    dbgln_if(PCI_DEBUG, "PCI: Getting capabilities pointer for {}", address);
    if (read16_field(address, PCI_STATUS) & (1 << 4)) {
        dbgln_if(PCI_DEBUG, "PCI: Found capabilities pointer for {}", address);
        return read8_field(address, PCI_CAPABILITIES_POINTER);
    }
    dbgln_if(PCI_DEBUG, "PCI: No capabilities pointer for {}", address);
    return {};
}

UNMAP_AFTER_INIT Vector<Capability> Access::get_capabilities(Address address)
{
    dbgln_if(PCI_DEBUG, "PCI: Getting capabilities for {}", address);
    auto capabilities_pointer = get_capabilities_pointer(address);
    if (!capabilities_pointer.has_value()) {
        dbgln_if(PCI_DEBUG, "PCI: No capabilities for {}", address);
        return {};
    }
    Vector<Capability> capabilities;
    auto capability_pointer = capabilities_pointer.value();
    while (capability_pointer != 0) {
        dbgln_if(PCI_DEBUG, "PCI: Reading in capability at {:#02x} for {}", capability_pointer, address);
        u16 capability_header = read16_field(address, capability_pointer);
        u8 capability_id = capability_header & 0xff;
        capabilities.append({ address, capability_id, capability_pointer });
        capability_pointer = capability_header >> 8;
    }
    return capabilities;
}

UNMAP_AFTER_INIT void Access::enumerate_functions(int type, u8 bus, u8 device, u8 function, bool recursive)
{
    dbgln_if(PCI_DEBUG, "PCI: Enumerating function type={}, bus={}, device={}, function={}", type, bus, device, function);
    Address address(0, bus, device, function);
    auto read_type = (read8_field(address, PCI_CLASS) << 8u) | read8_field(address, PCI_SUBCLASS);
    if (type == -1 || type == read_type) {
        m_physical_ids.append(PhysicalID { address, { read16_field(address, PCI_VENDOR_ID), read16_field(address, PCI_DEVICE_ID) }, get_capabilities(address) });
    }

    if (read_type == PCI_TYPE_BRIDGE && recursive && (!m_enumerated_buses.get(read8_field(address, PCI_SECONDARY_BUS)))) {
        u8 secondary_bus = read8_field(address, PCI_SECONDARY_BUS);
        dbgln_if(PCI_DEBUG, "PCI: Found secondary bus: {}", secondary_bus);
        VERIFY(secondary_bus != bus);
        m_enumerated_buses.set(secondary_bus, true);
        enumerate_bus(type, secondary_bus, recursive);
    }
}

UNMAP_AFTER_INIT void Access::enumerate_device(int type, u8 bus, u8 device, bool recursive)
{
    dbgln_if(PCI_DEBUG, "PCI: Enumerating device type={}, bus={}, device={}", type, bus, device);
    Address address(0, bus, device, 0);
    if (read16_field(address, PCI_VENDOR_ID) == PCI_NONE)
        return;
    enumerate_functions(type, bus, device, 0, recursive);
    if (!(read8_field(address, PCI_HEADER_TYPE) & 0x80))
        return;
    for (u8 function = 1; function < 8; ++function) {
        Address address(0, bus, device, function);
        if (read16_field(address, PCI_VENDOR_ID) != PCI_NONE)
            enumerate_functions(type, bus, device, function, recursive);
    }
}

UNMAP_AFTER_INIT void Access::enumerate_bus(int type, u8 bus, bool recursive)
{
    dbgln_if(PCI_DEBUG, "PCI: Enumerating bus type={}, bus={}", type, bus);
    for (u8 device = 0; device < 32; ++device)
        enumerate_device(type, bus, device, recursive);
}

void Access::fast_enumerate(Function<void(Address, ID)>& callback) const
{
    MutexLocker locker(m_scan_lock);
    VERIFY(!m_physical_ids.is_empty());
    for (auto& physical_id : m_physical_ids) {
        callback(physical_id.address(), physical_id.id());
    }
}

PhysicalID Access::get_physical_id(Address address) const
{
    for (auto physical_id : m_physical_ids) {
        if (physical_id.address().domain() == address.domain()
            && physical_id.address().bus() == address.bus()
            && physical_id.address().device() == address.device()
            && physical_id.address().function() == address.function()) {
            return physical_id;
        }
    }
    VERIFY_NOT_REACHED();
}

}
