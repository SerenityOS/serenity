/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/Optional.h>
#include <AK/StringView.h>
#include <Kernel/Arch/x86/CPU.h>
#include <Kernel/Debug.h>
#include <Kernel/PCI/MMIOAccess.h>
#include <Kernel/VM/MemoryManager.h>

namespace Kernel {
namespace PCI {

#define MEMORY_RANGE_PER_BUS (PCI_MMIO_CONFIG_SPACE_SIZE * PCI_MAX_FUNCTIONS_PER_DEVICE * PCI_MAX_DEVICES_PER_BUS)

u32 MMIOAccess::segment_count() const
{
    return m_segments.size();
}

u8 MMIOAccess::segment_start_bus(u32 seg) const
{
    auto segment = m_segments.get(seg);
    VERIFY(segment.has_value());
    return segment.value().get_start_bus();
}

u8 MMIOAccess::segment_end_bus(u32 seg) const
{
    auto segment = m_segments.get(seg);
    VERIFY(segment.has_value());
    return segment.value().get_end_bus();
}

PhysicalAddress MMIOAccess::determine_memory_mapped_bus_region(u32 segment, u8 bus) const
{
    VERIFY(bus >= segment_start_bus(segment) && bus <= segment_end_bus(segment));
    auto seg = m_segments.get(segment);
    VERIFY(seg.has_value());
    return seg.value().get_paddr().offset(MEMORY_RANGE_PER_BUS * (bus - seg.value().get_start_bus()));
}

UNMAP_AFTER_INIT void MMIOAccess::initialize(PhysicalAddress mcfg)
{
    if (!Access::is_initialized()) {
        new MMIOAccess(mcfg);
        dbgln_if(PCI_DEBUG, "PCI: MMIO access initialised.");
    }
}

UNMAP_AFTER_INIT MMIOAccess::MMIOAccess(PhysicalAddress p_mcfg)
    : m_mcfg(p_mcfg)
{
    dmesgln("PCI: Using MMIO for PCI configuration space access");

    auto checkup_region = MM.allocate_kernel_region(p_mcfg.page_base(), (PAGE_SIZE * 2), "PCI MCFG Checkup", Region::Access::Read | Region::Access::Write);
    dbgln_if(PCI_DEBUG, "PCI: Checking MCFG Table length to choose the correct mapping size");
    auto* sdt = (ACPI::Structures::SDTHeader*)checkup_region->vaddr().offset(p_mcfg.offset_in_page()).as_ptr();
    u32 length = sdt->length;
    u8 revision = sdt->revision;

    dbgln("PCI: MCFG, length: {}, revision: {}", length, revision);
    checkup_region->unmap();

    auto mcfg_region = MM.allocate_kernel_region(p_mcfg.page_base(), page_round_up(length) + PAGE_SIZE, "PCI Parsing MCFG", Region::Access::Read | Region::Access::Write);

    auto& mcfg = *(ACPI::Structures::MCFG*)mcfg_region->vaddr().offset(p_mcfg.offset_in_page()).as_ptr();
    dbgln_if(PCI_DEBUG, "PCI: Checking MCFG @ {}, {}", VirtualAddress(&mcfg), PhysicalAddress(p_mcfg.get()));

    for (u32 index = 0; index < ((mcfg.header.length - sizeof(ACPI::Structures::MCFG)) / sizeof(ACPI::Structures::PCI_MMIO_Descriptor)); index++) {
        u8 start_bus = mcfg.descriptors[index].start_pci_bus;
        u8 end_bus = mcfg.descriptors[index].end_pci_bus;
        u32 lower_addr = mcfg.descriptors[index].base_addr;

        m_segments.set(index, { PhysicalAddress(lower_addr), start_bus, end_bus });
        dmesgln("PCI: New PCI segment @ {}, PCI buses ({}-{})", PhysicalAddress { lower_addr }, start_bus, end_bus);
    }
    mcfg_region->unmap();
    dmesgln("PCI: MMIO segments: {}", m_segments.size());

    InterruptDisabler disabler;
    VERIFY(m_segments.contains(0));

    // Note: we need to map this region before enumerating the hardware and adding
    // PCI::PhysicalID objects to the vector, because get_capabilities calls
    // PCI::read16 which will need this region to be mapped.
    m_mapped_region = MM.allocate_kernel_region(determine_memory_mapped_bus_region(0, m_segments.get(0).value().get_start_bus()), MEMORY_RANGE_PER_BUS, "PCI ECAM", Region::Access::Read | Region::Access::Write);
    dbgln("PCI ECAM Mapped region @ {}", m_mapped_region->vaddr());

    enumerate_hardware([&](const Address& address, ID id) {
        m_physical_ids.append({ address, id, get_capabilities(address) });
    });
}
void MMIOAccess::map_bus_region(u32 segment, u8 bus)
{
    VERIFY(m_access_lock.is_locked());
    if (m_mapped_bus == bus)
        return;
    m_mapped_region = MM.allocate_kernel_region(determine_memory_mapped_bus_region(segment, bus), MEMORY_RANGE_PER_BUS, "PCI ECAM", Region::Access::Read | Region::Access::Write);
}

VirtualAddress MMIOAccess::get_device_configuration_space(Address address)
{
    VERIFY(m_access_lock.is_locked());
    dbgln_if(PCI_DEBUG, "PCI: Getting device configuration space for {}", address);
    map_bus_region(address.seg(), address.bus());
    return m_mapped_region->vaddr().offset(PCI_MMIO_CONFIG_SPACE_SIZE * address.function() + (PCI_MMIO_CONFIG_SPACE_SIZE * PCI_MAX_FUNCTIONS_PER_DEVICE) * address.device());
}

u8 MMIOAccess::read8_field(Address address, u32 field)
{
    ScopedSpinLock lock(m_access_lock);
    VERIFY(field <= 0xfff);
    dbgln_if(PCI_DEBUG, "PCI: MMIO Reading 8-bit field {:#08x} for {}", field, address);
    return *((volatile u8*)(get_device_configuration_space(address).get() + (field & 0xfff)));
}

u16 MMIOAccess::read16_field(Address address, u32 field)
{
    ScopedSpinLock lock(m_access_lock);
    VERIFY(field < 0xfff);
    dbgln_if(PCI_DEBUG, "PCI: MMIO Reading 16-bit field {:#08x} for {}", field, address);
    u16 data = 0;
    read_possibly_unaligned_data<u16>(get_device_configuration_space(address).offset(field & 0xfff).as_ptr(), data);
    return data;
}

u32 MMIOAccess::read32_field(Address address, u32 field)
{
    ScopedSpinLock lock(m_access_lock);
    VERIFY(field <= 0xffc);
    dbgln_if(PCI_DEBUG, "PCI: MMIO Reading 32-bit field {:#08x} for {}", field, address);
    u32 data = 0;
    read_possibly_unaligned_data<u32>(get_device_configuration_space(address).offset(field & 0xfff).as_ptr(), data);
    return data;
}

void MMIOAccess::write8_field(Address address, u32 field, u8 value)
{
    ScopedSpinLock lock(m_access_lock);
    VERIFY(field <= 0xfff);
    dbgln_if(PCI_DEBUG, "PCI: MMIO Writing 8-bit field {:#08x}, value={:#02x} for {}", field, value, address);
    *((volatile u8*)(get_device_configuration_space(address).get() + (field & 0xfff))) = value;
}
void MMIOAccess::write16_field(Address address, u32 field, u16 value)
{
    ScopedSpinLock lock(m_access_lock);
    VERIFY(field < 0xfff);
    dbgln_if(PCI_DEBUG, "PCI: MMIO Writing 16-bit field {:#08x}, value={:#02x} for {}", field, value, address);
    write_possibly_unaligned_data<u16>(get_device_configuration_space(address).offset(field & 0xfff).as_ptr(), value);
}
void MMIOAccess::write32_field(Address address, u32 field, u32 value)
{
    ScopedSpinLock lock(m_access_lock);
    VERIFY(field <= 0xffc);
    dbgln_if(PCI_DEBUG, "PCI: MMIO Writing 32-bit field {:#08x}, value={:#02x} for {}", field, value, address);
    write_possibly_unaligned_data<u32>(get_device_configuration_space(address).offset(field & 0xfff).as_ptr(), value);
}

void MMIOAccess::enumerate_hardware(Function<void(Address, ID)> callback)
{
    for (u16 seg = 0; seg < m_segments.size(); seg++) {
        dbgln_if(PCI_DEBUG, "PCI: Enumerating Memory mapped IO segment {}", seg);
        // Single PCI host controller.
        if ((early_read8_field(Address(seg), PCI_HEADER_TYPE) & 0x80) == 0) {
            enumerate_bus(-1, 0, callback, true);
            return;
        }

        // Multiple PCI host controllers.
        for (u8 function = 0; function < 8; ++function) {
            if (early_read16_field(Address(seg, 0, 0, function), PCI_VENDOR_ID) == PCI_NONE)
                break;
            enumerate_bus(-1, function, callback, false);
        }
    }
}

MMIOAccess::MMIOSegment::MMIOSegment(PhysicalAddress segment_base_addr, u8 start_bus, u8 end_bus)
    : m_base_addr(segment_base_addr)
    , m_start_bus(start_bus)
    , m_end_bus(end_bus)
{
}

u8 MMIOAccess::MMIOSegment::get_start_bus() const
{
    return m_start_bus;
}

u8 MMIOAccess::MMIOSegment::get_end_bus() const
{
    return m_end_bus;
}

size_t MMIOAccess::MMIOSegment::get_size() const
{
    return (PCI_MMIO_CONFIG_SPACE_SIZE * PCI_MAX_FUNCTIONS_PER_DEVICE * PCI_MAX_DEVICES_PER_BUS * (get_end_bus() - get_start_bus()));
}

PhysicalAddress MMIOAccess::MMIOSegment::get_paddr() const
{
    return m_base_addr;
}

}
}
