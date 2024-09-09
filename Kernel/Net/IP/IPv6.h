/*
 * Copyright (c) 2024, kleines Filmröllchen <filmroellchen@serenityos.org>
 * Copyright (c) 2024, sdomi <ja@sdomi.pl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Assertions.h>
#include <AK/Endian.h>
#include <AK/IPv6Address.h>
#include <AK/Types.h>
#include <Kernel/Net/IP/IP.h>

namespace Kernel {

// Header extensions and special values only.
// For transport protocol numbers, see Kernel::TransportProtocol
enum class IPv6NextHeader : u8 {
    HopByHopOption = 0,
    Routing = 43,
    Fragment = 44,
    NoNextHeader = 59,
    DestinationOptions = 60,
};

// https://datatracker.ietf.org/doc/html/rfc8200#section-3
class [[gnu::packed]] IPv6PacketHeader {
public:
    u8 version() const { return (m_version_and_traffic >> 28) & 0xf; }
    void set_version(u8 version) { m_version_and_traffic = (m_version_and_traffic & ~0xf0000000) | (version << 28); }

    u8 traffic_class() const { return (m_version_and_traffic >> 20) & 0x0ff; }
    void set_traffic_class(u8 traffic_class) { m_version_and_traffic = (m_version_and_traffic & ~0x0ff00000) | (traffic_class << 20); }

    u32 flow_label() const { return m_version_and_traffic & 0xfffff; }
    void set_flow_label(u32 flow_label) { m_version_and_traffic = (m_version_and_traffic & ~0xfffff) | flow_label; }

    u16 length() const { return m_length; }
    void set_length(u16 length) { m_length = length; }

    // Aka. TTL
    u8 hop_limit() const { return m_hop_limit; }
    void set_hop_limit(u8 hop_limit) { m_hop_limit = hop_limit; }

    u8 next_header() const { return m_next_header; }
    void set_next_header(u8 next_header) { m_next_header = next_header; }

    IPv6Address const& source() const { return m_source; }
    void set_source(IPv6Address const& address) { m_source = address; }

    IPv6Address const& destination() const { return m_destination; }
    void set_destination(IPv6Address const& address) { m_destination = address; }

    void* payload() { return &m_payload[0]; }
    void const* payload() const { return &m_payload[0]; }

    u16 payload_size() const { return m_length; }

private:
    NetworkOrdered<u32> m_version_and_traffic;
    NetworkOrdered<u16> m_length;
    u8 m_next_header { static_cast<u8>(IPv6NextHeader::NoNextHeader) };
    u8 m_hop_limit { 0 };
    IPv6Address m_source;
    IPv6Address m_destination;
    u8 m_payload[0];
};

static_assert(AssertSize<IPv6PacketHeader, 10 * 32 / 8>());

// https://www.rfc-editor.org/rfc/rfc2460
// section 8.1, checksumming
struct [[gnu::packed]] IPv6PseudoHeader {
    IPv6Address source_address;
    IPv6Address target_address;
    NetworkOrdered<u32> packet_length;
    u8 zero[3] { 0 };
    TransportProtocol next_header;
};

static_assert(AssertSize<IPv6PseudoHeader, 2 * 16 + 8>());

}
