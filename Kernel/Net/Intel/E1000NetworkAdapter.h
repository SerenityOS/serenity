/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <AK/SetOnce.h>
#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Bus/PCI/Device.h>
#include <Kernel/Interrupts/PCIIRQHandler.h>
#include <Kernel/Library/IOWindow.h>
#include <Kernel/Net/MDIO.h>
#include <Kernel/Net/NetworkAdapter.h>
#include <Kernel/Security/Random.h>
#include <Kernel/Tasks/Process.h>
#include <Kernel/Tasks/WaitQueue.h>

namespace Kernel {

class E1000NetworkAdapter
    : public NetworkAdapter
    , public MDIO::Clause22::Interface
    , public PCI::Device {
public:
    static ErrorOr<bool> probe(PCI::DeviceIdentifier const&);
    static ErrorOr<NonnullRefPtr<NetworkAdapter>> create(PCI::DeviceIdentifier const&);
    virtual ErrorOr<void> initialize(Badge<NetworkingManagement>) override;

    virtual ~E1000NetworkAdapter() override;

    virtual void send_raw(ReadonlyBytes) override;
    virtual bool link_up() override { return m_link_up; }
    virtual i32 link_speed() override;
    virtual bool link_full_duplex() override;

    virtual StringView device_name() const override { return "E1000"sv; }
    virtual Type adapter_type() const override { return Type::Ethernet; }

private:
    static constexpr size_t rx_buffer_size = 2048;
    static constexpr size_t tx_buffer_size = 2048;

    struct RxDescriptor {
        uint64_t addr { 0 };
        uint16_t length { 0 };
        uint16_t checksum { 0 };
        uint8_t status { 0 };
        uint8_t errors { 0 };
        uint16_t special { 0 };
    };
    static_assert(AssertSize<RxDescriptor, 16>());

    struct TxDescriptor {
        uint64_t addr { 0 };
        uint16_t length { 0 };
        uint8_t cso { 0 };
        uint8_t cmd { 0 };
        uint8_t status { 0 };
        uint8_t css { 0 };
        uint16_t special { 0 };
    };
    static_assert(AssertSize<TxDescriptor, 16>());

    enum class HardwareFeatures {
        None = 0u,

        MDIOAccess = 1u << 0,
        HasQueueEnableBit = 1u << 1,
        HasPreconfiguredPHYAddress = 1u << 2,
    };
    AK_ENUM_BITWISE_FRIEND_OPERATORS(E1000NetworkAdapter::HardwareFeatures);

    HardwareFeatures determine_hardware_features();

    ErrorOr<void> setup_interrupts();
    void setup_link();

    E1000NetworkAdapter(StringView, PCI::DeviceIdentifier const&,
        NonnullOwnPtr<IOWindow> registers_io_window, NonnullOwnPtr<Memory::Region> rx_buffer_region,
        NonnullOwnPtr<Memory::Region> tx_buffer_region,
        Memory::TypedMapping<RxDescriptor volatile[]> rx_descriptors,
        Memory::TypedMapping<TxDescriptor volatile[]> tx_descriptors);

    bool handle_interrupt();

    class InterruptHandler final : public PCI::IRQHandler {
    public:
        static ErrorOr<NonnullOwnPtr<InterruptHandler>> create(E1000NetworkAdapter& network_adapter, InterruptNumber interrupt_number)
        {
            auto interrupt_handler = TRY(adopt_nonnull_own_or_enomem(new (nothrow) InterruptHandler(network_adapter, interrupt_number)));
            dmesgln_pci(network_adapter, "Interrupt number: {}", interrupt_number);
            interrupt_handler->enable_irq();
            return interrupt_handler;
        }

    private:
        InterruptHandler(E1000NetworkAdapter& network_adapter, InterruptNumber interrupt_number)
            : PCI::IRQHandler(network_adapter, interrupt_number)
            , m_network_adapter(network_adapter)
        {
        }

        virtual StringView purpose() const override { return "E1000 Interrupt Handler"sv; }
        virtual bool handle_irq() override
        {
            return m_network_adapter.handle_interrupt();
        }

        E1000NetworkAdapter& m_network_adapter;
    };

    virtual StringView class_name() const override { return "E1000NetworkAdapter"sv; }

    // ^MDIO::Clause22::Interface
    virtual u16 read_phy_register(u8 phy_id, MDIO::Clause22::RegisterAddress address) override;
    virtual void write_phy_register(u8 phy_id, MDIO::Clause22::RegisterAddress address, u16 value) override;

    void read_mac_address();

    void initialize_rx_descriptors();
    void initialize_tx_descriptors();

    void out32(u16 address, u32);
    u32 in32(u16 address);

    void receive();

    static constexpr size_t number_of_rx_descriptors = 256;
    static constexpr size_t number_of_tx_descriptors = 256;

    HardwareFeatures m_hardware_features;

    NonnullOwnPtr<IOWindow> m_registers_io_window;

    Memory::TypedMapping<RxDescriptor volatile[]> m_rx_descriptors;
    Memory::TypedMapping<TxDescriptor volatile[]> m_tx_descriptors;

    NonnullOwnPtr<Memory::Region> m_rx_buffer_region;
    NonnullOwnPtr<Memory::Region> m_tx_buffer_region;
    Array<void*, number_of_rx_descriptors> m_rx_buffers;
    Array<void*, number_of_tx_descriptors> m_tx_buffers;

    bool m_link_up { false };
    EntropySource m_entropy_source;

    Mutex m_write_lock;
    WaitQueue m_wait_queue;

    OwnPtr<InterruptHandler> m_interrupt_handler;
    RefPtr<Process> m_mdio_handling_process;
};

}
