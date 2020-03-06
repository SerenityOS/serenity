/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <Kernel/Net/E1000NetworkAdapter.h>
#include <Kernel/Thread.h>
#include <LibBareMetal/IO.h>

//#define E1000_DEBUG

namespace Kernel {

#define REG_CTRL 0x0000
#define REG_STATUS 0x0008
#define REG_EEPROM 0x0014
#define REG_CTRL_EXT 0x0018
#define REG_IMASK 0x00D0
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
#define ECTRL_SLU 0x40              //set link up
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

void E1000NetworkAdapter::detect(const PCI::Address& address)
{
    if (address.is_null())
        return;
    static const PCI::ID qemu_bochs_vbox_id = { 0x8086, 0x100e };
    const PCI::ID id = PCI::get_id(address);
    if (id != qemu_bochs_vbox_id)
        return;
    u8 irq = PCI::get_interrupt_line(address);
    (void)adopt(*new E1000NetworkAdapter(address, irq)).leak_ref();
}

E1000NetworkAdapter::E1000NetworkAdapter(PCI::Address address, u8 irq)
    : PCI::Device(address, irq)
    , m_io_base(PCI::get_BAR1(pci_address()) & ~1)
    , m_rx_descriptors_region(MM.allocate_contiguous_kernel_region(PAGE_ROUND_UP(sizeof(e1000_rx_desc) * number_of_rx_descriptors + 16), "E1000 RX", Region::Access::Read | Region::Access::Write))
    , m_tx_descriptors_region(MM.allocate_contiguous_kernel_region(PAGE_ROUND_UP(sizeof(e1000_tx_desc) * number_of_tx_descriptors + 16), "E1000 TX", Region::Access::Read | Region::Access::Write))
{
    set_interface_name("e1k");

    klog() << "E1000: Found at PCI address @ " << String::format("%w", pci_address().seg()) << ":" << String::format("%b", pci_address().bus()) << ":" << String::format("%b", pci_address().slot()) << "." << String::format("%b", pci_address().function());

    enable_bus_mastering(pci_address());

    size_t mmio_base_size = PCI::get_BAR_Space_Size(pci_address(), 0);
    m_mmio_region = MM.allocate_kernel_region(PhysicalAddress(page_base_of(PCI::get_BAR0(pci_address()))), PAGE_ROUND_UP(mmio_base_size), "E1000 MMIO", Region::Access::Read | Region::Access::Write, false, false);
    m_mmio_base = m_mmio_region->vaddr();
    m_use_mmio = true;
    m_interrupt_line = PCI::get_interrupt_line(pci_address());
    klog() << "E1000: port base: " << m_io_base;
    klog() << "E1000: MMIO base: " << PhysicalAddress(PCI::get_BAR0(pci_address()) & 0xfffffffc);
    klog() << "E1000: MMIO base size: " << mmio_base_size << " bytes";
    klog() << "E1000: Interrupt line: " << m_interrupt_line;
    detect_eeprom();
    klog() << "E1000: Has EEPROM? " << m_has_eeprom;
    read_mac_address();
    const auto& mac = mac_address();
    klog() << "E1000: MAC address: " << String::format("%b", mac[0]) << ":" << String::format("%b", mac[1]) << ":" << String::format("%b", mac[2]) << ":" << String::format("%b", mac[3]) << ":" << String::format("%b", mac[4]) << ":" << String::format("%b", mac[5]);

    u32 flags = in32(REG_CTRL);
    out32(REG_CTRL, flags | ECTRL_SLU);

    initialize_rx_descriptors();
    initialize_tx_descriptors();

    out32(REG_IMASK, 0x1f6dc);
    out32(REG_IMASK, 0xff & ~4);
    in32(0xc0);

    enable_irq();
}

E1000NetworkAdapter::~E1000NetworkAdapter()
{
}

void E1000NetworkAdapter::handle_irq(RegisterState&)
{
    out32(REG_IMASK, 0x1);

    u32 status = in32(0xc0);
    if (status & 4) {
        u32 flags = in32(REG_CTRL);
        out32(REG_CTRL, flags | ECTRL_SLU);
    }
    if (status & 0x10) {
        // Threshold OK?
    }
    if (status & 0x80) {
        receive();
    }

    m_wait_queue.wake_all();
}

void E1000NetworkAdapter::detect_eeprom()
{
    out32(REG_EEPROM, 0x1);
    for (volatile int i = 0; i < 999; ++i) {
        u32 data = in32(REG_EEPROM);
        if (data & 0x10) {
            m_has_eeprom = true;
            return;
        }
    }
    m_has_eeprom = false;
}

u32 E1000NetworkAdapter::read_eeprom(u8 address)
{
    u16 data = 0;
    u32 tmp = 0;
    if (m_has_eeprom) {
        out32(REG_EEPROM, ((u32)address << 8) | 1);
        while (!((tmp = in32(REG_EEPROM)) & (1 << 4)))
            ;
    } else {
        out32(REG_EEPROM, ((u32)address << 2) | 1);
        while (!((tmp = in32(REG_EEPROM)) & (1 << 1)))
            ;
    }
    data = (tmp >> 16) & 0xffff;
    return data;
}

void E1000NetworkAdapter::read_mac_address()
{
    if (m_has_eeprom) {
        u8 mac[6];
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
        ASSERT_NOT_REACHED();
    }
}

bool E1000NetworkAdapter::link_up()
{
    return (in32(REG_STATUS) & STATUS_LU);
}

void E1000NetworkAdapter::initialize_rx_descriptors()
{
    auto* rx_descriptors = (e1000_tx_desc*)m_rx_descriptors_region->vaddr().as_ptr();
    for (int i = 0; i < number_of_rx_descriptors; ++i) {
        auto& descriptor = rx_descriptors[i];
        m_rx_buffers_regions.append(MM.allocate_contiguous_kernel_region(PAGE_ROUND_UP(8192), "E1000 RX buffer", Region::Access::Read | Region::Access::Write));
        descriptor.addr = m_rx_buffers_regions[i]->vmobject().physical_pages()[0]->paddr().get();
        descriptor.status = 0;
    }

    out32(REG_RXDESCLO, m_rx_descriptors_region->vmobject().physical_pages()[0]->paddr().get());
    out32(REG_RXDESCHI, 0);
    out32(REG_RXDESCLEN, number_of_rx_descriptors * sizeof(e1000_rx_desc));
    out32(REG_RXDESCHEAD, 0);
    out32(REG_RXDESCTAIL, number_of_rx_descriptors - 1);

    out32(REG_RCTRL, RCTL_EN | RCTL_SBP | RCTL_UPE | RCTL_MPE | RCTL_LBM_NONE | RTCL_RDMTS_HALF | RCTL_BAM | RCTL_SECRC | RCTL_BSIZE_8192);
}

void E1000NetworkAdapter::initialize_tx_descriptors()
{
    auto* tx_descriptors = (e1000_tx_desc*)m_tx_descriptors_region->vaddr().as_ptr();
    for (int i = 0; i < number_of_tx_descriptors; ++i) {
        auto& descriptor = tx_descriptors[i];
        m_tx_buffers_regions.append(MM.allocate_contiguous_kernel_region(PAGE_ROUND_UP(8192), "E1000 TX buffer", Region::Access::Read | Region::Access::Write));
        descriptor.addr = m_tx_buffers_regions[i]->vmobject().physical_pages()[0]->paddr().get();
        descriptor.cmd = 0;
    }

    out32(REG_TXDESCLO, m_tx_descriptors_region->vmobject().physical_pages()[0]->paddr().get());
    out32(REG_TXDESCHI, 0);
    out32(REG_TXDESCLEN, number_of_tx_descriptors * sizeof(e1000_tx_desc));
    out32(REG_TXDESCHEAD, 0);
    out32(REG_TXDESCTAIL, 0);

    out32(REG_TCTRL, in32(REG_TCTRL) | TCTL_EN | TCTL_PSP);
    out32(REG_TIPG, 0x0060200A);
}

void E1000NetworkAdapter::out8(u16 address, u8 data)
{
#ifdef E1000_DEBUG
    dbg() << "E1000: OUT @ 0x" << address;
#endif
    if (m_use_mmio) {
        auto* ptr = (volatile u8*)(m_mmio_base.get() + address);
        *ptr = data;
        return;
    }
    m_io_base.offset(address).out(data);
}

void E1000NetworkAdapter::out16(u16 address, u16 data)
{
#ifdef E1000_DEBUG
    dbg() << "E1000: OUT @ 0x" << address;
#endif
    if (m_use_mmio) {
        auto* ptr = (volatile u16*)(m_mmio_base.get() + address);
        *ptr = data;
        return;
    }
    m_io_base.offset(address).out(data);
}

void E1000NetworkAdapter::out32(u16 address, u32 data)
{
#ifdef E1000_DEBUG
    dbg() << "E1000: OUT @ 0x" << address;
#endif
    if (m_use_mmio) {
        auto* ptr = (volatile u32*)(m_mmio_base.get() + address);
        *ptr = data;
        return;
    }
    m_io_base.offset(address).out(data);
}

u8 E1000NetworkAdapter::in8(u16 address)
{
#ifdef E1000_DEBUG
    dbg() << "E1000: IN @ 0x" << address;
#endif
    if (m_use_mmio)
        return *(volatile u8*)(m_mmio_base.get() + address);
    return m_io_base.offset(address).in<u8>();
}

u16 E1000NetworkAdapter::in16(u16 address)
{
#ifdef E1000_DEBUG
    dbg() << "E1000: IN @ 0x " << address;
#endif
    if (m_use_mmio)
        return *(volatile u16*)(m_mmio_base.get() + address);
    return m_io_base.offset(address).in<u16>();
}

u32 E1000NetworkAdapter::in32(u16 address)
{
#ifdef E1000_DEBUG
    dbg() << "E1000: IN @ 0x" << address;
#endif
    if (m_use_mmio)
        return *(volatile u32*)(m_mmio_base.get() + address);
    return m_io_base.offset(address).in<u32>();
}

void E1000NetworkAdapter::send_raw(const u8* data, size_t length)
{
    disable_irq();
    u32 tx_current = in32(REG_TXDESCTAIL);
#ifdef E1000_DEBUG
    klog() << "E1000: Sending packet (" << length << " bytes)";
#endif
    auto* tx_descriptors = (e1000_tx_desc*)m_tx_descriptors_region->vaddr().as_ptr();
    auto& descriptor = tx_descriptors[tx_current];
    ASSERT(length <= 8192);
    auto* vptr = (void*)m_tx_buffers_regions[tx_current]->vaddr().as_ptr();
    memcpy(vptr, data, length);
    descriptor.length = length;
    descriptor.status = 0;
    descriptor.cmd = CMD_EOP | CMD_IFCS | CMD_RS;
#ifdef E1000_DEBUG
    klog() << "E1000: Using tx descriptor " << tx_current << " (head is at " << in32(REG_TXDESCHEAD) << ")";
#endif
    tx_current = (tx_current + 1) % number_of_tx_descriptors;
    out32(REG_TXDESCTAIL, tx_current);
    cli();
    enable_irq();
    for (;;) {
        if (descriptor.status) {
            sti();
            break;
        }
        Thread::current->wait_on(m_wait_queue);
    }
#ifdef E1000_DEBUG
    klog() << "E1000: Sent packet, status is now " << String::format("%b", descriptor.status) << "!";
#endif
}

void E1000NetworkAdapter::receive()
{
    auto* rx_descriptors = (e1000_tx_desc*)m_rx_descriptors_region->vaddr().as_ptr();
    u32 rx_current;
    for (;;) {
        rx_current = in32(REG_RXDESCTAIL);
        if (rx_current == in32(REG_RXDESCHEAD))
            return;
        rx_current = (rx_current + 1) % number_of_rx_descriptors;
        if (!(rx_descriptors[rx_current].status & 1))
            break;
        auto* buffer = m_rx_buffers_regions[rx_current]->vaddr().as_ptr();
        u16 length = rx_descriptors[rx_current].length;
#ifdef E1000_DEBUG
        klog() << "E1000: Received 1 packet @ " << buffer << " (" << length << ") bytes!";
#endif
        did_receive(buffer, length);
        rx_descriptors[rx_current].status = 0;
        out32(REG_RXDESCTAIL, rx_current);
    }
}

}
