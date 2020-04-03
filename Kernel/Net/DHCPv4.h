/*
 * Copyright (c) 2020, The SerenityOS developers.
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

#include <AK/Assertions.h>
#include <AK/ByteBuffer.h>
#include <AK/HashMap.h>
#include <AK/IPv4Address.h>
#include <AK/NetworkOrdered.h>
#include <AK/StringBuilder.h>
#include <AK/StringView.h>
#include <AK/Traits.h>
#include <AK/Types.h>
#include <Kernel/Net/MACAddress.h>
#include <Kernel/Random.h>

enum class DHCPv4Flags : u16 {
    Broadcast = 1,
    /* everything else is reserved and must be zero */
};

enum class DHCPv4Ops : u8 {
    BootRequest = 1,
    BootReply = 2
};

enum class DHCPOptions : u8 {
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

template <>
struct AK::Traits<DHCPOptions> : public AK::GenericTraits<DHCPOptions> {
    static constexpr bool is_trivial() { return true; }
    static unsigned hash(DHCPOptions u) { return int_hash((u8)u); }
};

namespace Kernel {

struct ParsedDHCPv4Options {
    template <typename T>
    Optional<const T> get(DHCPOptions option_name) const
    {
        auto opt = options.get(option_name);
        if (!opt.has_value()) {
            return {};
        }
        auto& val = opt.value();
        if (val.length != sizeof(T))
            return {};
        return *(const T*)val.value;
    }

    template <typename T>
    Vector<T> get_many(DHCPOptions option_name, size_t max_number) const
    {
        Vector<T> values;

        auto opt = options.get(option_name);
        if (!opt.has_value()) {
            return {};
        }
        auto& val = opt.value();
        if (val.length < sizeof(T))
            return {};

        for (size_t i = 0; i < max_number; ++i) {
            auto offset = i * sizeof(T);
            if (offset >= val.length)
                break;
            values.append(*(T*)((u8*)const_cast<void*>(val.value) + offset));
        }

        return values;
    }

    String to_string() const
    {
        StringBuilder builder;
        builder.append("DHCP Options (");
        builder.appendf("%d", options.size());
        builder.append(" entries)\n");
        for (auto& opt : options) {
            builder.appendf("\toption %d (%d bytes):", (u8)opt.key, (u8)opt.value.length);
            for (auto i = 0; i < opt.value.length; ++i)
                builder.appendf(" %u ", ((const u8*)opt.value.value)[i]);
            builder.append('\n');
        }
        return builder.build();
    }

    struct DHCPOptionValue {
        u8 length;
        const void* value;
    };

    HashMap<DHCPOptions, DHCPOptionValue> options;
};

class [[gnu::packed]] DHCPv4Packet
{
public:
    u8 op() const { return m_op; }
    void set_op(DHCPv4Ops op) { m_op = (u8)op; }

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

    const IPv4Address& ciaddr() const { return m_ciaddr; }
    const IPv4Address& yiaddr() const { return m_yiaddr; }
    const IPv4Address& siaddr() const { return m_siaddr; }
    const IPv4Address& giaddr() const { return m_giaddr; }

    IPv4Address& ciaddr() { return m_ciaddr; }
    IPv4Address& yiaddr() { return m_yiaddr; }
    IPv4Address& siaddr() { return m_siaddr; }
    IPv4Address& giaddr() { return m_giaddr; }

    u8* options() { return m_options; }
    ParsedDHCPv4Options parse_options() const;

    const MACAddress& chaddr() const { return *(const MACAddress*)&m_chaddr[0]; }
    void set_chaddr(const MACAddress& mac) { *(MACAddress*)&m_chaddr[0] = mac; }

    StringView sname() const { return { (const char*)&m_sname[0] }; }
    StringView file() const { return { (const char*)&m_file[0] }; }

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
    u8 m_options[312] { 0 }; // variable, less than 312 bytes
};

class DHCPv4PacketBuilder {
public:
    DHCPv4PacketBuilder()
        : m_buffer(ByteBuffer::create_zeroed(sizeof(DHCPv4Packet)))
    {
        auto* options = peek().options();
        options[0] = 99;
        options[1] = 130;
        options[2] = 83;
        options[3] = 99;
    }

    void add_option(DHCPOptions option, u8 length, const void* data)
    {
        ASSERT(m_can_add);
        auto* options = peek().options();
        options[next_option_offset++] = (u8)option;
        __builtin_memcpy(options + next_option_offset, &length, 1);
        next_option_offset++;
        __builtin_memcpy(options + next_option_offset, data, length);
        next_option_offset += length;
    }

    void set_message_type(DHCPMessageType type) { add_option(DHCPOptions::DHCPMessageType, 1, &type); }

    DHCPv4Packet& peek() { return *(DHCPv4Packet*)m_buffer.data(); }
    DHCPv4Packet& build()
    {
        add_option(DHCPOptions::End, 0, nullptr);
        m_can_add = false;
        return *(DHCPv4Packet*)m_buffer.data();
    }
    size_t size() const { return m_buffer.size(); }

private:
    ByteBuffer m_buffer;
    size_t next_option_offset { 4 };
    bool m_can_add { true };
};

}
