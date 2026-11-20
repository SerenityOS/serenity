/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 * Copyright (c) 2026, Undefine <undefine@undefine.pl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Array.h>
#include <AK/GenericShorthands.h>
#include <AK/MACAddress.h>
#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Bus/PCI/IDs.h>
#include <Kernel/Debug.h>
#include <Kernel/Net/NetworkingManagement.h>
#include <Kernel/Net/Realtek/RTL8168NetworkAdapter.h>
#include <Kernel/Net/Realtek/RTL8168Registers.h>
#include <Kernel/Sections.h>

namespace Kernel {

#define TX_BUFFER_SIZE 0x1FF8
#define RX_BUFFER_SIZE 0x1FF8 // FIXME: this should be increased (0x3FFF)

UNMAP_AFTER_INIT ErrorOr<bool> RTL8168NetworkAdapter::probe(PCI::DeviceIdentifier const& pci_device_identifier)
{
    if (pci_device_identifier.hardware_id().vendor_id != PCI::VendorID::Realtek)
        return false;

    switch (pci_device_identifier.hardware_id().device_id) {
    case 0x8168:
    case 0x8125:
    case 0x8126:
    case 0x8127:
        return true;
    default:
        return false;
    }
}

UNMAP_AFTER_INIT ErrorOr<NonnullRefPtr<NetworkAdapter>> RTL8168NetworkAdapter::create(PCI::DeviceIdentifier const& pci_device_identifier)
{
    auto interface_name = TRY(NetworkingManagement::generate_interface_name_from_pci_address(pci_device_identifier));
    auto registers_io_window = TRY(IOWindow::create_for_pci_device_bar(pci_device_identifier, PCI::HeaderType0BaseRegister::BAR2));
    return TRY(adopt_nonnull_ref_or_enomem(new (nothrow) RTL8168NetworkAdapter(interface_name.representable_view(), pci_device_identifier, move(registers_io_window))));
}

bool RTL8168NetworkAdapter::determine_supported_version() const
{
    switch (m_version) {
    case ChipVersion::RTL8168B:
    case ChipVersion::RTL8168C_1:
    case ChipVersion::RTL8168C_2:
    case ChipVersion::RTL8168C_3:
    case ChipVersion::RTL8168C_4:
        return true;
    case ChipVersion::RTL8168CP_1:
    case ChipVersion::RTL8168CP_2:
    case ChipVersion::RTL8168D_1:
    case ChipVersion::RTL8168D_2:
    case ChipVersion::RTL8168DP_1:
    case ChipVersion::RTL8168DP_2:
        return false;
    case ChipVersion::RTL8168E:
    case ChipVersion::RTL8168EVL:
        return true;
    case ChipVersion::RTL8168F_1:
    case ChipVersion::RTL8168F_2:
    case ChipVersion::RTL8411:
    case ChipVersion::RTL8168G:
    case ChipVersion::RTL8168GU:
    case ChipVersion::RTL8411B:
        return false;
    case ChipVersion::RTL8168H:
        return true;
    case ChipVersion::RTL8168EP:
    case ChipVersion::RTL8168FP:
    case ChipVersion::RTL8125A:
        return false;
    case ChipVersion::RTL8125B:
        return true;
    case ChipVersion::RTL8125D:
    case ChipVersion::RTL8125BP:
    case ChipVersion::RTL8126A:
        return true;
    case ChipVersion::RTL8127A:
        return false;
    case ChipVersion::Unknown:
        return false;
    }

    VERIFY_NOT_REACHED();
}

UNMAP_AFTER_INIT RTL8168NetworkAdapter::RTL8168NetworkAdapter(StringView interface_name, PCI::DeviceIdentifier const& device_identifier, NonnullOwnPtr<IOWindow> registers_io_window)
    : NetworkAdapter(interface_name)
    , PCI::Device(device_identifier)
    , m_registers_io_window(move(registers_io_window))
    // FIXME: Synchronize DMA buffer accesses correctly and set the MemoryType to NonCacheable.
    , m_rx_descriptors(Memory::allocate_dma_region_as_typed_array<RXDescriptor volatile>(number_of_rx_descriptors + 1, "RTL8168 RX"sv, Memory::Region::Access::ReadWrite, Memory::MemoryType::IO).release_value_but_fixme_should_propagate_errors())
    , m_tx_descriptors(Memory::allocate_dma_region_as_typed_array<TXDescriptor volatile>(number_of_tx_descriptors + 1, "RTL8168 TX"sv, Memory::Region::Access::ReadWrite, Memory::MemoryType::IO).release_value_but_fixme_should_propagate_errors())
{
    dmesgln_pci(*this, "Found @ {}", device_identifier.address());
    dmesgln_pci(*this, "I/O port base: {}", m_registers_io_window);
}

UNMAP_AFTER_INIT ErrorOr<void> RTL8168NetworkAdapter::initialize(Badge<NetworkingManagement>)
{
    PCI::enable_memory_space(device_identifier());
    PCI::enable_bus_mastering(device_identifier());

    m_version = TRY(identify_chip_version());
    dmesgln_pci(*this, "Version detected - {} ({})", possible_device_name(), (u8)m_version);

    if (!determine_supported_version())
        dmesgln_pci(*this, "Support for your chip version ({}) is not fully implemented yet, the chip might not work entirely properly, please open a GH issue and include this message.", (u8)m_version);

    // set initial REG_RXCFG
    auto rx_config = RXConfig::MaxRXDMAUnlimited;
    if (m_version <= ChipVersion::RTL8168B)
        rx_config |= RXConfig::NoRXThreshold;
    else if (m_version <= ChipVersion::RTL8168CP_2 || (m_version >= ChipVersion::RTL8168EVL && m_version <= ChipVersion::RTL8168F_2))
        rx_config |= RXConfig::_128IntEnable | RXConfig::MultiEnable;
    else if (m_version >= ChipVersion::RTL8168G && m_version <= ChipVersion::RTL8168FP)
        rx_config |= RXConfig::_128IntEnable | RXConfig::MultiEnable | RXConfig::EarlyOff;
    else if (m_version == ChipVersion::RTL8125A)
        rx_config |= RXConfig::FetchDefault8125;
    else if (m_version >= ChipVersion::RTL8125B)
        rx_config |= RXConfig::FetchDefault8125 | RXConfig::PauseSlot;
    else
        rx_config |= RXConfig::_128IntEnable;

    out32(RTL8168Register::RXConfig, rx_config);

    // disable interrupts
    set_interrupt_mask(0);

    // initialize hardware
    if (m_version >= ChipVersion::RTL8125A) {
        m_ocp_base_address = OCP_STANDARD_PHY_BASE;

        out32(RTL8168Register::Misc, in32(RTL8168Register::Misc) | Misc::RXDVGateEnable);

        while ((in32(RTL8168Register::TXConfig) & TXConfig::Empty) == 0)
            Processor::wait_check();
        while ((in8(RTL8168Register::MCU) & (MCU::RXEmpty | MCU::TXEmpty)) == 0)
            Processor::wait_check();

        out8(RTL8168Register::Command, in8(RTL8168Register::Command) & ~(Command::RXEnable | Command::TXEnable));
        out8(RTL8168Register::MCU, in8(RTL8168Register::MCU) & ~MCU::NowIsOOB);

        ocp_update(0xe8de, 0, 1 << 14);
        while ((in8(RTL8168Register::MCU) & MCU::LinkListReady) == 0)
            Processor::wait_check();

        ocp_out(0xc0aa, 0x07d0);
        ocp_out(0xc0a6, 0x0150);
        ocp_out(0xc01e, 0x5555);
        while ((in8(RTL8168Register::MCU) & MCU::LinkListReady) == 0)
            Processor::wait_check();
    } else if (m_version >= ChipVersion::RTL8168G) {
        m_ocp_base_address = OCP_STANDARD_PHY_BASE;

        if (m_version == ChipVersion::RTL8168EP) {
            // disable CMAC
            out8(RTL8168Register::IBCR2, in8(RTL8168Register::IBCR2) & ~1);

            while ((in8(RTL8168Register::IBISR0) & 0x2) != 0)
                Processor::wait_check();

            out8(RTL8168Register::IBISR0, in8(RTL8168Register::IBISR0) | 0x20);
            out8(RTL8168Register::IBCR0, in8(RTL8168Register::IBCR0) & ~1);
        }

        // enable RXDV gate
        out32(RTL8168Register::Misc, in32(RTL8168Register::Misc) | Misc::RXDVGateEnable);

        while ((in32(RTL8168Register::TXConfig) & TXConfig::Empty) == 0)
            Processor::wait_check();

        while ((in8(RTL8168Register::MCU) & (MCU::RXEmpty | MCU::TXEmpty)) == 0)
            Processor::wait_check();

        out8(RTL8168Register::Command, in8(RTL8168Register::Command) & ~(Command::RXEnable | Command::TXEnable));
        out8(RTL8168Register::MCU, in8(RTL8168Register::MCU) & ~MCU::NowIsOOB);

        // vendor magic values ???
        auto data = ocp_in(0xe8de);
        data &= ~(1 << 14);
        ocp_out(0xe8de, data);

        while ((in8(RTL8168Register::MCU) & MCU::LinkListReady) == 0)
            Processor::wait_check();

        // vendor magic values ???
        data = ocp_in(0xe8de);
        data |= (1 << 15);
        ocp_out(0xe8de, data);

        while ((in8(RTL8168Register::MCU) & MCU::LinkListReady) == 0)
            Processor::wait_check();
    }

    // clear interrupts
    set_interrupt_status(0xFFFF);

    pci_commit();

    // software reset
    reset();

    read_mac_address();
    dmesgln_pci(*this, "MAC address: {}", mac_address().to_string());

    // notify about driver start
    if (m_version >= ChipVersion::RTL8168DP_1 && m_version <= ChipVersion::RTL8168DP_2) {
        // if check_dash
        //  notify
        TODO();
    } else if (m_version == ChipVersion::RTL8168EP) {
        // if check_dash
        //  notify
        TODO();
    }

    TRY(startup());
    return {};
}

ErrorOr<void> RTL8168NetworkAdapter::startup()
{
    // initialize descriptors
    initialize_rx_descriptors();
    initialize_tx_descriptors();

    // register irq
    TRY(reserve_irqs(1, m_version <= ChipVersion::RTL8168B ? PCI::AllowedInterruptTypes::Pin : PCI::AllowedInterruptTypes::PinAndMSIAndMSIX));

    auto interrupt_number = TRY(allocate_irq(0));
    m_interrupt_handler = TRY(InterruptHandler::create(*this, interrupt_number));

    // version specific phy configuration
    configure_phy();
    pci_commit();

    // disable interrupts
    set_interrupt_mask(0);
    set_interrupt_status(0xFFFF);
    pci_commit();

    // send stop command
    out8(RTL8168Register::Command, Command::Stop);

    reset();

    // software reset phy
    phy_out(PHY_REG_BMCR, phy_in(PHY_REG_BMCR) | BMCR_RESET);
    while ((phy_in(PHY_REG_BMCR) & BMCR_RESET) != 0)
        Processor::wait_check();

    set_phy_speed();

    // set C+ command
    out16(RTL8168Register::CPlusCommand, in16(RTL8168Register::CPlusCommand) | CPlusCommand::VerifyChecksum | CPlusCommand::VLANStrip);
    in16(RTL8168Register::CPlusCommand); // C+ Command barrier

    if (m_version == ChipVersion::RTL8168C_2 || m_version == ChipVersion::RTL8168C_3) {
        if (in8(RTL8168Register::MACDebug) & 0x80)
            out8(RTL8168Register::GPIO, in8(RTL8168Register::GPIO) | GPIO::Enable);
        else
            out8(RTL8168Register::GPIO, in8(RTL8168Register::GPIO) & ~GPIO::Enable);
    }

    // power up phy
    if (m_version >= ChipVersion::RTL8168D_1 && m_version <= ChipVersion::RTL8168E) {
        out8(RTL8168Register::PMCH, in8(RTL8168Register::PMCH) | 0x80);
    } else if (m_version >= ChipVersion::RTL8168GU) {
        out8(RTL8168Register::PMCH, in8(RTL8168Register::PMCH) | 0xC0);
    } else if (m_version == ChipVersion::RTL8168G) {
        out8(RTL8168Register::PMCH, in8(RTL8168Register::PMCH) | 0xC0);
        // vendor magic values ???
        eri_update(0x1a8, ERI_MASK_1111, 0xfc000000, 0, ERI_EXGMAC);
    }

    // wakeup phy (more vendor magic values)
    phy_out(0x1F, 0);
    if (m_version <= ChipVersion::RTL8168DP_2) {
        phy_out(0x0E, 0);
    }
    phy_out(PHY_REG_BMCR, BMCR_AUTO_NEGOTIATE); // send known good phy write (acts as a phy barrier)
    start_hardware();
    pci_commit();

    // re-enable interrupts
    u32 enabled_interrupts = Interrupt::RXOK | Interrupt::RXError | Interrupt::TXOK | Interrupt::TXError | Interrupt::RXOverflow | Interrupt::LinkChange | Interrupt::SystemError;
    set_interrupt_mask(enabled_interrupts);
    pci_commit();

    // update link status
    m_link_up = (read_phy_status() & PHYStatus::LinkStatus) != 0;
    autoconfigure_link_local_ipv6();

    return {};
}

void RTL8168NetworkAdapter::rar_exgmac_set()
{
    auto mac = mac_address();

    auto const w = to_array<u16>({
        (u16)(mac[0] | (mac[1] << 8)),
        (u16)(mac[2] | (mac[3] << 8)),
        (u16)(mac[4] | (mac[5] << 8)),
    });

    auto const exg_mac_registers = to_array<ExgMacRegister>({
        { 0xe0, ERI_MASK_1111, (u32)(w[0] | (w[1] << 16)) },
        { 0xe4, ERI_MASK_1111, (u32)w[2] },
        { 0xf0, ERI_MASK_1111, (u32)(w[0] << 16) },
        { 0xf4, ERI_MASK_1111, (u32)(w[1] | (w[2] << 16)) },
    });

    exgmac_out_batch(exg_mac_registers);
}

void RTL8168NetworkAdapter::start_hardware()
{
    // unlock config registers
    out8(RTL8168Register::CFG9346, CFG9346::Unlock);

    // configure the maximum transmit packet size
    if (m_version <= ChipVersion::RTL8168FP)
        out8(RTL8168Register::MaxTXPacketSize, MTPS_JUMBO);

    // configure the maximum receive packet size
    out16(RTL8168Register::RMS, RX_BUFFER_SIZE);

    auto cplus_command = in16(RTL8168Register::CPlusCommand);
    cplus_command |= CPlusCommand::PacketControlDisable;
    // undocumented magic value???
    cplus_command |= 0x1;
    out16(RTL8168Register::CPlusCommand, cplus_command);

    // setup interrupt moderation, magic from vendor (Linux Driver uses 0x5151, *BSD Driver uses 0x5100, RTL Driver use 0x5f51???)
    if (m_version <= ChipVersion::RTL8168FP)
        out16(RTL8168Register::InterruptModeration, 0x5151);

    // point to tx descriptors
    out64(RTL8168Register::TXAddress, m_tx_descriptors.paddr.get());

    // point to rx descriptors
    out64(RTL8168Register::RXAddress, m_rx_descriptors.paddr.get());

    // configure tx: use the maximum dma transfer size, default interframe gap time.
    out32(RTL8168Register::TXConfig, TXConfig::MaxTXDMAUnlimited | TXConfig::InterFrameGap);

    // disable interrupt coalescing
    if (m_version >= ChipVersion::RTL8125A) {
        out8(RTL8168Register::INT_CFG0_8125, 0x00);

        if (first_is_one_of(m_version, ChipVersion::RTL8125B, ChipVersion::RTL8126A, ChipVersion::RTL8127A))
            out16(RTL8168Register::INT_CFG1_8125, 0x0000);

        if (first_is_one_of(m_version, ChipVersion::RTL8125A, ChipVersion::RTL8125D, ChipVersion::RTL8125BP, ChipVersion::RTL8127A)) {
            for (u16 i = 0xA00; i < 0xB00; i += 4)
                out32(static_cast<RTL8168Register>(i), 0);
        } else if (first_is_one_of(m_version, ChipVersion::RTL8125B, ChipVersion::RTL8126A)) {
            for (u16 i = 0xA00; i < 0xA80; i += 4)
                out32(static_cast<RTL8168Register>(i), 0);
        }
    }

    // version specific quirks and tweaks
    hardware_quirks();

    // known good read (acts as a barrier)
    in32(RTL8168Register::TXConfig);

    // relock config registers
    out8(RTL8168Register::CFG9346, 0);

    // enable rx/tx
    out8(RTL8168Register::Command, Command::RXEnable | Command::TXEnable);
    pci_commit();

    // turn on all multicast
    out32(RTL8168Register::MulticastAddress0, 0xFFFFFFFF);
    out32(RTL8168Register::MulticastAddress4, 0xFFFFFFFF);

    // configure rx mode: accept physical (MAC) match, multicast, and broadcast
    out32(RTL8168Register::RXConfig, (in32(RTL8168Register::RXConfig) & ~RXConfig::AcceptMask) | RXConfig::AcceptPhysicalMatch | RXConfig::AcceptMulticast | RXConfig::AcceptBroadcast);

    // disable early-rx interrupts
    out16(RTL8168Register::MultipleInterrupt, in16(RTL8168Register::MultipleInterrupt) & 0xF000);
}

void RTL8168NetworkAdapter::set_phy_speed()
{
    // wakeup phy
    phy_out(0x1F, 0);

    // advertise all available features to get best connection possible
    auto auto_negotiation_advertisement = phy_in(PHY_REG_ANAR);
    auto_negotiation_advertisement |= ADVERTISE_10_HALF;    // 10 mbit half duplex
    auto_negotiation_advertisement |= ADVERTISE_10_FULL;    // 10 mbit full duplex
    auto_negotiation_advertisement |= ADVERTISE_100_HALF;   // 100 mbit half duplex
    auto_negotiation_advertisement |= ADVERTISE_100_FULL;   // 100 mbit full duplex
    auto_negotiation_advertisement |= ADVERTISE_PAUSE_CAP;  // capable of pause flow control
    auto_negotiation_advertisement |= ADVERTISE_PAUSE_ASYM; // capable of asymmetric pause flow control
    phy_out(PHY_REG_ANAR, auto_negotiation_advertisement);

    auto gigabyte_control = phy_in(PHY_REG_GBCR);
    gigabyte_control |= ADVERTISE_1000_HALF; // 1000 mbit half dulpex
    gigabyte_control |= ADVERTISE_1000_FULL; // 1000 mbit full duplex
    phy_out(PHY_REG_GBCR, gigabyte_control);

    // restart auto-negotiation with set advertisements
    phy_out(PHY_REG_BMCR, BMCR_AUTO_NEGOTIATE | BMCR_RESTART_AUTO_NEGOTIATE);
}

UNMAP_AFTER_INIT void RTL8168NetworkAdapter::initialize_rx_descriptors()
{
    for (size_t i = 0; i < number_of_rx_descriptors; ++i) {
        auto& descriptor = m_rx_descriptors[i];
        auto region = MM.allocate_contiguous_kernel_region(Memory::page_round_up(RX_BUFFER_SIZE).release_value_but_fixme_should_propagate_errors(), "RTL8168 RX buffer"sv, Memory::Region::Access::ReadWrite).release_value();
        memset(region->vaddr().as_ptr(), 0, region->size()); // MM already zeros out newly allocated pages, but we do it again in case that ever changes
        m_rx_buffers_regions.try_append(move(region)).release_value_but_fixme_should_propagate_errors();

        descriptor.buffer_size = RX_BUFFER_SIZE;
        descriptor.flags = RXDescriptor::Ownership; // let the NIC know it can use this descriptor
        auto physical_address = m_rx_buffers_regions[i]->physical_page(0)->paddr().get();
        descriptor.buffer_address_low = physical_address & 0xFFFFFFFF;
        descriptor.buffer_address_high = (u64)physical_address >> 32; // cast to prevent shift count >= with of type warnings in 32 bit systems
    }
    m_rx_descriptors[number_of_rx_descriptors - 1].flags = m_rx_descriptors[number_of_rx_descriptors - 1].flags | RXDescriptor::EndOfRing;
}

UNMAP_AFTER_INIT void RTL8168NetworkAdapter::initialize_tx_descriptors()
{
    for (size_t i = 0; i < number_of_tx_descriptors; ++i) {
        auto& descriptor = m_tx_descriptors[i];
        auto region = MM.allocate_contiguous_kernel_region(Memory::page_round_up(TX_BUFFER_SIZE).release_value_but_fixme_should_propagate_errors(), "RTL8168 TX buffer"sv, Memory::Region::Access::ReadWrite).release_value();
        memset(region->vaddr().as_ptr(), 0, region->size()); // MM already zeros out newly allocated pages, but we do it again in case that ever changes
        m_tx_buffers_regions.try_append(move(region)).release_value_but_fixme_should_propagate_errors();

        descriptor.flags = TXDescriptor::FirstSegment | TXDescriptor::LastSegment;
        auto physical_address = m_tx_buffers_regions[i]->physical_page(0)->paddr().get();
        descriptor.buffer_address_low = physical_address & 0xFFFFFFFF;
        descriptor.buffer_address_high = (u64)physical_address >> 32;
    }
    m_tx_descriptors[number_of_tx_descriptors - 1].flags = m_tx_descriptors[number_of_tx_descriptors - 1].flags | TXDescriptor::EndOfRing;
}

UNMAP_AFTER_INIT RTL8168NetworkAdapter::~RTL8168NetworkAdapter() = default;

bool RTL8168NetworkAdapter::handle_interrupt()
{
    bool was_handled = false;
    for (;;) {
        u32 status = read_interrupt_status();
        set_interrupt_status(status);

        m_entropy_source.add_random_event(status);

        dbgln_if(RTL8168_DEBUG, "RTL8168: handle_irq status={:#04x}", status);

        if ((status & (Interrupt::RXOK | Interrupt::RXError | Interrupt::TXOK | Interrupt::TXError | Interrupt::RXOverflow | Interrupt::LinkChange | Interrupt::RXFIFOOverflow | Interrupt::SystemError)) == 0)
            break;

        was_handled = true;
        if (status & Interrupt::RXOK) {
            dbgln_if(RTL8168_DEBUG, "RTL8168: RX ready");
            receive();
        }
        if (status & Interrupt::RXError) {
            dbgln_if(RTL8168_DEBUG, "RTL8168: RX error - invalid packet");
        }
        if (status & Interrupt::TXOK) {
            dbgln_if(RTL8168_DEBUG, "RTL8168: TX complete");
            m_wait_queue.wake_one();
        }
        if (status & Interrupt::TXError) {
            dbgln_if(RTL8168_DEBUG, "RTL8168: TX error - invalid packet");
        }
        if (status & Interrupt::RXOverflow) {
            dmesgln_pci(*this, "RX descriptor unavailable (packet lost)");
            receive();
        }
        if (status & Interrupt::LinkChange) {
            m_link_up = (read_phy_status() & PHYStatus::LinkStatus) != 0;
            dmesgln_pci(*this, "Link status changed up={}", m_link_up);
            autoconfigure_link_local_ipv6();
        }
        if (status & Interrupt::RXFIFOOverflow) {
            dmesgln_pci(*this, "RX FIFO overflow");
            receive();
        }
        if (status & Interrupt::SystemError) {
            dmesgln_pci(*this, "Fatal system error");
        }
    }
    return was_handled;
}

void RTL8168NetworkAdapter::reset()
{
    out8(RTL8168Register::Command, Command::Reset);
    while ((in8(RTL8168Register::Command) & Command::Reset) != 0)
        Processor::wait_check();
}

void RTL8168NetworkAdapter::pci_commit()
{
    // read any register to commit previous PCI write
    in8(RTL8168Register::Command);
}

UNMAP_AFTER_INIT void RTL8168NetworkAdapter::read_mac_address()
{
    MACAddress mac {};
    for (int i = 0; i < 6; i++)
        mac[i] = in8(static_cast<RTL8168Register>(static_cast<u16>(RTL8168Register::MACAddress) + i));
    set_mac_address(mac);
}

void RTL8168NetworkAdapter::send_raw(ReadonlyBytes payload)
{
    dbgln_if(RTL8168_DEBUG, "RTL8168: send_raw length={}", payload.size());

    if (payload.size() > TX_BUFFER_SIZE) {
        dmesgln_pci(*this, "Packet was too big; discarding");
        return;
    }

    auto& free_descriptor = m_tx_descriptors[m_tx_free_index];

    if ((free_descriptor.flags & TXDescriptor::Ownership) != 0) {
        dbgln_if(RTL8168_DEBUG, "RTL8168: No free TX buffers, sleeping until one is available");
        m_wait_queue.wait_forever("RTL8168NetworkAdapter"sv);
        return send_raw(payload);
        // if we woke up a TX descriptor is guaranteed to be available, so this should never recurse more than once
        // but this can probably be done more cleanly
    }

    dbgln_if(RTL8168_DEBUG, "RTL8168: Chose descriptor {}", m_tx_free_index);
    memcpy(m_tx_buffers_regions[m_tx_free_index]->vaddr().as_ptr(), payload.data(), payload.size());

    auto descriptor_index = m_tx_free_index;
    m_tx_free_index = (m_tx_free_index + 1) % number_of_tx_descriptors;

    // Ensure all flags are set correctly
    u16 flags = TXDescriptor::Ownership | TXDescriptor::FirstSegment | TXDescriptor::LastSegment;
    if (descriptor_index == number_of_tx_descriptors - 1)
        flags |= TXDescriptor::EndOfRing;

    free_descriptor.frame_length = payload.size() & 0x3FFF;
    free_descriptor.flags = flags;

    doorbell(); // FIXME: this shouldn't be done so often, we should look into doing this using the watchdog timer
}

void RTL8168NetworkAdapter::receive()
{
    for (u16 i = 0; i < number_of_rx_descriptors; ++i) {
        auto descriptor_index = (m_rx_free_index + i) % number_of_rx_descriptors;
        auto& descriptor = m_rx_descriptors[descriptor_index];

        if ((descriptor.flags & RXDescriptor::Ownership) != 0) {
            m_rx_free_index = descriptor_index;
            break;
        }

        u16 flags = descriptor.flags;
        u16 length = descriptor.buffer_size & 0x3FFF;

        dbgln_if(RTL8168_DEBUG, "RTL8168: receive, flags={:#04x}, length={}, descriptor={}", flags, length, descriptor_index);

        if (length > RX_BUFFER_SIZE || (flags & RXDescriptor::ErrorSummary) != 0) {
            dmesgln_pci(*this, "receive got bad packet, flags={:#04x}, length={}", flags, length);
        } else if ((flags & RXDescriptor::FirstSegment) != 0 && (flags & RXDescriptor::LastSegment) == 0) {
            VERIFY_NOT_REACHED();
            // Our maximum received packet size is smaller than the descriptor buffer size, so packets should never be segmented
            // if this happens on a real NIC it might not respect that, and we will have to support packet segmentation
        } else {
            did_receive({ m_rx_buffers_regions[descriptor_index]->vaddr().as_ptr(), length });
        }

        descriptor.buffer_size = RX_BUFFER_SIZE;
        flags = RXDescriptor::Ownership;
        if (descriptor_index == number_of_rx_descriptors - 1)
            flags |= RXDescriptor::EndOfRing;
        descriptor.flags = flags; // let the NIC know it can use this descriptor again
    }
}

void RTL8168NetworkAdapter::set_interrupt_mask(u32 mask)
{
    if (m_version >= ChipVersion::RTL8125A) {
        out32(RTL8168Register::InterruptMask_8125, mask);
    } else {
        VERIFY((mask & 0xFFFF0000) == 0);
        out16(RTL8168Register::InterruptMask, mask);
    }
}

u32 RTL8168NetworkAdapter::read_interrupt_status()
{
    if (m_version >= ChipVersion::RTL8125A)
        return in32(RTL8168Register::InterruptStatus_8125);

    return in16(RTL8168Register::InterruptStatus);
}

void RTL8168NetworkAdapter::set_interrupt_status(u32 status)
{
    if (m_version >= ChipVersion::RTL8125A) {
        out32(RTL8168Register::InterruptStatus_8125, status);
    } else {
        VERIFY((status & 0xFFFF0000) == 0);
        out16(RTL8168Register::InterruptStatus, status);
    }
}

void RTL8168NetworkAdapter::doorbell()
{
    if (m_version >= ChipVersion::RTL8125A)
        out8(RTL8168Register::TXStart_8125, 0x1);
    else
        out8(RTL8168Register::TXStart, TXStart::Start);
}

u32 RTL8168NetworkAdapter::read_phy_status()
{
    if (m_version >= ChipVersion::RTL8125A)
        return in32(RTL8168Register::PHYStatus);

    return in8(RTL8168Register::PHYStatus);
}

void RTL8168NetworkAdapter::out8(RTL8168Register reg, u8 data)
{
    m_registers_io_window->write8(to_underlying(reg), data);
}

void RTL8168NetworkAdapter::out16(RTL8168Register reg, u16 data)
{
    m_registers_io_window->write16(to_underlying(reg), data);
}

void RTL8168NetworkAdapter::out32(RTL8168Register reg, u32 data)
{
    m_registers_io_window->write32(to_underlying(reg), data);
}

void RTL8168NetworkAdapter::out64(RTL8168Register reg, u64 data)
{
    // ORDER MATTERS: Some NICs require the high part of the address to be written first
    m_registers_io_window->write32(to_underlying(reg) + 4, (u32)(data >> 32));
    m_registers_io_window->write32(to_underlying(reg), (u32)(data & 0xFFFFFFFF));
}

u8 RTL8168NetworkAdapter::in8(RTL8168Register reg)
{
    return m_registers_io_window->read8(to_underlying(reg));
}

u16 RTL8168NetworkAdapter::in16(RTL8168Register reg)
{
    return m_registers_io_window->read16(to_underlying(reg));
}

u32 RTL8168NetworkAdapter::in32(RTL8168Register reg)
{
    return m_registers_io_window->read32(to_underlying(reg));
}

void RTL8168NetworkAdapter::phy_out(u8 address, u16 data)
{
    if (m_version == ChipVersion::RTL8168DP_1 || m_version == ChipVersion::RTL8168DP_2) {
        TODO();
    } else if (m_version >= ChipVersion::RTL8168G) {
        if (address == 0x1F) {
            m_ocp_base_address = data ? data << 4 : OCP_STANDARD_PHY_BASE;
            return;
        }

        if (m_ocp_base_address != OCP_STANDARD_PHY_BASE)
            address -= 0x10;

        ocp_phy_out(m_ocp_base_address + address * 2, data);
    } else {
        VERIFY((address & 0xE0) == 0); // register address is only 5 bit
        out32(RTL8168Register::PHYAccess, PHY_FLAG | (address & 0x1F) << 16 | (data & 0xFFFF));
        while ((in32(RTL8168Register::PHYAccess) & PHY_FLAG) != 0)
            Processor::wait_check();
    }
}

u16 RTL8168NetworkAdapter::phy_in(u8 address)
{
    if (m_version == ChipVersion::RTL8168DP_1 || m_version == ChipVersion::RTL8168DP_2) {
        TODO();
    } else if (m_version >= ChipVersion::RTL8168G) {
        if (m_ocp_base_address != OCP_STANDARD_PHY_BASE)
            address -= 0x10;

        return ocp_phy_in(m_ocp_base_address + address * 2);
    } else {
        VERIFY((address & 0xE0) == 0); // register address is only 5 bit
        out32(RTL8168Register::PHYAccess, (address & 0x1F) << 16);
        while ((in32(RTL8168Register::PHYAccess) & PHY_FLAG) == 0)
            Processor::wait_check();
        return in32(RTL8168Register::PHYAccess) & 0xFFFF;
    }
}

void RTL8168NetworkAdapter::phy_update(u32 address, u32 set, u32 clear)
{
    auto value = phy_in(address);
    phy_out(address, (value & ~clear) | set);
}

void RTL8168NetworkAdapter::phy_out_batch(ReadonlySpan<PhyRegister> phy_registers)
{
    for (auto const& phy_register : phy_registers) {
        phy_out(phy_register.address, phy_register.data);
    }
}

void RTL8168NetworkAdapter::extended_phy_out(u8 address, u16 data)
{
    VERIFY((address & 0xE0) == 0); // register address is only 5 bit
    out32(RTL8168Register::EPHYAccess, EPHY_FLAG | (address & 0x1F) << 16 | (data & 0xFFFF));
    while ((in32(RTL8168Register::EPHYAccess) & EPHY_FLAG) != 0)
        Processor::wait_check();
}

u16 RTL8168NetworkAdapter::extended_phy_in(u8 address)
{
    VERIFY((address & 0xE0) == 0); // register address is only 5 bit
    out32(RTL8168Register::EPHYAccess, (address & 0x1F) << 16);
    while ((in32(RTL8168Register::EPHYAccess) & EPHY_FLAG) == 0)
        Processor::wait_check();
    return in32(RTL8168Register::EPHYAccess) & 0xFFFF;
}

void RTL8168NetworkAdapter::extended_phy_initialize(ReadonlySpan<EPhyUpdate> ephy_info)
{
    for (auto const& info : ephy_info) {
        auto updated_value = (extended_phy_in(info.offset) & ~info.clear) | info.set;
        extended_phy_out(info.offset, updated_value);
    }
}

void RTL8168NetworkAdapter::eri_out(u32 address, u32 mask, u32 data, u32 type)
{
    out32(RTL8168Register::ERIData, data);
    out32(RTL8168Register::ERIAddress, ERI_FLAG | type | mask | address);
    while ((in32(RTL8168Register::ERIAddress) & ERI_FLAG) != 0)
        Processor::wait_check();
}

u32 RTL8168NetworkAdapter::eri_in(u32 address, u32 type)
{
    out32(RTL8168Register::ERIAddress, type | ERI_MASK_1111 | address);
    while ((in32(RTL8168Register::ERIAddress) & ERI_FLAG) == 0)
        Processor::wait_check();
    return in32(RTL8168Register::ERIData);
}

void RTL8168NetworkAdapter::eri_update(u32 address, u32 mask, u32 set, u32 clear, u32 type)
{
    auto value = eri_in(address, type);
    eri_out(address, mask, (value & ~clear) | set, type);
}

void RTL8168NetworkAdapter::exgmac_out_batch(ReadonlySpan<ExgMacRegister> exgmac_registers)
{
    for (auto const& exgmac_register : exgmac_registers) {
        eri_out(exgmac_register.address, exgmac_register.mask, exgmac_register.value, ERI_EXGMAC);
    }
}

void RTL8168NetworkAdapter::csi_out(u32 address, u32 data)
{
    VERIFY(m_version >= ChipVersion::RTL8168C_1);
    out32(RTL8168Register::CSIData, data);
    auto modifier = CSI_BYTE_ENABLE;
    if (m_version == ChipVersion::RTL8411) {
        modifier |= CSI_FUNC_NIC;
    } else if (m_version == ChipVersion::RTL8411B) {
        modifier |= CSI_FUNC_NIC2;
    }
    out32(RTL8168Register::CSIAddress, CSI_FLAG | (address & 0xFFF) | modifier);
    while ((in32(RTL8168Register::CSIAddress) & CSI_FLAG) != 0)
        Processor::wait_check();
}

u32 RTL8168NetworkAdapter::csi_in(u32 address)
{
    VERIFY(m_version >= ChipVersion::RTL8168C_1);
    auto modifier = CSI_BYTE_ENABLE;
    if (m_version == ChipVersion::RTL8411) {
        modifier |= CSI_FUNC_NIC;
    } else if (m_version == ChipVersion::RTL8411B) {
        modifier |= CSI_FUNC_NIC2;
    }
    out32(RTL8168Register::CSIAddress, (address & 0xFFF) | modifier);
    while ((in32(RTL8168Register::CSIAddress) & CSI_FLAG) == 0)
        Processor::wait_check();
    return in32(RTL8168Register::CSIData) & 0xFFFF;
}

void RTL8168NetworkAdapter::csi_enable(u32 bits)
{
    auto csi = csi_in(0x70c) & 0x00ffffff;
    csi_out(0x70c, csi | bits);
}

void RTL8168NetworkAdapter::ocp_out(u32 address, u32 data)
{
    VERIFY((address & 0xFFFF0001) == 0);
    out32(RTL8168Register::OCPData, OCP_FLAG | address << 15 | data);
}

u32 RTL8168NetworkAdapter::ocp_in(u32 address)
{
    VERIFY((address & 0xFFFF0001) == 0);
    out32(RTL8168Register::OCPData, address << 15);
    return in32(RTL8168Register::OCPData);
}

void RTL8168NetworkAdapter::ocp_update(u32 address, u32 set, u32 clear)
{
    ocp_out(address, ((ocp_in(address) & ~clear) | set) & 0xFFFF);
}

void RTL8168NetworkAdapter::ocp_phy_out(u32 address, u32 data)
{
    VERIFY((address & 0xFFFF0001) == 0);
    out32(RTL8168Register::GPHYOCP, OCP_FLAG | (address << 15) | data);
    while ((in32(RTL8168Register::GPHYOCP) & OCP_FLAG) != 0)
        Processor::wait_check();
}

u16 RTL8168NetworkAdapter::ocp_phy_in(u32 address)
{
    VERIFY((address & 0xFFFF0001) == 0);
    out32(RTL8168Register::GPHYOCP, address << 15);
    while ((in32(RTL8168Register::GPHYOCP) & OCP_FLAG) == 0)
        Processor::wait_check();
    return in32(RTL8168Register::GPHYOCP) & 0xFFFF;
}

ErrorOr<RTL8168NetworkAdapter::ChipVersion> RTL8168NetworkAdapter::identify_chip_version()
{
    struct VersionInfo {
        u32 hardware_version_id_0;
        Optional<u32> hardware_version_id_1;
        Optional<RTL8168NetworkAdapter::ChipVersion> version;
    };

    static constexpr auto VERSION_LIST = to_array<VersionInfo>({
        { 0x300, {}, {} },

        { 0x380, {}, ChipVersion::RTL8168B },

        { 0x3C0, 0x0, ChipVersion::RTL8168C_1 },
        { 0x3C0, 0x2, ChipVersion::RTL8168C_2 },
        { 0x3C0, 0x3, ChipVersion::RTL8168C_3 },
        { 0x3C0, {}, ChipVersion::RTL8168C_4 },

        { 0x3C8, 0x0, ChipVersion::RTL8168CP_1 },
        { 0x3C8, {}, ChipVersion::RTL8168CP_2 },

        { 0x280, 0x1, ChipVersion::RTL8168D_1 },
        { 0x280, {}, ChipVersion::RTL8168D_2 },

        { 0x288, 0x0, {} },
        { 0x288, 0x2, ChipVersion::RTL8168DP_1 },
        { 0x288, 0x3, ChipVersion::RTL8168DP_2 },

        { 0x2C0, {}, ChipVersion::RTL8168E },

        { 0x2C8, {}, ChipVersion::RTL8168EVL },

        { 0x480, 0x0, ChipVersion::RTL8168F_1 },
        { 0x480, 0x1, ChipVersion::RTL8168F_2 },

        { 0x488, {}, ChipVersion::RTL8411 },

        { 0x4C0, 0x0, ChipVersion::RTL8168G },

        { 0x500, 0x0, {} },
        { 0x500, 0x1, {} },
        { 0x500, 0x2, ChipVersion::RTL8168EP },

        { 0x508, 0x1, ChipVersion::RTL8168GU },

        { 0x5C8, 0x0, ChipVersion::RTL8411B },

        { 0x540, 0x0, {} },
        { 0x540, 0x1, ChipVersion::RTL8168H },

        { 0x548, 0x2, ChipVersion::RTL8168FP },
        { 0x548, 0x3, ChipVersion::RTL8168FP },

        { 0x608, 0x1, ChipVersion::RTL8125A },

        { 0x640, 0x1, ChipVersion::RTL8125B },

        { 0x648, 0x1, ChipVersion::RTL8126A },
        { 0x648, 0x2, ChipVersion::RTL8126A },

        { 0x680, 0x1, ChipVersion::RTL8125BP },

        { 0x688, 0x0, ChipVersion::RTL8125D },
        { 0x688, 0x1, ChipVersion::RTL8125D },
        { 0x688, 0x2, ChipVersion::RTL8125D },
        { 0x688, 0x3, ChipVersion::RTL8125D },

        { 0x6C0, 0x0, ChipVersion::RTL8168H },

        { 0x6C8, 0x1, ChipVersion::RTL8127A },

        { 0x708, 0x0, ChipVersion::RTL8125D },
    });

    auto transmit_config = in32(RTL8168Register::TXConfig);
    auto hardware_version_id_0 = (transmit_config & 0x7c800000) >> 20;
    auto hardware_version_id_1 = (transmit_config & 0x700000) >> 20;

    for (auto const& version : VERSION_LIST) {
        if (version.hardware_version_id_0 != hardware_version_id_0)
            continue;

        if (version.hardware_version_id_1.has_value() && version.hardware_version_id_1.value() != hardware_version_id_1)
            continue;

        if (version.version.has_value())
            return version.version.value();

        dmesgln_pci(*this, "Unsupported chip revision");
        return ENOTSUP;
    }

    dmesgln_pci(*this, "Unable to determine device version: {:#04x} ({:#01x})", hardware_version_id_0, hardware_version_id_1);
    return ChipVersion::Unknown;
}

StringView RTL8168NetworkAdapter::possible_device_name()
{
    switch (m_version) {
    case ChipVersion::RTL8168B:
        return "RTL8168B/8111B"sv;
    case ChipVersion::RTL8168C_1:
    case ChipVersion::RTL8168C_2:
    case ChipVersion::RTL8168C_3:
    case ChipVersion::RTL8168C_4:
        return "RTL8168C/8111C"sv;
    case ChipVersion::RTL8168CP_1:
    case ChipVersion::RTL8168CP_2:
        return "RTL8168CP/8111CP"sv;
    case ChipVersion::RTL8168D_1:
    case ChipVersion::RTL8168D_2:
        return "RTL8168D/8111D"sv;
    case ChipVersion::RTL8168DP_1:
    case ChipVersion::RTL8168DP_2:
        return "RTL8168DP/8111DP"sv;
    case ChipVersion::RTL8168E:
        return "RTL8168E/8111E"sv;
    case ChipVersion::RTL8168EVL:
        return "RTL8168E-VL/8111E-VL"sv;
    case ChipVersion::RTL8168F_1:
    case ChipVersion::RTL8168F_2:
        return "RTL8168F/8111F"sv;
    case ChipVersion::RTL8411:
        return "RTL8411"sv;
    case ChipVersion::RTL8168G:
        return "RTL8168G/8111G"sv;
    case ChipVersion::RTL8168GU:
        return "RTL8168GU/8111GU"sv;
    case ChipVersion::RTL8411B:
        return "RTL8411B"sv;
    case ChipVersion::RTL8168H:
        return "RTL8168H/8111H"sv;
    case ChipVersion::RTL8168EP:
        return "RTL8168EP/8111EP"sv;
    case ChipVersion::RTL8168FP:
        return "RTL8168FP/8117"sv;
    case ChipVersion::RTL8125A:
        return "RTL8125A"sv;
    case ChipVersion::RTL8125B:
        return "RTL8125B"sv;
    case ChipVersion::RTL8125D:
        return "RTL8125D/RTL8125K/RTL9151A/RTL8125CP"sv;
    case ChipVersion::RTL8125BP:
        return "RTL8125BP"sv;
    case ChipVersion::RTL8126A:
        return "RTL8126A"sv;
    case ChipVersion::RTL8127A:
        return "RTL8127A"sv;
    case ChipVersion::Unknown:
        return "Unknown"sv;
    }
    VERIFY_NOT_REACHED();
}

bool RTL8168NetworkAdapter::link_full_duplex()
{
    u32 phystatus = read_phy_status();
    return !!(phystatus & (PHYStatus::FullDuplex | PHYStatus::Speed1000MF | PHYStatus::Speed2500MF | PHYStatus::Speed5000MF | PHYStatus::Speed10000MF));
}

i32 RTL8168NetworkAdapter::link_speed()
{
    if (!link_up())
        return NetworkAdapter::LINKSPEED_INVALID;

    auto phystatus = read_phy_status();

    if (phystatus & PHYStatus::Speed10000MF)
        return 10000;
    if (phystatus & PHYStatus::Speed5000MF)
        return 5000;
    if (phystatus & PHYStatus::Speed2500MF)
        return 2500;
    if (phystatus & PHYStatus::Speed1000MF)
        return 1000;
    if (phystatus & PHYStatus::Speed100)
        return 100;
    if (phystatus & PHYStatus::Speed10)
        return 10;

    return NetworkAdapter::LINKSPEED_INVALID;
}
}
