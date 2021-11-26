/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <AK/Ptr32.h>
#include <AK/Types.h>
#include <Kernel/Bus/USB/USBTransfer.h>

namespace Kernel::USB {

enum class PacketID : u8 {
    IN = 0x69,
    OUT = 0xe1,
    SETUP = 0x2d
};

// Transfer Descriptor register bit offsets/masks
constexpr u16 TD_CONTROL_STATUS_ACTLEN = 0x7ff;
constexpr u8 TD_CONTROL_STATUS_ACTIVE_SHIFT = 23;
constexpr u8 TD_CONTROL_STATUS_INT_ON_COMPLETE_SHIFT = 24;
constexpr u8 TD_CONTROL_STATUS_ISOCHRONOUS_SHIFT = 25;
constexpr u8 TD_CONTROL_STATUS_LS_DEVICE_SHIFT = 26;
constexpr u8 TD_CONTROL_STATUS_ERR_CTR_SHIFT_SHIFT = 27;
constexpr u8 TD_CONTROL_STATUS_SPD_SHIFT = 29;

constexpr u8 TD_TOKEN_PACKET_ID_SHIFT = 0;
constexpr u8 TD_TOKEN_DEVICE_ADDR_SHIFT = 8;
constexpr u8 TD_TOKEN_ENDPOINT_SHIFT = 15;
constexpr u8 TD_TOKEN_DATA_TOGGLE_SHIFT = 19;
constexpr u8 TD_TOKEN_MAXLEN_SHIFT = 21;

//
// Transfer Descriptor
//
// Describes a single transfer event from, or to the Universal Serial Bus.
// These are, generally, attached to Queue Heads, and then executed by the
// USB Host Controller.
// Must be 16-byte aligned
//
struct QueueHead;
struct alignas(16) TransferDescriptor final {
    enum LinkPointerBits {
        Terminate = 1,
        QHSelect = 2,
        DepthFlag = 4,
    };

    enum StatusBits {
        Reserved = (1 << 16),
        BitStuffError = (1 << 17),
        CRCTimeoutError = (1 << 18),
        NAKReceived = (1 << 19),
        BabbleDetected = (1 << 20),
        DataBufferError = (1 << 21),
        Stalled = (1 << 22),
        Active = (1 << 23),
        ErrorMask = BitStuffError | CRCTimeoutError | NAKReceived | BabbleDetected | DataBufferError | Stalled
    };

    enum ControlBits {
        InterruptOnComplete = (1 << 24),
        IsochronousSelect = (1 << 25),
        LowSpeedDevice = (1 << 26),
        ShortPacketDetect = (1 << 29),
    };

    TransferDescriptor() = delete;
    TransferDescriptor(u32 paddr)
        : m_paddr(paddr)
    {
    }
    ~TransferDescriptor() = delete; // Prevent anything except placement new on this object

    u32 link_ptr() const { return m_link_ptr; }
    u32 paddr() const { return m_paddr; }
    u32 status() const { return m_control_status; }
    u32 token() const { return m_token; }
    u32 buffer_ptr() const { return m_buffer_ptr; }
    u16 actual_packet_length() const { return (m_control_status + 1) & 0x7ff; }

    bool in_use() const { return m_in_use; }
    bool stalled() const { return m_control_status & StatusBits::Stalled; }
    bool last_in_chain() const { return m_link_ptr & LinkPointerBits::Terminate; }
    bool active() const { return m_control_status & StatusBits::Active; }

    void set_active()
    {
        u32 ctrl = m_control_status;
        ctrl |= StatusBits::Active;
        m_control_status = ctrl;
    }

    void set_isochronous()
    {
        u32 ctrl = m_control_status;
        ctrl |= ControlBits::IsochronousSelect;
        m_control_status = ctrl;
    }

    void set_interrupt_on_complete()
    {
        u32 ctrl = m_control_status;
        ctrl |= ControlBits::InterruptOnComplete;
        m_control_status = ctrl;
    }

    void set_lowspeed()
    {
        u32 ctrl = m_control_status;
        ctrl |= ControlBits::LowSpeedDevice;
        m_control_status = ctrl;
    }

    void set_error_retry_counter(u8 num_retries)
    {
        VERIFY(num_retries <= 3);
        u32 ctrl = m_control_status;
        ctrl |= (num_retries << 27);
        m_control_status = ctrl;
    }

    void set_short_packet_detect()
    {
        u32 ctrl = m_control_status;
        ctrl |= ControlBits::ShortPacketDetect;
        m_control_status = ctrl;
    }

    void set_control_status(u32 control_status) { m_control_status = control_status; }
    void set_in_use(bool in_use) { m_in_use = in_use; }
    void set_max_len(u16 max_len)
    {
        VERIFY(max_len < 0x500 || max_len == 0x7ff);
        m_token |= (max_len << 21);
    }

    void set_device_endpoint(u8 endpoint)
    {
        VERIFY(endpoint <= 0xf);
        m_token |= (endpoint << 18);
    }

    void set_device_address(u8 address)
    {
        VERIFY(address <= 0x7f);
        m_token |= (address << 8);
    }

    void set_data_toggle(bool toggle)
    {
        m_token |= ((toggle ? (1 << 19) : 0));
    }

    void set_packet_id(PacketID pid) { m_token |= static_cast<u32>(pid); }
    void link_queue_head(u32 qh_paddr)
    {
        m_link_ptr = qh_paddr;
        m_link_ptr |= LinkPointerBits::QHSelect;
    }

    void print()
    {
        dbgln("UHCI: TD({:#04x}) @ {:#04x}: link_ptr={:#04x}, status={:#04x}, token={:#04x}, buffer_ptr={:#04x}", this, m_paddr, m_link_ptr, (u32)m_control_status, m_token, m_buffer_ptr);

        // Now let's print the flags!
        dbgln("UHCI: TD({:#04x}) @ {:#04x}: link_ptr={}{}{}, status={}{}{}{}{}{}{}",
            this,
            m_paddr,
            (last_in_chain()) ? "T " : "",
            (m_link_ptr & static_cast<u32>(LinkPointerBits::QHSelect)) ? "QH " : "",
            (m_link_ptr & static_cast<u32>(LinkPointerBits::DepthFlag)) ? "Vf " : "",
            (m_control_status & static_cast<u32>(StatusBits::BitStuffError)) ? "BITSTUFF " : "",
            (m_control_status & static_cast<u32>(StatusBits::CRCTimeoutError)) ? "CRCTIMEOUT " : "",
            (m_control_status & static_cast<u32>(StatusBits::NAKReceived)) ? "NAK " : "",
            (m_control_status & static_cast<u32>(StatusBits::BabbleDetected)) ? "BABBLE " : "",
            (m_control_status & static_cast<u32>(StatusBits::DataBufferError)) ? "DATAERR " : "",
            (stalled()) ? "STALL " : "",
            (active()) ? "ACTIVE " : "");
    }

    // FIXME: For the love of God, use AK SMART POINTERS PLEASE!!
    TransferDescriptor* next_td() { return m_next_td; }
    const TransferDescriptor* next_td() const { return m_next_td; }
    void set_next_td(TransferDescriptor* td) { m_next_td = td; }

    TransferDescriptor* prev_td() { return m_prev_td; }
    const TransferDescriptor* prev_td() const { return m_prev_td; }
    void set_previous_td(TransferDescriptor* td) { m_prev_td = td; }

    void insert_next_transfer_descriptor(TransferDescriptor* td)
    {
        m_link_ptr = td->paddr();
        td->set_previous_td(this);
        set_next_td(td);

        // Let's set some bits for the link ptr
        m_link_ptr |= static_cast<u32>(LinkPointerBits::DepthFlag);
    }

    void terminate() { m_link_ptr |= static_cast<u32>(LinkPointerBits::Terminate); }

    void set_buffer_address(Ptr32<u8> buffer)
    {
        u8* buffer_address = &*buffer;
        m_buffer_ptr = reinterpret_cast<uintptr_t>(buffer_address);
    }

    // DEBUG FUNCTIONS!
    void set_token(u32 token)
    {
        m_token = token;
    }

    void set_status(u32 status)
    {
        m_control_status = status;
    }

    void free()
    {
        m_link_ptr = 0;
        m_control_status = 0;
        m_token = 0;
        m_in_use = false;
        m_next_td = nullptr;
        m_prev_td = nullptr;
    }

private:
    u32 m_link_ptr;                // Points to another Queue Head or Transfer Descriptor
    volatile u32 m_control_status; // Control and status bits
    u32 m_token;                   // Contains all information required to fill in a USB Start Token
    u32 m_buffer_ptr;              // Points to a data buffer for this transaction (i.e what we want to send or recv)

    // These values will be ignored by the controller, but we can use them for configuration/bookkeeping
    u32 m_paddr;                                     // Physical address where this TransferDescriptor is located
    Ptr32<TransferDescriptor> m_next_td { nullptr }; // Pointer to first TD
    Ptr32<TransferDescriptor> m_prev_td { nullptr }; // Pointer to first TD
    bool m_in_use;                                   // Has this TD been allocated (and therefore in use)?
};

static_assert(AssertSize<TransferDescriptor, 32>()); // Transfer Descriptor is always 8 Dwords

//
// Queue Head
//
// Description here please!
//
struct alignas(16) QueueHead {
    enum class LinkPointerBits : u32 {
        Terminate = 1,
        QHSelect = 2,
    };

    QueueHead() = delete;
    QueueHead(u32 paddr)
        : m_paddr(paddr)
    {
    }
    ~QueueHead() = delete; // Prevent anything except placement new on this object

    u32 link_ptr() const { return m_link_ptr; }
    u32 element_link_ptr() const { return m_element_link_ptr; }
    u32 paddr() const { return m_paddr; }
    bool in_use() const { return m_in_use; }

    void set_in_use(bool in_use) { m_in_use = in_use; }
    void set_link_ptr(u32 val) { m_link_ptr = val; }

    // FIXME: For the love of God, use AK SMART POINTERS PLEASE!!
    QueueHead* next_qh() { return m_next_qh; }
    const QueueHead* next_qh() const { return m_next_qh; }
    void set_next_qh(QueueHead* qh) { m_next_qh = qh; }

    QueueHead* prev_qh() { return m_prev_qh; }
    const QueueHead* prev_qh() const { return m_prev_qh; }
    void set_previous_qh(QueueHead* qh)
    {
        m_prev_qh = qh;
    }

    void link_next_queue_head(QueueHead* qh)
    {
        m_link_ptr = qh->paddr();
        m_link_ptr |= static_cast<u32>(LinkPointerBits::QHSelect);
    }

    void attach_transfer_queue(QueueHead& qh)
    {
        m_element_link_ptr = qh.paddr();
        m_element_link_ptr = m_element_link_ptr | static_cast<u32>(LinkPointerBits::QHSelect);
    }

    void terminate_with_stray_descriptor(TransferDescriptor* td)
    {
        m_link_ptr = td->paddr();
        m_link_ptr |= static_cast<u32>(LinkPointerBits::Terminate);
    }

    // TODO: Should we pass in an array or vector of TDs instead????
    void attach_transfer_descriptor_chain(TransferDescriptor* td)
    {
        m_first_td = td;
        m_element_link_ptr = td->paddr();
    }

    TransferDescriptor* get_first_td()
    {
        return m_first_td;
    }

    void terminate() { m_link_ptr |= static_cast<u32>(LinkPointerBits::Terminate); }

    void terminate_element_link_ptr()
    {
        m_element_link_ptr = static_cast<u32>(LinkPointerBits::Terminate);
    }

    void set_transfer(Transfer* transfer)
    {
        m_transfer = transfer;
    }

    Transfer* transfer()
    {
        return m_transfer;
    }

    void print()
    {
        dbgln("UHCI: QH({:#04x}) @ {:#04x}: link_ptr={:#04x}, element_link_ptr={:#04x}", this, m_paddr, m_link_ptr, (FlatPtr)m_element_link_ptr);
    }

    void free()
    {
        m_link_ptr = 0;
        m_element_link_ptr = 0;
        m_first_td = nullptr;
        m_transfer = nullptr;
        m_next_qh = nullptr;
        m_prev_qh = nullptr;
        m_in_use = false;
    }

private:
    u32 m_link_ptr { 0 };                  // Pointer to the next horizontal object that the controller will execute after this one
    volatile u32 m_element_link_ptr { 0 }; // Pointer to the first data object in the queue (can be modified by hw)

    // These values will be ignored by the controller, but we can use them for configuration/bookkeeping
    // Any addresses besides `paddr` are assumed virtual and can be dereferenced
    u32 m_paddr { 0 };                                // Physical address where this QueueHead is located
    Ptr32<QueueHead> m_next_qh { nullptr };           // Next QH
    Ptr32<QueueHead> m_prev_qh { nullptr };           // Previous QH
    Ptr32<TransferDescriptor> m_first_td { nullptr }; // Pointer to first TD
    Ptr32<Transfer> m_transfer { nullptr };           // Pointer to transfer linked to this queue head
    bool m_in_use { false };                          // Is this QH currently in use?
};

static_assert(AssertSize<QueueHead, 32>()); // Queue Head is always 8 Dwords
}
