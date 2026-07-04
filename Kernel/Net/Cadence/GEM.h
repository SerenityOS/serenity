/*
 * Copyright (c) 2025-2026, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

// The Cadence GEM ethernet controller is used in e.g. Zynq UltraScale+ devices, for which some docs about the GEM are available:
// - Technical reference manual: Chapter 34 (GEM Ethernet), https://docs.amd.com/v/u/en-US/ug1085-zynq-ultrascale-trm
// - Register reference: https://docs.amd.com/r/en-US/ug1087-zynq-ultrascale-registers/GEM-Module

#include <Kernel/Memory/TypedMapping.h>
#include <Kernel/Net/MDIO.h>
#include <Kernel/Net/NetworkAdapter.h>
#include <Kernel/Tasks/Process.h>
#include <Kernel/Tasks/WaitQueue.h>

namespace Kernel {

class CadenceGEMNetworkAdapter
    : public NetworkAdapter
    , public MDIO::Clause22::Interface {
public:
    virtual ~CadenceGEMNetworkAdapter();

    static ErrorOr<NonnullRefPtr<NetworkAdapter>> create(PCI::DeviceIdentifier const&);

    virtual StringView class_name() const override { return "CadenceGEMNetworkAdapter"sv; }
    virtual Type adapter_type() const override { return Type::Ethernet; }

    virtual ErrorOr<void> initialize(Badge<NetworkingManagement>) override;

    virtual bool link_up() override { return m_link_up; }
    virtual i32 link_speed() override;
    virtual bool link_full_duplex() override;

    struct Registers;

    struct RxBufferDescriptorEntry;
    struct TxBufferDescriptorEntry;

protected:
    CadenceGEMNetworkAdapter(StringView interface_name, MACAddress, Memory::TypedMapping<Registers volatile>);

    virtual void send_raw(ReadonlyBytes) override;

    ErrorOr<void> initialize();
    ErrorOr<void> initialize_rx_descriptors();
    ErrorOr<void> initialize_tx_descriptors();

    virtual void register_and_enable_interrupt() = 0;
    virtual u32 mdio_clock_input_frequency() = 0;

    bool handle_interrupt();

    // ^MDIO::Clause22::Interface
    virtual void on_phy_link_status_change(MDIO::LinkStatus) override;
    virtual u16 read_phy_register(u8 phy_id, MDIO::Clause22::RegisterAddress address) override;
    virtual void write_phy_register(u8 phy_id, MDIO::Clause22::RegisterAddress address, u16 value) override;

private:
    bool m_link_up { false };

    RxBufferDescriptorEntry* rx_descriptor_entry(size_t index);
    TxBufferDescriptorEntry* tx_descriptor_entry(size_t index);

    static Optional<u8> determine_mdc_clock_divisor(u32 input_frequency);

    Memory::TypedMapping<Registers volatile> m_registers;

    static constexpr size_t RECEIVE_BUFFER_SIZE = 1536;
    static constexpr size_t TRANSMIT_BUFFER_SIZE = 1536;

    static constexpr size_t RX_DESCRIPTOR_COUNT = 128;
    static constexpr size_t TX_DESCRIPTOR_COUNT = 128;

    Array<OwnPtr<Memory::Region>, RX_DESCRIPTOR_COUNT> m_rx_buffers;
    OwnPtr<Memory::Region> m_rx_descriptor_list_region;

    Array<OwnPtr<Memory::Region>, TX_DESCRIPTOR_COUNT> m_tx_buffers;
    OwnPtr<Memory::Region> m_tx_descriptor_list_region;

    size_t m_rx_dequeue_index { 0 };
    size_t m_tx_enqueue_index { 0 };

    WaitQueue m_wait_queue;
    Mutex m_transmit_mutex;

    RefPtr<Process> m_mdio_handling_process;
};

}
