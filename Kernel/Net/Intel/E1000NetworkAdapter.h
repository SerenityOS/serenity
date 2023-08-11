/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Bus/PCI/Device.h>
#include <Kernel/Interrupts/IRQHandler.h>
#include <Kernel/Library/IOWindow.h>
#include <Kernel/Net/NetworkAdapter.h>
#include <Kernel/Security/Random.h>

namespace Kernel {

class E1000NetworkAdapter : public NetworkAdapter
    , public PCI::Device
    , public IRQHandler {
public:
    static ErrorOr<bool> probe(PCI::DeviceIdentifier const&);
    static ErrorOr<NonnullRefPtr<NetworkAdapter>> create(PCI::DeviceIdentifier const&);
    virtual ErrorOr<void> initialize(Badge<NetworkingManagement>) override;

    virtual ~E1000NetworkAdapter() override;

    virtual void send_raw(ReadonlyBytes) override;
    virtual bool link_up() override { return m_link_up; }
    virtual i32 link_speed() override;
    virtual bool link_full_duplex() override;

    virtual StringView purpose() const override { return class_name(); }
    virtual StringView device_name() const override { return "E1000"sv; }
    virtual Type adapter_type() const override { return Type::Ethernet; }

protected:
    static constexpr size_t rx_buffer_size = 8192;
    static constexpr size_t tx_buffer_size = 8192;

    void setup_interrupts();
    void setup_link();

    E1000NetworkAdapter(StringView, PCI::DeviceIdentifier const&, u8 irq,
        NonnullOwnPtr<IOWindow> registers_io_window, NonnullOwnPtr<Memory::Region> rx_buffer_region,
        NonnullOwnPtr<Memory::Region> tx_buffer_region, NonnullOwnPtr<Memory::Region> rx_descriptors_region,
        NonnullOwnPtr<Memory::Region> tx_descriptors_region);

    virtual bool handle_irq(RegisterState const&) override;
    virtual StringView class_name() const override { return "E1000NetworkAdapter"sv; }

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

    virtual void detect_eeprom();
    virtual u32 read_eeprom(u8 address);
    void read_mac_address();

    void initialize_rx_descriptors();
    void initialize_tx_descriptors();

    void out8(u16 address, u8);
    void out16(u16 address, u16);
    void out32(u16 address, u32);
    u8 in8(u16 address);
    u16 in16(u16 address);
    u32 in32(u16 address);

    void receive();

    static constexpr size_t number_of_rx_descriptors = 256;
    static constexpr size_t number_of_tx_descriptors = 256;

    NonnullOwnPtr<IOWindow> m_registers_io_window;

    NonnullOwnPtr<Memory::Region> m_rx_descriptors_region;
    NonnullOwnPtr<Memory::Region> m_tx_descriptors_region;
    NonnullOwnPtr<Memory::Region> m_rx_buffer_region;
    NonnullOwnPtr<Memory::Region> m_tx_buffer_region;
    Array<void*, number_of_rx_descriptors> m_rx_buffers;
    Array<void*, number_of_tx_descriptors> m_tx_buffers;
    bool m_has_eeprom { false };
    bool m_link_up { false };
    EntropySource m_entropy_source;

    WaitQueue m_wait_queue;
};
}
