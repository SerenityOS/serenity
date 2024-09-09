/*
 * Copyright (c) 2024, kleines Filmröllchen <filmroellchen@serenityos.org>
 * Copyright (c) 2024, sdomi <ja@sdomi.pl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/MACAddress.h>
#include <Kernel/Net/IP/IPv6.h>

// https://www.rfc-editor.org/rfc/rfc4443

// Section 2.1
struct ICMPv6Type {
    enum {
        DestinationUnreachable = 1,
        PacketTooBig = 2,
        TimeExceeded = 3,
        ParameterProblem = 4,
        EchoRequest = 128,
        EchoReply = 129,
        NeighborSolicitation = 135,
        NeighborAdvertisement = 136,
    };
};

class [[gnu::packed]] ICMPv6Header {
public:
    ICMPv6Header() = default;
    ~ICMPv6Header() = default;

    u8 type() const { return m_type; }
    void set_type(u8 type) { m_type = type; }

    u8 code() const { return m_code; }
    void set_code(u8 code) { m_code = code; }

    u16 checksum() const { return m_checksum; }
    void set_checksum(u16 checksum) { m_checksum = checksum; }

    void* payload() { return &m_payload[0]; }
    void const* payload() const { return &m_payload[0]; }

private:
    u8 m_type { 0 };
    u8 m_code { 0 };
    NetworkOrdered<u16> m_checksum { 0 };
    u8 m_payload[0];
};

static_assert(AssertSize<ICMPv6Header, 4>());

class [[gnu::packed]] ICMPv6Echo {
public:
    void* payload() { return &m_payload[0]; }
    void const* payload() const { return &m_payload[0]; }

    ICMPv6Header header;
    NetworkOrdered<u16> identifier;
    NetworkOrdered<u16> sequence_number;

private:
    u8 m_payload[0];
};

static_assert(AssertSize<ICMPv6Echo, 8>());

// https://www.rfc-editor.org/rfc/rfc2461

// Section 4.3
struct [[gnu::packed]] ICMPv6NeighborSolicitation {
    ICMPv6Header header;
    u32 reserved;
    IPv6Address target_address;
};

static_assert(AssertSize<ICMPv6NeighborSolicitation, 6 * 32 / 8>());

// Section 4.4
class [[gnu::packed]] ICMPv6NeighborAdvertisement {
public:
    ICMPv6Header header;
    NetworkOrdered<u32> flags;
    IPv6Address target_address;

    bool override() const { return (flags >> 29) & 1; }
    void set_override(bool override) { flags = (flags & ~(1 << 29)) | (override << 29); }

    bool solicited() const { return (flags >> 30) & 1; }
    void set_solicited(bool solicited) { flags = (flags & ~(1 << 30)) | (solicited << 30); }

    bool router() const { return flags >> 31; }
    void set_router(bool router) { flags = (flags & ~(1 << 31)) | (router << 31); }
};

static_assert(AssertSize<ICMPv6NeighborAdvertisement, 6 * 32 / 8>());

// Section 4.6.1
struct [[gnu::packed]] ICMPv6OptionLinkLayerAddress {
    u8 type;   // default: Target link-layer address
    u8 length; // multiplied by 8, rounded up
    MACAddress address;
};

static_assert(AssertSize<ICMPv6OptionLinkLayerAddress, 8>());
