/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
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

#pragma once

#include <AK/OwnPtr.h>
#include <AK/Types.h>

namespace Kernel::USB {

enum class PacketID : u8 {
    IN = 0x69,
    OUT = 0xe1,
    SETUP = 0x2d
};

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
    enum class LinkPointerBits : u32 {
        Terminate = 1,
        QHSelect = 2,
        DepthFlag = 4,
    };

    enum class StatusBits : u32 {
        Reserved = (1 << 16),
        BitStuffError = (1 << 17),
        CRCTimeoutError = (1 << 18),
        NAKReceived = (1 << 19),
        BabbleDetected = (1 << 20),
        DataBufferError = (1 << 21),
        Stalled = (1 << 22),
        Active = (1 << 23)
    };

    enum class ControlBits : u32 {
        InterruptOnComplete = (1 << 24),
        IsochronousSelect = (1 << 25),
    };

    TransferDescriptor() = delete;
    TransferDescriptor(u32 paddr)
        : m_paddr(paddr)
    {
    }
    ~TransferDescriptor() = delete; // Prevent anything except placement new on this object

    u32 link_ptr() const { return m_link_ptr; }
    u32 paddr() const { return m_paddr; }
    u32 status() const { return (m_control_status >> 16) & 0xff; }
    u32 token() const { return m_token; }
    u32 buffer_ptr() const { return m_buffer_ptr; }

    bool in_use() const { return m_in_use; }
    bool stalled() const { return m_control_status & static_cast<u32>(StatusBits::Stalled); }
    bool last_in_chain() const { return m_link_ptr & static_cast<u32>(LinkPointerBits::Terminate); }
    bool active() const { return m_link_ptr & static_cast<u32>(StatusBits::Active); }

    void set_active()
    {
        u32 ctrl = m_control_status;
        ctrl |= static_cast<u32>(StatusBits::Active);
        m_control_status = ctrl;
    }

    void set_isochronous()
    {
        u32 ctrl = m_control_status;
        ctrl |= static_cast<u32>(ControlBits::IsochronousSelect);
        m_control_status = ctrl;
    }

    void set_control_status(u32 control_status) { m_control_status = control_status; }
    void set_in_use(bool in_use) { m_in_use = in_use; }
    void set_max_len(u16 max_len)
    {
        ASSERT(max_len < 0x500 || max_len == 0x7ff);
        m_token |= (max_len << 21);
    }

    void set_device_address(u8 address)
    {
        ASSERT(address <= 0x7f);
        m_token |= (address << 8);
    }

    void set_packet_id(PacketID pid) { m_token |= static_cast<u32>(pid); }
    void link_queue_head(u32 qh_paddr)
    {
        m_link_ptr = qh_paddr;
        m_link_ptr |= static_cast<u32>(LinkPointerBits::QHSelect);
    }

    void print()
    {
        dbgln("UHCI: TD({}) @ {}: link_ptr={}, status={}, token={}, buffer_ptr={}", this, m_paddr, m_link_ptr, (u32)m_control_status, m_token, m_buffer_ptr);

        // Now let's print the flags!
        dbgln("UHCI: TD({}) @ {}: link_ptr={}{}{}, status={}{}{}{}{}{}{}",
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

    void set_buffer_address(u32 buffer) { m_buffer_ptr = buffer; }

    // DEBUG FUNCTIONS!
    void set_token(u32 token)
    {
        m_token = token;
    }

    void set_status(u32 status)
    {
        m_control_status = status;
    }

private:
    u32 m_link_ptr;                // Points to another Queue Head or Transfer Descriptor
    volatile u32 m_control_status; // Control and status bits
    u32 m_token;                   // Contains all information required to fill in a USB Start Token
    u32 m_buffer_ptr;              // Points to a data buffer for this transaction (i.e what we want to send or recv)

    // These values will be ignored by the controller, but we can use them for configuration/bookkeeping
    u32 m_paddr;                   // Physical address where this TransferDescriptor is located
    TransferDescriptor* m_next_td; // Pointer to first TD
    TransferDescriptor* m_prev_td; // Pointer to first TD
    bool m_in_use;                 // Has this TD been allocated (and therefore in use)?
};

static_assert(sizeof(TransferDescriptor) == 32); // Transfer Descriptor is always 8 Dwords

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
        set_next_qh(qh);
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

    void terminate() { m_link_ptr |= static_cast<u32>(LinkPointerBits::Terminate); }

    void terminate_element_link_ptr()
    {
        m_element_link_ptr = static_cast<u32>(LinkPointerBits::Terminate);
    }

    // Clean the chain of transfer descriptors
    void clean_chain()
    {
        // TODO
    }

    void print()
    {
        dbgln("UHCI: QH({}) @ {}: link_ptr={}, element_link_ptr={}", this, m_paddr, m_link_ptr, (FlatPtr)m_element_link_ptr);
    }

private:
    u32 m_link_ptr { 0 };                  // Pointer to the next horizontal object that the controller will execute after this one
    volatile u32 m_element_link_ptr { 0 }; // Pointer to the first data object in the queue (can be modified by hw)

    // These values will be ignored by the controller, but we can use them for configuration/bookkeeping
    // Any addresses besides `paddr` are assumed virtual and can be dereferenced
    u32 m_paddr { 0 };                          // Physical address where this QueueHead is located
    QueueHead* m_next_qh { nullptr };           // Next QH
    QueueHead* m_prev_qh { nullptr };           // Previous QH
    TransferDescriptor* m_first_td { nullptr }; // Pointer to first TD
    bool m_in_use { false };                    // Is this QH currently in use?
};

static_assert(sizeof(QueueHead) == 32); // Queue Head is always 8 Dwords
}
