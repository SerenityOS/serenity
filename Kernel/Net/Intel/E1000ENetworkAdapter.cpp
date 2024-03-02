/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/MACAddress.h>
#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Bus/PCI/IDs.h>
#include <Kernel/Net/Intel/E1000ENetworkAdapter.h>
#include <Kernel/Net/NetworkingManagement.h>
#include <Kernel/Sections.h>

namespace Kernel {

#define REG_EECD 0x0010
#define REG_EEPROM 0x0014

// EECD Register

#define EECD_PRES 0x100

UNMAP_AFTER_INIT ErrorOr<void> E1000ENetworkAdapter::initialize()
{
    dmesgln("E1000e: Found @ {}", device_identifier().address());
    enable_bus_mastering(device_identifier());

    dmesgln("E1000e: IO base: {}", m_registers_io_window);
    dmesgln("E1000e: Interrupt line: {}", interrupt_number());
    detect_eeprom();
    dmesgln("E1000e: Has EEPROM? {}", m_has_eeprom);
    read_mac_address();
    auto const& mac = mac_address();
    dmesgln("E1000e: MAC address: {}", mac.to_string());

    initialize_rx_descriptors();
    initialize_tx_descriptors();

    setup_link();
    setup_interrupts();
    return {};
}

UNMAP_AFTER_INIT E1000ENetworkAdapter::E1000ENetworkAdapter(StringView interface_name,
    PCI::DeviceIdentifier const& device_identifier, u8 irq,
    NonnullOwnPtr<IOWindow> registers_io_window, NonnullOwnPtr<Memory::Region> rx_buffer_region,
    NonnullOwnPtr<Memory::Region> tx_buffer_region, NonnullOwnPtr<Memory::Region> rx_descriptors_region,
    NonnullOwnPtr<Memory::Region> tx_descriptors_region)
    : E1000NetworkAdapter(interface_name, device_identifier, irq, move(registers_io_window),
        move(rx_buffer_region),
        move(tx_buffer_region),
        move(rx_descriptors_region),
        move(tx_descriptors_region))
{
}

UNMAP_AFTER_INIT E1000ENetworkAdapter::~E1000ENetworkAdapter() = default;

UNMAP_AFTER_INIT void E1000ENetworkAdapter::detect_eeprom()
{
    // Section 13.4.3 of https://www.intel.com/content/dam/doc/manual/pci-pci-x-family-gbe-controllers-software-dev-manual.pdf
    m_has_eeprom = in32(REG_EECD) & EECD_PRES;
}

UNMAP_AFTER_INIT u32 E1000ENetworkAdapter::read_eeprom(u8 address)
{
    VERIFY(m_has_eeprom);
    u16 data = 0;
    u32 tmp = 0;
    out32(REG_EEPROM, ((u32)address << 2) | 1);
    while (!((tmp = in32(REG_EEPROM)) & (1 << 1)))
        Processor::wait_check();
    data = (tmp >> 16) & 0xffff;
    return data;
}

}
