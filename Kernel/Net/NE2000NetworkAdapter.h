/*
 * Copyright (c) 2021, the SerenityOS developers.
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

class NE2000NetworkAdapter final : public NetworkAdapter
    , public PCI::Device
    , public IRQHandler {
public:
    static RefPtr<NE2000NetworkAdapter> try_to_initialize(PCI::DeviceIdentifier const&);

    virtual ~NE2000NetworkAdapter() override;

    virtual void send_raw(ReadonlyBytes) override;
    virtual bool link_up() override
    {
        // Pure NE2000 doesn't seem to have a link status indicator, so
        // just assume that it's up.
        return true;
    }
    virtual i32 link_speed() override
    {
        // Can only do 10mbit..
        return 10;
    }
    virtual bool link_full_duplex() override { return true; }

    virtual StringView purpose() const override { return class_name(); }

private:
    NE2000NetworkAdapter(PCI::Address, u8, NonnullOwnPtr<KString>);
    virtual bool handle_irq(const RegisterState&) override;
    virtual StringView class_name() const override { return "NE2000NetworkAdapter"sv; }

    int ram_test();
    void reset();

    void rdma_read(size_t address, Bytes payload);
    void rdma_write(size_t address, ReadonlyBytes payload);

    void receive();

    void out8(u16 address, u8 data);
    void out16(u16 address, u16 data);
    u8 in8(u16 address);
    u16 in16(u16 address);

    IOAddress m_io_base;
    int m_ring_read_ptr;
    u8 m_interrupt_line { 0 };

    MACAddress m_mac_address;
    EntropySource m_entropy_source;

    WaitQueue m_wait_queue;
};
}
