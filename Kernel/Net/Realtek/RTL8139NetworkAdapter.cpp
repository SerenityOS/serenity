/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/MACAddress.h>
#include <Kernel/Arch/x86/IO.h>
#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Debug.h>
#include <Kernel/Net/NetworkingManagement.h>
#include <Kernel/Net/Realtek/RTL8139NetworkAdapter.h>
#include <Kernel/Sections.h>

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
#define REG_ANLPAR 0x68

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
#define MSR_SPEED_10 0x08
#define MSR_RX_FLOW_CONTROL_ENABLE 0x40

#define BMCR_SPEED 0x2000
#define BMCR_AUTO_NEGOTIATE 0x1000
#define BMCR_DUPLEX 0x0100

#define ANLPAR_10FD 0x0040
#define ANLPAR_TXFD 0x0100

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

UNMAP_AFTER_INIT RefPtr<RTL8139NetworkAdapter> RTL8139NetworkAdapter::try_to_initialize(PCI::DeviceIdentifier const& pci_device_identifier)
{
    constexpr PCI::HardwareID rtl8139_id = { 0x10EC, 0x8139 };
    if (pci_device_identifier.hardware_id() != rtl8139_id)
        return {};
    u8 irq = pci_device_identifier.interrupt_line().value();
    // FIXME: Better propagate errors here
    auto interface_name_or_error = NetworkingManagement::generate_interface_name_from_pci_address(pci_device_identifier);
    if (interface_name_or_error.is_error())
        return {};
    return adopt_ref_if_nonnull(new (nothrow) RTL8139NetworkAdapter(pci_device_identifier.address(), irq, interface_name_or_error.release_value()));
}

UNMAP_AFTER_INIT RTL8139NetworkAdapter::RTL8139NetworkAdapter(PCI::Address address, u8 irq, NonnullOwnPtr<KString> interface_name)
    : NetworkAdapter(move(interface_name))
    , PCI::Device(address)
    , IRQHandler(irq)
    , m_io_base(PCI::get_BAR0(pci_address()) & ~1)
    , m_rx_buffer(MM.allocate_contiguous_kernel_region(Memory::page_round_up(RX_BUFFER_SIZE + PACKET_SIZE_MAX).release_value_but_fixme_should_propagate_errors(), "RTL8139 RX", Memory::Region::Access::ReadWrite).release_value())
    , m_packet_buffer(MM.allocate_contiguous_kernel_region(Memory::page_round_up(PACKET_SIZE_MAX).release_value_but_fixme_should_propagate_errors(), "RTL8139 Packet buffer", Memory::Region::Access::ReadWrite).release_value())
{
    m_tx_buffers.ensure_capacity(RTL8139_TX_BUFFER_COUNT);

    dmesgln("RTL8139: Found @ {}", pci_address());

    enable_bus_mastering(pci_address());

    dmesgln("RTL8139: I/O port base: {}", m_io_base);
    dmesgln("RTL8139: Interrupt line: {}", interrupt_number());

    // we add space to account for overhang from the last packet - the rtl8139
    // can optionally guarantee that packets will be contiguous by
    // purposefully overrunning the rx buffer
    dbgln("RTL8139: RX buffer: {}", m_rx_buffer->physical_page(0)->paddr());

    for (int i = 0; i < RTL8139_TX_BUFFER_COUNT; i++) {
        m_tx_buffers.append(MM.allocate_contiguous_kernel_region(Memory::page_round_up(TX_BUFFER_SIZE).release_value_but_fixme_should_propagate_errors(), "RTL8139 TX", Memory::Region::Access::Write | Memory::Region::Access::Read).release_value());
        dbgln("RTL8139: TX buffer {}: {}", i, m_tx_buffers[i]->physical_page(0)->paddr());
    }

    reset();

    read_mac_address();
    const auto& mac = mac_address();
    dmesgln("RTL8139: MAC address: {}", mac.to_string());

    enable_irq();
}

UNMAP_AFTER_INIT RTL8139NetworkAdapter::~RTL8139NetworkAdapter()
{
}

bool RTL8139NetworkAdapter::handle_irq(const RegisterState&)
{
    bool was_handled = false;
    for (;;) {
        int status = in16(REG_ISR);
        out16(REG_ISR, status);

        m_entropy_source.add_random_event(status);

        dbgln_if(RTL8139_DEBUG, "RTL8139: handle_irq status={:#04x}", status);

        if ((status & (INT_RXOK | INT_RXERR | INT_TXOK | INT_TXERR | INT_RX_BUFFER_OVERFLOW | INT_LINK_CHANGE | INT_RX_FIFO_OVERFLOW | INT_LENGTH_CHANGE | INT_SYSTEM_ERROR)) == 0)
            break;

        was_handled = true;
        if (status & INT_RXOK) {
            dbgln_if(RTL8139_DEBUG, "RTL8139: RX ready");
            receive();
        }
        if (status & INT_RXERR) {
            dmesgln("RTL8139: RX error - resetting device");
            reset();
        }
        if (status & INT_TXOK) {
            dbgln_if(RTL8139_DEBUG, "RTL8139: TX complete");
        }
        if (status & INT_TXERR) {
            dmesgln("RTL8139: TX error - resetting device");
            reset();
        }
        if (status & INT_RX_BUFFER_OVERFLOW) {
            dmesgln("RTL8139: RX buffer overflow");
        }
        if (status & INT_LINK_CHANGE) {
            m_link_up = (in8(REG_MSR) & MSR_LINKB) == 0;
            dmesgln("RTL8139: Link status changed up={}", m_link_up);
        }
        if (status & INT_RX_FIFO_OVERFLOW) {
            dmesgln("RTL8139: RX FIFO overflow");
        }
        if (status & INT_LENGTH_CHANGE) {
            dmesgln("RTL8139: Cable length change");
        }
        if (status & INT_SYSTEM_ERROR) {
            dmesgln("RTL8139: System error - resetting device");
            reset();
        }
    }
    return was_handled;
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

    // Set the initial link up status.
    m_link_up = (in8(REG_MSR) & MSR_LINKB) == 0;
}

UNMAP_AFTER_INIT void RTL8139NetworkAdapter::read_mac_address()
{
    MACAddress mac {};
    for (int i = 0; i < 6; i++)
        mac[i] = in8(REG_MAC + i);
    set_mac_address(mac);
}

void RTL8139NetworkAdapter::send_raw(ReadonlyBytes payload)
{
    dbgln_if(RTL8139_DEBUG, "RTL8139: send_raw length={}", payload.size());

    if (payload.size() > PACKET_SIZE_MAX) {
        dmesgln("RTL8139: Packet was too big; discarding");
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
        dmesgln("RTL8139: Hardware buffers full; discarding packet");
        return;
    }

    dbgln_if(RTL8139_DEBUG, "RTL8139: Chose buffer {}", hw_buffer);
    m_tx_next_buffer = (hw_buffer + 1) % 4;

    memcpy(m_tx_buffers[hw_buffer]->vaddr().as_ptr(), payload.data(), payload.size());
    memset(m_tx_buffers[hw_buffer]->vaddr().as_ptr() + payload.size(), 0, TX_BUFFER_SIZE - payload.size());

    // the rtl8139 will not actually emit packets onto the network if they're
    // smaller than 64 bytes. the rtl8139 adds a checksum to the end of each
    // packet, and that checksum is four bytes long, so we pad the packet to
    // 60 bytes if necessary to make sure the whole thing is large enough.
    auto length = payload.size();
    if (length < 60) {
        dbgln_if(RTL8139_DEBUG, "RTL8139: adjusting payload size from {} to 60", length);
        length = 60;
    }

    out32(REG_TXSTATUS0 + (hw_buffer * 4), length);
}

void RTL8139NetworkAdapter::receive()
{
    auto* start_of_packet = m_rx_buffer->vaddr().as_ptr() + m_rx_buffer_offset;

    u16 status = *(const u16*)(start_of_packet + 0);
    u16 length = *(const u16*)(start_of_packet + 2);

    dbgln_if(RTL8139_DEBUG, "RTL8139: receive, status={:#04x}, length={}, offset={}", status, length, m_rx_buffer_offset);

    if (!(status & RX_OK) || (status & (RX_INVALID_SYMBOL_ERROR | RX_CRC_ERROR | RX_FRAME_ALIGNMENT_ERROR)) || (length >= PACKET_SIZE_MAX) || (length < PACKET_SIZE_MIN)) {
        dmesgln("RTL8139: receive got bad packet, status={:#04x}, length={}", status, length);
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

bool RTL8139NetworkAdapter::link_full_duplex()
{
    // Note: this code assumes auto-negotiation is enabled (which is now always the case) and
    // bases the duplex state on the link partner advertisement.
    // If non-auto-negotiation is ever implemented this should be changed.
    u16 anlpar = in16(REG_ANLPAR);
    return !!(anlpar & (ANLPAR_TXFD | ANLPAR_10FD));
}

i32 RTL8139NetworkAdapter::link_speed()
{
    if (!link_up())
        return NetworkAdapter::LINKSPEED_INVALID;

    u16 msr = in16(REG_MSR);
    return msr & MSR_SPEED_10 ? 10 : 100;
}

}
