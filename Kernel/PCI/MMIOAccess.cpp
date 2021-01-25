/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
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
#include <Kernel/Debug.h>
#include <Kernel/PCI/MMIOAccess.h>
#include <Kernel/VM/MemoryManager.h>

namespace Kernel {
namespace PCI {

class MMIOSegment {
public:
    MMIOSegment(PhysicalAddress, u8, u8);
    u8 get_start_bus() const;
    u8 get_end_bus() const;
    size_t get_size() const;
    PhysicalAddress get_paddr() const;

private:
    PhysicalAddress m_base_addr;
    u8 m_start_bus;
    u8 m_end_bus;
};

#define PCI_MMIO_CONFIG_SPACE_SIZE 4096

DeviceConfigurationSpaceMapping::DeviceConfigurationSpaceMapping(Address device_address, const MMIOSegment& mmio_segment)
    : m_device_address(device_address)
    , m_mapped_region(MM.allocate_kernel_region(PAGE_ROUND_UP(PCI_MMIO_CONFIG_SPACE_SIZE), "PCI MMIO Device Access", Region::Access::Read | Region::Access::Write).release_nonnull())
{
    PhysicalAddress segment_lower_addr = mmio_segment.get_paddr();
    PhysicalAddress device_physical_mmio_space = segment_lower_addr.offset(
        PCI_MMIO_CONFIG_SPACE_SIZE * m_device_address.function() + (PCI_MMIO_CONFIG_SPACE_SIZE * PCI_MAX_FUNCTIONS_PER_DEVICE) * m_device_address.slot() + (PCI_MMIO_CONFIG_SPACE_SIZE * PCI_MAX_FUNCTIONS_PER_DEVICE * PCI_MAX_DEVICES_PER_BUS) * (m_device_address.bus() - mmio_segment.get_start_bus()));
    m_mapped_region->physical_page_slot(0) = PhysicalPage::create(device_physical_mmio_space, false, false);
    m_mapped_region->remap();
}

uint32_t MMIOAccess::segment_count() const
{
    return m_segments.size();
}

uint8_t MMIOAccess::segment_start_bus(u32 seg) const
{
    auto segment = m_segments.get(seg);
    ASSERT(segment.has_value());
    return segment.value().get_start_bus();
}

uint8_t MMIOAccess::segment_end_bus(u32 seg) const
{
    auto segment = m_segments.get(seg);
    ASSERT(segment.has_value());
    return segment.value().get_end_bus();
}

void MMIOAccess::initialize(PhysicalAddress mcfg)
{
    if (!Access::is_initialized()) {
        new MMIOAccess(mcfg);
#if PCI_DEBUG
        dbgln("PCI: MMIO access initialised.");
#endif
    }
}

MMIOAccess::MMIOAccess(PhysicalAddress p_mcfg)
    : m_mcfg(p_mcfg)
{
    klog() << "PCI: Using MMIO for PCI configuration space access";

    auto checkup_region = MM.allocate_kernel_region(p_mcfg.page_base(), (PAGE_SIZE * 2), "PCI MCFG Checkup", Region::Access::Read | Region::Access::Write);
#if PCI_DEBUG
    dbgln("PCI: Checking MCFG Table length to choose the correct mapping size");
#endif

    auto* sdt = (ACPI::Structures::SDTHeader*)checkup_region->vaddr().offset(p_mcfg.offset_in_page()).as_ptr();
    u32 length = sdt->length;
    u8 revision = sdt->revision;

    klog() << "PCI: MCFG, length - " << length << ", revision " << revision;
    checkup_region->unmap();

    auto mcfg_region = MM.allocate_kernel_region(p_mcfg.page_base(), PAGE_ROUND_UP(length) + PAGE_SIZE, "PCI Parsing MCFG", Region::Access::Read | Region::Access::Write);

    auto& mcfg = *(ACPI::Structures::MCFG*)mcfg_region->vaddr().offset(p_mcfg.offset_in_page()).as_ptr();
    dbgln<PCI_DEBUG>("PCI: Checking MCFG @ {}, {}", VirtualAddress(&mcfg), PhysicalAddress(p_mcfg.get()));

    for (u32 index = 0; index < ((mcfg.header.length - sizeof(ACPI::Structures::MCFG)) / sizeof(ACPI::Structures::PCI_MMIO_Descriptor)); index++) {
        u8 start_bus = mcfg.descriptors[index].start_pci_bus;
        u8 end_bus = mcfg.descriptors[index].end_pci_bus;
        u32 lower_addr = mcfg.descriptors[index].base_addr;

        m_segments.set(index, { PhysicalAddress(lower_addr), start_bus, end_bus });
        klog() << "PCI: New PCI segment @ " << PhysicalAddress(lower_addr) << ", PCI buses (" << start_bus << "-" << end_bus << ")";
    }
    mcfg_region->unmap();
    klog() << "PCI: MMIO segments - " << m_segments.size();

    InterruptDisabler disabler;

    enumerate_hardware([&](const Address& address, ID id) {
        m_mapped_device_regions.append(make<DeviceConfigurationSpaceMapping>(address, m_segments.get(address.seg()).value()));
        m_physical_ids.append({ address, id, get_capabilities(address) });
        dbgln<PCI_DEBUG>("PCI: Mapping device @ pci ({}) {} {}", address, m_mapped_device_regions.last().vaddr(), m_mapped_device_regions.last().paddr());
    });
}

Optional<VirtualAddress> MMIOAccess::get_device_configuration_space(Address address)
{
    dbgln<PCI_DEBUG>("PCI: Getting device configuration space for {}", address);
    for (auto& mapping : m_mapped_device_regions) {
        auto checked_address = mapping.address();
        dbgln<PCI_DEBUG>("PCI Device Configuration Space Mapping: Check if {} was requested", checked_address);
        if (address.seg() == checked_address.seg()
            && address.bus() == checked_address.bus()
            && address.slot() == checked_address.slot()
            && address.function() == checked_address.function()) {
            dbgln<PCI_DEBUG>("PCI Device Configuration Space Mapping: Found {}", checked_address);
            return mapping.vaddr();
        }
    }

    dbgln<PCI_DEBUG>("PCI: No device configuration space found for {}", address);
    return {};
}

u8 MMIOAccess::read8_field(Address address, u32 field)
{
    InterruptDisabler disabler;
    ASSERT(field <= 0xfff);
    dbgln<PCI_DEBUG>("PCI: MMIO Reading 8-bit field {:#08x} for {}", field, address);
    return *((u8*)(get_device_configuration_space(address).value().get() + (field & 0xfff)));
}

u16 MMIOAccess::read16_field(Address address, u32 field)
{
    InterruptDisabler disabler;
    ASSERT(field < 0xfff);
    dbgln<PCI_DEBUG>("PCI: MMIO Reading 16-bit field {:#08x} for {}", field, address);
    return *((u16*)(get_device_configuration_space(address).value().get() + (field & 0xfff)));
}

u32 MMIOAccess::read32_field(Address address, u32 field)
{
    InterruptDisabler disabler;
    ASSERT(field <= 0xffc);
    dbgln<PCI_DEBUG>("PCI: MMIO Reading 32-bit field {:#08x} for {}", field, address);
    return *((u32*)(get_device_configuration_space(address).value().get() + (field & 0xfff)));
}

void MMIOAccess::write8_field(Address address, u32 field, u8 value)
{
    InterruptDisabler disabler;
    ASSERT(field <= 0xfff);
    dbgln<PCI_DEBUG>("PCI: MMIO Writing 8-bit field {:#08x}, value={:#02x} for {}", field, value, address);
    *((u8*)(get_device_configuration_space(address).value().get() + (field & 0xfff))) = value;
}
void MMIOAccess::write16_field(Address address, u32 field, u16 value)
{
    InterruptDisabler disabler;
    ASSERT(field < 0xfff);
    dbgln<PCI_DEBUG>("PCI: MMIO Writing 16-bit field {:#08x}, value={:#02x} for {}", field, value, address);
    *((u16*)(get_device_configuration_space(address).value().get() + (field & 0xfff))) = value;
}
void MMIOAccess::write32_field(Address address, u32 field, u32 value)
{
    InterruptDisabler disabler;
    ASSERT(field <= 0xffc);
    dbgln<PCI_DEBUG>("PCI: MMIO Writing 32-bit field {:#08x}, value={:#02x} for {}", field, value, address);
    *((u32*)(get_device_configuration_space(address).value().get() + (field & 0xfff))) = value;
}

void MMIOAccess::enumerate_hardware(Function<void(Address, ID)> callback)
{
    for (u16 seg = 0; seg < m_segments.size(); seg++) {
        dbgln<PCI_DEBUG>("PCI: Enumerating Memory mapped IO segment {}", seg);
        // Single PCI host controller.
        if ((early_read8_field(Address(seg), PCI_HEADER_TYPE) & 0x80) == 0) {
            enumerate_bus(-1, 0, callback);
            return;
        }

        // Multiple PCI host controllers.
        for (u8 function = 0; function < 8; ++function) {
            if (early_read16_field(Address(seg, 0, 0, function), PCI_VENDOR_ID) == PCI_NONE)
                break;
            enumerate_bus(-1, function, callback);
        }
    }
}

MMIOSegment::MMIOSegment(PhysicalAddress segment_base_addr, u8 start_bus, u8 end_bus)
    : m_base_addr(segment_base_addr)
    , m_start_bus(start_bus)
    , m_end_bus(end_bus)
{
}

u8 MMIOSegment::get_start_bus() const
{
    return m_start_bus;
}

u8 MMIOSegment::get_end_bus() const
{
    return m_end_bus;
}

size_t MMIOSegment::get_size() const
{
    return (PCI_MMIO_CONFIG_SPACE_SIZE * PCI_MAX_FUNCTIONS_PER_DEVICE * PCI_MAX_DEVICES_PER_BUS * (get_end_bus() - get_start_bus()));
}

PhysicalAddress MMIOSegment::get_paddr() const
{
    return m_base_addr;
}

}
}
