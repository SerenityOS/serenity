/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullOwnPtrVector.h>
#include <AK/OwnPtr.h>
#include <Kernel/IO.h>
#include <Kernel/Interrupts/IRQHandler.h>
#include <Kernel/Net/NetworkAdapter.h>
#include <Kernel/PCI/Access.h>
#include <Kernel/PCI/Device.h>
#include <Kernel/Random.h>

namespace Kernel {

class E1000NetworkAdapter final : public NetworkAdapter
    , public PCI::Device {
public:
    static void detect();

    E1000NetworkAdapter(PCI::Address, u8 irq);
    virtual ~E1000NetworkAdapter() override;

    virtual void send_raw(ReadonlyBytes) override;
    virtual bool link_up() override;

    virtual const char* purpose() const override { return class_name(); }

private:
    virtual void handle_irq(const RegisterState&) override;
    virtual const char* class_name() const override { return "E1000NetworkAdapter"; }

    struct [[gnu::packed]] e1000_rx_desc {
        volatile uint64_t addr { 0 };
        volatile uint16_t length { 0 };
        volatile uint16_t checksum { 0 };
        volatile uint8_t status { 0 };
        volatile uint8_t errors { 0 };
        volatile uint16_t special { 0 };
    };

    struct [[gnu::packed]] e1000_tx_desc {
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

    IOAddress m_io_base;
    VirtualAddress m_mmio_base;
    OwnPtr<Region> m_rx_descriptors_region;
    OwnPtr<Region> m_tx_descriptors_region;
    NonnullOwnPtrVector<Region> m_rx_buffers_regions;
    NonnullOwnPtrVector<Region> m_tx_buffers_regions;
    OwnPtr<Region> m_mmio_region;
    u8 m_interrupt_line { 0 };
    bool m_has_eeprom { false };
    bool m_use_mmio { false };
    EntropySource m_entropy_source;

    static constexpr size_t number_of_rx_descriptors = 32;
    static constexpr size_t number_of_tx_descriptors = 8;

    WaitQueue m_wait_queue;
};
}
