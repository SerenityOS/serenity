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
#include <Kernel/PCI/MMIOAccess.h>
#include <Kernel/VM/MemoryManager.h>

namespace Kernel {
namespace PCI {

class MMIOSegment {
public:
    MMIOSegment(PhysicalAddress, u8, u8);
    u8 get_start_bus();
    u8 get_end_bus();
    size_t get_size();
    PhysicalAddress get_paddr();

private:
    PhysicalAddress m_base_addr;
    u8 m_start_bus;
    u8 m_end_bus;
};

#define PCI_MMIO_CONFIG_SPACE_SIZE 4096

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
    if (!Access::is_initialized())
        new MMIOAccess(mcfg);
}

MMIOAccess::MMIOAccess(PhysicalAddress p_mcfg)
    : m_mcfg(p_mcfg)
    , m_mapped_address(ChangeableAddress(0xFFFF, 0xFF, 0xFF, 0xFF))
{
    klog() << "PCI: Using MMIO for PCI configuration space access";
    m_mmio_window_region = MM.allocate_kernel_region(PAGE_ROUND_UP(PCI_MMIO_CONFIG_SPACE_SIZE), "PCI MMIO", Region::Access::Read | Region::Access::Write);

    auto checkup_region = MM.allocate_kernel_region(p_mcfg.page_base(), (PAGE_SIZE * 2), "PCI MCFG Checkup", Region::Access::Read | Region::Access::Write);
#ifdef PCI_DEBUG
    dbg() << "PCI: Checking MCFG Table length to choose the correct mapping size";
#endif

    auto* sdt = (ACPI::Structures::SDTHeader*)checkup_region->vaddr().offset(p_mcfg.offset_in_page()).as_ptr();
    u32 length = sdt->length;
    u8 revision = sdt->revision;

    klog() << "PCI: MCFG, length - " << length << ", revision " << revision;
    checkup_region->unmap();

    auto mcfg_region = MM.allocate_kernel_region(p_mcfg.page_base(), PAGE_ROUND_UP(length) + PAGE_SIZE, "PCI Parsing MCFG", Region::Access::Read | Region::Access::Write);

    auto& mcfg = *(ACPI::Structures::MCFG*)mcfg_region->vaddr().offset(p_mcfg.offset_in_page()).as_ptr();
#ifdef PCI_DEBUG
    dbg() << "PCI: Checking MCFG @ V " << &mcfg << ", P 0x" << String::format("%x", p_mcfg.get());
#endif

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

#ifdef PCI_DEBUG
    dbg() << "PCI: mapped address (" << String::format("%w", m_mapped_address.seg()) << ":" << String::format("%b", m_mapped_address.bus()) << ":" << String::format("%b", m_mapped_address.slot()) << "." << String::format("%b", m_mapped_address.function()) << ")";
#endif
    map_device(Address(0, 0, 0, 0));
#ifdef PCI_DEBUG
    dbg() << "PCI: Default mapped address (" << String::format("%w", m_mapped_address.seg()) << ":" << String::format("%b", m_mapped_address.bus()) << ":" << String::format("%b", m_mapped_address.slot()) << "." << String::format("%b", m_mapped_address.function()) << ")";
#endif

    enumerate_hardware([&](const Address& address, ID id) {
        m_physical_ids.append({ address, id });
    });
}

void MMIOAccess::map_device(Address address)
{
    if (m_mapped_address == address)
        return;
    // FIXME: Map and put some lock!
    ASSERT_INTERRUPTS_DISABLED();
    auto segment = m_segments.get(address.seg());
    ASSERT(segment.has_value());
    PhysicalAddress segment_lower_addr = segment.value().get_paddr();
    PhysicalAddress device_physical_mmio_space = segment_lower_addr.offset(
        PCI_MMIO_CONFIG_SPACE_SIZE * address.function() + (PCI_MMIO_CONFIG_SPACE_SIZE * PCI_MAX_FUNCTIONS_PER_DEVICE) * address.slot() + (PCI_MMIO_CONFIG_SPACE_SIZE * PCI_MAX_FUNCTIONS_PER_DEVICE * PCI_MAX_DEVICES_PER_BUS) * (address.bus() - segment.value().get_start_bus()));

#ifdef PCI_DEBUG
    dbg() << "PCI: Mapping device @ pci (" << String::format("%w", address.seg()) << ":" << String::format("%b", address.bus()) << ":" << String::format("%b", address.slot()) << "." << String::format("%b", address.function()) << ")"
          << " V 0x" << String::format("%x", m_mmio_window_region->vaddr().get()) << " P 0x" << String::format("%x", device_physical_mmio_space.get());
#endif
    m_mmio_window_region->physical_page_slot(0) = PhysicalPage::create(device_physical_mmio_space, false, false);
    m_mmio_window_region->remap();
    m_mapped_address = address;
}

u8 MMIOAccess::read8_field(Address address, u32 field)
{
    InterruptDisabler disabler;
    ASSERT(field <= 0xfff);
#ifdef PCI_DEBUG
    dbg() << "PCI: Reading field " << field << ", Address(" << String::format("%w", address.seg()) << ":" << String::format("%b", address.bus()) << ":" << String::format("%b", address.slot()) << "." << String::format("%b", address.function()) << ")";
#endif
    map_device(address);
    return *((u8*)(m_mmio_window_region->vaddr().get() + (field & 0xfff)));
}

u16 MMIOAccess::read16_field(Address address, u32 field)
{
    InterruptDisabler disabler;
    ASSERT(field < 0xfff);
#ifdef PCI_DEBUG
    dbg() << "PCI: Reading field " << field << ", Address(" << String::format("%w", address.seg()) << ":" << String::format("%b", address.bus()) << ":" << String::format("%b", address.slot()) << "." << String::format("%b", address.function()) << ")";
#endif
    map_device(address);
    return *((u16*)(m_mmio_window_region->vaddr().get() + (field & 0xfff)));
}

u32 MMIOAccess::read32_field(Address address, u32 field)
{
    InterruptDisabler disabler;
    ASSERT(field <= 0xffc);
#ifdef PCI_DEBUG
    dbg() << "PCI: Reading field " << field << ", Address(" << String::format("%w", address.seg()) << ":" << String::format("%b", address.bus()) << ":" << String::format("%b", address.slot()) << "." << String::format("%b", address.function()) << ")";
#endif
    map_device(address);
    return *((u32*)(m_mmio_window_region->vaddr().get() + (field & 0xfff)));
}

void MMIOAccess::write8_field(Address address, u32 field, u8 value)
{
    InterruptDisabler disabler;
    ASSERT(field <= 0xfff);
#ifdef PCI_DEBUG
    dbg() << "PCI: Writing to field " << field << ", Address(" << String::format("%w", address.seg()) << ":" << String::format("%b", address.bus()) << ":" << String::format("%b", address.slot()) << "." << String::format("%b", address.function()) << ") value 0x" << String::format("%x", value);
#endif
    map_device(address);
    *((u8*)(m_mmio_window_region->vaddr().get() + (field & 0xfff))) = value;
}
void MMIOAccess::write16_field(Address address, u32 field, u16 value)
{
    InterruptDisabler disabler;
    ASSERT(field < 0xfff);
#ifdef PCI_DEBUG
    dbg() << "PCI: Writing to field " << field << ", Address(" << String::format("%w", address.seg()) << ":" << String::format("%b", address.bus()) << ":" << String::format("%b", address.slot()) << "." << String::format("%b", address.function()) << ") value 0x" << String::format("%x", value);
#endif
    map_device(address);
    *((u16*)(m_mmio_window_region->vaddr().get() + (field & 0xfff))) = value;
}
void MMIOAccess::write32_field(Address address, u32 field, u32 value)
{
    InterruptDisabler disabler;
    ASSERT(field <= 0xffc);
#ifdef PCI_DEBUG
    dbg() << "PCI: Writing to field " << field << ", Address(" << String::format("%w", address.seg()) << ":" << String::format("%b", address.bus()) << ":" << String::format("%b", address.slot()) << "." << String::format("%b", address.function()) << ") value 0x" << String::format("%x", value);
#endif
    map_device(address);
    *((u32*)(m_mmio_window_region->vaddr().get() + (field & 0xfff))) = value;
}

void MMIOAccess::enumerate_hardware(Function<void(Address, ID)> callback)
{
    for (u16 seg = 0; seg < m_segments.size(); seg++) {
#ifdef PCI_DEBUG
        dbg() << "PCI: Enumerating Memory mapped IO segment " << seg;
#endif
        // Single PCI host controller.
        if ((read8_field(Address(seg), PCI_HEADER_TYPE) & 0x80) == 0) {
            enumerate_bus(-1, 0, callback);
            return;
        }

        // Multiple PCI host controllers.
        for (u8 function = 0; function < 8; ++function) {
            if (read16_field(Address(seg, 0, 0, function), PCI_VENDOR_ID) == PCI_NONE)
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

u8 MMIOSegment::get_start_bus()
{
    return m_start_bus;
}

u8 MMIOSegment::get_end_bus()
{
    return m_end_bus;
}

size_t MMIOSegment::get_size()
{
    return (PCI_MMIO_CONFIG_SPACE_SIZE * PCI_MAX_FUNCTIONS_PER_DEVICE * PCI_MAX_DEVICES_PER_BUS * (get_end_bus() - get_start_bus()));
}

PhysicalAddress MMIOSegment::get_paddr()
{
    return m_base_addr;
}

}
}
