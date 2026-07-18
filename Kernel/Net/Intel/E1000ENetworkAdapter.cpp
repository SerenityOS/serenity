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
#include <Kernel/Tasks/Process.h>

namespace Kernel {

#define REG_EECD 0x0010
#define REG_EEPROM 0x0014
#define REG_MDIC 0x0020

// EECD Register

#define EECD_PRES 0x100

// MIDC Register

#define REG_MDIC_DATA_MASK (0xffff << 0)
#define REG_MDIC_DATA_OFFSET 0
#define REG_MDIC_REGADD_OFFSET 16
#define REG_MDIC_R (1 << 28)
#define REG_MDIC_OP_WRITE (0b01 << 26)
#define REG_MDIC_OP_READ (0b10 << 26)
#define REG_MDIC_PHYADD_OFFSET 21

#define GIGABIT_PHY_ID 1

static bool is_valid_device_id(u16 device_id)
{
    switch (device_id) {
    case 0x10D3: // 82574L
        return true;
    default:
        return false;
    }
}

UNMAP_AFTER_INIT ErrorOr<bool> E1000ENetworkAdapter::probe(PCI::DeviceIdentifier const& pci_device_identifier)
{
    if (pci_device_identifier.hardware_id().vendor_id != PCI::VendorID::Intel)
        return false;
    return is_valid_device_id(pci_device_identifier.hardware_id().device_id);
}

UNMAP_AFTER_INIT ErrorOr<NonnullRefPtr<NetworkAdapter>> E1000ENetworkAdapter::create(PCI::DeviceIdentifier const& pci_device_identifier)
{
    auto interface_name = TRY(NetworkingManagement::generate_interface_name_from_pci_address(pci_device_identifier));
    auto registers_io_window = TRY(IOWindow::create_for_pci_device_bar(pci_device_identifier, PCI::HeaderType0BaseRegister::BAR0));

    auto rx_buffer_region = TRY(MM.allocate_contiguous_kernel_region(rx_buffer_size * number_of_rx_descriptors, "E1000 RX buffers"sv, Memory::Region::Access::ReadWrite));
    auto tx_buffer_region = MM.allocate_contiguous_kernel_region(tx_buffer_size * number_of_tx_descriptors, "E1000 TX buffers"sv, Memory::Region::Access::ReadWrite).release_value();
    auto rx_descriptors_region = TRY(Memory::allocate_dma_region_as_typed_array<RxDescriptor volatile>(number_of_rx_descriptors, "E1000 RX Descriptors"sv, Memory::Region::Access::ReadWrite));
    auto tx_descriptors_region = TRY(Memory::allocate_dma_region_as_typed_array<TxDescriptor volatile>(number_of_tx_descriptors, "E1000 TX Descriptors"sv, Memory::Region::Access::ReadWrite));

    return TRY(adopt_nonnull_ref_or_enomem(new (nothrow) E1000ENetworkAdapter(interface_name.representable_view(),
        pci_device_identifier,
        move(registers_io_window),
        move(rx_buffer_region),
        move(tx_buffer_region),
        move(rx_descriptors_region),
        move(tx_descriptors_region))));
}

UNMAP_AFTER_INIT ErrorOr<void> E1000ENetworkAdapter::initialize(Badge<NetworkingManagement>)
{
    dmesgln("E1000e: Found @ {}", device_identifier().address());
    enable_bus_mastering(device_identifier());

    dmesgln("E1000e: IO base: {}", m_registers_io_window);
    read_mac_address();
    auto const& mac = mac_address();
    dmesgln("E1000e: MAC address: {}", mac.to_string());

    m_mdio_handling_process = TRY(spawn_mdio_handling_task(GIGABIT_PHY_ID));

    initialize_rx_descriptors();
    initialize_tx_descriptors();

    setup_link();
    TRY(setup_interrupts());
    autoconfigure_link_local_ipv6();
    return {};
}

UNMAP_AFTER_INIT E1000ENetworkAdapter::E1000ENetworkAdapter(StringView interface_name,
    PCI::DeviceIdentifier const& device_identifier,
    NonnullOwnPtr<IOWindow> registers_io_window, NonnullOwnPtr<Memory::Region> rx_buffer_region,
    NonnullOwnPtr<Memory::Region> tx_buffer_region, Memory::TypedMapping<RxDescriptor volatile[]> rx_descriptors,
    Memory::TypedMapping<TxDescriptor volatile[]> tx_descriptors)
    : E1000NetworkAdapter(interface_name, device_identifier, move(registers_io_window),
          move(rx_buffer_region),
          move(tx_buffer_region),
          move(rx_descriptors),
          move(tx_descriptors))
{
}

UNMAP_AFTER_INIT E1000ENetworkAdapter::~E1000ENetworkAdapter()
{
    if (m_mdio_handling_process) {
        m_mdio_handling_process->die();
        // Block until all threads exited to prevent UAF
        ErrorOr<siginfo_t> result = siginfo_t {};
        (void)Thread::current()->block<Thread::WaitBlocker>({}, WEXITED, m_mdio_handling_process.release_nonnull(), result);
    }
}

u16 E1000ENetworkAdapter::read_phy_register(u8 phy_id, MDIO::Clause22::RegisterAddress address)
{
    // FIXME: Newer controllers (like the I211) don't have a PHYADD field in the MDIC register.
    //        They instead have it in a separate register.
    out32(REG_MDIC, REG_MDIC_OP_READ | (to_underlying(address) << REG_MDIC_REGADD_OFFSET) | (phy_id << REG_MDIC_PHYADD_OFFSET));

    for (;;) {
        u32 mdic = in32(REG_MDIC);
        if ((mdic & REG_MDIC_R) == 0) {
            Processor::wait_check();
            continue;
        }

        return (mdic & REG_MDIC_DATA_MASK) >> REG_MDIC_DATA_OFFSET;
    }
}

void E1000ENetworkAdapter::write_phy_register(u8 phy_id, MDIO::Clause22::RegisterAddress address, u16 value)
{
    // FIXME: Newer controllers (like the I211) don't have a PHYADD field in the MDIC register.
    //        They instead have it in a separate register.
    out32(REG_MDIC, REG_MDIC_OP_WRITE | (to_underlying(address) << REG_MDIC_REGADD_OFFSET) | (value << REG_MDIC_DATA_OFFSET) | (phy_id << REG_MDIC_PHYADD_OFFSET));

    while ((in32(REG_MDIC) & REG_MDIC_R) == 0)
        Processor::wait_check();
}

}
