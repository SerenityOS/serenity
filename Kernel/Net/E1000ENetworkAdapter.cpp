/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/MACAddress.h>
#include <Kernel/Debug.h>
#include <Kernel/Net/E1000ENetworkAdapter.h>
#include <Kernel/PCI/IDs.h>

namespace Kernel {

#define REG_EEPROM 0x0014

static bool is_valid_device_id(u16 device_id)
{
    switch (device_id) {
    case 0x10D3: // 82574
        return true;
    default:
        return false;
    }
}

UNMAP_AFTER_INIT RefPtr<E1000ENetworkAdapter> E1000ENetworkAdapter::try_to_initialize(PCI::Address address)
{
    auto id = PCI::get_id(address);
    if (id.vendor_id != (u16)PCIVendorID::Intel)
        return {};
    if (!is_valid_device_id(id.device_id))
        return {};
    u8 irq = PCI::get_interrupt_line(address);
    auto adapter = adopt_ref_if_nonnull(new E1000ENetworkAdapter(address, irq));
    if (!adapter)
        return {};
    if (adapter->initialize())
        return adapter;
    return {};
}

UNMAP_AFTER_INIT bool E1000ENetworkAdapter::initialize()
{
    dmesgln("E1000e: Found @ {}", pci_address());

    m_io_base = IOAddress(PCI::get_BAR2(pci_address()) & ~1);

    enable_bus_mastering(pci_address());

    size_t mmio_base_size = PCI::get_BAR_space_size(pci_address(), 0);
    m_mmio_region = MM.allocate_kernel_region(PhysicalAddress(page_base_of(PCI::get_BAR0(pci_address()))), page_round_up(mmio_base_size), "E1000e MMIO", Region::Access::Read | Region::Access::Write, Region::Cacheable::No);
    if (!m_mmio_region)
        return false;
    m_mmio_base = m_mmio_region->vaddr();
    m_use_mmio = true;
    m_interrupt_line = PCI::get_interrupt_line(pci_address());
    dmesgln("E1000e: port base: {}", m_io_base);
    dmesgln("E1000e: MMIO base: {}", PhysicalAddress(PCI::get_BAR0(pci_address()) & 0xfffffffc));
    dmesgln("E1000e: MMIO base size: {} bytes", mmio_base_size);
    dmesgln("E1000e: Interrupt line: {}", m_interrupt_line);
    detect_eeprom();
    dmesgln("E1000e: Has EEPROM? {}", m_has_eeprom);
    read_mac_address();
    const auto& mac = mac_address();
    dmesgln("E1000e: MAC address: {}", mac.to_string());

    initialize_rx_descriptors();
    initialize_tx_descriptors();

    setup_link();
    setup_interrupts();
    return true;
}

UNMAP_AFTER_INIT E1000ENetworkAdapter::E1000ENetworkAdapter(PCI::Address address, u8 irq)
    : E1000NetworkAdapter(address, irq)
{
}

UNMAP_AFTER_INIT E1000ENetworkAdapter::~E1000ENetworkAdapter()
{
}

UNMAP_AFTER_INIT void E1000ENetworkAdapter::detect_eeprom()
{
    // FIXME: Try to find a way to detect if EEPROM exists instead of assuming it is
    m_has_eeprom = true;
}

UNMAP_AFTER_INIT u32 E1000ENetworkAdapter::read_eeprom(u8 address)
{
    VERIFY(m_has_eeprom);
    u16 data = 0;
    u32 tmp = 0;
    if (m_has_eeprom) {
        out32(REG_EEPROM, ((u32)address << 2) | 1);
        while (!((tmp = in32(REG_EEPROM)) & (1 << 1)))
            ;
    } else {
        out32(REG_EEPROM, ((u32)address << 2) | 1);
        while (!((tmp = in32(REG_EEPROM)) & (1 << 1)))
            ;
    }
    data = (tmp >> 16) & 0xffff;
    return data;
}

}
