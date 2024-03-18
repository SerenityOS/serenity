/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/MACAddress.h>
#include <Kernel/Bus/PCI/IDs.h>
#include <Kernel/Net/Intel/E1000ENetworkAdapter.h>
#include <Kernel/Net/NetworkingManagement.h>
#include <Kernel/Sections.h>

namespace Kernel {

#define REG_EECD 0x0010
#define REG_EEPROM 0x0014

// EECD Register

#define EECD_PRES 0x100

ErrorOr<NonnullRefPtr<E1000ENetworkAdapter>> E1000ENetworkAdapter::create(PCI::Device& pci_device)
{
    u8 irq = pci_device.device_id().interrupt_line().value();
    auto interface_name = TRY(NetworkingManagement::generate_interface_name_from_pci_address(pci_device));
    auto registers_io_window = TRY(IOWindow::create_for_pci_device_bar(pci_device, PCI::HeaderType0BaseRegister::BAR0));

    auto rx_buffer_region = TRY(MM.allocate_contiguous_kernel_region(rx_buffer_size * number_of_rx_descriptors, "E1000 RX buffers"sv, Memory::Region::Access::ReadWrite));
    auto tx_buffer_region = MM.allocate_contiguous_kernel_region(tx_buffer_size * number_of_tx_descriptors, "E1000 TX buffers"sv, Memory::Region::Access::ReadWrite).release_value();
    auto rx_descriptors_region = TRY(MM.allocate_contiguous_kernel_region(TRY(Memory::page_round_up(sizeof(e1000_rx_desc) * number_of_rx_descriptors)), "E1000 RX Descriptors"sv, Memory::Region::Access::ReadWrite));
    auto tx_descriptors_region = TRY(MM.allocate_contiguous_kernel_region(TRY(Memory::page_round_up(sizeof(e1000_tx_desc) * number_of_tx_descriptors)), "E1000 TX Descriptors"sv, Memory::Region::Access::ReadWrite));

    auto adapter = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) E1000ENetworkAdapter(interface_name.representable_view(),
        pci_device,
        irq, move(registers_io_window),
        move(rx_buffer_region),
        move(tx_buffer_region),
        move(rx_descriptors_region),
        move(tx_descriptors_region))));
    TRY(adapter->initialize());

    return adapter;
}

ErrorOr<void> E1000ENetworkAdapter::initialize()
{
    dmesgln("E1000e: Found @ {}", m_pci_device->device_id().address());
    m_pci_device->enable_bus_mastering();

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

E1000ENetworkAdapter::E1000ENetworkAdapter(StringView interface_name,
    PCI::Device& device_identifier, u8 irq,
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

E1000ENetworkAdapter::~E1000ENetworkAdapter() = default;

void E1000ENetworkAdapter::detect_eeprom()
{
    // Section 13.4.3 of https://www.intel.com/content/dam/doc/manual/pci-pci-x-family-gbe-controllers-software-dev-manual.pdf
    m_has_eeprom = in32(REG_EECD) & EECD_PRES;
}

u32 E1000ENetworkAdapter::read_eeprom(u8 address)
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
