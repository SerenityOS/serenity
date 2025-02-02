/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "E1000Descriptors.h"
#include "E1000Registers.h"
#include <AK/OwnPtr.h>
#include <AK/SetOnce.h>
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
        NonnullOwnPtr<Memory::Region> tx_buffer_region,
        Memory::TypedMapping<E1000::RxDescriptor volatile[]> rx_descriptors,
        Memory::TypedMapping<E1000::TxDescriptor volatile[]> tx_descriptors);

    virtual bool handle_irq() override;
    virtual StringView class_name() const override { return "E1000NetworkAdapter"sv; }

    virtual void detect_eeprom();
    virtual u16 read_eeprom(u16 address);
    void read_mac_address();

    void initialize_rx_descriptors();
    void initialize_tx_descriptors();
    void set_tipg();

    void receive();

    static constexpr size_t number_of_rx_descriptors = 256;
    static constexpr size_t number_of_tx_descriptors = 256;

    using Register = E1000::Register;
    E1000::RegisterMap m_registers;

    Memory::TypedMapping<E1000::RxDescriptor volatile[]> m_rx_descriptors;
    Memory::TypedMapping<E1000::TxDescriptor volatile[]> m_tx_descriptors;

    NonnullOwnPtr<Memory::Region> m_rx_buffer_region;
    NonnullOwnPtr<Memory::Region> m_tx_buffer_region;
    Array<void*, number_of_rx_descriptors> m_rx_buffers;
    Array<void*, number_of_tx_descriptors> m_tx_buffers;
    SetOnce m_has_eeprom;
    bool m_link_up { false };
    EntropySource m_entropy_source;

    WaitQueue m_wait_queue;
};
}
