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

#define REG_CTRL 0x0000
#define REG_STATUS 0x0008
#define REG_EEPROM 0x0014
#define REG_CTRL_EXT 0x0018
#define REG_INTERRUPT_CAUSE_READ 0x00C0
#define REG_INTERRUPT_RATE 0x00C4
#define REG_INTERRUPT_MASK_SET 0x00D0
#define REG_INTERRUPT_MASK_CLEAR 0x00D8
#define REG_RCTRL 0x0100
#define REG_RXDESCLO 0x2800
#define REG_RXDESCHI 0x2804
#define REG_RXDESCLEN 0x2808
#define REG_RXDESCHEAD 0x2810
#define REG_RXDESCTAIL 0x2818
#define REG_TCTRL 0x0400
#define REG_TXDESCLO 0x3800
#define REG_TXDESCHI 0x3804
#define REG_TXDESCLEN 0x3808
#define REG_TXDESCHEAD 0x3810
#define REG_TXDESCTAIL 0x3818
#define REG_RDTR 0x2820             // RX Delay Timer Register
#define REG_RXDCTL 0x3828           // RX Descriptor Control
#define REG_RADV 0x282C             // RX Int. Absolute Delay Timer
#define REG_RSRPD 0x2C00            // RX Small Packet Detect Interrupt
#define REG_TIPG 0x0410             // Transmit Inter Packet Gap
#define ECTRL_SLU 0x40              // set link up
#define RCTL_EN (1 << 1)            // Receiver Enable
#define RCTL_SBP (1 << 2)           // Store Bad Packets
#define RCTL_UPE (1 << 3)           // Unicast Promiscuous Enabled
#define RCTL_MPE (1 << 4)           // Multicast Promiscuous Enabled
#define RCTL_LPE (1 << 5)           // Long Packet Reception Enable
#define RCTL_LBM_NONE (0 << 6)      // No Loopback
#define RCTL_LBM_PHY (3 << 6)       // PHY or external SerDesc loopback
#define RTCL_RDMTS_HALF (0 << 8)    // Free Buffer Threshold is 1/2 of RDLEN
#define RTCL_RDMTS_QUARTER (1 << 8) // Free Buffer Threshold is 1/4 of RDLEN
#define RTCL_RDMTS_EIGHTH (2 << 8)  // Free Buffer Threshold is 1/8 of RDLEN
#define RCTL_MO_36 (0 << 12)        // Multicast Offset - bits 47:36
#define RCTL_MO_35 (1 << 12)        // Multicast Offset - bits 46:35
#define RCTL_MO_34 (2 << 12)        // Multicast Offset - bits 45:34
#define RCTL_MO_32 (3 << 12)        // Multicast Offset - bits 43:32
#define RCTL_BAM (1 << 15)          // Broadcast Accept Mode
#define RCTL_VFE (1 << 18)          // VLAN Filter Enable
#define RCTL_CFIEN (1 << 19)        // Canonical Form Indicator Enable
#define RCTL_CFI (1 << 20)          // Canonical Form Indicator Bit Value
#define RCTL_DPF (1 << 22)          // Discard Pause Frames
#define RCTL_PMCF (1 << 23)         // Pass MAC Control Frames
#define RCTL_SECRC (1 << 26)        // Strip Ethernet CRC

// Buffer Sizes
#define RCTL_BSIZE_256 (3 << 16)
#define RCTL_BSIZE_512 (2 << 16)
#define RCTL_BSIZE_1024 (1 << 16)
#define RCTL_BSIZE_2048 (0 << 16)
#define RCTL_BSIZE_4096 ((3 << 16) | (1 << 25))
#define RCTL_BSIZE_8192 ((2 << 16) | (1 << 25))
#define RCTL_BSIZE_16384 ((1 << 16) | (1 << 25))

// Transmit Command

#define CMD_EOP (1 << 0)  // End of Packet
#define CMD_IFCS (1 << 1) // Insert FCS
#define CMD_IC (1 << 2)   // Insert Checksum
#define CMD_RS (1 << 3)   // Report Status
#define CMD_RPS (1 << 4)  // Report Packet Sent
#define CMD_VLE (1 << 6)  // VLAN Packet Enable
#define CMD_IDE (1 << 7)  // Interrupt Delay Enable

// TCTL Register

#define TCTL_EN (1 << 1)      // Transmit Enable
#define TCTL_PSP (1 << 3)     // Pad Short Packets
#define TCTL_CT_SHIFT 4       // Collision Threshold
#define TCTL_COLD_SHIFT 12    // Collision Distance
#define TCTL_SWXOFF (1 << 22) // Software XOFF Transmission
#define TCTL_RTLC (1 << 24)   // Re-transmit on Late Collision

#define TSTA_DD (1 << 0) // Descriptor Done
#define TSTA_EC (1 << 1) // Excess Collisions
#define TSTA_LC (1 << 2) // Late Collision
#define LSTA_TU (1 << 3) // Transmit Underrun

// STATUS Register

#define STATUS_FD 0x01
#define STATUS_LU 0x02
#define STATUS_TXOFF 0x08
#define STATUS_SPEED 0xC0
#define STATUS_SPEED_10MB 0x00
#define STATUS_SPEED_100MB 0x40
#define STATUS_SPEED_1000MB1 0x80
#define STATUS_SPEED_1000MB2 0xC0

// Interrupt Masks

#define INTERRUPT_TXDW (1 << 0)
#define INTERRUPT_TXQE (1 << 1)
#define INTERRUPT_LSC (1 << 2)
#define INTERRUPT_RXSEQ (1 << 3)
#define INTERRUPT_RXDMT0 (1 << 4)
#define INTERRUPT_RXO (1 << 6)
#define INTERRUPT_RXT0 (1 << 7)
#define INTERRUPT_MDAC (1 << 9)
#define INTERRUPT_RXCFG (1 << 10)
#define INTERRUPT_PHYINT (1 << 12)
#define INTERRUPT_TXD_LOW (1 << 15)
#define INTERRUPT_SRPD (1 << 16)

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
    auto rx_descriptors = TRY(Memory::allocate_dma_region_as_typed_array<RxDescriptor volatile>(number_of_rx_descriptors, "E1000 RX Descriptors"sv, Memory::Region::Access::ReadWrite, Memory::MemoryType::IO));
    auto tx_descriptors = TRY(Memory::allocate_dma_region_as_typed_array<TxDescriptor volatile>(number_of_tx_descriptors, "E1000 TX Descriptors"sv, Memory::Region::Access::ReadWrite, Memory::MemoryType::IO));

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

    dmesgln_pci(*this, "IO base: {}", m_registers_io_window);
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

    m_link_up = ((in32(REG_STATUS) & STATUS_LU) != 0);
    autoconfigure_link_local_ipv6();

    return {};
}

UNMAP_AFTER_INIT void E1000NetworkAdapter::setup_link()
{
    u32 flags = in32(REG_CTRL);
    out32(REG_CTRL, flags | ECTRL_SLU);
}

UNMAP_AFTER_INIT void E1000NetworkAdapter::setup_interrupts()
{
    out32(REG_INTERRUPT_RATE, 6000); // Interrupt rate of 1.536 milliseconds
    out32(REG_INTERRUPT_MASK_SET, INTERRUPT_LSC | INTERRUPT_RXT0 | INTERRUPT_RXO);
    in32(REG_INTERRUPT_CAUSE_READ);
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
    , m_registers_io_window(move(registers_io_window))
    , m_rx_descriptors(move(rx_descriptors))
    , m_tx_descriptors(move(tx_descriptors))
    , m_rx_buffer_region(move(rx_buffer_region))
    , m_tx_buffer_region(move(tx_buffer_region))
{
}

UNMAP_AFTER_INIT E1000NetworkAdapter::~E1000NetworkAdapter() = default;

bool E1000NetworkAdapter::handle_irq()
{
    u32 status = in32(REG_INTERRUPT_CAUSE_READ);

    m_entropy_source.add_random_event(status);

    if (status == 0)
        return false;

    if (status & INTERRUPT_LSC) {
        u32 flags = in32(REG_CTRL);
        out32(REG_CTRL, flags | ECTRL_SLU);

        m_link_up = ((in32(REG_STATUS) & STATUS_LU) != 0);
        autoconfigure_link_local_ipv6();
    }
    if (status & INTERRUPT_RXDMT0) {
        // Threshold OK?
    }
    if (status & INTERRUPT_RXO) {
        dbgln_if(E1000_DEBUG, "E1000: RX buffer overrun");
    }
    if (status & INTERRUPT_RXT0) {
        receive();
    }

    m_wait_queue.wake_all();

    out32(REG_INTERRUPT_CAUSE_READ, 0xffffffff);
    return true;
}

UNMAP_AFTER_INIT void E1000NetworkAdapter::detect_eeprom()
{
    out32(REG_EEPROM, 0x1);
    for (int i = 0; i < 999; ++i) {
        u32 data = in32(REG_EEPROM);
        if (data & 0x10) {
            m_has_eeprom.set();
            return;
        }
    }
}

UNMAP_AFTER_INIT u32 E1000NetworkAdapter::read_eeprom(u8 address)
{
    u16 data = 0;
    u32 tmp = 0;
    if (m_has_eeprom.was_set()) {
        out32(REG_EEPROM, ((u32)address << 8) | 1);
        while (!((tmp = in32(REG_EEPROM)) & (1 << 4)))
            Processor::wait_check();
    } else {
        out32(REG_EEPROM, ((u32)address << 2) | 1);
        while (!((tmp = in32(REG_EEPROM)) & (1 << 1)))
            Processor::wait_check();
    }
    data = (tmp >> 16) & 0xffff;
    return data;
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
        descriptor.addr = m_rx_buffer_region->physical_page(rx_buffer_page_count * i)->paddr().get();
        descriptor.status = 0;
    }

    out32(REG_RXDESCLO, m_rx_descriptors.paddr.get());
    out32(REG_RXDESCHI, 0);
    out32(REG_RXDESCLEN, number_of_rx_descriptors * sizeof(RxDescriptor));
    out32(REG_RXDESCHEAD, 0);
    out32(REG_RXDESCTAIL, number_of_rx_descriptors - 1);

    out32(REG_RCTRL, RCTL_EN | RCTL_SBP | RCTL_UPE | RCTL_MPE | RCTL_LBM_NONE | RTCL_RDMTS_HALF | RCTL_BAM | RCTL_SECRC | RCTL_BSIZE_8192);
}

UNMAP_AFTER_INIT void E1000NetworkAdapter::initialize_tx_descriptors()
{
    constexpr auto tx_buffer_page_count = tx_buffer_size / PAGE_SIZE;

    for (size_t i = 0; i < number_of_tx_descriptors; ++i) {
        auto& descriptor = m_tx_descriptors[i];
        m_tx_buffers[i] = m_tx_buffer_region->vaddr().as_ptr() + tx_buffer_size * i;
        descriptor.addr = m_tx_buffer_region->physical_page(tx_buffer_page_count * i)->paddr().get();
        descriptor.cmd = 0;
    }

    out32(REG_TXDESCLO, m_tx_descriptors.paddr.get());
    out32(REG_TXDESCHI, 0);
    out32(REG_TXDESCLEN, number_of_tx_descriptors * sizeof(TxDescriptor));
    out32(REG_TXDESCHEAD, 0);
    out32(REG_TXDESCTAIL, 0);

    out32(REG_TCTRL, in32(REG_TCTRL) | TCTL_EN | TCTL_PSP);
    out32(REG_TIPG, 0x0060200A);
}

void E1000NetworkAdapter::out8(u16 address, u8 data)
{
    dbgln_if(E1000_DEBUG, "E1000: OUT8 {:#02x} @ {:#04x}", data, address);
    m_registers_io_window->write8(address, data);
}

void E1000NetworkAdapter::out16(u16 address, u16 data)
{
    dbgln_if(E1000_DEBUG, "E1000: OUT16 {:#04x} @ {:#04x}", data, address);
    m_registers_io_window->write16(address, data);
}

void E1000NetworkAdapter::out32(u16 address, u32 data)
{
    dbgln_if(E1000_DEBUG, "E1000: OUT32 {:#08x} @ {:#04x}", data, address);
    m_registers_io_window->write32(address, data);
}

u8 E1000NetworkAdapter::in8(u16 address)
{
    dbgln_if(E1000_DEBUG, "E1000: IN8 @ {:#04x}", address);
    return m_registers_io_window->read8(address);
}

u16 E1000NetworkAdapter::in16(u16 address)
{
    dbgln_if(E1000_DEBUG, "E1000: IN16 @ {:#04x}", address);
    return m_registers_io_window->read16(address);
}

u32 E1000NetworkAdapter::in32(u16 address)
{
    dbgln_if(E1000_DEBUG, "E1000: IN32 @ {:#04x}", address);
    return m_registers_io_window->read32(address);
}

void E1000NetworkAdapter::send_raw(ReadonlyBytes payload)
{
    disable_irq();
    size_t tx_current = in32(REG_TXDESCTAIL) % number_of_tx_descriptors;
    dbgln_if(E1000_DEBUG, "E1000: Sending packet ({} bytes)", payload.size());
    auto& descriptor = m_tx_descriptors[tx_current];
    VERIFY(payload.size() <= 8192);
    auto* vptr = (void*)m_tx_buffers[tx_current];
    memcpy(vptr, payload.data(), payload.size());
    descriptor.length = payload.size();
    descriptor.status = 0;
    descriptor.cmd = CMD_EOP | CMD_IFCS | CMD_RS;
    dbgln_if(E1000_DEBUG, "E1000: Using tx descriptor {} (head is at {})", tx_current, in32(REG_TXDESCHEAD));
    tx_current = (tx_current + 1) % number_of_tx_descriptors;
    Processor::disable_interrupts();
    enable_irq();
    out32(REG_TXDESCTAIL, tx_current);
    for (;;) {
        if (descriptor.status) {
            Processor::enable_interrupts();
            break;
        }
        m_wait_queue.wait_forever("E1000NetworkAdapter"sv);
    }
    dbgln_if(E1000_DEBUG, "E1000: Sent packet, status is now {:#02x}!", (u8)descriptor.status);
}

void E1000NetworkAdapter::receive()
{
    u32 rx_current;
    for (;;) {
        rx_current = in32(REG_RXDESCTAIL) % number_of_rx_descriptors;
        rx_current = (rx_current + 1) % number_of_rx_descriptors;
        if (!(m_rx_descriptors[rx_current].status & 1))
            break;
        auto* buffer = m_rx_buffers[rx_current];
        u16 length = m_rx_descriptors[rx_current].length;
        VERIFY(length <= 8192);
        dbgln_if(E1000_DEBUG, "E1000: Received 1 packet @ {:p} ({} bytes)", buffer, length);
        did_receive({ buffer, length });
        m_rx_descriptors[rx_current].status = 0;
        out32(REG_RXDESCTAIL, rx_current);
    }
}

i32 E1000NetworkAdapter::link_speed()
{
    if (!link_up())
        return NetworkAdapter::LINKSPEED_INVALID;

    u32 speed = in32(REG_STATUS) & STATUS_SPEED;
    switch (speed) {
    case STATUS_SPEED_10MB:
        return 10;
    case STATUS_SPEED_100MB:
        return 100;
    case STATUS_SPEED_1000MB1:
    case STATUS_SPEED_1000MB2:
        return 1000;
    default:
        return NetworkAdapter::LINKSPEED_INVALID;
    }
}

bool E1000NetworkAdapter::link_full_duplex()
{
    u32 status = in32(REG_STATUS);
    return !!(status & STATUS_FD);
}

}
