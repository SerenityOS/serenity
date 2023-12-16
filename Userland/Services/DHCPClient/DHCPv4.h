/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Assertions.h>
#include <AK/Endian.h>
#include <AK/HashMap.h>
#include <AK/IPv4Address.h>
#include <AK/MACAddress.h>
#include <AK/StringBuilder.h>
#include <AK/StringView.h>
#include <AK/Traits.h>
#include <AK/Types.h>
#include <string.h>

enum class DHCPv4Flags : u16 {
    Broadcast = 1 << 15,
    /* everything else is reserved and must be zero */
};

enum class DHCPv4Op : u8 {
    BootRequest = 1,
    BootReply = 2
};

enum class DHCPOption : u8 {
    // BOOTP
    Pad = 0,
    SubnetMask,
    TimeOffset,
    Router,
    TimeServer,
    NameServer,
    DomainNameServer,
    LogServer,
    CookieServer,
    LPRServer,
    ImpressServer,
    ResourceLocationServer,
    HostName,
    BootFileSize,
    MeritDumpFile,
    DomainName,
    SwapServer,
    RootPath,
    ExtensionsPath,
    IPForwardingEnableDisable,
    NonLocalSourceRoutingEnableDisable,
    PolicyFilter,
    MaximumDatagramReassemblySize,
    DefaultIPTTL,
    PathMTUAgingTimeout,
    PathMTUPlateauTable,
    InterfaceMTU,
    AllSubnetsAreLocal,
    BroadcastAddress,
    PerformMaskDiscovery,
    MaskSupplier,
    PerformRouterDiscovery,
    RouterSolicitationAddress,
    StaticRoute,
    TrailerEncapsulation,
    ARPCacheTimeout,
    EthernetEncapsulation,
    TCPDefaultTTL,
    TCPKeepaliveInterval,
    TCPKeepaliveGarbage,
    NetworkInformationServiceDomain,
    NetworkInformationServers,
    NetworkTimeProtocolServers,
    VendorSpecificInformation,
    NetBIOSOverTCPIPNameServer,
    NetBIOSOverTCPIPDatagramDistributionServer,
    NetBIOSOverTCPIPNodeType,
    NetBIOSOverTCPIPScope,
    XWindowSystemFontServer, // wow
    XWindowSystemDisplayManager,
    // DHCP
    RequestedIPAddress = 50,
    IPAddressLeaseTime,
    OptionOverload,
    DHCPMessageType,
    ServerIdentifier,
    ParameterRequestList,
    Message,
    MaximumDHCPMessageSize,
    RenewalT1Time,
    RenewalT2Time,
    ClassIdentifier,
    ClientIdentifier,
    End = 255
};

enum class DHCPMessageType : u8 {
    DHCPDiscover = 1,
    DHCPOffer,
    DHCPRequest,
    DHCPDecline,
    DHCPAck,
    DHCPNak,
    DHCPRelease,
};

template<>
struct AK::Traits<DHCPOption> : public DefaultTraits<DHCPOption> {
    static constexpr bool is_trivial() { return true; }
    static unsigned hash(DHCPOption u) { return int_hash((u8)u); }
};

struct ParsedDHCPv4Options {
    template<typename T>
    Optional<T const> get(DHCPOption option_name) const
    requires(IsTriviallyCopyable<T>)
    {
        auto option = options.get(option_name);
        if (!option.has_value()) {
            return {};
        }
        auto& value = option.value();
        if (value.length != sizeof(T))
            return {};
        T t;
        __builtin_memcpy(&t, value.value, value.length);
        return t;
    }

    template<typename T>
    Vector<T> get_many(DHCPOption option_name, size_t max_number) const
    {
        Vector<T> values;

        auto option = options.get(option_name);
        if (!option.has_value()) {
            return {};
        }
        auto& value = option.value();
        if (value.length < sizeof(T))
            return {};

        for (size_t i = 0; i < max_number; ++i) {
            auto offset = i * sizeof(T);
            if (offset >= value.length)
                break;
            values.append(*(T*)((u8*)const_cast<void*>(value.value) + offset));
        }

        return values;
    }

    ByteString to_byte_string() const
    {
        StringBuilder builder;
        builder.append("DHCP Options ("sv);
        builder.appendff("{}", options.size());
        builder.append(" entries)\n"sv);
        for (auto& opt : options) {
            builder.appendff("\toption {} ({} bytes):", (u8)opt.key, (u8)opt.value.length);
            for (auto i = 0; i < opt.value.length; ++i)
                builder.appendff(" {} ", ((u8 const*)opt.value.value)[i]);
            builder.append('\n');
        }
        return builder.to_byte_string();
    }

    struct DHCPOptionValue {
        u8 length;
        void const* value;
    };

    HashMap<DHCPOption, DHCPOptionValue> options;
};

constexpr auto DHCPV4_OPTION_FIELD_MAX_LENGTH = 312;

class [[gnu::packed]] DHCPv4Packet {
public:
    u8 op() const { return m_op; }
    void set_op(DHCPv4Op op) { m_op = (u8)op; }

    u8 htype() const { return m_htype; }
    void set_htype(u8 htype) { m_htype = htype; }

    u8 hlen() const { return m_hlen; }
    void set_hlen(u8 hlen) { m_hlen = hlen; }

    u8 hops() const { return m_hops; }
    void set_hops(u8 hops) { m_hops = hops; }

    u32 xid() const { return m_xid; }
    void set_xid(u32 xid) { m_xid = xid; }

    u16 secs() const { return m_secs; }
    void set_secs(u16 secs) { m_secs = secs; }

    u16 flags() const { return m_flags; }
    void set_flags(DHCPv4Flags flags) { m_flags = (u16)flags; }

    IPv4Address const& ciaddr() const { return m_ciaddr; }
    IPv4Address const& yiaddr() const { return m_yiaddr; }
    IPv4Address const& siaddr() const { return m_siaddr; }
    IPv4Address const& giaddr() const { return m_giaddr; }

    IPv4Address& ciaddr() { return m_ciaddr; }
    IPv4Address& yiaddr() { return m_yiaddr; }
    IPv4Address& siaddr() { return m_siaddr; }
    IPv4Address& giaddr() { return m_giaddr; }

    u8* options() { return m_options; }
    ParsedDHCPv4Options parse_options() const;

    MACAddress const& chaddr() const { return *(MACAddress const*)&m_chaddr[0]; }
    void set_chaddr(MACAddress const& mac) { *(MACAddress*)&m_chaddr[0] = mac; }

    StringView sname() const
    {
        char const* sname_ptr = reinterpret_cast<char const*>(&m_sname[0]);
        return { sname_ptr, strlen(sname_ptr) };
    }

    StringView file() const
    {
        char const* file_ptr = reinterpret_cast<char const*>(&m_file[0]);
        return { file_ptr, strlen(file_ptr) };
    }

private:
    NetworkOrdered<u8> m_op;
    NetworkOrdered<u8> m_htype;
    NetworkOrdered<u8> m_hlen;
    NetworkOrdered<u8> m_hops;
    NetworkOrdered<u32> m_xid;
    NetworkOrdered<u16> m_secs;
    NetworkOrdered<u16> m_flags;
    IPv4Address m_ciaddr;
    IPv4Address m_yiaddr;
    IPv4Address m_siaddr;
    IPv4Address m_giaddr;
    u8 m_chaddr[16]; // 10 bytes of padding at the end
    u8 m_sname[64] { 0 };
    u8 m_file[128] { 0 };
    u8 m_options[DHCPV4_OPTION_FIELD_MAX_LENGTH] { 0 }; // variable, less than 312 bytes
};

class DHCPv4PacketBuilder {
public:
    DHCPv4PacketBuilder()
    {
        auto* options = m_packet.options();
        // set the magic DHCP cookie value
        options[0] = 99;
        options[1] = 130;
        options[2] = 83;
        options[3] = 99;
    }

    void add_option(DHCPOption option, u8 length, void const* data)
    {
        VERIFY(m_can_add);
        // we need enough space to fit the option value, its length, and its data
        VERIFY(next_option_offset + length + 2 < DHCPV4_OPTION_FIELD_MAX_LENGTH);

        auto* options = m_packet.options();
        options[next_option_offset++] = (u8)option;
        memcpy(options + next_option_offset, &length, 1);
        next_option_offset++;
        if (data && length)
            memcpy(options + next_option_offset, data, length);
        next_option_offset += length;
    }

    void set_message_type(DHCPMessageType type) { add_option(DHCPOption::DHCPMessageType, 1, &type); }

    DHCPv4Packet& peek() { return m_packet; }
    DHCPv4Packet& build()
    {
        add_option(DHCPOption::End, 0, nullptr);
        m_can_add = false;
        return m_packet;
    }

private:
    DHCPv4Packet m_packet;
    size_t next_option_offset { 4 };
    bool m_can_add { true };
};
