/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/MACAddress.h>
#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Bus/PCI/IDs.h>
#include <Kernel/Debug.h>
#include <Kernel/Net/Intel/E1000NetworkAdapter.h>
#include <Kernel/Net/NetworkingManagement.h>
#include <Kernel/Sections.h>

namespace Kernel {

// https://www.intel.com/content/dam/doc/manual/pci-pci-x-family-gbe-controllers-software-dev-manual.pdf Section 5.2
UNMAP_AFTER_INIT static bool is_valid_device_id(u16 device_id)
{
    // FIXME: It would be nice to distinguish which particular device it is.
    //        Especially since it's needed to determine which registers we can access.
    //        The reason I haven't done it now is because there's some IDs with multiple devices
    //        and some devices with multiple IDs.
    switch (device_id) {
    case 0x1019: // 82547EI-A0, 82547EI-A1, 82547EI-B0, 82547GI-B0
    case 0x101A: // 82547EI-B0
    case 0x1010: // 82546EB-A1
    case 0x1012: // 82546EB-A1
    case 0x101D: // 82546EB-A1
    case 0x1079: // 82546GB-B0
    case 0x107A: // 82546GB-B0
    case 0x107B: // 82546GB-B0
    case 0x100F: // 82545EM-A
    case 0x1011: // 82545EM-A
    case 0x1026: // 82545GM-B
    case 0x1027: // 82545GM-B
    case 0x1028: // 82545GM-B
    case 0x1107: // 82544EI-A4
    case 0x1112: // 82544GC-A4
    case 0x1013: // 82541EI-A0, 82541EI-B0
    case 0x1018: // 82541EI-B0
    case 0x1076: // 82541GI-B1, 82541PI-C0
    case 0x1077: // 82541GI-B1
    case 0x1078: // 82541ER-C0
    case 0x1017: // 82540EP-A
    case 0x1016: // 82540EP-A
    case 0x100E: // 82540EM-A
    case 0x1015: // 82540EM-A
        return true;
    default:
        return false;
    }
}

UNMAP_AFTER_INIT ErrorOr<bool> E1000NetworkAdapter::probe(PCI::DeviceIdentifier const& pci_device_identifier)
{
    if (pci_device_identifier.hardware_id().vendor_id != PCI::VendorID::Intel)
        return false;
    return is_valid_device_id(pci_device_identifier.hardware_id().device_id);
}

UNMAP_AFTER_INIT ErrorOr<NonnullRefPtr<NetworkAdapter>> E1000NetworkAdapter::create(PCI::DeviceIdentifier const& pci_device_identifier)
{
    u8 irq = pci_device_identifier.interrupt_line().value();
    auto interface_name = TRY(NetworkingManagement::generate_interface_name_from_pci_address(pci_device_identifier));
    auto registers_io_window = TRY(IOWindow::create_for_pci_device_bar(pci_device_identifier, PCI::HeaderType0BaseRegister::BAR0));

    auto rx_buffer_region = TRY(MM.allocate_contiguous_kernel_region(rx_buffer_size * number_of_rx_descriptors, "E1000 RX buffers"sv, Memory::Region::Access::ReadWrite));
    auto tx_buffer_region = MM.allocate_contiguous_kernel_region(tx_buffer_size * number_of_tx_descriptors, "E1000 TX buffers"sv, Memory::Region::Access::ReadWrite).release_value();
    // FIXME: Synchronize DMA buffer accesses correctly and set the MemoryType to NonCacheable.
    auto rx_descriptors = TRY(Memory::allocate_dma_region_as_typed_array<E1000::RxDescriptor volatile>(number_of_rx_descriptors, "E1000 RX Descriptors"sv, Memory::Region::Access::ReadWrite, Memory::MemoryType::IO));
    auto tx_descriptors = TRY(Memory::allocate_dma_region_as_typed_array<E1000::TxDescriptor volatile>(number_of_tx_descriptors, "E1000 TX Descriptors"sv, Memory::Region::Access::ReadWrite, Memory::MemoryType::IO));

    return TRY(adopt_nonnull_ref_or_enomem(new (nothrow) E1000NetworkAdapter(interface_name.representable_view(),
        pci_device_identifier,
        irq, move(registers_io_window),
        move(rx_buffer_region),
        move(tx_buffer_region),
        move(rx_descriptors),
        move(tx_descriptors))));
}

UNMAP_AFTER_INIT ErrorOr<void> E1000NetworkAdapter::initialize(Badge<NetworkingManagement>)
{
    dmesgln_pci(*this, "Found @ {}", device_identifier().address());

    enable_bus_mastering(device_identifier());

    dmesgln_pci(*this, "IO base: {}", m_registers.window());
    dmesgln_pci(*this, "Interrupt line: {}", interrupt_number());
    detect_eeprom();
    dmesgln_pci(*this, "Has EEPROM? {}", m_has_eeprom.was_set());
    read_mac_address();
    auto const& mac = mac_address();
    dmesgln_pci(*this, "MAC address: {}", mac.to_string());

    initialize_rx_descriptors();
    initialize_tx_descriptors();

    setup_link();
    setup_interrupts();

    m_link_up = m_registers.read<Register::Status>().link_up != 0;
    autoconfigure_link_local_ipv6();

    return {};
}

UNMAP_AFTER_INIT void E1000NetworkAdapter::setup_link()
{
    auto ctrl = m_registers.read<Register::Ctrl>();
    ctrl.set_link_up = 1;
    m_registers.write<Register::Ctrl>(ctrl);
}

UNMAP_AFTER_INIT void E1000NetworkAdapter::setup_interrupts()
{
    // FIXME: Do this properly,
    //        like set the interrupt rate depending on the current utilization and link speed
    m_registers.write<Register::InterruptThrottling>(6000); // Interrupt rate of 1.536 milliseconds

    // We want: Link status change, RX timer, RX overrun
    using InterruptMask = E1000::Interrupt;
    m_registers.write<Register::InterruptMask>(
        InterruptMask::LSC | InterruptMask::RXT0 | InterruptMask::RXO);

    (void)m_registers.read<Register::InterruptCauseR>();

    enable_irq();
}

UNMAP_AFTER_INIT E1000NetworkAdapter::E1000NetworkAdapter(StringView interface_name,
    PCI::DeviceIdentifier const& device_identifier, u8 irq,
    NonnullOwnPtr<IOWindow> registers_io_window, NonnullOwnPtr<Memory::Region> rx_buffer_region,
    NonnullOwnPtr<Memory::Region> tx_buffer_region, Memory::TypedMapping<E1000::RxDescriptor volatile[]> rx_descriptors,
    Memory::TypedMapping<E1000::TxDescriptor volatile[]> tx_descriptors)
    : NetworkAdapter(interface_name)
    , PCI::Device(device_identifier)
    , IRQHandler(irq)
    , m_registers(move(registers_io_window))
    , m_rx_descriptors(move(rx_descriptors))
    , m_tx_descriptors(move(tx_descriptors))
    , m_rx_buffer_region(move(rx_buffer_region))
    , m_tx_buffer_region(move(tx_buffer_region))
{
}

UNMAP_AFTER_INIT E1000NetworkAdapter::~E1000NetworkAdapter() = default;

bool E1000NetworkAdapter::handle_irq()
{
    auto irq_cause = m_registers.read<Register::InterruptCauseR>();

    m_entropy_source.add_random_event(irq_cause);

    using enum E1000::Interrupt;
    using E1000::has_flag;

    if (irq_cause == None)
        return false;

    // Let's be honest and only handle the interrupts we care about
    if (!E1000::has_any_flag(irq_cause,
            LSC | RXO | RXT0))
        return false;

    if (has_flag(irq_cause, LSC)) {
        auto ctrl = m_registers.read<Register::Ctrl>();
        ctrl.set_link_up = 1;
        m_registers.write<Register::Ctrl>(ctrl);

        m_link_up = m_registers.read<Register::Status>().link_up != 0;

        autoconfigure_link_local_ipv6();
    }
    if (has_flag(irq_cause, RXO)) {
        dbgln_if(E1000_DEBUG, "E1000: RX buffer overrun");
    }
    if (has_flag(irq_cause, RXT0)) {
        // Note: "RXDW" on newer NICs, but it sounds like it has the same meaning
        receive();
    }

    m_wait_queue.wake_all();

    m_registers.write<Register::InterruptCauseR>(InterruptClear);
    return true;
}

UNMAP_AFTER_INIT void E1000NetworkAdapter::detect_eeprom()
{
    // Note: In this revision this actually seems to also be used to detect 14 bit addressing

    // The 82544GC/EI models do not have an EEPROM present bit
    // So we cannot use that to determine if the EEPROM is present
    // But have to try to read from it, to see if it's there
    E1000::EEPROMRead eerd = {};
    eerd.address_8.start = 1;
    m_registers.write<Register::EEPROMRead>(eerd);
    for (int i = 0; i < 999; ++i) {
        auto data = m_registers.read<Register::EEPROMRead>();
        if (data.address_8.done) {
            m_has_eeprom.set();
            return;
        }
        Processor::wait_check();
    }
}

UNMAP_AFTER_INIT u16 E1000NetworkAdapter::read_eeprom(u16 address)
{
    if (m_has_eeprom.was_set()) {
        // 8 bit addressing
        VERIFY(address < 0xFF);
        E1000::EEPROMRead eerd {};
        eerd.address_8.start = 1;
        eerd.address_8.address = address;
        m_registers.write<Register::EEPROMRead>(eerd);
        while (eerd = m_registers.read<Register::EEPROMRead>(), !eerd.address_8.done)
            Processor::wait_check();
        return eerd.address_8.data;
    }

    // Lets try 14 bit addressing???
    E1000::EEPROMRead eerd = {};
    eerd.address_14.start = 1;
    eerd.address_14.address = address;
    m_registers.write<Register::EEPROMRead>(eerd);
    while (eerd = m_registers.read<Register::EEPROMRead>(), !eerd.address_14.done)
        Processor::wait_check();
    return eerd.address_14.data;
}

UNMAP_AFTER_INIT void E1000NetworkAdapter::read_mac_address()
{
    if (m_has_eeprom.was_set()) {
        MACAddress mac {};
        u32 tmp = read_eeprom(0);
        mac[0] = tmp & 0xff;
        mac[1] = tmp >> 8;
        tmp = read_eeprom(1);
        mac[2] = tmp & 0xff;
        mac[3] = tmp >> 8;
        tmp = read_eeprom(2);
        mac[4] = tmp & 0xff;
        mac[5] = tmp >> 8;
        set_mac_address(mac);
    } else {
        VERIFY_NOT_REACHED();
    }
}

UNMAP_AFTER_INIT void E1000NetworkAdapter::initialize_rx_descriptors()
{
    constexpr auto rx_buffer_page_count = rx_buffer_size / PAGE_SIZE;
    for (size_t i = 0; i < number_of_rx_descriptors; ++i) {
        auto& descriptor = m_rx_descriptors[i];
        m_rx_buffers[i] = m_rx_buffer_region->vaddr().as_ptr() + rx_buffer_size * i;
        descriptor.legacy.length = 0;
        descriptor.legacy.addr = m_rx_buffer_region->physical_page(rx_buffer_page_count * i)->paddr().get();
        descriptor.legacy.status = E1000::RxDescriptorStatus::None;
    }

    m_registers.write<Register::RXDescLow>(m_rx_descriptors.paddr.get() & 0xffffffff);
    m_registers.write<Register::RXDescHigh>(m_rx_descriptors.paddr.get() >> 32);
    m_registers.write<Register::RXDescLength>(number_of_rx_descriptors * sizeof(E1000::RxDescriptor));
    m_registers.write<Register::RXDescHead>(0);
    m_registers.write<Register::RXDescTail>(number_of_rx_descriptors - 1);

    E1000::ReceiveControl rctl = m_registers.read<Register::RCtrl>();
    rctl.enable = 0;
    m_registers.write<Register::RCtrl>(rctl);

    rctl.enable = 1;
    rctl.store_bad_frames = 1;
    rctl.unicast_promiscuous_enable = 1;
    rctl.multicast_promiscuous_enable = 1;
    rctl.loopback_mode = E1000::LoopbackMode::None;
    rctl.broadcast_accept_mode = 1;
    rctl.strip_ethernet_crc = 1;
    rctl.buffer_size = E1000::ReceiveControl::BufferSize::Size8192;
    rctl.buffer_size_extension = 1;
    rctl.read_descriptor_minimum_threshold_size = E1000::ReceiveControl::FreeBufferThreshold::Half;

    m_registers.write<Register::RCtrl>(rctl);
}

UNMAP_AFTER_INIT void E1000NetworkAdapter::initialize_tx_descriptors()
{
    constexpr auto tx_buffer_page_count = tx_buffer_size / PAGE_SIZE;

    for (size_t i = 0; i < number_of_tx_descriptors; ++i) {
        auto& descriptor = m_tx_descriptors[i];
        m_tx_buffers[i] = m_tx_buffer_region->vaddr().as_ptr() + tx_buffer_size * i;
        descriptor.addr = m_tx_buffer_region->physical_page(tx_buffer_page_count * i)->paddr().get();
        descriptor.cmd = {};
    }

    m_registers.write<Register::TXDescLow>(m_tx_descriptors.paddr.get() & 0xffffffff);
    m_registers.write<Register::TXDescHigh>(m_tx_descriptors.paddr.get() >> 32);
    m_registers.write<Register::TXDescLength>(number_of_tx_descriptors * sizeof(E1000::TxDescriptor));
    m_registers.write<Register::TXDescHead>(0);
    m_registers.write<Register::TXDescTail>(0);

    E1000::TransmitControl tctl = m_registers.read<Register::TCtrl>();
    tctl.enable = 0;
    m_registers.write<Register::TCtrl>(tctl);

    tctl.enable = 1;
    tctl.pad_short_packets = 1;
    m_registers.write<Register::TCtrl>(tctl);

    set_tipg();
}

void E1000NetworkAdapter::set_tipg()
{
    auto tipg = E1000::TransmitInterPacketGap {};
    // 8254x 14.5 Transmit Initialization
    tipg.ipgt = 8;
    tipg.ipgr1 = 8;
    tipg.ipgr = 6;
    m_registers.write<Register::TIPG>(tipg);
}

void E1000NetworkAdapter::send_raw(ReadonlyBytes payload)
{
    disable_irq();

    size_t tx_current = m_registers.read<Register::TXDescTail>() % number_of_tx_descriptors;
    [[maybe_unused]] auto tx_head = m_registers.read<Register::TXDescHead>();

    dbgln_if(E1000_DEBUG, "E1000: Sending packet ({} bytes)", payload.size());
    dbgln_if(E1000_DEBUG, "E1000: Using tx descriptor {} (head is at {})", tx_current, tx_head);
    auto& descriptor = m_tx_descriptors[tx_current];

    void* buffer = m_tx_buffers[tx_current];
    memcpy(buffer, payload.data(), payload.size());

    descriptor.length = payload.size();
    descriptor.status = 0;
    using enum E1000::TXCommand;
    descriptor.cmd = EOP | IFCS | RS; // Single packet, insert FCS, report status
    tx_current = (tx_current + 1) % number_of_tx_descriptors;

    // FIXME: This seems odd to me:
    //        We disable interrupts and then wait for and irq to happen...
    Processor::disable_interrupts();
    enable_irq();

    m_registers.write<Register::TXDescTail>(tx_current);
    for (;;) {
        if (descriptor.status) {
            Processor::enable_interrupts();
            break;
        }
        m_wait_queue.wait_forever("E1000NetworkAdapter"sv);
    }
    // FIXME: This should probably do some error checking
    dbgln_if(E1000_DEBUG, "E1000: Sent packet, status is now {:#02x}!", (u8)descriptor.status);
}

void E1000NetworkAdapter::receive()
{
    u32 rx_current;
    for (;;) {
        rx_current = m_registers.read<Register::RXDescTail>() % number_of_rx_descriptors;
        rx_current = (rx_current + 1) % number_of_rx_descriptors;

        // FIXME: We may receive packets split across multiple descriptors
        if (!has_flag(m_rx_descriptors[rx_current].legacy.status, E1000::RxDescriptorStatus::DD))
            break;
        // FIXME: Support split packets
        VERIFY(has_flag(m_rx_descriptors[rx_current].legacy.status, E1000::RxDescriptorStatus::EOP));

        auto* buffer = m_rx_buffers[rx_current];
        u16 length = m_rx_descriptors[rx_current].legacy.length;

        VERIFY(length <= 8192);

        dbgln_if(E1000_DEBUG, "E1000: Received 1 packet @ {:p} ({} bytes)", buffer, length);
        did_receive({ buffer, length });

        m_rx_descriptors[rx_current].legacy.status = E1000::RxDescriptorStatus::None;
        m_registers.write<Register::RXDescTail>(rx_current);
    }
}

i32 E1000NetworkAdapter::link_speed()
{
    if (!link_up())
        return NetworkAdapter::LINKSPEED_INVALID;

    auto status = m_registers.read<Register::Status>();
    using enum E1000::LinkSpeed;
    switch (status.speed) {
    case Speed10M:
        return 10;
    case Speed100M:
        return 100;
    case Speed1000M_1:
    case Speed1000M_2:
        return 1000;
    default:
        return NetworkAdapter::LINKSPEED_INVALID;
    }
}

bool E1000NetworkAdapter::link_full_duplex()
{
    auto status = m_registers.read<Register::Status>();
    return status.full_duplex != 0;
}

}
