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

#include <Kernel/Net/IPv4FragmentReassembler.h>

//#define IPV4_FRAGMENT_REASSEMBLER_DEBUG

namespace Kernel {
void IPv4FragmentReassembler::register_fragment(const EthernetFrameHeader& eth, const IPv4Packet& packet)
{
    FragmentIdentifier ident { packet };
    if (m_fragments.contains(ident)) {
        // FIXME: the RFC says to prolong the timer every time we
        //        receive a fragment by its TTL, for simplicity' sake
        //        we are not doing this right now
        //
        auto& descriptor = *const_cast<FragmentedPacket*>(m_fragments.get(ident).value());
#ifdef IPV4_FRAGMENT_REASSEMBLER_DEBUG
        dbg() << "ipv4_reassembler: completing previous fragment of size _" << descriptor.chunks().size() << "_";
#endif
        descriptor.add_fragment(packet);
#ifdef IPV4_FRAGMENT_REASSEMBLER_DEBUG
        dbg() << "ipv4_reassembler: Total fragment size is now _" << descriptor.chunks().size() << "_";
#endif
        if (descriptor.is_complete()) {
#ifdef IPV4_FRAGMENT_REASSEMBLER_DEBUG
            dbg() << "ipv4_reassembler: Complete packet of size _" << descriptor.chunks().size() << "_ received";
#endif
            process_changes(descriptor);
            m_fragments.remove(ident);
        }
    } else {
        timeval tv { 30, 0 };
        auto timer_id = TimerQueue::the().add_timer(tv, [&] {
            m_fragments.remove(ident);
        });
        m_fragments.set(ident, make<FragmentedPacket>(packet, eth, timer_id));
    }
}
void IPv4FragmentReassembler::process_changes(FragmentedPacket& descriptor)
{
    // cancel the recv timeout timer
    TimerQueue::the().cancel_timer(descriptor.timer_id());
    // reconstruct an Ethernet packet and hand it back to the NetworkTask
    auto buffer = ByteBuffer::create_zeroed(descriptor.chunks().size() + sizeof(EthernetFrameHeader) + sizeof(IPv4Packet));
    descriptor.reconstruct_ether_packet(buffer);
    auto& eth = *(EthernetFrameHeader*)buffer.data();
    handle_ipv4(*this, eth, buffer.size());
}

void FragmentedPacket::reconstruct_ether_packet(ByteBuffer& buffer)
{
    auto& eth = *(EthernetFrameHeader*)buffer.data();
    auto& ipv4 = *(IPv4Packet*)eth.payload();
    ipv4.set_version(4);
    ipv4.set_internet_header_length(5);
    ipv4.set_source(m_packet.source());
    ipv4.set_destination(m_packet.destination());
    ipv4.set_protocol(m_packet.protocol());
    ipv4.set_length(sizeof(IPv4Packet) + m_chunks.size());
    ipv4.set_ident(m_packet.ident());
    ipv4.set_ttl(m_packet.ttl());
    ipv4.set_checksum(ipv4.compute_checksum());
    __builtin_memcpy(ipv4.payload(), m_chunks.data(), m_chunks.size());
}

void FragmentedPacket::add_fragment(const IPv4Packet& packet)
{
#ifdef IPV4_FRAGMENT_REASSEMBLER_DEBUG
    dbg() << "ipv4_reassembler: Fragment is missing these holes: ( ";
    for (auto& el : holes())
        dbg() << "  {from=" << el.key << ", size=" << el.value << "}";
    dbg() << "), seen_last=" << last_packet_seen() << " and we have " << chunks().size() << " bytes";
#endif
    auto fragment_offset = packet.fragment_offset();

    auto fix_holes = [&](bool only_fix = false) {
        if (m_holes.contains(fragment_offset)) {
            // bingo
            auto hole = m_holes.get(fragment_offset).value();
#ifdef IPV4_FRAGMENT_REASSEMBLER_DEBUG
            dbg() << "ipv4_reassembler: We can (partially) fill a _" << hole << "_ chunk hole with this fragment";
#endif
            m_holes.remove(fragment_offset);
            if (packet.payload_size() / 8 < hole) {
                auto fragment_block_count = packet.payload_size() / 8;
                m_holes.set(fragment_offset + fragment_block_count, hole - fragment_block_count);
            }
            if (!only_fix) {
                m_chunks.overwrite(fragment_offset * 8, packet.payload(), packet.payload_size());
            }
        } else {
            // we have to go the slow route and check all of the holes now
            Optional<size_t> hole_start {}, hole_size {};
            for (auto& hole : m_holes) {
                if (hole.key < fragment_offset && hole.key + hole.value > fragment_offset) {
                    hole_start = hole.key;
                    hole_size = hole.value;
                    break;
                }
            }
            if (hole_start.has_value()) {
#ifdef IPV4_FRAGMENT_REASSEMBLER_DEBUG
                dbg() << "ipv4_reassembler: We can partially fill the _" << hole_start.value() << "_ hole here";
#endif
                // yay we found a hole to partially fill
                auto start = hole_start.value();
                u16 size = hole_size.value();
                u16 fragment_block_count = packet.payload_size() / 8;
                m_holes.remove(start);
                m_holes.set(start, fragment_offset - start);
                u16 fragment_end = fragment_offset + fragment_block_count;
                u16 hole_end = start + size;
                if (fragment_end < hole_end)
                    m_holes.set(fragment_end, hole_end - fragment_end);
                if (!only_fix) {
                    m_chunks.overwrite(fragment_offset * 8, packet.payload(), packet.payload_size());
                }
            } else if (!only_fix) {
                // This packet belongs nowhere, it is crap data
                dbg() << "ipv4_reassembler: Received fragment does not fit into any hole, expected=" << m_expected_next_offset << ", holes: (";
                for (auto& hole : m_holes)
                    dbg() << "  { from=" << hole.key << ", size=" << hole.value << " }";
                dbg() << ")";
                ASSERT_NOT_REACHED();
            }
        }
    };

    auto is_last = !(packet.flags() & (u16)IPv4PacketFlags::MoreFragments);
#ifdef IPV4_FRAGMENT_REASSEMBLER_DEBUG
    dbg() << "ipv4_reassembler: adding fragment (offset=" << fragment_offset << ") which has MF=" << !is_last;
#endif
    if (m_expected_next_offset == fragment_offset) {
#ifdef IPV4_FRAGMENT_REASSEMBLER_DEBUG
        dbg() << "ipv4_reassembler: received expected fragment at offset " << m_expected_next_offset << " with size " << packet.payload_size();
#endif
        m_chunks.append(packet.payload(), packet.payload_size());
        m_expected_next_offset += packet.payload_size() / 8;
        // check if we had any holes, just in case
        fix_holes(true);
    } else {
        // something got delayed
        if (m_expected_next_offset < fragment_offset) {
#ifdef IPV4_FRAGMENT_REASSEMBLER_DEBUG
            dbg() << "ipv4_reassembler: We got something beyond what we expected (exp:" << m_expected_next_offset << ", got:" << fragment_offset << ")";
#endif
            // we got something beyond what we expected, so we have no holes to fill with this
            auto hole_size = fragment_offset - m_expected_next_offset;
#ifdef IPV4_FRAGMENT_REASSEMBLER_DEBUG
            dbg() << "ipv4_reassembler:  This creates a hole starting at " << m_expected_next_offset << " with size " << hole_size;
#endif
            m_holes.set(m_expected_next_offset, hole_size);
            // extend our buffer
            // FIXME: This could be misused to make us run out of memory
            m_chunks.grow(m_chunks.size() + hole_size * 8);
            m_chunks.append(packet.payload(), packet.payload_size());
            m_expected_next_offset = fragment_offset + packet.payload_size() / 8;
        } else {
            // This is either an invalid packet, or it fills one of the holes
            fix_holes();
        }
    }
    if (is_last) {
        m_last_packet_seen = true;
    }
}
}
