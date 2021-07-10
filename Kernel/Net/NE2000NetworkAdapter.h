/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Bus/PCI/Device.h>
#include <Kernel/IO.h>
#include <Kernel/Net/NetworkAdapter.h>
#include <Kernel/Random.h>

namespace Kernel {

class NE2000NetworkAdapter final : public NetworkAdapter
    , public PCI::Device {
public:
    static RefPtr<NE2000NetworkAdapter> try_to_initialize(PCI::Address);

    virtual ~NE2000NetworkAdapter() override;

    virtual void send_raw(ReadonlyBytes) override;
    virtual bool link_up() override { return m_link_up; }

    virtual StringView purpose() const override { return class_name(); }

private:
    NE2000NetworkAdapter(PCI::Address, u8 irq);
    virtual bool handle_irq(const RegisterState&) override;
    virtual const char* class_name() const override { return "NE2000NetworkAdapter"; }

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
    bool m_link_up { false };

    MACAddress m_mac_address;
    EntropySource m_entropy_source;

    WaitQueue m_wait_queue;
};
}
