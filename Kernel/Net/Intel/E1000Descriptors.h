/*
 * Copyright (c) 2024, Leon Albrecht <leon.a@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/EnumBits.h>
#include <AK/Format.h>
#include <AK/StdLibExtraDetails.h>
#include <AK/StringBuilder.h>
#include <AK/Types.h>

namespace Kernel::E1000 {

// 3.3.13
enum class TXCommand : u8 {
    EOP = 1 << 0,  // End of Packet
    IFCS = 1 << 1, // Insert FCS
    IC = 1 << 2,   // Insert Checksum
    RS = 1 << 3,   // Report Status
    RPS = 1 << 4,  // Report Packet Sent (82544GC/EI only)
    DEXT = 1 << 5, // Descriptor Extension (82576 and later, Do Not Use for now)
    VLE = 1 << 6,  // VLAN Packet Enable
    ID = 1 << 7,   // Interrupt Delay Enable
};
AK_ENUM_BITWISE_OPERATORS(TXCommand);

struct TxDescriptor {
    uint64_t addr { 0 };
    uint16_t length { 0 };
    uint8_t cso { 0 };
    E1000::TXCommand cmd { 0 };
    uint8_t status { 0 };
    uint8_t css { 0 };
    uint16_t special { 0 };
};
static_assert(AssertSize<TxDescriptor, 16>());

enum class RxDescriptorStatus : u8 {
    None = 0,
    DD = 1 << 0, // Descriptor Done
    EOP = 1 << 1,
    IXSM = 1 << 2,  // Ignore Checksum Indication (reserved on later NICs)
    VP = 1 << 3,    // VLAN Packet
    UDPCS = 1 << 4, // UDP Checksum calculated
    L4CS = 1 << 5,  // L4 Checksum calculated
    IPCS = 1 << 6,  // IP Checksum calculated
    PIF = 1 << 7,   // Passed in-exact filter
};
AK_ENUM_BITWISE_OPERATORS(RxDescriptorStatus);

struct LegacyRxDescriptor {
    uint64_t addr { 0 };
    uint16_t length { 0 };
    uint16_t checksum { 0 };
    RxDescriptorStatus status { 0 };
    uint8_t errors { 0 };
    uint16_t special { 0 };
};
static_assert(AssertSize<LegacyRxDescriptor, 16>());

struct AdvancedRxDescriptor {
    u64 packet_buffer_address { 0 }; // Note: bit 0 is A0/NSE
    u64 header_buffer_address { 0 }; // Note: bit 0 is DD
};
static_assert(AssertSize<AdvancedRxDescriptor, 16>());

enum class RxDescriptorExtendedStatus : u64 {
    DD = 1 << 0, // Descriptor Done
    EOP = 1 << 1,
    // Below only in last descriptor of a packet
    IXSM = 1 << 2,  // Ignore Checksum Indication (Reserved)
    VP = 1 << 3,    // VLAN Packet
    UDPCS = 1 << 4, // UDP Checksum calculated
    L4CS = 1 << 5,  // L4 Checksum calculated
    IPCS = 1 << 6,  // IP Checksum calculated
    PIF = 1 << 7,   // Passed in-exact filter
    // Rsv = 1 << 8,
    VEXT = 1 << 9,      // First Vlan on a double VLAN packet
    UDPV = 1 << 10,     // UDP checksum valid
    LLINT = 1 << 11,    // Low Latency Interrupt caused
    StripCRC = 1 << 12, // Striped CRC
    // Rsv : 14:13,
    TSIP = 1 << 15, // Time Stamp in Packet
    TS = 1 << 16,   // Time Stamped Packet (Time Sync)
    // Rsv : 19:17,
};
AK_ENUM_BITWISE_OPERATORS(RxDescriptorExtendedStatus);

enum class RxDescriptorExtendedError : u64 {
    // 2:0 reserved
    HBO = 1 << 3, // Header Buffer Overflow
    // 6:4 reserved
    // 8:7 reserved, used to be SECERR (ref:82576eb)
    L4E = 1 << 9,  // TCP/UDB Checksum Error
    IPE = 1 << 10, // IPv4 checksum Error
    RXE = 1 << 11, // RX data Error
};

struct AdvancedRxDescriptorWriteBack {
    enum class RSSType : u64 {
        None = 0x0,
        HASH_TCP_IPV4 = 0x1,
        HASH_IPV4 = 0x2,
        HASH_TCP_IPV6 = 0x3,
        HASH_IPV6_EX = 0x4,
        HASH_IPV6 = 0x5,
        HASH_UDP_IPV4 = 0x6,
        HASH_UDP_IPV6 = 0x7,
        HASH_TCP_IPV6_EX = 0x8,
        // 0xA:0xF reserved
    };
    enum class PacketType : u64 {
        IPV4 = 1 << 0,
        IPV4E = 1 << 1,
        IPV6 = 1 << 2,
        IPV6E = 1 << 3,
        TCP = 1 << 4,
        UDP = 1 << 5,
        SCTP = 1 << 6,
        NFS = 1 << 7,
        // 10:8 = ETQF
        L2 = 1 << 11,
        VLAN = 1 << 12,
    };
    RSSType rss_type : 4;
    PacketType packet_type : 13;
    u64 : 4; // rsv-reserved, also the 82576 docs say this is 5 bits in one place, 4 in another
             // while the i211 docs say its 22 bits, which is clearly a typo, but there these may also contain two extra header_len bits
             // but those dont seem to be mentioned in the explanation later
    u64 header_len : 10;
    u64 split_header : 1; // SPH
    u64 rss_hash : 32;
    RxDescriptorExtendedStatus extended_status : 20;
    RxDescriptorExtendedError extended_error : 12;
    u64 pkt_len : 16;
    u64 vlan_tag : 16;
};
static_assert(AssertSize<AdvancedRxDescriptorWriteBack, 16>());

AK_ENUM_BITWISE_OPERATORS(AdvancedRxDescriptorWriteBack::PacketType);

union RxDescriptor {
    LegacyRxDescriptor legacy;
    AdvancedRxDescriptor advanced;
    AdvancedRxDescriptorWriteBack advanced_write_back;
};
static_assert(AssertSize<RxDescriptor, 16>());

}

namespace AK {
template<>
struct Formatter<Kernel::E1000::AdvancedRxDescriptorWriteBack::PacketType> : StandardFormatter {

    ErrorOr<void> format(FormatBuilder& builder, Kernel::E1000::AdvancedRxDescriptorWriteBack::PacketType value)
    {
        if (has_flag(value, Kernel::E1000::AdvancedRxDescriptorWriteBack::PacketType::IPV4))
            TRY(builder.put_literal("IPv4|"sv));
        if (has_flag(value, Kernel::E1000::AdvancedRxDescriptorWriteBack::PacketType::IPV4E))
            TRY(builder.put_literal("IPv4E|"sv));
        if (has_flag(value, Kernel::E1000::AdvancedRxDescriptorWriteBack::PacketType::IPV6))
            TRY(builder.put_literal("IPv6|"sv));
        if (has_flag(value, Kernel::E1000::AdvancedRxDescriptorWriteBack::PacketType::IPV6E))
            TRY(builder.put_literal("IPv6E|"sv));
        if (has_flag(value, Kernel::E1000::AdvancedRxDescriptorWriteBack::PacketType::TCP))
            TRY(builder.put_literal("TCP"sv));
        if (has_flag(value, Kernel::E1000::AdvancedRxDescriptorWriteBack::PacketType::UDP))
            TRY(builder.put_literal("UDP"sv));
        if (has_flag(value, Kernel::E1000::AdvancedRxDescriptorWriteBack::PacketType::SCTP))
            TRY(builder.put_literal("SCTP"sv));
        if (has_flag(value, Kernel::E1000::AdvancedRxDescriptorWriteBack::PacketType::NFS))
            TRY(builder.put_literal("NFS"sv));
        if (has_flag(value, Kernel::E1000::AdvancedRxDescriptorWriteBack::PacketType::L2))
            TRY(builder.put_literal("|L2"sv));
        if (has_flag(value, Kernel::E1000::AdvancedRxDescriptorWriteBack::PacketType::VLAN))
            TRY(builder.put_literal("|VLAN"sv));
        return {};
    }
};

template<>
struct Formatter<Kernel::E1000::RxDescriptorExtendedStatus> : StandardFormatter {
    using RxDescriptorExtendedStatus = Kernel::E1000::RxDescriptorExtendedStatus;
    ErrorOr<void> format(FormatBuilder& builder, RxDescriptorExtendedStatus status)
    {
        if (to_underlying(status) == 0) {
            TRY(builder.put_literal("None"sv));
            return {};
        }

        if (has_flag(status, RxDescriptorExtendedStatus::DD)) {
            TRY(builder.put_literal("DD"sv));
            status = status & ~RxDescriptorExtendedStatus::DD;
            if (to_underlying(status) != 0)
                TRY(builder.put_literal("|"sv));
        }
        if (has_flag(status, RxDescriptorExtendedStatus::EOP)) {
            TRY(builder.put_literal("EOP"sv));
            status = status & ~RxDescriptorExtendedStatus::EOP;
            if (to_underlying(status) != 0)
                TRY(builder.put_literal("|"sv));
        }
        if (has_flag(status, RxDescriptorExtendedStatus::IXSM)) {
            TRY(builder.put_literal("IXSM"sv));
            status = status & ~RxDescriptorExtendedStatus::IXSM;
            if (to_underlying(status) != 0)
                TRY(builder.put_literal("|"sv));
        }
        if (has_flag(status, RxDescriptorExtendedStatus::VP)) {
            TRY(builder.put_literal("VP"sv));
            status = status & ~RxDescriptorExtendedStatus::VP;
            if (to_underlying(status) != 0)
                TRY(builder.put_literal("|"sv));
        }
        if (has_flag(status, RxDescriptorExtendedStatus::UDPCS)) {
            TRY(builder.put_literal("UDPCS"sv));
            status = status & ~RxDescriptorExtendedStatus::UDPCS;
            if (to_underlying(status) != 0)
                TRY(builder.put_literal("|"sv));
        }
        if (has_flag(status, RxDescriptorExtendedStatus::L4CS)) {
            TRY(builder.put_literal("L4CS"sv));
            status = status & ~RxDescriptorExtendedStatus::L4CS;
            if (to_underlying(status) != 0)
                TRY(builder.put_literal("|"sv));
        }
        if (has_flag(status, RxDescriptorExtendedStatus::IPCS)) {
            TRY(builder.put_literal("IPCS"sv));
            status = status & ~RxDescriptorExtendedStatus::IPCS;
            if (to_underlying(status) != 0)
                TRY(builder.put_literal("|"sv));
        }
        if (has_flag(status, RxDescriptorExtendedStatus::PIF)) {
            TRY(builder.put_literal("PIF"sv));
            status = status & ~RxDescriptorExtendedStatus::PIF;
            if (to_underlying(status) != 0)
                TRY(builder.put_literal("|"sv));
        }
        if (has_flag(status, RxDescriptorExtendedStatus::VEXT)) {
            TRY(builder.put_literal("VEXT"sv));
            status = status & ~RxDescriptorExtendedStatus::VEXT;
            if (to_underlying(status) != 0)
                TRY(builder.put_literal("|"sv));
        }
        if (has_flag(status, RxDescriptorExtendedStatus::UDPV)) {
            TRY(builder.put_literal("UDPV"sv));
            status = status & ~RxDescriptorExtendedStatus::UDPV;
            if (to_underlying(status) != 0)
                TRY(builder.put_literal("|"sv));
        }
        if (has_flag(status, RxDescriptorExtendedStatus::LLINT)) {
            TRY(builder.put_literal("LLINT"sv));
            status = status & ~RxDescriptorExtendedStatus::LLINT;
            if (to_underlying(status) != 0)
                TRY(builder.put_literal("|"sv));
        }
        if (has_flag(status, RxDescriptorExtendedStatus::StripCRC)) {
            TRY(builder.put_literal("StripCRC"sv));
            status = status & ~RxDescriptorExtendedStatus::StripCRC;
            if (to_underlying(status) != 0)
                TRY(builder.put_literal("|"sv));
        }
        if (has_flag(status, RxDescriptorExtendedStatus::TSIP)) {
            TRY(builder.put_literal("TSIP"sv));
            status = status & ~RxDescriptorExtendedStatus::TSIP;
            if (to_underlying(status) != 0)
                TRY(builder.put_literal("|"sv));
        }
        if (has_flag(status, RxDescriptorExtendedStatus::TS)) {
            TRY(builder.put_literal("TS"sv));
            status = status & ~RxDescriptorExtendedStatus::TS;
            if (to_underlying(status) != 0)
                TRY(builder.put_literal("|"sv));
        }

        if (to_underlying(status) != 0)
            TRY(builder.put_u64(to_underlying(status), 16, true, false, false, false, FormatBuilder::Align::Right, 0, ' ', FormatBuilder::SignMode::OnlyIfNeeded, false));
        return {};
    }
};

template<>
struct Formatter<Kernel::E1000::RxDescriptorStatus> : Formatter<Kernel::E1000::RxDescriptorExtendedStatus> {
    ErrorOr<void> format(FormatBuilder& builder, Kernel::E1000::RxDescriptorStatus status)
    {
        auto extended_status = static_cast<Kernel::E1000::RxDescriptorExtendedStatus>(to_underlying(status));
        return Formatter<Kernel::E1000::RxDescriptorExtendedStatus>::format(builder, extended_status);
    }
};

template<>
struct Formatter<Kernel::E1000::AdvancedRxDescriptorWriteBack> : StandardFormatter {
    ErrorOr<void> format(FormatBuilder& builder, Kernel::E1000::AdvancedRxDescriptorWriteBack const& descriptor)
    {
        return builder.builder().try_appendff(
            R"~~~({{
        RSS {:#01x}({:#04x})
        PacketType {}
        HeaderLen {}
        SplitHeader {}
        Status {}
        Error {:#03x}
        Length {}B
        VLAN {:#02x}
}})~~~",
            (u8)descriptor.rss_type,
            (u32)descriptor.rss_hash,
            auto(descriptor.packet_type),
            (u16)descriptor.header_len,
            (bool)descriptor.split_header,
            auto(descriptor.extended_status),
            (u16)descriptor.extended_error,
            (u16)descriptor.pkt_len,
            (u16)descriptor.vlan_tag);
    }
};

};
