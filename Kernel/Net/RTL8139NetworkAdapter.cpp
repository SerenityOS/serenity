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

#include <Kernel/IO.h>
#include <Kernel/Net/RTL8139NetworkAdapter.h>

//#define RTL8139_DEBUG

namespace Kernel {

#define REG_MAC 0x00
#define REG_MAR0 0x08
#define REG_MAR4 0x12
#define REG_TXSTATUS0 0x10
#define REG_TXADDR0 0x20
#define REG_RXBUF 0x30
#define REG_COMMAND 0x37
#define REG_CAPR 0x38
#define REG_IMR 0x3C
#define REG_ISR 0x3E
#define REG_TXCFG 0x40
#define REG_RXCFG 0x44
#define REG_MPC 0x4C
#define REG_CFG9346 0x50
#define REG_CONFIG1 0x52
#define REG_MSR 0x58
#define REG_BMCR 0x62

#define TX_STATUS_OWN 0x2000
#define TX_STATUS_THRESHOLD_MAX 0x3F0000

#define COMMAND_RX_EMPTY 0x01
#define COMMAND_TX_ENABLE 0x04
#define COMMAND_RX_ENABLE 0x08
#define COMMAND_RESET 0x10

#define INT_RXOK 0x01
#define INT_RXERR 0x02
#define INT_TXOK 0x04
#define INT_TXERR 0x08
#define INT_RX_BUFFER_OVERFLOW 0x10
#define INT_LINK_CHANGE 0x20
#define INT_RX_FIFO_OVERFLOW 0x40
#define INT_LENGTH_CHANGE 0x2000
#define INT_SYSTEM_ERROR 0x8000

#define CFG9346_NONE 0x00
#define CFG9346_EEM0 0x40
#define CFG9346_EEM1 0x80

#define TXCFG_TXRR_ZERO 0x00
#define TXCFG_MAX_DMA_16B 0x000
#define TXCFG_MAX_DMA_32B 0x100
#define TXCFG_MAX_DMA_64B 0x200
#define TXCFG_MAX_DMA_128B 0x300
#define TXCFG_MAX_DMA_256B 0x400
#define TXCFG_MAX_DMA_512B 0x500
#define TXCFG_MAX_DMA_1K 0x600
#define TXCFG_MAX_DMA_2K 0x700
#define TXCFG_IFG11 0x3000000

#define RXCFG_AAP 0x01
#define RXCFG_APM 0x02
#define RXCFG_AM 0x04
#define RXCFG_AB 0x08
#define RXCFG_AR 0x10
#define RXCFG_WRAP_INHIBIT 0x80
#define RXCFG_MAX_DMA_16B 0x000
#define RXCFG_MAX_DMA_32B 0x100
#define RXCFG_MAX_DMA_64B 0x200
#define RXCFG_MAX_DMA_128B 0x300
#define RXCFG_MAX_DMA_256B 0x400
#define RXCFG_MAX_DMA_512B 0x500
#define RXCFG_MAX_DMA_1K 0x600
#define RXCFG_MAX_DMA_UNLIMITED 0x0700
#define RXCFG_RBLN_8K 0x0000
#define RXCFG_RBLN_16K 0x0800
#define RXCFG_RBLN_32K 0x1000
#define RXCFG_RBLN_64K 0x1800
#define RXCFG_FTH_NONE 0xE000

#define MSR_LINKB 0x02
#define MSR_RX_FLOW_CONTROL_ENABLE 0x40

#define BMCR_SPEED 0x2000
#define BMCR_AUTO_NEGOTIATE 0x1000
#define BMCR_DUPLEX 0x0100

#define RX_MULTICAST 0x8000
#define RX_PHYSICAL_MATCH 0x4000
#define RX_BROADCAST 0x2000
#define RX_INVALID_SYMBOL_ERROR 0x20
#define RX_RUNT 0x10
#define RX_LONG 0x08
#define RX_CRC_ERROR 0x04
#define RX_FRAME_ALIGNMENT_ERROR 0x02
#define RX_OK 0x01

#define PACKET_SIZE_MAX 0x600
#define PACKET_SIZE_MIN 0x16

#define RX_BUFFER_SIZE 32768
#define TX_BUFFER_SIZE PACKET_SIZE_MAX

void RTL8139NetworkAdapter::detect()
{
    static const PCI::ID rtl8139_id = { 0x10EC, 0x8139 };
    PCI::enumerate([&](const PCI::Address& address, PCI::ID id) {
        if (address.is_null())
            return;
        if (id != rtl8139_id)
            return;
        u8 irq = PCI::get_interrupt_line(address);
        (void)adopt(*new RTL8139NetworkAdapter(address, irq)).leak_ref();
    });
}

RTL8139NetworkAdapter::RTL8139NetworkAdapter(PCI::Address address, u8 irq)
    : PCI::Device(address, irq)
    , m_io_base(PCI::get_BAR0(pci_address()) & ~1)
    , m_rx_buffer(MM.allocate_contiguous_kernel_region(PAGE_ROUND_UP(RX_BUFFER_SIZE + PACKET_SIZE_MAX), "RTL8139 RX", Region::Access::Read | Region::Access::Write))
    , m_packet_buffer(MM.allocate_contiguous_kernel_region(PAGE_ROUND_UP(PACKET_SIZE_MAX), "RTL8139 Packet buffer", Region::Access::Read | Region::Access::Write))
{
    m_tx_buffers.ensure_capacity(RTL8139_TX_BUFFER_COUNT);
    set_interface_name("rtl8139");

    klog() << "RTL8139: Found @ " << pci_address();

    enable_bus_mastering(pci_address());

    m_interrupt_line = PCI::get_interrupt_line(pci_address());
    klog() << "RTL8139: port base: " << m_io_base;
    klog() << "RTL8139: Interrupt line: " << m_interrupt_line;

    // we add space to account for overhang from the last packet - the rtl8139
    // can optionally guarantee that packets will be contiguous by
    // purposefully overrunning the rx buffer
    klog() << "RTL8139: RX buffer: " << m_rx_buffer->physical_page(0)->paddr();

    for (int i = 0; i < RTL8139_TX_BUFFER_COUNT; i++) {
        m_tx_buffers.append(MM.allocate_contiguous_kernel_region(PAGE_ROUND_UP(TX_BUFFER_SIZE), "RTL8139 TX", Region::Access::Write | Region::Access::Read));
        klog() << "RTL8139: TX buffer " << i << ": " << m_tx_buffers[i]->physical_page(0)->paddr();
    }

    reset();

    read_mac_address();
    const auto& mac = mac_address();
    klog() << "RTL8139: MAC address: " << mac.to_string().characters();

    enable_irq();
}

RTL8139NetworkAdapter::~RTL8139NetworkAdapter()
{
}

void RTL8139NetworkAdapter::handle_irq(const RegisterState&)
{
    for (;;) {
        int status = in16(REG_ISR);
        out16(REG_ISR, status);

        m_entropy_source.add_random_event(status);

#ifdef RTL8139_DEBUG
        klog() << "RTL8139NetworkAdapter::handle_irq status=0x" << String::format("%x", status);
#endif

        if ((status & (INT_RXOK | INT_RXERR | INT_TXOK | INT_TXERR | INT_RX_BUFFER_OVERFLOW | INT_LINK_CHANGE | INT_RX_FIFO_OVERFLOW | INT_LENGTH_CHANGE | INT_SYSTEM_ERROR)) == 0)
            break;

        if (status & INT_RXOK) {
#ifdef RTL8139_DEBUG
            klog() << "RTL8139NetworkAdapter: rx ready";
#endif
            receive();
        }
        if (status & INT_RXERR) {
            klog() << "RTL8139NetworkAdapter: rx error - resetting device";
            reset();
        }
        if (status & INT_TXOK) {
#ifdef RTL8139_DEBUG
            klog() << "RTL8139NetworkAdapter: tx complete";
#endif
        }
        if (status & INT_TXERR) {
            klog() << "RTL8139NetworkAdapter: tx error - resetting device";
            reset();
        }
        if (status & INT_RX_BUFFER_OVERFLOW) {
            klog() << "RTL8139NetworkAdapter: rx buffer overflow";
        }
        if (status & INT_LINK_CHANGE) {
            m_link_up = (in8(REG_MSR) & MSR_LINKB) == 0;
            klog() << "RTL8139NetworkAdapter: link status changed up=" << m_link_up;
        }
        if (status & INT_RX_FIFO_OVERFLOW) {
            klog() << "RTL8139NetworkAdapter: rx fifo overflow";
        }
        if (status & INT_LENGTH_CHANGE) {
            klog() << "RTL8139NetworkAdapter: cable length change";
        }
        if (status & INT_SYSTEM_ERROR) {
            klog() << "RTL8139NetworkAdapter: system error - resetting device";
            reset();
        }
    }
}

void RTL8139NetworkAdapter::reset()
{
    m_rx_buffer_offset = 0;
    m_tx_next_buffer = 0;

    // reset the device to clear out all the buffers and config
    out8(REG_COMMAND, COMMAND_RESET);
    while ((in8(REG_COMMAND) & COMMAND_RESET) != 0)
        ;

    // unlock config registers
    out8(REG_CFG9346, CFG9346_EEM0 | CFG9346_EEM1);
    // turn on multicast
    out32(REG_MAR0, 0xffffffff);
    out32(REG_MAR4, 0xffffffff);
    // enable rx/tx
    out8(REG_COMMAND, COMMAND_RX_ENABLE | COMMAND_TX_ENABLE);
    // device might be in sleep mode, this will take it out
    out8(REG_CONFIG1, 0);
    // set up rx buffer
    out32(REG_RXBUF, m_rx_buffer->physical_page(0)->paddr().get());
    // reset missed packet counter
    out8(REG_MPC, 0);
    // "basic mode control register" options - 100mbit, full duplex, auto
    // negotiation
    out16(REG_BMCR, BMCR_SPEED | BMCR_AUTO_NEGOTIATE | BMCR_DUPLEX);
    // enable flow control
    out8(REG_MSR, MSR_RX_FLOW_CONTROL_ENABLE);
    // configure rx: accept physical (MAC) match, multicast, and broadcast,
    // use the optional contiguous packet feature, the maximum dma transfer
    // size, a 32k buffer, and no fifo threshold
    out32(REG_RXCFG, RXCFG_APM | RXCFG_AM | RXCFG_AB | RXCFG_WRAP_INHIBIT | RXCFG_MAX_DMA_UNLIMITED | RXCFG_RBLN_32K | RXCFG_FTH_NONE);
    // configure tx: default retry count (16), max DMA burst size of 1024
    // bytes, interframe gap time of the only allowable value. the DMA burst
    // size is important - silent failures have been observed with 2048 bytes.
    out32(REG_TXCFG, TXCFG_TXRR_ZERO | TXCFG_MAX_DMA_1K | TXCFG_IFG11);
    // tell the chip where we want it to DMA from for outgoing packets.
    for (int i = 0; i < 4; i++)
        out32(REG_TXADDR0 + (i * 4), m_tx_buffers[i]->physical_page(0)->paddr().get());
    // re-lock config registers
    out8(REG_CFG9346, CFG9346_NONE);
    // enable rx/tx again in case they got turned off (apparently some cards
    // do this?)
    out8(REG_COMMAND, COMMAND_RX_ENABLE | COMMAND_TX_ENABLE);

    // choose irqs, then clear any pending
    out16(REG_IMR, INT_RXOK | INT_RXERR | INT_TXOK | INT_TXERR | INT_RX_BUFFER_OVERFLOW | INT_LINK_CHANGE | INT_RX_FIFO_OVERFLOW | INT_LENGTH_CHANGE | INT_SYSTEM_ERROR);
    out16(REG_ISR, 0xffff);
}

void RTL8139NetworkAdapter::read_mac_address()
{
    u8 mac[6];
    for (int i = 0; i < 6; i++)
        mac[i] = in8(REG_MAC + i);
    set_mac_address(mac);
}

void RTL8139NetworkAdapter::send_raw(ReadonlyBytes payload)
{
#ifdef RTL8139_DEBUG
    klog() << "RTL8139NetworkAdapter::send_raw length=" << payload.size();
#endif

    if (payload.size() > PACKET_SIZE_MAX) {
        klog() << "RTL8139NetworkAdapter: packet was too big; discarding";
        return;
    }

    int hw_buffer = -1;
    for (int i = 0; i < RTL8139_TX_BUFFER_COUNT; i++) {
        int potential_buffer = (m_tx_next_buffer + i) % 4;

        auto status = in32(REG_TXSTATUS0 + (potential_buffer * 4));
        if (status & TX_STATUS_OWN) {
            hw_buffer = potential_buffer;
            break;
        }
    }

    if (hw_buffer == -1) {
        klog() << "RTL8139NetworkAdapter: hardware buffers full; discarding packet";
        return;
    } else {
#ifdef RTL8139_DEBUG
        klog() << "RTL8139NetworkAdapter: chose buffer " << hw_buffer << " @ " << PhysicalAddress(m_tx_buffers[hw_buffer]);
#endif
        m_tx_next_buffer = (hw_buffer + 1) % 4;
    }

    memcpy(m_tx_buffers[hw_buffer]->vaddr().as_ptr(), payload.data(), payload.size());
    memset(m_tx_buffers[hw_buffer]->vaddr().as_ptr() + payload.size(), 0, TX_BUFFER_SIZE - payload.size());

    // the rtl8139 will not actually emit packets onto the network if they're
    // smaller than 64 bytes. the rtl8139 adds a checksum to the end of each
    // packet, and that checksum is four bytes long, so we pad the packet to
    // 60 bytes if necessary to make sure the whole thing is large enough.
    auto length = payload.size();
    if (length < 60) {
#ifdef RTL8139_DEBUG
        klog() << "RTL8139NetworkAdapter: adjusting payload size from " << length << " to 60";
#endif
        length = 60;
    }

    out32(REG_TXSTATUS0 + (hw_buffer * 4), length);
}

void RTL8139NetworkAdapter::receive()
{
    auto* start_of_packet = m_rx_buffer->vaddr().as_ptr() + m_rx_buffer_offset;

    u16 status = *(const u16*)(start_of_packet + 0);
    u16 length = *(const u16*)(start_of_packet + 2);

#ifdef RTL8139_DEBUG
    klog() << "RTL8139NetworkAdapter::receive status=0x" << String::format("%x", status) << " length=" << length << " offset=" << m_rx_buffer_offset;
#endif

    if (!(status & RX_OK) || (status & (RX_INVALID_SYMBOL_ERROR | RX_CRC_ERROR | RX_FRAME_ALIGNMENT_ERROR)) || (length >= PACKET_SIZE_MAX) || (length < PACKET_SIZE_MIN)) {
        klog() << "RTL8139NetworkAdapter::receive got bad packet status=0x" << String::format("%x", status) << " length=" << length;
        reset();
        return;
    }

    // we never have to worry about the packet wrapping around the buffer,
    // since we set RXCFG_WRAP_INHIBIT, which allows the rtl8139 to write data
    // past the end of the allotted space.
    memcpy(m_packet_buffer->vaddr().as_ptr(), (const u8*)(start_of_packet + 4), length - 4);
    // let the card know that we've read this data
    m_rx_buffer_offset = ((m_rx_buffer_offset + length + 4 + 3) & ~3) % RX_BUFFER_SIZE;
    out16(REG_CAPR, m_rx_buffer_offset - 0x10);
    m_rx_buffer_offset %= RX_BUFFER_SIZE;

    did_receive({ m_packet_buffer->vaddr().as_ptr(), (size_t)(length - 4) });
}

void RTL8139NetworkAdapter::out8(u16 address, u8 data)
{
    m_io_base.offset(address).out(data);
}

void RTL8139NetworkAdapter::out16(u16 address, u16 data)
{
    m_io_base.offset(address).out(data);
}

void RTL8139NetworkAdapter::out32(u16 address, u32 data)
{
    m_io_base.offset(address).out(data);
}

u8 RTL8139NetworkAdapter::in8(u16 address)
{
    return m_io_base.offset(address).in<u8>();
}

u16 RTL8139NetworkAdapter::in16(u16 address)
{
    return m_io_base.offset(address).in<u16>();
}

u32 RTL8139NetworkAdapter::in32(u16 address)
{
    return m_io_base.offset(address).in<u32>();
}

}
