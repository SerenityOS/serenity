/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteReader.h>
#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Bus/PCI/Controller/MemoryBackedHostBridge.h>
#include <Kernel/Memory/MemoryManager.h>

namespace Kernel::PCI {

NonnullOwnPtr<MemoryBackedHostBridge> MemoryBackedHostBridge::must_create(Domain const& domain, PhysicalAddress start_address)
{
    return adopt_own_if_nonnull(new (nothrow) MemoryBackedHostBridge(domain, start_address)).release_nonnull();
}

MemoryBackedHostBridge::MemoryBackedHostBridge(PCI::Domain const& domain, PhysicalAddress start_address)
    : HostController(domain)
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
    auto region_or_error = MM.allocate_mmio_kernel_region(bus_base_address, memory_range_per_bus, "PCI ECAM"sv, Memory::Region::Access::ReadWrite);
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
