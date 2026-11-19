/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 * Copyright (c) 2026, Undefine <undefine@undefine.pl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <AK/Span.h>
#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Bus/PCI/Device.h>
#include <Kernel/Interrupts/IRQHandler.h>
#include <Kernel/Interrupts/PCIIRQHandler.h>
#include <Kernel/Library/IOWindow.h>
#include <Kernel/Net/NetworkAdapter.h>
#include <Kernel/Security/Random.h>

namespace Kernel {

enum class RTL8168Register : u16;

// RTL8618 / RTL8111 Driver based on https://people.freebsd.org/~wpaul/RealTek/RTL8111B_8168B_Registers_DataSheet_1.0.pdf
class RTL8168NetworkAdapter final : public NetworkAdapter
    , public PCI::Device {
public:
    static ErrorOr<bool> probe(PCI::DeviceIdentifier const&);
    static ErrorOr<NonnullRefPtr<NetworkAdapter>> create(PCI::DeviceIdentifier const&);
    virtual ErrorOr<void> initialize(Badge<NetworkingManagement>) override;

    virtual ~RTL8168NetworkAdapter() override;

    virtual void send_raw(ReadonlyBytes) override;
    virtual bool link_up() override { return m_link_up; }
    virtual bool link_full_duplex() override;
    virtual i32 link_speed() override;

    virtual StringView device_name() const override { return class_name(); }
    virtual Type adapter_type() const override { return Type::Ethernet; }

private:
    static constexpr size_t number_of_rx_descriptors = 256;
    static constexpr size_t number_of_tx_descriptors = 256;

    RTL8168NetworkAdapter(StringView, PCI::DeviceIdentifier const&, NonnullOwnPtr<IOWindow> registers_io_window);

    bool handle_interrupt();

    class InterruptHandler final : public PCI::IRQHandler {
    public:
        static ErrorOr<NonnullOwnPtr<InterruptHandler>> create(RTL8168NetworkAdapter& network_adapter, InterruptNumber interrupt_number)
        {
            auto interrupt_handler = TRY(adopt_nonnull_own_or_enomem(new (nothrow) InterruptHandler(network_adapter, interrupt_number)));
            interrupt_handler->enable_irq();
            return interrupt_handler;
        }

    private:
        InterruptHandler(RTL8168NetworkAdapter& network_adapter, InterruptNumber interrupt_number)
            : PCI::IRQHandler(network_adapter, interrupt_number)
            , m_network_adapter(network_adapter)
        {
        }

        virtual StringView purpose() const override { return "RTL8168 Interrupt Handler"sv; }
        virtual bool handle_irq() override
        {
            return m_network_adapter.handle_interrupt();
        }

        RTL8168NetworkAdapter& m_network_adapter;
    };

    virtual StringView class_name() const override { return "RTL8168NetworkAdapter"sv; }

    bool determine_supported_version() const;

    struct TXDescriptor {
        u16 frame_length; // top 2 bits are reserved
        u16 flags;
        u16 vlan_tag;
        u16 vlan_flags;
        u32 buffer_address_low;
        u32 buffer_address_high;

        // flags bit field
        static constexpr u16 Ownership = 0x8000u;
        static constexpr u16 EndOfRing = 0x4000u;
        static constexpr u16 FirstSegment = 0x2000u;
        static constexpr u16 LastSegment = 0x1000u;
        static constexpr u16 LargeSend = 0x800u;
    };

    static_assert(AssertSize<TXDescriptor, 16u>());

    struct RXDescriptor {
        u16 buffer_size; // top 2 bits are reserved
        u16 flags;
        u16 vlan_tag;
        u16 vlan_flags;
        u32 buffer_address_low;
        u32 buffer_address_high;

        // flags bit field
        static constexpr u16 Ownership = 0x8000u;
        static constexpr u16 EndOfRing = 0x4000u;
        static constexpr u16 FirstSegment = 0x2000u;
        static constexpr u16 LastSegment = 0x1000u;
        static constexpr u16 MulticastPacket = 0x800u;
        static constexpr u16 PhysicalPacket = 0x400u;
        static constexpr u16 BroadcastPacket = 0x200u;
        static constexpr u16 WatchdogTimerExpired = 0x40;
        static constexpr u16 ErrorSummary = 0x20;
        static constexpr u16 RuntPacket = 0x10;
        static constexpr u16 CRCError = 0x8;
    };

    static_assert(AssertSize<RXDescriptor, 16u>());

    enum class ChipVersion : u8 {
        Unknown = 0,

        RTL8168B = 17,

        RTL8168C_1 = 19,
        RTL8168C_2 = 20,
        RTL8168C_3 = 21,
        RTL8168C_4 = 22,

        RTL8168CP_1 = 18,
        RTL8168CP_2 = 23, // 24

        RTL8168D_1 = 25,
        RTL8168D_2 = 26,

        RTL8168DP_1 = 28,
        RTL8168DP_2 = 31,

        RTL8168E = 32, // 33
        RTL8168EVL = 34,

        RTL8168F_1 = 35,
        RTL8168F_2 = 36,

        RTL8411 = 38,

        RTL8168G = 40,
        RTL8168GU = 42,

        RTL8411B = 44,

        RTL8168H = 46,

        RTL8168EP = 51,
        RTL8168FP = 52,

        RTL8125A = 61,
        RTL8125B = 63,
        RTL8125D = 64,
        RTL8125BP = 66,

        RTL8126A = 70,

        RTL8127A = 80,
    };

    ErrorOr<ChipVersion> identify_chip_version();
    StringView possible_device_name();

    void reset();
    void pci_commit();
    void read_mac_address();
    void set_phy_speed();
    void start_hardware();
    ErrorOr<void> startup();

    void configure_phy();
    void configure_phy_b();
    void configure_phy_c_1();
    void configure_phy_c_2();
    void configure_phy_c_3();
    void configure_phy_e_2();
    void configure_phy_h_2();
    void configure_phy_8126_a();

    void rar_exgmac_set();

    void hardware_quirks();
    void hardware_quirks_b();
    void hardware_quirks_c_1();
    void hardware_quirks_c_2();
    void hardware_quirks_c_3();
    void hardware_quirks_e_2();
    void hardware_quirks_h();

    void hardware_quirks_8125_generic();
    void hardware_quirks_8125_b();
    void hardware_quirks_8126_a();

    void initialize_rx_descriptors();
    void initialize_tx_descriptors();

    void receive();

    void set_interrupt_mask(u32 mask);
    u32 read_interrupt_status();
    void set_interrupt_status(u32 status);

    void doorbell();
    u32 read_phy_status();

    void out8(RTL8168Register reg, u8 data);
    void out16(RTL8168Register reg, u16 data);
    void out32(RTL8168Register reg, u32 data);
    void out64(RTL8168Register reg, u64 data);
    u8 in8(RTL8168Register reg);
    u16 in16(RTL8168Register reg);
    u32 in32(RTL8168Register reg);

    void phy_out(u8 address, u16 data);
    u16 phy_in(u8 address);
    void phy_update(u32 address, u32 set, u32 clear);
    struct PhyRegister {
        u16 address;
        u16 data;
    };
    void phy_out_batch(ReadonlySpan<PhyRegister>);

    void extended_phy_out(u8 address, u16 data);
    u16 extended_phy_in(u8 address);
    struct EPhyUpdate {
        u32 offset;
        u16 clear;
        u16 set;
    };
    void extended_phy_initialize(ReadonlySpan<EPhyUpdate>);

    void eri_out(u32 address, u32 mask, u32 data, u32 type);
    u32 eri_in(u32 address, u32 type);
    void eri_update(u32 address, u32 mask, u32 set, u32 clear, u32 type);
    struct ExgMacRegister {
        u16 address;
        u16 mask;
        u32 value;
    };
    void exgmac_out_batch(ReadonlySpan<ExgMacRegister>);

    void csi_out(u32 address, u32 data);
    u32 csi_in(u32 address);
    void csi_enable(u32 bits);

    void ocp_out(u32 address, u32 data);
    u32 ocp_in(u32 address);
    void ocp_update(u32 address, u32 set, u32 clear);

    void ocp_phy_out(u32 address, u32 data);
    u16 ocp_phy_in(u32 address);

    OwnPtr<InterruptHandler> m_interrupt_handler;

    ChipVersion m_version { ChipVersion::Unknown };
    NonnullOwnPtr<IOWindow> m_registers_io_window;
    u32 m_ocp_base_address { 0 };
    Memory::TypedMapping<RXDescriptor volatile[]> m_rx_descriptors;
    Vector<NonnullOwnPtr<Memory::Region>> m_rx_buffers_regions;
    u16 m_rx_free_index { 0 };
    Memory::TypedMapping<TXDescriptor volatile[]> m_tx_descriptors;
    Vector<NonnullOwnPtr<Memory::Region>> m_tx_buffers_regions;
    u16 m_tx_free_index { 0 };
    bool m_link_up { false };
    EntropySource m_entropy_source;
    DeprecatedWaitQueue m_wait_queue;
};
}
