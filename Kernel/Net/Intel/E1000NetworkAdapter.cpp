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
    auto rx_descriptors = TRY(Memory::allocate_dma_region_as_typed_array<RxDescriptor volatile>(number_of_rx_descriptors, "E1000 RX Descriptors"sv, Memory::Region::Access::ReadWrite));
    auto tx_descriptors = TRY(Memory::allocate_dma_region_as_typed_array<TxDescriptor volatile>(number_of_tx_descriptors, "E1000 TX Descriptors"sv, Memory::Region::Access::ReadWrite));

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
    check_quirks();
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

UNMAP_AFTER_INIT void E1000NetworkAdapter::check_quirks()
{

    u16 device_id = device_identifier().hardware_id().device_id;
    // The 82541xx and 82547GI/EI have a different EEPROM access method
    switch (device_id) {
    case 0x1019: // 82547EI-A0, 82547EI-A1, 82547EI-B0, 82547GI-B0
    case 0x101A: // 82547EI-B0
    case 0x1013: // 82541EI-A0, 82541EI-B0
    case 0x1018: // 82541EI-B0
    case 0x1076: // 82541GI-B1, 82541PI-C0
    case 0x1077: // 82541GI-B1
    case 0x1078: // 82541ER-C0
        m_is_82541xx_82547GI_EI.set();
        break;
    default:
        break;
    }
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
    //        There's probably a way to find an ideal interrupt rate etc
    m_registers.write<Register::InterruptThrottling>(6000); // Interrupt rate of 1.536 milliseconds
    // We want: Link status change, RX timer, RX overrun
    using InterruptMask = E1000::InterruptMask;
    m_registers.write<Register::InterruptMask>(InterruptMask::LSC | InterruptMask::RXT0 | InterruptMask::RXO);
    (void)m_registers.read<Register::InterruptCauseR>();
    enable_irq();
}

UNMAP_AFTER_INIT E1000NetworkAdapter::E1000NetworkAdapter(StringView interface_name,
    PCI::DeviceIdentifier const& device_identifier, u8 irq,
    NonnullOwnPtr<IOWindow> registers_io_window, NonnullOwnPtr<Memory::Region> rx_buffer_region,
    NonnullOwnPtr<Memory::Region> tx_buffer_region, Memory::TypedMapping<RxDescriptor volatile[]> rx_descriptors,
    Memory::TypedMapping<TxDescriptor volatile[]> tx_descriptors)
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

    if (irq_cause.raw == 0)
        return false;

    if (irq_cause.link_status_change) {
        auto ctrl = m_registers.read<Register::Ctrl>();
        ctrl.set_link_up = 1;
        m_registers.write<Register::Ctrl>(ctrl);

        m_link_up = m_registers.read<Register::Status>().link_up != 0;

        autoconfigure_link_local_ipv6();
    }
    if (irq_cause.receive_descriptor_minimum_threshold_reached) {
        // Threshold OK?
    }
    if (irq_cause.receiver_overrun) {
        dbgln_if(E1000_DEBUG, "E1000: RX buffer overrun");
    }
    if (irq_cause.receive_timer_interrupt) {
        receive();
    }

    m_wait_queue.wake_all();

    m_registers.write<Register::InterruptCauseR>({ .raw = 0xffffffff });
    return true;
}

UNMAP_AFTER_INIT void E1000NetworkAdapter::detect_eeprom()
{
    // FIXME: On most devices we can check the EECD.EE_PRES register (this is what the E1000E driver does)
    //        But this is not supported on the 82544GC/EI

    m_registers.write<Register::EEPROMRead>({ .start = 1 });
    for (int i = 0; i < 999; ++i) {
        auto data = m_registers.read<Register::EEPROMRead>();
        if (m_is_82541xx_82547GI_EI.was_set()) {
            if (data.variant_82541xx_or_82547GI_EI.done) {
                m_has_eeprom.set();
                return;
            }
        } else {
            if (data.done) {
                m_has_eeprom.set();
                return;
            }
        }
        Processor::wait_check();
    }

    dmesgln_pci(*this, "E1000: EEPROM failed to initialize");
}

UNMAP_AFTER_INIT u16 E1000NetworkAdapter::read_eeprom(u16 address)
{
    // FIXME: Should this just return 0 then?
    VERIFY(m_has_eeprom.was_set());

    // FIXME: Maybe add a timeout to the loops?

    if (m_is_82541xx_82547GI_EI.was_set()) {
        E1000::EEPROMRead eerd = { .variant_82541xx_or_82547GI_EI { .start = 1, .address = address } };
        m_registers.write<Register::EEPROMRead>(eerd);
        while (eerd = m_registers.read<Register::EEPROMRead>(),
            !eerd.variant_82541xx_or_82547GI_EI.done)
            Processor::wait_check();
        return eerd.variant_82541xx_or_82547GI_EI.data;
    }

    // The normal EEPROM only has an 8 bit address field
    VERIFY(address < 0xFF);
    E1000::EEPROMRead eerd = { .start = 1, .address = address };
    m_registers.write<Register::EEPROMRead>(eerd);
    while (eerd = m_registers.read<Register::EEPROMRead>(), !eerd.done)
        Processor::wait_check();
    return eerd.data;
}

UNMAP_AFTER_INIT void E1000NetworkAdapter::read_mac_address()
{
    VERIFY(m_has_eeprom.was_set());

    // 5.6.1 Ethernet Address (00h-02h)
    MACAddress mac {};

    u16 tmp = read_eeprom(0);
    mac[0] = tmp & 0xff;
    mac[1] = tmp >> 8;

    tmp = read_eeprom(1);
    mac[2] = tmp & 0xff;
    mac[3] = tmp >> 8;

    tmp = read_eeprom(2);
    mac[4] = tmp & 0xff;
    mac[5] = tmp >> 8;

    set_mac_address(mac);
}

UNMAP_AFTER_INIT void E1000NetworkAdapter::initialize_rx_descriptors()
{
    constexpr auto rx_buffer_page_count = rx_buffer_size / PAGE_SIZE;
    for (size_t i = 0; i < number_of_rx_descriptors; ++i) {
        auto& descriptor = m_rx_descriptors[i];
        m_rx_buffers[i] = m_rx_buffer_region->vaddr().as_ptr() + rx_buffer_size * i;
        descriptor.addr = m_rx_buffer_region->physical_page(rx_buffer_page_count * i)->paddr().get();
        descriptor.status = 0;
    }

    m_registers.write<Register::RXDescLow>(m_rx_descriptors.paddr.get() & 0xffffffff);
    m_registers.write<Register::RXDescHigh>(m_rx_descriptors.paddr.get() >> 32);
    m_registers.write<Register::RXDescLength>(number_of_rx_descriptors * sizeof(RxDescriptor));
    m_registers.write<Register::RXDescHead>(0);
    m_registers.write<Register::RXDescTail>(number_of_rx_descriptors - 1);

    E1000::ReceiveControl rctl;
    rctl.enable = 1,
    rctl.store_bad_frames = 1,
    rctl.unicast_promiscuous_enable = 1,
    rctl.multicast_promiscuous_enable = 1,
    rctl.loopback_mode = E1000::LoopbackMode::None,
    rctl.read_descriptor_minimum_threshold_size = E1000::ReceiveControl::FreeBufferThreshold::Half,
    rctl.broadcast_accept_mode = 1,
    rctl.buffer_size_extension = 1,
    rctl.buffer_size = E1000::ReceiveControl::BufferSize::Size8192,
    rctl.strip_ethernet_crc = 1,
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
    m_registers.write<Register::TXDescLength>(number_of_tx_descriptors * sizeof(TxDescriptor));
    m_registers.write<Register::TXDescHead>(0);
    m_registers.write<Register::TXDescTail>(0);

    E1000::TransmitControl tctl = m_registers.read<Register::TCtrl>();
    tctl.enable = 1;
    tctl.pad_short_packets = 1;
    m_registers.write<Register::TCtrl>(tctl);

    // FIXME: Do this properly
    // IPGT:= 10 (8/6 for the 82544GC/EI) - IPG Transmit Time            (10 bit)
    //        The 82544GC/EI has a different value for IPGT depending if it is in IEEE 802.3 mode or 10/100/1000BASE-T mode
    // IPGR1:= 8 - IPG Receive Time 1 (half duplex only) ~= 2/3 of IPGR2 (10 bit)
    // IPGR2:= 6 (+6) - IPG Receive Time 2 (half duplex only)            (10 bit)
    // Recommendation are for "IEEE 802.3 minimum IPG value of 96-bit time"
    // The magic value here has:
    // 10, 8, 6 so it should be fine for now
    m_registers.write<Register::TIPG>(0x0060200A);
}

void E1000NetworkAdapter::send_raw(ReadonlyBytes payload)
{
    VERIFY(payload.size() <= 8192);

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

        if (!(m_rx_descriptors[rx_current].status & 1))
            break;

        auto* buffer = m_rx_buffers[rx_current];
        u16 length = m_rx_descriptors[rx_current].length;

        VERIFY(length <= 8192);

        dbgln_if(E1000_DEBUG, "E1000: Received 1 packet @ {:p} ({} bytes)", buffer, length);
        did_receive({ buffer, length });

        m_rx_descriptors[rx_current].status = 0;
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
