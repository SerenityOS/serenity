/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteReader.h>
#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Bus/PCI/Controller/MemoryBackedHostBridge.h>
#include <Kernel/Library/StdLib.h>
#include <Kernel/Memory/MemoryManager.h>

namespace Kernel::PCI {

ErrorOr<NonnullRefPtr<MemoryBackedHostBridge>> MemoryBackedHostBridge::create(Domain const& domain, RefPtr<Bus> parent_bus, PhysicalAddress start_address)
{
    auto region = TRY(MM.allocate_kernel_region(start_address, mmio_device_space_size, "PCI ECAM"sv, Memory::Region::Access::ReadWrite));
    Array<u8, to_underlying(PCI::RegisterOffset::__END)> host_bridge_device_registers;
    memcpy(host_bridge_device_registers.data(), region->vaddr().as_ptr(), host_bridge_device_registers.size());

    auto get_u16_field = [host_bridge_device_registers](PCI::RegisterOffset offset) -> u16 {
        return host_bridge_device_registers[to_underlying(offset)]
            | (static_cast<u16>(host_bridge_device_registers[to_underlying(offset) + 1]) << 8);
    };
    auto get_u8_field = [host_bridge_device_registers](PCI::RegisterOffset offset) -> u16 {
        return host_bridge_device_registers[to_underlying(offset)];
    };

    Address address(domain.domain_number(), domain.start_bus(), 0, 0);
    HardwareID id = { get_u16_field(PCI::RegisterOffset::VENDOR_ID), get_u16_field(PCI::RegisterOffset::DEVICE_ID) };
    ClassCode class_code = get_u8_field(PCI::RegisterOffset::CLASS);
    SubclassCode subclass_code = get_u8_field(PCI::RegisterOffset::SUBCLASS);
    ProgrammingInterface prog_if = get_u8_field(PCI::RegisterOffset::PROG_IF);
    RevisionID revision_id = get_u8_field(PCI::RegisterOffset::REVISION_ID);
    SubsystemID subsystem_id = get_u16_field(PCI::RegisterOffset::SUBSYSTEM_ID);
    SubsystemVendorID subsystem_vendor_id = get_u16_field(PCI::RegisterOffset::SUBSYSTEM_VENDOR_ID);
    InterruptLine interrupt_line = get_u8_field(PCI::RegisterOffset::INTERRUPT_LINE);
    InterruptPin interrupt_pin = get_u8_field(PCI::RegisterOffset::INTERRUPT_PIN);
    auto identifier = EnumerableDeviceIdentifier { address, id, revision_id, class_code, subclass_code, prog_if, subsystem_id, subsystem_vendor_id, interrupt_line, interrupt_pin };
    auto host_bridge_device = TRY(Device::from_enumerable_identifier(identifier));

    dbgln("PCI: Memory Backed Host Bridge: {:04X}:{:02X}:{:02X}:{:02X}: vendor, dev_id {:04x}:{:04x}, starting at {:p}",
        (u32)domain.domain_number(),
        (u8)domain.start_bus(),
        (u8)0,
        (u8)0,
        (u16)id.vendor_id,
        (u16)id.device_id,
        start_address);

    if (parent_bus)
        host_bridge_device->set_parent_bus(*parent_bus);
    auto bus = TRY(Bus::create(domain.start_bus(), host_bridge_device, parent_bus));
    auto bridge = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) MemoryBackedHostBridge(domain, bus, start_address)));
    host_bridge_device->set_host_controller(*bridge);

    {
        SpinlockLocker locker(bridge->m_access_lock);
        TRY(bridge->enumerate_capabilities_for_function(bus, host_bridge_device));
    }

    return bridge;
}

MemoryBackedHostBridge::MemoryBackedHostBridge(PCI::Domain const& domain, NonnullRefPtr<Bus> root_bus, PhysicalAddress start_address)
    : HostController(domain, root_bus)
    , m_start_address(start_address)
{
}

u8 MemoryBackedHostBridge::read8_field_locked(BusNumber bus, DeviceNumber device, FunctionNumber function, u32 field)
{
    VERIFY(m_access_lock.is_locked());
    VERIFY(field <= 0xfff);
    return *((u8 volatile*)(get_device_configuration_memory_mapped_space(bus, device, function).get() + (field & 0xfff)));
}
u16 MemoryBackedHostBridge::read16_field_locked(BusNumber bus, DeviceNumber device, FunctionNumber function, u32 field)
{
    VERIFY(m_access_lock.is_locked());
    VERIFY(field < 0xfff);
    u16 data = 0;
    ByteReader::load<u16>(get_device_configuration_memory_mapped_space(bus, device, function).offset(field & 0xfff).as_ptr(), data);
    return data;
}
u32 MemoryBackedHostBridge::read32_field_locked(BusNumber bus, DeviceNumber device, FunctionNumber function, u32 field)
{
    VERIFY(m_access_lock.is_locked());
    VERIFY(field <= 0xffc);
    u32 data = 0;
    ByteReader::load<u32>(get_device_configuration_memory_mapped_space(bus, device, function).offset(field & 0xfff).as_ptr(), data);
    return data;
}
void MemoryBackedHostBridge::write8_field_locked(BusNumber bus, DeviceNumber device, FunctionNumber function, u32 field, u8 value)
{
    VERIFY(m_access_lock.is_locked());
    VERIFY(field <= 0xfff);
    *((u8 volatile*)(get_device_configuration_memory_mapped_space(bus, device, function).get() + (field & 0xfff))) = value;
}
void MemoryBackedHostBridge::write16_field_locked(BusNumber bus, DeviceNumber device, FunctionNumber function, u32 field, u16 value)
{
    VERIFY(m_access_lock.is_locked());
    VERIFY(field < 0xfff);
    ByteReader::store<u16>(get_device_configuration_memory_mapped_space(bus, device, function).offset(field & 0xfff).as_ptr(), value);
}
void MemoryBackedHostBridge::write32_field_locked(BusNumber bus, DeviceNumber device, FunctionNumber function, u32 field, u32 value)
{
    VERIFY(m_access_lock.is_locked());
    VERIFY(field <= 0xffc);
    ByteReader::store<u32>(get_device_configuration_memory_mapped_space(bus, device, function).offset(field & 0xfff).as_ptr(), value);
}

void MemoryBackedHostBridge::map_bus_region(BusNumber bus)
{
    VERIFY(m_access_lock.is_locked());
    if (m_mapped_bus == bus && m_mapped_bus_region)
        return;
    auto bus_base_address = determine_memory_mapped_bus_base_address(bus);
    auto region_or_error = MM.allocate_kernel_region(bus_base_address, memory_range_per_bus, "PCI ECAM"sv, Memory::Region::Access::ReadWrite);
    // FIXME: Find a way to propagate error from here.
    if (region_or_error.is_error())
        VERIFY_NOT_REACHED();
    m_mapped_bus_region = region_or_error.release_value();
    m_mapped_bus = bus;
    dbgln_if(PCI_DEBUG, "PCI: New PCI ECAM Mapped region for bus {} @ {} {}", bus, m_mapped_bus_region->vaddr(), m_mapped_bus_region->physical_page(0)->paddr());
}

VirtualAddress MemoryBackedHostBridge::get_device_configuration_memory_mapped_space(BusNumber bus, DeviceNumber device, FunctionNumber function)
{
    VERIFY(m_access_lock.is_locked());
    map_bus_region(bus);
    return m_mapped_bus_region->vaddr().offset(mmio_device_space_size * function.value() + (mmio_device_space_size * to_underlying(Limits::MaxFunctionsPerDevice)) * device.value());
}

PhysicalAddress MemoryBackedHostBridge::determine_memory_mapped_bus_base_address(BusNumber bus) const
{
    auto start_bus = min(bus.value(), m_domain.start_bus());
    return m_start_address.offset(memory_range_per_bus * (bus.value() - start_bus));
}

}
