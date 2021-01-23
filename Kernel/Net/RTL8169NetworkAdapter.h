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

#pragma once

#include <AK/NonnullOwnPtrVector.h>
#include <AK/OwnPtr.h>
#include <Kernel/IO.h>
#include <Kernel/Net/NetworkAdapter.h>
#include <Kernel/PCI/Access.h>
#include <Kernel/PCI/Device.h>
#include <Kernel/Random.h>

namespace Kernel {

class RTL8169EntryCleaner;
class RTL8169NetworkAdapter final : public NetworkAdapter
    , public PCI::Device {
    friend class RTL8169EntryCleaner;

private:
    enum class ControllerRevisionID : u16 {
        Invalid = 0, // Note: this value represents an old version of RTL8169, which we don't support.
        RTL8169s = 0x008,
        RTL8169sb = 0x100,
        RTL8169sc = 0x180,
        RTL8169sc2 = 0x980,
    };

    struct [[gnu::packed]] RXDescriptor {
        u32 attributes;
        u32 vlan; /* unused */
        u32 buffer_address_low;
        u32 buffer_address_high;
    };

    struct [[gnu::packed]] TXDescriptor {
        u32 attributes;
        u32 vlan; /* unused */
        u32 buffer_address_low;
        u32 buffer_address_high;
    };

public:
    static void detect();

    RTL8169NetworkAdapter(PCI::Address, u8 irq);
    virtual ~RTL8169NetworkAdapter() override;

    virtual void send_raw(ReadonlyBytes) override;
    virtual bool link_up() override { return m_link_up; }

    virtual const char* purpose() const override { return class_name(); }

private:
    virtual void handle_irq(const RegisterState&) override;
    virtual const char* class_name() const override { return "RTL8169NetworkAdapter"; }

    u8 phy_status() const;
    void clear_interrupt_status();
    bool is_interrupt_status_clear(u16 interrupt_status) const;

    Optional<size_t> find_first_available_tx_segment_descriptor() const;
    void notify_waiting_packets();

    void reset();
    void invoke_wakeup();
    void invoke_reset_command();
    void read_mac_address();

    void lock_config_registers();
    void unlock_config_registers();

    void fetch_packets_from_segments(size_t first_index, size_t last_index);
    void receive();
    Optional<size_t> find_first_rx_segment_descriptor_index() const;
    Optional<size_t> find_last_rx_segment_descriptor_index() const;

    void pci_commit() const;

    void restore_rx_descriptors_default_state(size_t first_index, size_t last_index) const;

    void set_rx_descriptors_default_state() const;
    void set_tx_descriptors_default_state() const;

    ControllerRevisionID get_revision_id() const;

    void out8(u16 address, u8 data);
    void out16(u16 address, u16 data);
    void out32(u16 address, u32 data);
    u8 in8(u16 address) const;
    u16 in16(u16 address) const;
    u32 in32(u16 address) const;

    IOAddress m_io_base;
    u8 m_interrupt_line { 0 };
    size_t m_rx_count { 0 };
    size_t m_rx_error_count { 0 };
    NonnullOwnPtrVector<Region> m_rx_buffers;
    NonnullOwnPtrVector<Region> m_tx_buffers;
    NonnullOwnPtr<Region> m_packet_buffer;
    NonnullOwnPtr<Region> m_rx_descriptors;
    NonnullOwnPtr<Region> m_tx_descriptors;
    OwnPtr<Region> m_operational_registers { nullptr };
    bool m_link_up { false };
    ControllerRevisionID m_revision_id;
    EntropySource m_entropy_source;
};
}
