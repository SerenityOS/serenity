/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <Kernel/Arch/x86/IO.h>
#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Bus/PCI/Device.h>
#include <Kernel/Interrupts/IRQHandler.h>
#include <Kernel/Net/NetworkAdapter.h>
#include <Kernel/Random.h>

namespace Kernel {

#define RTL8139_TX_BUFFER_COUNT 4

class RTL8139NetworkAdapter final : public NetworkAdapter
    , public PCI::Device
    , public IRQHandler {
public:
    static RefPtr<RTL8139NetworkAdapter> try_to_initialize(PCI::DeviceIdentifier const&);

    virtual ~RTL8139NetworkAdapter() override;

    virtual void send_raw(ReadonlyBytes) override;
    virtual bool link_up() override { return m_link_up; }
    virtual i32 link_speed() override;
    virtual bool link_full_duplex() override;

    virtual StringView purpose() const override { return class_name(); }

private:
    RTL8139NetworkAdapter(PCI::Address, u8 irq, NonnullOwnPtr<KString>);
    virtual bool handle_irq(const RegisterState&) override;
    virtual StringView class_name() const override { return "RTL8139NetworkAdapter"sv; }

    void reset();
    void read_mac_address();

    void receive();

    void out8(u16 address, u8 data);
    void out16(u16 address, u16 data);
    void out32(u16 address, u32 data);
    u8 in8(u16 address);
    u16 in16(u16 address);
    u32 in32(u16 address);

    IOAddress m_io_base;
    u8 m_interrupt_line { 0 };
    OwnPtr<Memory::Region> m_rx_buffer;
    u16 m_rx_buffer_offset { 0 };
    Vector<OwnPtr<Memory::Region>> m_tx_buffers;
    u8 m_tx_next_buffer { 0 };
    OwnPtr<Memory::Region> m_packet_buffer;
    bool m_link_up { false };
    EntropySource m_entropy_source;
};
}
