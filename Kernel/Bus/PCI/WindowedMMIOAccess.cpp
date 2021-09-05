/*
 * Copyright (c) 2020-2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteReader.h>
#include <AK/Optional.h>
#include <AK/StringView.h>
#include <Kernel/Arch/x86/InterruptDisabler.h>
#include <Kernel/Bus/PCI/WindowedMMIOAccess.h>
#include <Kernel/Debug.h>
#include <Kernel/Memory/MemoryManager.h>
#include <Kernel/Sections.h>

namespace Kernel {
namespace PCI {

UNMAP_AFTER_INIT DeviceConfigurationSpaceMapping::DeviceConfigurationSpaceMapping(Address device_address, const MMIOAccess::MMIOSegment& mmio_segment)
    : m_device_address(device_address)
    , m_mapped_region(MM.allocate_kernel_region(Memory::page_round_up(PCI_MMIO_CONFIG_SPACE_SIZE), "PCI MMIO Device Access", Memory::Region::Access::ReadWrite).release_value())
{
    PhysicalAddress segment_lower_addr = mmio_segment.get_paddr();
    PhysicalAddress device_physical_mmio_space = segment_lower_addr.offset(
        PCI_MMIO_CONFIG_SPACE_SIZE * m_device_address.function() + (PCI_MMIO_CONFIG_SPACE_SIZE * PCI_MAX_FUNCTIONS_PER_DEVICE) * m_device_address.device() + (PCI_MMIO_CONFIG_SPACE_SIZE * PCI_MAX_FUNCTIONS_PER_DEVICE * PCI_MAX_DEVICES_PER_BUS) * (m_device_address.bus() - mmio_segment.get_start_bus()));
    m_mapped_region->physical_page_slot(0) = Memory::PhysicalPage::create(device_physical_mmio_space, Memory::MayReturnToFreeList::No);
    m_mapped_region->remap();
}

UNMAP_AFTER_INIT void WindowedMMIOAccess::initialize(PhysicalAddress mcfg)
{
    if (!Access::is_initialized()) {
        new WindowedMMIOAccess(mcfg);
        dbgln_if(PCI_DEBUG, "PCI: MMIO access initialised.");
    }
}

UNMAP_AFTER_INIT WindowedMMIOAccess::WindowedMMIOAccess(PhysicalAddress p_mcfg)
    : MMIOAccess(p_mcfg)
{
    dmesgln("PCI: Using MMIO (mapping per device) for PCI configuration space access");

    InterruptDisabler disabler;

    enumerate_hardware([&](const Address& address, ID) {
        m_mapped_device_regions.append(make<DeviceConfigurationSpaceMapping>(address, m_segments.get(address.seg()).value()));
    });
}

Optional<VirtualAddress> WindowedMMIOAccess::get_device_configuration_space(Address address)
{
    dbgln_if(PCI_DEBUG, "PCI: Getting device configuration space for {}", address);
    for (auto& mapping : m_mapped_device_regions) {
        auto checked_address = mapping.address();
        dbgln_if(PCI_DEBUG, "PCI Device Configuration Space Mapping: Check if {} was requested", checked_address);
        if (address.seg() == checked_address.seg()
            && address.bus() == checked_address.bus()
            && address.device() == checked_address.device()
            && address.function() == checked_address.function()) {
            dbgln_if(PCI_DEBUG, "PCI Device Configuration Space Mapping: Found {}", checked_address);
            return mapping.vaddr();
        }
    }

    dbgln_if(PCI_DEBUG, "PCI: No device configuration space found for {}", address);
    return {};
}

u8 WindowedMMIOAccess::read8_field(Address address, u32 field)
{
    InterruptDisabler disabler;
    VERIFY(field <= 0xfff);
    dbgln_if(PCI_DEBUG, "PCI: MMIO Reading 8-bit field {:#08x} for {}", field, address);
    return *((u8*)(get_device_configuration_space(address).value().get() + (field & 0xfff)));
}

u16 WindowedMMIOAccess::read16_field(Address address, u32 field)
{
    InterruptDisabler disabler;
    VERIFY(field < 0xfff);
    dbgln_if(PCI_DEBUG, "PCI: MMIO Reading 16-bit field {:#08x} for {}", field, address);
    u16 data = 0;
    ByteReader::load<u16>(get_device_configuration_space(address).value().offset(field & 0xfff).as_ptr(), data);
    return data;
}

u32 WindowedMMIOAccess::read32_field(Address address, u32 field)
{
    InterruptDisabler disabler;
    VERIFY(field <= 0xffc);
    dbgln_if(PCI_DEBUG, "PCI: MMIO Reading 32-bit field {:#08x} for {}", field, address);
    u32 data = 0;
    ByteReader::load<u32>(get_device_configuration_space(address).value().offset(field & 0xfff).as_ptr(), data);
    return data;
}

void WindowedMMIOAccess::write8_field(Address address, u32 field, u8 value)
{
    InterruptDisabler disabler;
    VERIFY(field <= 0xfff);
    dbgln_if(PCI_DEBUG, "PCI: MMIO Writing 8-bit field {:#08x}, value={:#02x} for {}", field, value, address);
    *((u8*)(get_device_configuration_space(address).value().get() + (field & 0xfff))) = value;
}
void WindowedMMIOAccess::write16_field(Address address, u32 field, u16 value)
{
    InterruptDisabler disabler;
    VERIFY(field < 0xfff);
    dbgln_if(PCI_DEBUG, "PCI: MMIO Writing 16-bit field {:#08x}, value={:#02x} for {}", field, value, address);
    ByteReader::store<u16>(get_device_configuration_space(address).value().offset(field & 0xfff).as_ptr(), value);
}
void WindowedMMIOAccess::write32_field(Address address, u32 field, u32 value)
{
    InterruptDisabler disabler;
    VERIFY(field <= 0xffc);
    dbgln_if(PCI_DEBUG, "PCI: MMIO Writing 32-bit field {:#08x}, value={:#02x} for {}", field, value, address);
    ByteReader::store<u32>(get_device_configuration_space(address).value().offset(field & 0xfff).as_ptr(), value);
}

}
}
