/*
 * Copyright (c) 2021, the SerenityOS developers
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
#include <Kernel/IO.h>
#include <Kernel/Net/NetworkAdapter.h>
#include <Kernel/PCI/Access.h>
#include <Kernel/PCI/Device.h>
#include <Kernel/Random.h>

namespace Kernel {

class NE2000NetworkAdapter final : public NetworkAdapter
    , public PCI::Device {
public:
    static void detect();

    NE2000NetworkAdapter(PCI::Address, u8 irq);
    virtual ~NE2000NetworkAdapter() override;

    virtual void send_raw(ReadonlyBytes) override;
    virtual bool link_up() override { return m_link_up; }

    virtual const char* purpose() const override { return class_name(); }

private:
    virtual void handle_irq(const RegisterState&) override;
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
