/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
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

#include <AK/Atomic.h>
#include <AK/MACAddress.h>
#include <Kernel/IO.h>
#include <Kernel/Net/RTL8169NetworkAdapter.h>
#include <Kernel/VM/MemoryManager.h>

//#define RTL8169_DEBUG

namespace Kernel {

#define REG_MAC 0x00
#define REG_MAR0 0x8
#define REG_MAR4 0xC
#define REG_DTCCR 0x10 /* Dump Tally Counter Command Register */
#define REG_TNPDS 0x20 /* Transmit Normal Priority Descriptors (0x20-0x27) */
#define REG_THPDS 0x28 /* Transmit High Priority Descriptors (0x28-0x2F) */
#define REG_CMD 0x37
#define REG_TPPOLL 0x38
#define REG_IMR 0x3C
#define REG_ISR 0x3E
#define REG_TXCFG 0x40
#define REG_RXCFG 0x44
#define REG_MPC 0x4C
#define REG_9346CR 0x50
#define REG_CONFIG1 0x52
#define REG_PHYSTATUS 0x6C
#define REG_RMS 0xDA /* Receive (Rx) Packet Maximum Size (0xDAh-0x0DB) */
#define REG_CPLUSCMD 0xE0
#define REG_RDSAR 0xE4 /* Receive Descriptor Start Address */
#define REG_TMS 0xEC   /* max tx packet size */

#define FIRST_SEGMENT_DESCRIPTOR (1 << 29)
#define LAST_SEGMENT_DESCRIPTOR (1 << 28)
#define OWNERSHIP_BIT (1 << 31)
#define ERROR_SUMMARY_BIT (1 << 21)
#define END_RING_DESCRIPTOR (1 << 30)

#define MAX_RX_BUFFER_SIZE 0x1FFF
#define MAX_TX_BUFFER_SIZE 0x1000

#define RX_BUFFERS_COUNT 256
#define TX_BUFFERS_COUNT 256

#define CONTROLLER_REVISION_MASK 0xfc8

enum class InterruptFlag {
    ReceiveOK = (1 << 0),
    ReceiveError = (1 << 1),
    TransmitOK = (1 << 2),
    TransmitError = (1 << 3),
    RXBufferOverflow = (1 << 4),
    LinkChange = (1 << 5),
    RxFIFOOverflow = (1 << 6),
    TXDescriptorUnavailable = (1 << 7),
    SoftwareInterrupt = (1 << 8),
    Timeout = (1 << 14),
    SystemError = (1 << 15)
};

enum class PHYStatus {
    FullDuplex = (1 << 0),
    LinkOK = (1 << 1),
    Link10Mbps = (1 << 2),
    Link100Mbps = (1 << 3),
    Link1000Mbps = (1 << 4),
    ReceiveFlowControl = (1 << 5),
    TransmitFlowControl = (1 << 6),
    TBI = (1 << 7),
};

enum class Command {
    TransmitEnable = (1 << 2),
    ReceiverEnable = (1 << 3),
    Reset = (1 << 4)
};

enum class OperationMode {
    Mode93C46 = 0b10000000,
    AutoLoad = 0b01000000,
    Configuration = 0b11000000,
};

enum class Config1 {
    IOMapped = 0b100,
    MemoryMapped = 0b1000,
};

enum class TransmitPriorityPolling {
    HighPriorityQueue = 0b10000000,
    NormalPriorityQueue = 0b01000000,
    ForcedSoftwareInterrupt = 1
};

enum class TransmitConfiguration {
    MaxDMAUnlimited = 0x0700,
};

enum class ReceiveConfiguartion {
    MaxDMAUnlimited = 0b111 << 8,
    NoRXThreshold = 0b111 << 13,
    AcceptAllPackets = 1,
    AcceptPhysicalMatchPackets = (1 << 1),
    AcceptMulticastPackets = (1 << 2),
    AcceptBroadcastPackets = (1 << 3),
    AcceptRunt = (1 << 4),
    AcceptError = (1 << 5)
};

struct [[gnu::packed]] TallyCounters {
    u64 tx_packets_ok;
    u64 rx_packets_ok;
    u64 tx_errors;
    u32 rx_errors;
    u16 missed_packets;
    u16 frame_alignment_errors;
    u32 tx_packets_ok_with_collision;
    u32 tx_packets_ok_with_collisions;
    u64 rx_packets_matched_destination_ok;
    u64 rx_packets_broadcast_destination_ok;
    u32 rx_packets_multicast_destination_ok;
    u16 tx_abort_packets;
    u16 tx_underrun_packets;
};

class RTL8169EntryCleaner {
public:
    RTL8169EntryCleaner(volatile RTL8169NetworkAdapter::RXDescriptor& descriptor)
        : m_descriptor(descriptor)
    {
    }
    ~RTL8169EntryCleaner()
    {
        u32 eor = m_descriptor.attributes & END_RING_DESCRIPTOR;

        m_descriptor.vlan = 0;
        full_memory_barrier();
        m_descriptor.attributes = OWNERSHIP_BIT | (PAGE_SIZE - 1) | eor;
    }

private:
    volatile RTL8169NetworkAdapter::RXDescriptor& m_descriptor;
};

void RTL8169NetworkAdapter::detect()
{
    static const PCI::ID rtl8169_id = { 0x10EC, 0x8169 };
    PCI::enumerate([&](const PCI::Address& address, PCI::ID id) {
        if (address.is_null())
            return;
        if (id != rtl8169_id)
            return;
        u8 irq = PCI::get_interrupt_line(address);
        [[maybe_unused]] auto& unused = adopt(*new RTL8169NetworkAdapter(address, irq)).leak_ref();
    });
}

RTL8169NetworkAdapter::ControllerRevisionID RTL8169NetworkAdapter::get_revision_id() const
{
    return (RTL8169NetworkAdapter::ControllerRevisionID)((in32(REG_TXCFG) >> 20) & CONTROLLER_REVISION_MASK);
}

RTL8169NetworkAdapter::RTL8169NetworkAdapter(PCI::Address address, u8 irq)
    : PCI::Device(address, irq)
    , m_io_base(PCI::get_BAR0(pci_address()) & ~1)
    , m_packet_buffer(MM.allocate_kernel_region(page_round_up(PAGE_SIZE * 4), "RTL8169 Packet Buffer", Region::Access::Read | Region::Access::Write, AllocationStrategy::AllocateNow, Region::Cacheable::Yes).release_nonnull())
    , m_rx_descriptors(MM.allocate_kernel_region(page_round_up(PAGE_SIZE), "RTL8169 RX Descriptors", Region::Access::Read | Region::Access::Write, AllocationStrategy::AllocateNow, Region::Cacheable::Yes).release_nonnull())
    , m_tx_descriptors(MM.allocate_kernel_region(page_round_up(PAGE_SIZE), "RTL8169 TX Descriptors", Region::Access::Read | Region::Access::Write, AllocationStrategy::AllocateNow, Region::Cacheable::Yes).release_nonnull())
{
    VERIFY((in8(REG_CONFIG1) & (u8)Config1::MemoryMapped));
    m_operational_registers = MM.allocate_kernel_region(PhysicalAddress(PCI::get_BAR1(pci_address())).page_base(), PAGE_SIZE, "RTL8169 Registers", Region::Access::Read | Region::Access::Write);

    m_revision_id = get_revision_id();
    // Note: 0 value represents RTL8169, which we don't support.
    if (m_revision_id == ControllerRevisionID::Invalid)
        return;

    set_interface_name("rtl8169");
    dmesgln("RTL8169: Found @ {}", pci_address());

    enable_bus_mastering(pci_address());
    m_interrupt_line = PCI::get_interrupt_line(pci_address());
    dmesgln("RTL8169: port base: {}", m_io_base);
    dmesgln("RTL8169: Interrupt line: {}", m_interrupt_line);

    for (size_t index = 0; index < RX_BUFFERS_COUNT; index++) {
        m_tx_buffers.append(MM.allocate_kernel_region(page_round_up(PAGE_SIZE), "RTL8169 TX Buffer", Region::Access::Read | Region::Access::Write, AllocationStrategy::AllocateNow).release_nonnull());
        m_rx_buffers.append(MM.allocate_kernel_region(page_round_up(PAGE_SIZE), "RTL8169 RX Buffer", Region::Access::Read | Region::Access::Write, AllocationStrategy::AllocateNow).release_nonnull());
    }
    reset();

    read_mac_address();
    const auto& mac = mac_address();
    dmesgln("RTL8169: MAC address: {}", mac.to_string().characters());

    m_link_up = (phy_status() & (u8)PHYStatus::LinkOK) != 0;
    enable_irq();
}

u8 RTL8169NetworkAdapter::phy_status() const
{
    return in8(REG_PHYSTATUS);
}

void RTL8169NetworkAdapter::clear_interrupt_status()
{
    out16(REG_ISR, 0xc0ff);
    pci_commit();
}

bool RTL8169NetworkAdapter::is_interrupt_status_clear(u16 interrupt_status) const
{
    return (interrupt_status & ((u16)InterruptFlag::ReceiveOK | (u16)InterruptFlag::ReceiveError | (u16)InterruptFlag::TransmitOK | (u16)InterruptFlag::TransmitError | (u16)InterruptFlag::RXBufferOverflow | (u16)InterruptFlag::LinkChange | (u16)InterruptFlag::RxFIFOOverflow | (u16)InterruptFlag::TXDescriptorUnavailable | (u16)InterruptFlag::Timeout | (u16)InterruptFlag::SystemError)) == 0;
}

RTL8169NetworkAdapter::~RTL8169NetworkAdapter()
{
}

void RTL8169NetworkAdapter::handle_irq(const RegisterState&)
{
    for (;;) {
        u16 status = in16(REG_ISR);
        clear_interrupt_status();

        m_entropy_source.add_random_event(status);

        dbgln_if(RTL8169_DEBUG, "RTL8169NetworkAdapter::handle_irq status={:x}", status);
        if (is_interrupt_status_clear(status)) {
            break;
        }

        if (status & (u16)InterruptFlag::ReceiveOK) {

            dbgln_if(RTL8169_DEBUG,"RTL8169NetworkAdapter: rx ready");
            receive();
        }
        if (status & (u16)InterruptFlag::Timeout) {
            dbgln_if(RTL8169_DEBUG, "RTL8169NetworkAdapter: timeout");
        }
        if (status & (u16)InterruptFlag::ReceiveError) {
            dmesgln("RTL8169NetworkAdapter: rx error - resetting device");
            reset();
        }
        if (status & (u16)InterruptFlag::TransmitOK) {
            dbgln_if(RTL8169_DEBUG, "RTL8169NetworkAdapter: rx ready");
        }
        if (status & (u16)InterruptFlag::TransmitError) {
            dbgln_if(RTL8169_DEBUG, "RTL8169NetworkAdapter: tx error - resetting device");
            reset();
        }
        if (status & (u16)InterruptFlag::RxFIFOOverflow) {
            dbgln_if(RTL8169_DEBUG, "RTL8169NetworkAdapter: rx fifo overflow");
        }
        if (status & (u16)InterruptFlag::RXBufferOverflow) {
            dbgln_if(RTL8169_DEBUG, "RTL8169NetworkAdapter: rx buffer overflow");
            VERIFY_NOT_REACHED();
            break;
        }
        if (status & (u16)InterruptFlag::LinkChange) {
            m_link_up = (phy_status() & (u8)PHYStatus::LinkOK) != 0;
            dbgln_if(RTL8169_DEBUG, "RTL8169NetworkAdapter: link status changed up={} full duplex? {}, gigabit? {}", m_link_up, ((phy_status() & 1) ? "yes" : "no"), ((phy_status() & (1 << 4)) ? "yes" : "no"));
            dbgln_if(RTL8169_DEBUG, "RTL8169NetworkAdapter: RxFlow ? {}, Tx Flow? {}", ((phy_status() & (1 << 5)) ? "yes" : "no"), ((phy_status() & (1 << 6)) ? "yes" : "no"));
        }

        if (status & (u16)InterruptFlag::SystemError) {
            dbgln_if(RTL8169_DEBUG, "RTL8169NetworkAdapter: system error - resetting device");
            reset();
        }
    }
}

void RTL8169NetworkAdapter::set_rx_descriptors_default_state() const
{
    auto rx_descriptors = (volatile RXDescriptor*)(m_rx_descriptors->vaddr().as_ptr());
    for (size_t index = 0; index < RX_BUFFERS_COUNT - 1; index++) {
        auto& rx_descriptor = rx_descriptors[index];
        rx_descriptor.buffer_address_low = m_rx_buffers[index].physical_page(0)->paddr().get();
        rx_descriptor.attributes = (PAGE_SIZE - 1) | OWNERSHIP_BIT;
    }
    auto& last_rx_descriptor = rx_descriptors[RX_BUFFERS_COUNT - 1];
    last_rx_descriptor.buffer_address_low = m_rx_buffers[RX_BUFFERS_COUNT - 1].physical_page(0)->paddr().get();
    last_rx_descriptor.attributes = (PAGE_SIZE - 1) | END_RING_DESCRIPTOR | OWNERSHIP_BIT;
}

void RTL8169NetworkAdapter::set_tx_descriptors_default_state() const
{
    auto tx_descriptors = (volatile TXDescriptor*)(m_tx_descriptors->vaddr().as_ptr());
    for (size_t index = 0; index < TX_BUFFERS_COUNT; index++) {
        auto& tx_descriptor = tx_descriptors[index];
        tx_descriptor.buffer_address_low = m_tx_buffers[index].physical_page(0)->paddr().get();
        tx_descriptor.attributes = 0;
    }
}

void RTL8169NetworkAdapter::lock_config_registers()
{
    out8(REG_9346CR, 0);
}
void RTL8169NetworkAdapter::unlock_config_registers()
{
    out8(REG_9346CR, (u8)OperationMode::Configuration);
}

void RTL8169NetworkAdapter::invoke_reset_command()
{
    out8(REG_CMD, (u8)Command::Reset);
    while ((in8(REG_CMD) & (u8)Command::Reset) != 0)
        ;
}

void RTL8169NetworkAdapter::invoke_wakeup()
{
    out8(0x82, 1);
}

void RTL8169NetworkAdapter::reset()
{
    invoke_reset_command();
    // reset descriptors
    set_rx_descriptors_default_state();
    set_tx_descriptors_default_state();

    invoke_wakeup();

    unlock_config_registers();

    // turn on multicast
    out32(REG_MAR0, 0xffffffff);
    out32(REG_MAR4, 0xffffffff);

    out16(REG_CPLUSCMD, (1 << 5) | (1 << 6));
    // enable rx/tx
    out8(REG_CMD, (u8)Command::ReceiverEnable | (u8)Command::TransmitEnable);

    // set maximum rx packet size
    out16(REG_RMS, MAX_RX_BUFFER_SIZE);

    // set maximum tx packet size, only if the device is RTL8169SB
    if (get_revision_id() == ControllerRevisionID::RTL8169sb)
        out16(REG_TMS, 0x3B);

    // set up rx descriptors
    out32(REG_TNPDS, m_tx_descriptors->physical_page(0)->paddr().get());
    out32(REG_TNPDS + 4, 0);

    // set up tx descriptors
    out32(REG_RDSAR, m_rx_descriptors->physical_page(0)->paddr().get());
    out32(REG_RDSAR + 4, 0);

    out32(REG_THPDS, 0);
    out32(REG_THPDS + 4, 0);

    // reset missed packet counter
    out16(REG_MPC, 0);
    out8(REG_MPC + 1, 0);

    out32(REG_RXCFG,
        ((u32)ReceiveConfiguartion::AcceptPhysicalMatchPackets
            | (u32)ReceiveConfiguartion::AcceptMulticastPackets
            | (u32)ReceiveConfiguartion::AcceptBroadcastPackets
            | (u32)ReceiveConfiguartion::MaxDMAUnlimited
            | (u32)ReceiveConfiguartion::NoRXThreshold));
    out32(REG_TXCFG, (u32)TransmitConfiguration::MaxDMAUnlimited);

    lock_config_registers();

    // disable interrupt coalescing
    out16(226, 0);

    // choose irqs, then clear any pending
    out16(REG_IMR,
        (u16)InterruptFlag::ReceiveOK
            | (u16)InterruptFlag::ReceiveError
            | (u16)InterruptFlag::TransmitOK
            | (u16)InterruptFlag::TransmitError
            | (u16)InterruptFlag::RXBufferOverflow
            | (u16)InterruptFlag::LinkChange
            | (u16)InterruptFlag::RxFIFOOverflow
            | (u16)InterruptFlag::TXDescriptorUnavailable
            | (u16)InterruptFlag::Timeout
            | (u16)InterruptFlag::SystemError);
    out16(REG_ISR, 0xffff);

    out16(REG_CPLUSCMD, 0);
    pci_commit();
    // enable rx/tx
    out8(REG_CMD, (u8)Command::ReceiverEnable | (u8)Command::TransmitEnable);
}

void RTL8169NetworkAdapter::notify_waiting_packets()
{
    full_memory_barrier();
    out8(REG_TPPOLL, (u8)TransmitPriorityPolling::NormalPriorityQueue);
}

void RTL8169NetworkAdapter::read_mac_address()
{
    MACAddress mac {};
    for (int i = 0; i < 6; i++)
        mac[i] = in8(REG_MAC + i);
    set_mac_address(mac);
}

Optional<size_t> RTL8169NetworkAdapter::find_first_available_tx_segment_descriptor() const
{
    auto tx_descriptors = (volatile TXDescriptor*)(m_tx_descriptors->vaddr().as_ptr());
    for (size_t index = 0; index < TX_BUFFERS_COUNT - 1; index++) {
        auto& tx_descriptor = tx_descriptors[index];
        if (!(tx_descriptor.attributes & OWNERSHIP_BIT))
            return index;
    }
    return {};
}

void RTL8169NetworkAdapter::pci_commit() const
{
    /* Read an arbitrary register to commit a preceding PCI write */
    in8(REG_CMD);
}

void RTL8169NetworkAdapter::send_raw(ReadonlyBytes payload)
{
    if (!m_link_up) {
        dbgln_if(RTL8169_DEBUG, "RTL8169NetworkAdapter::send_raw - link down");
        return;
    }

    VERIFY(payload.size() < MAX_TX_BUFFER_SIZE);
    auto descriptor_index = find_first_available_tx_segment_descriptor();
    if (!descriptor_index.has_value()) {
        dmesgln("RTL8169NetworkAdapter::send_raw - no available descriptor!");
        return;
    }
    auto& tx_descriptor = (volatile TXDescriptor&)((m_tx_descriptors->vaddr().as_ptr())[descriptor_index.value()]);

    VERIFY(!(tx_descriptor.attributes & OWNERSHIP_BIT));

    full_memory_barrier();

    tx_descriptor.attributes = payload.size() | END_RING_DESCRIPTOR | OWNERSHIP_BIT | FIRST_SEGMENT_DESCRIPTOR | LAST_SEGMENT_DESCRIPTOR;
    memcpy(m_tx_buffers[descriptor_index.value()].vaddr().as_ptr(), payload.data(), payload.size());
    memset(m_tx_buffers[descriptor_index.value()].vaddr().as_ptr() + payload.size(), 0, PAGE_SIZE - payload.size());

    full_memory_barrier();

    notify_waiting_packets();
    while ((tx_descriptor.attributes & OWNERSHIP_BIT) != 0)
        ;
    full_memory_barrier();
    tx_descriptor.attributes = 0;
}

void RTL8169NetworkAdapter::restore_rx_descriptors_default_state(size_t first_index, size_t last_index) const
{
    full_memory_barrier();
    auto rx_descriptors = (volatile RXDescriptor*)(m_rx_descriptors->vaddr().as_ptr());
    if (first_index == last_index) {
        full_memory_barrier();
        auto& status_rx_descriptor = rx_descriptors[first_index];
        status_rx_descriptor.buffer_address_low = m_rx_buffers[first_index].physical_page(0)->paddr().get();
        status_rx_descriptor.attributes = (PAGE_SIZE - 1) | OWNERSHIP_BIT | (first_index == (RX_BUFFERS_COUNT - 1) ? END_RING_DESCRIPTOR : 0);
        return;
    }
    for (size_t index = first_index; index < last_index; index++) {
        full_memory_barrier();
        auto& status_rx_descriptor = rx_descriptors[index];
        status_rx_descriptor.buffer_address_low = m_rx_buffers[index].physical_page(0)->paddr().get();
        status_rx_descriptor.attributes = (PAGE_SIZE - 1) | OWNERSHIP_BIT | (index == (RX_BUFFERS_COUNT - 1) ? END_RING_DESCRIPTOR : 0);
    }
    return;
}

void RTL8169NetworkAdapter::receive()
{
    size_t count;
    size_t limit = 16;
    auto rx_descriptors = (volatile RXDescriptor*)(m_rx_descriptors->vaddr().as_ptr());
    size_t rx_buffer_offset = 0;
    for (count = 0; count < limit; count++, m_rx_count++) {
        if (rx_descriptors[m_rx_count % RX_BUFFERS_COUNT].attributes & OWNERSHIP_BIT) {
            break;
        }
        full_memory_barrier();
        {
            auto& current_descriptor = rx_descriptors[m_rx_count % RX_BUFFERS_COUNT];
            RTL8169EntryCleaner cleaner(current_descriptor);
            if (current_descriptor.attributes & ERROR_SUMMARY_BIT) {
                dbgln("RTL8169: Corrupted packet received!");
                m_rx_error_count++;
                return;
            }
            if ((current_descriptor.attributes & (LAST_SEGMENT_DESCRIPTOR | FIRST_SEGMENT_DESCRIPTOR)) != (LAST_SEGMENT_DESCRIPTOR | FIRST_SEGMENT_DESCRIPTOR)) {
                dbgln("RTL8169: Incorrect MTU Packet received!");
                dbgln("RTL8169: {}", rx_descriptors->attributes & 0xfffffffff);
                VERIFY_NOT_REACHED();
            }
            VERIFY(rx_buffer_offset < m_packet_buffer->size());
            auto* start_of_packet = m_rx_buffers[m_rx_count % RX_BUFFERS_COUNT].vaddr().as_ptr();
            memcpy(m_packet_buffer->vaddr().as_ptr() + rx_buffer_offset, (const u8*)(start_of_packet), (size_t)(current_descriptor.attributes & (MAX_RX_BUFFER_SIZE)));
            rx_buffer_offset += ((size_t)current_descriptor.attributes & (MAX_RX_BUFFER_SIZE));
            did_receive({ m_packet_buffer->vaddr().as_ptr(), rx_buffer_offset });
        }
    }
}

void RTL8169NetworkAdapter::out8(u16 address, u8 data)
{
    if (m_operational_registers) {
        *(volatile u8*)m_operational_registers->vaddr().offset(address).as_ptr() = data;
        return;
    }
    m_io_base.offset(address).out(data);
}

void RTL8169NetworkAdapter::out16(u16 address, u16 data)
{
    if (m_operational_registers) {
        *(volatile u16*)m_operational_registers->vaddr().offset(address).as_ptr() = data;
        return;
    }
    m_io_base.offset(address).out(data);
}

void RTL8169NetworkAdapter::out32(u16 address, u32 data)
{
    if (m_operational_registers) {
        *(volatile u32*)m_operational_registers->vaddr().offset(address).as_ptr() = data;
        return;
    }
    m_io_base.offset(address).out(data);
}

u8 RTL8169NetworkAdapter::in8(u16 address) const
{
    if (m_operational_registers) {
        return *(volatile u8*)m_operational_registers->vaddr().offset(address).as_ptr();
    }
    return m_io_base.offset(address).in<u8>();
}

u16 RTL8169NetworkAdapter::in16(u16 address) const
{
    if (m_operational_registers) {
        return *(volatile u16*)m_operational_registers->vaddr().offset(address).as_ptr();
    }
    return m_io_base.offset(address).in<u16>();
}

u32 RTL8169NetworkAdapter::in32(u16 address) const
{
    if (m_operational_registers) {
        return *(volatile u32*)m_operational_registers->vaddr().offset(address).as_ptr();
    }
    return m_io_base.offset(address).in<u32>();
}

}
