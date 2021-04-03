/*
 * Copyright (c) 2020-2021, Liav A. <liavalb@hotmail.co.il>
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
#include <Kernel/PCI/WindowedMMIOAccess.h>
#include <Kernel/VM/MemoryManager.h>

namespace Kernel {
namespace PCI {

UNMAP_AFTER_INIT DeviceConfigurationSpaceMapping::DeviceConfigurationSpaceMapping(Address device_address, const MMIOAccess::MMIOSegment& mmio_segment)
    : m_device_address(device_address)
    , m_mapped_region(MM.allocate_kernel_region(page_round_up(PCI_MMIO_CONFIG_SPACE_SIZE), "PCI MMIO Device Access", Region::Access::Read | Region::Access::Write).release_nonnull())
{
    PhysicalAddress segment_lower_addr = mmio_segment.get_paddr();
    PhysicalAddress device_physical_mmio_space = segment_lower_addr.offset(
        PCI_MMIO_CONFIG_SPACE_SIZE * m_device_address.function() + (PCI_MMIO_CONFIG_SPACE_SIZE * PCI_MAX_FUNCTIONS_PER_DEVICE) * m_device_address.device() + (PCI_MMIO_CONFIG_SPACE_SIZE * PCI_MAX_FUNCTIONS_PER_DEVICE * PCI_MAX_DEVICES_PER_BUS) * (m_device_address.bus() - mmio_segment.get_start_bus()));
    m_mapped_region->physical_page_slot(0) = PhysicalPage::create(device_physical_mmio_space, false, false);
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
    return *((u16*)(get_device_configuration_space(address).value().get() + (field & 0xfff)));
}

u32 WindowedMMIOAccess::read32_field(Address address, u32 field)
{
    InterruptDisabler disabler;
    VERIFY(field <= 0xffc);
    dbgln_if(PCI_DEBUG, "PCI: MMIO Reading 32-bit field {:#08x} for {}", field, address);
    return *((u32*)(get_device_configuration_space(address).value().get() + (field & 0xfff)));
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
    *((u16*)(get_device_configuration_space(address).value().get() + (field & 0xfff))) = value;
}
void WindowedMMIOAccess::write32_field(Address address, u32 field, u32 value)
{
    InterruptDisabler disabler;
    VERIFY(field <= 0xffc);
    dbgln_if(PCI_DEBUG, "PCI: MMIO Writing 32-bit field {:#08x}, value={:#02x} for {}", field, value, address);
    *((u32*)(get_device_configuration_space(address).value().get() + (field & 0xfff))) = value;
}

}
}
