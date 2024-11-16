/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <AK/Span.h>
#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Bus/PCI/Device.h>
#include <Kernel/Interrupts/IRQHandler.h>
#include <Kernel/Library/IOWindow.h>
#include <Kernel/Net/NetworkAdapter.h>
#include <Kernel/Security/Random.h>

namespace Kernel {

// RTL8618 / RTL8111 Driver based on https://people.freebsd.org/~wpaul/RealTek/RTL8111B_8168B_Registers_DataSheet_1.0.pdf
class RTL8168NetworkAdapter final : public NetworkAdapter
    , public PCI::Device
    , public IRQHandler {
public:
    static ErrorOr<bool> probe(PCI::DeviceIdentifier const&);
    static ErrorOr<NonnullRefPtr<NetworkAdapter>> create(PCI::DeviceIdentifier const&);
    virtual ErrorOr<void> initialize(Badge<NetworkingManagement>) override;

    virtual ~RTL8168NetworkAdapter() override;

    virtual void send_raw(ReadonlyBytes) override;
    virtual bool link_up() override { return m_link_up; }
    virtual bool link_full_duplex() override;
    virtual i32 link_speed() override;

    virtual StringView purpose() const override { return class_name(); }
    virtual StringView device_name() const override { return class_name(); }
    virtual Type adapter_type() const override { return Type::Ethernet; }

private:
    // FIXME: should this be increased? (maximum allowed here is 1024) - memory usage vs packet loss chance tradeoff
    static constexpr size_t number_of_rx_descriptors = 64;
    static constexpr size_t number_of_tx_descriptors = 16;

    RTL8168NetworkAdapter(StringView, PCI::DeviceIdentifier const&, u8 irq, NonnullOwnPtr<IOWindow> registers_io_window);

    virtual bool handle_irq() override;
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
        Version1 = 1,
        Version2 = 2,
        Version3 = 3,
        Version4 = 4,
        Version5 = 5,
        Version6 = 6,
        Version7 = 7,
        Version8 = 8,
        Version9 = 9,
        Version10 = 10,
        Version11 = 11,
        Version12 = 12,
        Version13 = 13,
        Version14 = 14,
        Version15 = 15,
        Version16 = 16,
        Version17 = 17,
        Version18 = 18,
        Version19 = 19,
        Version20 = 20,
        Version21 = 21,
        Version22 = 22,
        Version23 = 23,
        Version24 = 24,
        Version25 = 25,
        Version26 = 26,
        Version27 = 27,
        Version28 = 28,
        Version29 = 29,
        Version30 = 30
    };

    void identify_chip_version();
    StringView possible_device_name();

    void reset();
    void pci_commit();
    void read_mac_address();
    void set_phy_speed();
    void start_hardware();
    void startup();

    void configure_phy();
    void configure_phy_b_1();
    void configure_phy_b_2();
    void configure_phy_c_1();
    void configure_phy_c_2();
    void configure_phy_c_3();
    void configure_phy_e_2();
    void configure_phy_h_1();
    void configure_phy_h_2();

    void rar_exgmac_set();

    void hardware_quirks();
    void hardware_quirks_b_1();
    void hardware_quirks_b_2();
    void hardware_quirks_c_1();
    void hardware_quirks_c_2();
    void hardware_quirks_c_3();
    void hardware_quirks_e_2();
    void hardware_quirks_h();

    void initialize_rx_descriptors();
    void initialize_tx_descriptors();

    void receive();

    void out8(u16 address, u8 data);
    void out16(u16 address, u16 data);
    void out32(u16 address, u32 data);
    void out64(u16 address, u64 data);
    u8 in8(u16 address);
    u16 in16(u16 address);
    u32 in32(u16 address);

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

    void ocp_phy_out(u32 address, u32 data);
    u16 ocp_phy_in(u32 address);

    ChipVersion m_version { ChipVersion::Unknown };
    bool m_version_uncertain { true };
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
    WaitQueue m_wait_queue;
};
}
