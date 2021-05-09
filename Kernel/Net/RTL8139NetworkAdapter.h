/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <Kernel/IO.h>
#include <Kernel/Net/NetworkAdapter.h>
#include <Kernel/PCI/Access.h>
#include <Kernel/PCI/Device.h>
#include <Kernel/Random.h>

namespace Kernel {

#define RTL8139_TX_BUFFER_COUNT 4

class RTL8139NetworkAdapter final : public NetworkAdapter
    , public PCI::Device {
public:
    static void detect();

    RTL8139NetworkAdapter(PCI::Address, u8 irq);
    virtual ~RTL8139NetworkAdapter() override;

    virtual void send_raw(ReadonlyBytes) override;
    virtual bool link_up() override { return m_link_up; }

    virtual const char* purpose() const override { return class_name(); }

private:
    virtual void handle_irq(const RegisterState&) override;
    virtual const char* class_name() const override { return "RTL8139NetworkAdapter"; }

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
    OwnPtr<Region> m_rx_buffer;
    u16 m_rx_buffer_offset { 0 };
    Vector<OwnPtr<Region>> m_tx_buffers;
    u8 m_tx_next_buffer { 0 };
    OwnPtr<Region> m_packet_buffer;
    bool m_link_up { false };
    EntropySource m_entropy_source;
};
}
