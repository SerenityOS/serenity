/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/MACAddress.h>
#include <Kernel/Arch/x86/IO.h>
#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Debug.h>
#include <Kernel/Net/NE2000/NetworkAdapter.h>
#include <Kernel/Net/NetworkingManagement.h>
#include <Kernel/Sections.h>

namespace Kernel {

/**
 * The NE2000 is an ancient 10 Mib/s Ethernet network card standard by Novell
 * from the late 80s. Based on National Semiconductor's DP8390 Ethernet chip
 * or compatible, they were known to be extremely bare-bones but also very
 * cheap entry-level cards.
 *
 * QEMU supports them with the ne2k_{isa,pci} devices, physical incarnations
 * were available from different manufacturers for the ISA bus and later on
 * the PCI bus, including:
 *  - Realtek's RTL8029
 *  - VIA Technologies, Inc.'s VT86C926
 *
 * Official documentation from National Semiconductor includes:
 *  - Datasheet "DP8390D/NS32490D NIC Network Interface Controller"
 *  - Application Note 874 "Writing Drivers for the DP8390 NIC Family of Ethernet Controllers"
 *
 * This driver supports only the PCI variant.
 *
 * Remember, friends don't let friends use NE2000 network cards :^)
 */

// Page 0 registers
static constexpr u8 REG_RW_COMMAND = 0x00;
static constexpr u8 BIT_COMMAND_STOP = (0b1 << 0);
static constexpr u8 BIT_COMMAND_START = (0b1 << 1);
static constexpr u8 BIT_COMMAND_TXP = (0b1 << 2);
static constexpr u8 BIT_COMMAND_DMA_READ = (0b001 << 3);
static constexpr u8 BIT_COMMAND_DMA_WRITE = (0b010 << 3);
static constexpr u8 BIT_COMMAND_DMA_SEND = (0b011 << 3);
static constexpr u8 BIT_COMMAND_DMA_ABORT = (0b100 << 3);
static constexpr u8 BIT_COMMAND_DMA_FIELD = (0b111 << 3);
static constexpr u8 BIT_COMMAND_PAGE1 = (0b01 << 6);
static constexpr u8 BIT_COMMAND_PAGE2 = (0b10 << 6);
static constexpr u8 BIT_COMMAND_PAGE_FIELD = (0b11 << 6);

static constexpr u8 REG_WR_PAGESTART = 0x01;
static constexpr u8 REG_WR_PAGESTOP = 0x02;
static constexpr u8 REG_RW_BOUNDARY = 0x03;
static constexpr u8 REG_RD_TRANSMITSTATUS = 0x04;
static constexpr u8 REG_WR_TRANSMITPAGE = 0x04;
static constexpr u8 REG_RD_NCR = 0x05;
static constexpr u8 REG_WR_TRANSMITBYTECOUNT0 = 0x05;
static constexpr u8 REG_WR_TRANSMITBYTECOUNT1 = 0x06;
static constexpr u8 REG_RW_INTERRUPTSTATUS = 0x07;
static constexpr u8 REG_RD_CRDMA0 = 0x08;
static constexpr u8 REG_WR_REMOTESTARTADDRESS0 = 0x08;
static constexpr u8 REG_RD_CRDMA1 = 0x09;
static constexpr u8 REG_WR_REMOTESTARTADDRESS1 = 0x09;
static constexpr u8 REG_WR_REMOTEBYTECOUNT0 = 0x0a;
static constexpr u8 REG_WR_REMOTEBYTECOUNT1 = 0x0b;

static constexpr u8 REG_RD_RECEIVESTATUS = 0x0c;
static constexpr u8 BIT_RECEIVESTATUS_PRX = (0b1 << 0);
static constexpr u8 BIT_RECEIVESTATUS_CRC = (0b1 << 1);
static constexpr u8 BIT_RECEIVESTATUS_FAE = (0b1 << 2);
static constexpr u8 BIT_RECEIVESTATUS_FO = (0b1 << 3);
static constexpr u8 BIT_RECEIVESTATUS_MPA = (0b1 << 4);

static constexpr u8 REG_WR_RECEIVECONFIGURATION = 0x0c;
static constexpr u8 BIT_RECEIVECONFIGURATION_SEP = (0b1 << 0);
static constexpr u8 BIT_RECEIVECONFIGURATION_AR = (0b1 << 1);
static constexpr u8 BIT_RECEIVECONFIGURATION_AB = (0b1 << 2);
static constexpr u8 BIT_RECEIVECONFIGURATION_AM = (0b1 << 3);
static constexpr u8 BIT_RECEIVECONFIGURATION_PRO = (0b1 << 4);
static constexpr u8 BIT_RECEIVECONFIGURATION_MON = (0b1 << 5);

static constexpr u8 REG_RD_FAE_TALLY = 0x0d;

static constexpr u8 REG_WR_TRANSMITCONFIGURATION = 0x0d;
static constexpr u8 BIT_WR_TRANSMITCONFIGURATION_LOOPBACK = (0b10 << 0);

static constexpr u8 REG_RD_CRC_TALLY = 0x0e;

static constexpr u8 REG_WR_DATACONFIGURATION = 0x0e;
static constexpr u8 BIT_DATACONFIGURATION_WTS = (0b1 << 0);
static constexpr u8 BIT_DATACONFIGURATION_BOS = (0b1 << 1);
static constexpr u8 BIT_DATACONFIGURATION_LS = (0b1 << 2);
static constexpr u8 BIT_DATACONFIGURATION_FIFO_8B = (0b10 << 5);

static constexpr u8 REG_RD_MISS_PKT_TALLY = 0x0f;

static constexpr u8 REG_WR_INTERRUPTMASK = 0x0f;
static constexpr u8 BIT_INTERRUPTMASK_PRX = (0b1 << 0);
static constexpr u8 BIT_INTERRUPTMASK_PTX = (0b1 << 1);
static constexpr u8 BIT_INTERRUPTMASK_RXE = (0b1 << 2);
static constexpr u8 BIT_INTERRUPTMASK_TXE = (0b1 << 3);
static constexpr u8 BIT_INTERRUPTMASK_OVW = (0b1 << 4);
static constexpr u8 BIT_INTERRUPTMASK_CNT = (0b1 << 5);
static constexpr u8 BIT_INTERRUPTMASK_RDC = (0b1 << 6);
static constexpr u8 BIT_INTERRUPTMASK_RST = (0b1 << 7);

static constexpr u8 REG_RW_IOPORT = 0x10;

// Page 1 registers
static constexpr u8 REG_RW_PHYSICALADDRESS0 = 0x01;
static constexpr u8 REG_RW_CURRENT = 0x07;

static constexpr int NE2K_PAGE_SIZE = 256;

static constexpr int NE2K_RAM_BEGIN = 16384;
static constexpr int NE2K_RAM_END = 32768;
static constexpr int NE2K_RAM_SIZE = NE2K_RAM_END - NE2K_RAM_BEGIN;

static constexpr int NE2K_RAM_SEND_BEGIN = 16384;
static constexpr int NE2K_RAM_SEND_END = 16384 + 6 * NE2K_PAGE_SIZE;
static constexpr int NE2K_RAM_SEND_SIZE = NE2K_RAM_SEND_END - NE2K_RAM_SEND_BEGIN;

static constexpr int NE2K_RAM_RECV_BEGIN = NE2K_RAM_SEND_END;
static constexpr int NE2K_RAM_RECV_END = NE2K_RAM_END;
static constexpr int NE2K_RAM_RECV_SIZE = NE2K_RAM_RECV_END - NE2K_RAM_RECV_BEGIN;

static_assert(NE2K_RAM_BEGIN % NE2K_PAGE_SIZE == 0);
static_assert(NE2K_RAM_END % NE2K_PAGE_SIZE == 0);
static_assert(NE2K_RAM_SEND_BEGIN % NE2K_PAGE_SIZE == 0);
static_assert(NE2K_RAM_SEND_END % NE2K_PAGE_SIZE == 0);
static_assert(NE2K_RAM_RECV_BEGIN % NE2K_PAGE_SIZE == 0);
static_assert(NE2K_RAM_RECV_END % NE2K_PAGE_SIZE == 0);

struct [[gnu::packed]] received_packet_header {
    u8 status;
    u8 next_packet_page;
    u16 length;
};

UNMAP_AFTER_INIT RefPtr<NE2000NetworkAdapter> NE2000NetworkAdapter::try_to_initialize(PCI::DeviceIdentifier const& pci_device_identifier)
{
    constexpr auto ne2k_ids = Array {
        PCI::HardwareID { 0x10EC, 0x8029 }, // RealTek RTL-8029(AS)

        // List of clones, taken from Linux's ne2k-pci.c
        PCI::HardwareID { 0x1050, 0x0940 }, // Winbond 89C940
        PCI::HardwareID { 0x11f6, 0x1401 }, // Compex RL2000
        PCI::HardwareID { 0x8e2e, 0x3000 }, // KTI ET32P2
        PCI::HardwareID { 0x4a14, 0x5000 }, // NetVin NV5000SC
        PCI::HardwareID { 0x1106, 0x0926 }, // Via 86C926
        PCI::HardwareID { 0x10bd, 0x0e34 }, // SureCom NE34
        PCI::HardwareID { 0x1050, 0x5a5a }, // Winbond W89C940F
        PCI::HardwareID { 0x12c3, 0x0058 }, // Holtek HT80232
        PCI::HardwareID { 0x12c3, 0x5598 }, // Holtek HT80229
        PCI::HardwareID { 0x8c4a, 0x1980 }, // Winbond W89C940 (misprogrammed)
    };
    if (!ne2k_ids.span().contains_slow(pci_device_identifier.hardware_id()))
        return {};
    u8 irq = pci_device_identifier.interrupt_line().value();
    // FIXME: Better propagate errors here
    auto interface_name_or_error = NetworkingManagement::generate_interface_name_from_pci_address(pci_device_identifier);
    if (interface_name_or_error.is_error())
        return {};
    return adopt_ref_if_nonnull(new (nothrow) NE2000NetworkAdapter(pci_device_identifier.address(), irq, interface_name_or_error.release_value()));
}

UNMAP_AFTER_INIT NE2000NetworkAdapter::NE2000NetworkAdapter(PCI::Address address, u8 irq, NonnullOwnPtr<KString> interface_name)
    : NetworkAdapter(move(interface_name))
    , PCI::Device(address)
    , IRQHandler(irq)
    , m_io_base(PCI::get_BAR0(pci_address()) & ~3)
{
    dmesgln("NE2000: Found @ {}", pci_address());

    dmesgln("NE2000: Port base: {}", m_io_base);
    dmesgln("NE2000: Interrupt line: {}", interrupt_number());

    int ram_errors = ram_test();
    dmesgln("NE2000: RAM test {}, got {} byte errors", (ram_errors == 0 ? "OK" : "KO"), ram_errors);

    reset();
    set_mac_address(m_mac_address);
    dmesgln("NE2000: MAC address: {}", m_mac_address.to_string());
    enable_irq();
}

UNMAP_AFTER_INIT NE2000NetworkAdapter::~NE2000NetworkAdapter()
{
}

bool NE2000NetworkAdapter::handle_irq(const RegisterState&)
{
    u8 status = in8(REG_RW_INTERRUPTSTATUS);

    m_entropy_source.add_random_event(status);

    dbgln_if(NE2000_DEBUG, "NE2000NetworkAdapter: Got interrupt, status={:#02x}", status);
    if (status == 0) {
        return false;
    }

    if (status & BIT_INTERRUPTMASK_PRX) {
        dbgln_if(NE2000_DEBUG, "NE2000NetworkAdapter: Interrupt for packet received");
    }
    if (status & BIT_INTERRUPTMASK_PTX) {
        dbgln_if(NE2000_DEBUG, "NE2000NetworkAdapter: Interrupt for packet sent");
    }
    if (status & BIT_INTERRUPTMASK_RXE) {
        u8 fae = in8(REG_RD_FAE_TALLY);
        u8 crc = in8(REG_RD_CRC_TALLY);
        u8 miss = in8(REG_RD_MISS_PKT_TALLY);
        dmesgln("NE2000NetworkAdapter: Packet reception error framing={} crc={} missed={}", fae, crc, miss);
        // TODO: handle counters
    }
    if (status & BIT_INTERRUPTMASK_TXE) {
        dmesgln("NE2000NetworkAdapter: Packet transmission error");
    }
    if (status & BIT_INTERRUPTMASK_OVW) {
        dmesgln("NE2000NetworkAdapter: Ring buffer reception overflow error");
        // TODO: handle counters
    }
    if (status & BIT_INTERRUPTMASK_CNT) {
        dmesgln("NE2000NetworkAdapter: Counter overflow error");
        // TODO: handle counters
    }
    if (status & BIT_INTERRUPTMASK_RST) {
        dmesgln("NE2000NetworkAdapter: NIC requires reset due to packet reception overflow");
        // TODO: proper reset procedure
        reset();
    }

    receive();
    m_wait_queue.wake_all();

    out8(REG_RW_INTERRUPTSTATUS, status);
    return true;
}

UNMAP_AFTER_INIT int NE2000NetworkAdapter::ram_test()
{
    IOAddress io(PCI::get_BAR0(pci_address()) & ~3);
    int errors = 0;

    out8(REG_RW_COMMAND, BIT_COMMAND_DMA_ABORT | BIT_COMMAND_STOP);
#if __BYTE_ORDER__ == __ORDER_LITTLE_ENDIAN__
    out8(REG_WR_DATACONFIGURATION, BIT_DATACONFIGURATION_FIFO_8B | BIT_DATACONFIGURATION_WTS);
#elif __BYTE_ORDER__ == __ORDER_BIG_ENDIAN__
    out8(REG_WR_DATACONFIGURATION, BIT_DATACONFIGURATION_FIFO_8B | BIT_DATACONFIGURATION_BOS | BIT_DATACONFIGURATION_WTS);
#else
#    error Unknown byte order
#endif
    out8(REG_WR_REMOTEBYTECOUNT0, 0x00);
    out8(REG_WR_REMOTEBYTECOUNT1, 0x00);
    out8(REG_WR_RECEIVECONFIGURATION, BIT_RECEIVECONFIGURATION_MON);
    out8(REG_RW_COMMAND, BIT_COMMAND_DMA_ABORT | BIT_COMMAND_START);
    Array<u8, NE2K_RAM_SIZE> buffer;

    const u8 patterns[3] = { 0x5a, 0xff, 0x00 };
    for (int i = 0; i < 3; ++i) {
        for (size_t j = 0; j < buffer.size(); ++j)
            buffer[j] = patterns[i];

        rdma_write(NE2K_RAM_BEGIN, buffer);
        rdma_read(NE2K_RAM_BEGIN, buffer);

        for (size_t j = 0; j < buffer.size(); ++j) {
            if (buffer[j] != patterns[i]) {
                if (errors < 16)
                    dbgln_if(NE2000_DEBUG, "NE2000NetworkAdapter: Bad adapter RAM @ {} expected={} got={}", PhysicalAddress(NE2K_RAM_BEGIN + j), patterns[i], buffer[j]);
                else if (errors == 16)
                    dbgln_if(NE2000_DEBUG, "NE2000NetworkAdapter: Too many RAM errors, silencing further output");
                errors++;
            }
        }
    }

    return errors;
}

void NE2000NetworkAdapter::reset()
{
    const u8 interrupt_mask = BIT_INTERRUPTMASK_PRX | BIT_INTERRUPTMASK_PTX | BIT_INTERRUPTMASK_RXE | BIT_INTERRUPTMASK_TXE | BIT_INTERRUPTMASK_OVW | BIT_INTERRUPTMASK_CNT;
    u8 prom[32];

    // Taken from DP8390D's datasheet section 11.0, "Initialization Procedures"
    out8(REG_RW_COMMAND, BIT_COMMAND_DMA_ABORT | BIT_COMMAND_STOP);
#if __BYTE_ORDER__ == __ORDER_LITTLE_ENDIAN__
    out8(REG_WR_DATACONFIGURATION, BIT_DATACONFIGURATION_FIFO_8B | BIT_DATACONFIGURATION_WTS);
#elif __BYTE_ORDER__ == __ORDER_BIG_ENDIAN__
    out8(REG_WR_DATACONFIGURATION, BIT_DATACONFIGURATION_FIFO_8B | BIT_DATACONFIGURATION_BOS | BIT_DATACONFIGURATION_WTS);
#else
#    error Unknown byte order
#endif
    out8(REG_WR_REMOTEBYTECOUNT0, 0x00);
    out8(REG_WR_REMOTEBYTECOUNT1, 0x00);
    out8(REG_WR_RECEIVECONFIGURATION, BIT_RECEIVECONFIGURATION_AB | BIT_RECEIVECONFIGURATION_AR);
    out8(REG_WR_TRANSMITCONFIGURATION, BIT_WR_TRANSMITCONFIGURATION_LOOPBACK);
    m_ring_read_ptr = NE2K_RAM_RECV_BEGIN >> 8;
    out8(REG_WR_PAGESTART, NE2K_RAM_RECV_BEGIN >> 8);
    out8(REG_RW_BOUNDARY, NE2K_RAM_RECV_BEGIN >> 8);
    out8(REG_WR_PAGESTOP, NE2K_RAM_RECV_END >> 8);
    out8(REG_RW_INTERRUPTSTATUS, 0xff);
    out8(REG_WR_INTERRUPTMASK, interrupt_mask);
    rdma_read(0, Bytes(prom, sizeof(prom)));
    for (int i = 0; i < 6; i++) {
        m_mac_address[i] = prom[i * 2];
    }

    out8(REG_RW_COMMAND, BIT_COMMAND_PAGE1 | BIT_COMMAND_DMA_ABORT | BIT_COMMAND_STOP);
    for (int i = 0; i < 6; i++) {
        out8(REG_RW_PHYSICALADDRESS0 + i, m_mac_address[i]);
    }
    out8(REG_RW_CURRENT, NE2K_RAM_RECV_BEGIN >> 8);

    out8(REG_RW_COMMAND, BIT_COMMAND_DMA_ABORT | BIT_COMMAND_START);
    out8(REG_WR_TRANSMITCONFIGURATION, 0xe0);
}

void NE2000NetworkAdapter::rdma_read(size_t address, Bytes payload)
{
    dbgln_if(NE2000_DEBUG, "NE2000NetworkAdapter: DMA read @ {} length={}", PhysicalAddress(address), payload.size());

    u8 command = in8(REG_RW_COMMAND) & ~(BIT_COMMAND_PAGE_FIELD | BIT_COMMAND_DMA_FIELD);
    out8(REG_RW_COMMAND, command | BIT_COMMAND_DMA_ABORT);
    out8(REG_RW_INTERRUPTSTATUS, BIT_INTERRUPTMASK_RDC);

    out8(REG_WR_REMOTEBYTECOUNT0, payload.size());
    out8(REG_WR_REMOTEBYTECOUNT1, payload.size() >> 8);
    out8(REG_WR_REMOTESTARTADDRESS0, address);
    out8(REG_WR_REMOTESTARTADDRESS1, address >> 8);

    command = in8(REG_RW_COMMAND) & ~(BIT_COMMAND_DMA_FIELD);
    out8(REG_RW_COMMAND, command | BIT_COMMAND_DMA_READ);

    for (size_t i = 0; i < payload.size(); i += 2) {
        u16 data = in16(REG_RW_IOPORT);
        payload[i] = data;
        if (i != payload.size() - 1)
            payload[i + 1] = data >> 8;
    }

    while (!(in8(REG_RW_INTERRUPTSTATUS) & BIT_INTERRUPTMASK_RDC))
        ;
}

void NE2000NetworkAdapter::rdma_write(size_t address, ReadonlyBytes payload)
{
    dbgln_if(NE2000_DEBUG, "NE2000NetworkAdapter: DMA write @ {} length={}", PhysicalAddress(address), payload.size());

    u8 command = in8(REG_RW_COMMAND) & ~(BIT_COMMAND_PAGE_FIELD | BIT_COMMAND_DMA_FIELD);
    out8(REG_RW_COMMAND, command | BIT_COMMAND_DMA_ABORT);
    out8(REG_RW_INTERRUPTSTATUS, BIT_INTERRUPTMASK_RDC);

    out8(REG_WR_REMOTEBYTECOUNT0, payload.size());
    out8(REG_WR_REMOTEBYTECOUNT1, payload.size() >> 8);
    out8(REG_WR_REMOTESTARTADDRESS0, address);
    out8(REG_WR_REMOTESTARTADDRESS1, address >> 8);

    command = in8(REG_RW_COMMAND) & ~(BIT_COMMAND_DMA_FIELD);
    out8(REG_RW_COMMAND, command | BIT_COMMAND_DMA_WRITE);

    for (size_t i = 0; i < payload.size(); i += 2) {
        u16 data = payload[i];
        if (i != payload.size() - 1)
            data |= payload[i + 1] << 8;
        out16(REG_RW_IOPORT, data);
    }

    while (!(in8(REG_RW_INTERRUPTSTATUS) & BIT_INTERRUPTMASK_RDC))
        ;
}

void NE2000NetworkAdapter::send_raw(ReadonlyBytes payload)
{
    dbgln_if(NE2000_DEBUG, "NE2000NetworkAdapter: Sending packet length={}", payload.size());

    if (payload.size() > NE2K_RAM_SEND_SIZE) {
        dmesgln("NE2000NetworkAdapter: Packet to send was too big; discarding");
        return;
    }

    while (in8(REG_RW_COMMAND) & BIT_COMMAND_TXP)
        m_wait_queue.wait_forever("NE2000NetworkAdapter");

    disable_irq();
    size_t packet_size = payload.size();
    if (packet_size < 64)
        packet_size = 64;
    rdma_write(NE2K_RAM_SEND_BEGIN, payload);
    out8(REG_WR_TRANSMITPAGE, NE2K_RAM_SEND_BEGIN >> 8);
    out8(REG_WR_TRANSMITBYTECOUNT0, packet_size);
    out8(REG_WR_TRANSMITBYTECOUNT1, packet_size >> 8);
    out8(REG_RW_COMMAND, BIT_COMMAND_DMA_ABORT | BIT_COMMAND_TXP | BIT_COMMAND_START);

    dbgln_if(NE2000_DEBUG, "NE2000NetworkAdapter: Packet submitted for transmission");

    enable_irq();
}

void NE2000NetworkAdapter::receive()
{
    while (true) {
        out8(REG_RW_COMMAND, BIT_COMMAND_PAGE1 | in8(REG_RW_COMMAND));
        u8 current = in8(REG_RW_CURRENT);
        out8(REG_RW_COMMAND, in8(REG_RW_COMMAND) & ~BIT_COMMAND_PAGE_FIELD);
        if (m_ring_read_ptr == current)
            break;

        size_t header_address = m_ring_read_ptr << 8;
        received_packet_header header;
        rdma_read(header_address, Bytes(reinterpret_cast<u8*>(&header), sizeof(header)));

        bool packet_ok = header.status & BIT_RECEIVESTATUS_PRX;
        dbgln_if(NE2000_DEBUG, "NE2000NetworkAdapter: Packet received {} length={}", (packet_ok ? "intact" : "damaged"), header.length);

        if (packet_ok) {
            size_t bytes_in_packet = sizeof(received_packet_header) + header.length;

            auto packet_result = NetworkByteBuffer::create_uninitialized(bytes_in_packet);
            u8 drop_buffer[NE2K_PAGE_SIZE];
            Bytes buffer { drop_buffer, array_size(drop_buffer) };
            bool will_drop { false };
            if (packet_result.is_error()) {
                dbgln("NE2000NetworkAdapter: Not enough memory for packet with length = {}, dropping.", header.length);
                will_drop = true;
            } else {
                buffer = packet_result.value().bytes();
            }

            int current_offset = 0;
            int ring_offset = header_address;

            while (bytes_in_packet > 0) {
                int copy_size = min(bytes_in_packet, NE2K_PAGE_SIZE);
                rdma_read(ring_offset, buffer.slice(current_offset, copy_size));
                if (!will_drop)
                    current_offset += copy_size;
                ring_offset += copy_size;
                bytes_in_packet -= copy_size;
                if (ring_offset == NE2K_RAM_RECV_END)
                    ring_offset = NE2K_RAM_RECV_BEGIN;
            }

            if (!will_drop)
                did_receive(buffer.slice(sizeof(received_packet_header)));
        }

        if (header.next_packet_page == (NE2K_RAM_RECV_BEGIN >> 8))
            out8(REG_RW_BOUNDARY, (NE2K_RAM_RECV_END >> 8) - 1);
        else
            out8(REG_RW_BOUNDARY, header.next_packet_page - 1);
        m_ring_read_ptr = header.next_packet_page;
    }
}

void NE2000NetworkAdapter::out8(u16 address, u8 data)
{
    m_io_base.offset(address).out(data);
}

void NE2000NetworkAdapter::out16(u16 address, u16 data)
{
    m_io_base.offset(address).out(data);
}

u8 NE2000NetworkAdapter::in8(u16 address)
{
    u8 data = m_io_base.offset(address).in<u8>();
    return data;
}

u16 NE2000NetworkAdapter::in16(u16 address)
{
    return m_io_base.offset(address).in<u16>();
}

}
