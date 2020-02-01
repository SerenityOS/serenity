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

class E1000NetworkAdapter final : public NetworkAdapter
    , public IRQHandler {
public:
    static void detect(const PCI::Address&);

    E1000NetworkAdapter(PCI::Address, u8 irq);
    virtual ~E1000NetworkAdapter() override;

    virtual void send_raw(const u8*, size_t) override;
    virtual bool link_up() override;

private:
    virtual void handle_irq() override;
    virtual const char* class_name() const override { return "E1000NetworkAdapter"; }

    struct [[gnu::packed]] e1000_rx_desc
    {
        volatile uint64_t addr { 0 };
        volatile uint16_t length { 0 };
        volatile uint16_t checksum { 0 };
        volatile uint8_t status { 0 };
        volatile uint8_t errors { 0 };
        volatile uint16_t special { 0 };
    };

    struct [[gnu::packed]] e1000_tx_desc
    {
        volatile uint64_t addr { 0 };
        volatile uint16_t length { 0 };
        volatile uint8_t cso { 0 };
        volatile uint8_t cmd { 0 };
        volatile uint8_t status { 0 };
        volatile uint8_t css { 0 };
        volatile uint16_t special { 0 };
    };

    void detect_eeprom();
    u32 read_eeprom(u8 address);
    void read_mac_address();

    void write_command(u16 address, u32);
    u32 read_command(u16 address);

    void initialize_rx_descriptors();
    void initialize_tx_descriptors();

    void out8(u16 address, u8);
    void out16(u16 address, u16);
    void out32(u16 address, u32);
    u8 in8(u16 address);
    u16 in16(u16 address);
    u32 in32(u16 address);

    void receive();

    PCI::Address m_pci_address;
    u16 m_io_base { 0 };
    VirtualAddress m_mmio_base;
    OwnPtr<Region> m_mmio_region;
    u8 m_interrupt_line { 0 };
    bool m_has_eeprom { false };
    bool m_use_mmio { false };

    static const int number_of_rx_descriptors = 32;
    static const int number_of_tx_descriptors = 8;

    e1000_rx_desc* m_rx_descriptors;
    e1000_tx_desc* m_tx_descriptors;

    WaitQueue m_wait_queue;
};
