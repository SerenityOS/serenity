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

#include <AK/ByteBuffer.h>
#include <AK/Function.h>
#include <AK/HashMap.h>
#include <AK/OwnPtr.h>
#include <Kernel/Net/EthernetFrameHeader.h>
#include <Kernel/Net/IPv4.h>
#include <Kernel/TimerQueue.h>

struct FragmentIdentifier {
    IPv4Address source;
    IPv4Address destination;
    u8 protocol;
    u16 identifier;

    FragmentIdentifier(const Kernel::IPv4Packet& packet)
        : source(packet.source())
        , destination(packet.destination())
        , protocol(packet.protocol())
        , identifier(packet.ident())
    {
    }

    bool operator==(const FragmentIdentifier& other) const
    {
        return source == other.source && destination == other.destination && protocol == other.protocol && identifier == other.identifier;
    }
};

// FIXME: move these into AK?
template<>
struct AK::Traits<FragmentIdentifier> : public AK::GenericTraits<FragmentIdentifier> {
    static constexpr bool is_trivial() { return false; }
    static unsigned hash(const FragmentIdentifier& ident)
    {
        return Traits<IPv4Address>::hash(ident.source) * 13 + Traits<IPv4Address>::hash(ident.destination) + 29 * int_hash((ident.protocol << 16) + ident.identifier);
    }
};

template<>
struct AK ::Traits<size_t> : public AK::GenericTraits<size_t> {
    static constexpr bool is_trivial() { return true; }
    static unsigned hash(size_t u) { return int_hash(u); }
};

namespace Kernel {

struct FragmentedPacket {
public:
    FragmentedPacket(const IPv4Packet& packet, const EthernetFrameHeader& eth, u64 timer_id)
        : m_chunks()
        , m_holes()
        , m_eth(eth)
        , m_packet(packet)
        , m_timer_id(timer_id)
    {
        add_fragment(packet);
    }

    void add_fragment(const IPv4Packet& packet);
    bool is_complete() const { return m_last_packet_seen && m_holes.size() == 0; };
    const ByteBuffer& chunks() const { return m_chunks; }
    const HashMap<size_t, size_t>& holes() const { return m_holes; }
    u64 timer_id() const { return m_timer_id; }
    bool last_packet_seen() const { return m_last_packet_seen; }

    void reconstruct_ether_packet(ByteBuffer& buffer);

private:
    ByteBuffer m_chunks;
    HashMap<size_t, size_t> m_holes;
    bool m_last_packet_seen { false };
    u16 m_expected_next_offset { 0 };
    EthernetFrameHeader m_eth;
    IPv4Packet m_packet;
    u64 m_timer_id { 0 };
};

class IPv4FragmentReassembler {
public:
    IPv4FragmentReassembler(AK::Function<void(IPv4FragmentReassembler&, const EthernetFrameHeader&, size_t)> handle_ipv4)
        : handle_ipv4(move(handle_ipv4))
    {
    }
    void register_fragment(const EthernetFrameHeader& eth, const IPv4Packet& packet);
    void process_changes(FragmentedPacket& descriptor);

private:
    HashMap<FragmentIdentifier, OwnPtr<FragmentedPacket>> m_fragments;
    AK::Function<void(IPv4FragmentReassembler&, const EthernetFrameHeader&, size_t)> handle_ipv4;
};
}
