/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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
#include <Kernel/IRQHandler.h>
#include <Kernel/Net/NetworkAdapter.h>
#include <Kernel/PCI/Access.h>

#define RTL8139_TX_BUFFER_COUNT 4

class RTL8139NetworkAdapter final : public NetworkAdapter
    , public IRQHandler {
public:
    static OwnPtr<RTL8139NetworkAdapter> autodetect();

    RTL8139NetworkAdapter(PCI::Address, u8 irq);
    virtual ~RTL8139NetworkAdapter() override;

    virtual void send_raw(const u8*, size_t) override;
    virtual bool link_up() override { return m_link_up; }

private:
    virtual void handle_irq() override;
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

    PCI::Address m_pci_address;
    u16 m_io_base { 0 };
    u8 m_interrupt_line { 0 };
    u32 m_rx_buffer_addr { 0 };
    u16 m_rx_buffer_offset { 0 };
    u32 m_tx_buffer_addr[RTL8139_TX_BUFFER_COUNT];
    u8 m_tx_next_buffer { 0 };
    u32 m_packet_buffer { 0 };
    bool m_link_up { false };
};
