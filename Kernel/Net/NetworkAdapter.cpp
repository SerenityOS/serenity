/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/HashTable.h>
#include <AK/Singleton.h>
#include <AK/StringBuilder.h>
#include <Kernel/Heap/kmalloc.h>
#include <Kernel/Lock.h>
#include <Kernel/Net/EtherType.h>
#include <Kernel/Net/EthernetFrameHeader.h>
#include <Kernel/Net/LoopbackAdapter.h>
#include <Kernel/Net/NetworkAdapter.h>
#include <Kernel/Process.h>
#include <Kernel/Random.h>
#include <Kernel/StdLib.h>

namespace Kernel {

static AK::Singleton<Lockable<HashTable<NetworkAdapter*>>> s_table;

static Lockable<HashTable<NetworkAdapter*>>& all_adapters()
{
    return *s_table;
}

void NetworkAdapter::for_each(Function<void(NetworkAdapter&)> callback)
{
    LOCKER(all_adapters().lock());
    for (auto& it : all_adapters().resource())
        callback(*it);
}

RefPtr<NetworkAdapter> NetworkAdapter::from_ipv4_address(const IPv4Address& address)
{
    LOCKER(all_adapters().lock());
    for (auto* adapter : all_adapters().resource()) {
        if (adapter->ipv4_address() == address)
            return adapter;
    }
    if (address[0] == 127)
        return LoopbackAdapter::the();
    return nullptr;
}

RefPtr<NetworkAdapter> NetworkAdapter::lookup_by_name(const StringView& name)
{
    NetworkAdapter* found_adapter = nullptr;
    for_each([&](auto& adapter) {
        if (adapter.name() == name)
            found_adapter = &adapter;
    });
    return found_adapter;
}

NetworkAdapter::NetworkAdapter()
{
    // FIXME: I wanna lock :(
    all_adapters().resource().set(this);
}

NetworkAdapter::~NetworkAdapter()
{
    // FIXME: I wanna lock :(
    all_adapters().resource().remove(this);
}

void NetworkAdapter::send(const MACAddress& destination, const ARPPacket& packet)
{
    size_t size_in_bytes = sizeof(EthernetFrameHeader) + sizeof(ARPPacket);
    auto buffer = ByteBuffer::create_zeroed(size_in_bytes);
    auto* eth = (EthernetFrameHeader*)buffer.data();
    eth->set_source(mac_address());
    eth->set_destination(destination);
    eth->set_ether_type(EtherType::ARP);
    m_packets_out++;
    m_bytes_out += size_in_bytes;
    memcpy(eth->payload(), &packet, sizeof(ARPPacket));
    send_raw({ (const u8*)eth, size_in_bytes });
}

int NetworkAdapter::send_ipv4(const MACAddress& destination_mac, const IPv4Address& destination_ipv4, IPv4Protocol protocol, const UserOrKernelBuffer& payload, size_t payload_size, u8 ttl)
{
    size_t ipv4_packet_size = sizeof(IPv4Packet) + payload_size;
    if (ipv4_packet_size > mtu())
        return send_ipv4_fragmented(destination_mac, destination_ipv4, protocol, payload, payload_size, ttl);

    size_t ethernet_frame_size = sizeof(EthernetFrameHeader) + sizeof(IPv4Packet) + payload_size;
    auto buffer = ByteBuffer::create_zeroed(ethernet_frame_size);
    auto& eth = *(EthernetFrameHeader*)buffer.data();
    eth.set_source(mac_address());
    eth.set_destination(destination_mac);
    eth.set_ether_type(EtherType::IPv4);
    auto& ipv4 = *(IPv4Packet*)eth.payload();
    ipv4.set_version(4);
    ipv4.set_internet_header_length(5);
    ipv4.set_source(ipv4_address());
    ipv4.set_destination(destination_ipv4);
    ipv4.set_protocol((u8)protocol);
    ipv4.set_length(sizeof(IPv4Packet) + payload_size);
    ipv4.set_ident(1);
    ipv4.set_ttl(ttl);
    ipv4.set_checksum(ipv4.compute_checksum());
    m_packets_out++;
    m_bytes_out += ethernet_frame_size;

    if (!payload.read(ipv4.payload(), payload_size))
        return -EFAULT;
    send_raw({ (const u8*)&eth, ethernet_frame_size });
    return 0;
}

int NetworkAdapter::send_ipv4_fragmented(const MACAddress& destination_mac, const IPv4Address& destination_ipv4, IPv4Protocol protocol, const UserOrKernelBuffer& payload, size_t payload_size, u8 ttl)
{
    // packets must be split on the 64-bit boundary
    auto packet_boundary_size = (mtu() - sizeof(IPv4Packet) - sizeof(EthernetFrameHeader)) & 0xfffffff8;
    auto fragment_block_count = (payload_size + packet_boundary_size) / packet_boundary_size;
    auto last_block_size = payload_size - packet_boundary_size * (fragment_block_count - 1);
    auto number_of_blocks_in_fragment = packet_boundary_size / 8;

    auto identification = get_good_random<u16>();

    size_t ethernet_frame_size = mtu();
    for (size_t packet_index = 0; packet_index < fragment_block_count; ++packet_index) {
        auto is_last_block = packet_index + 1 == fragment_block_count;
        auto packet_payload_size = is_last_block ? last_block_size : packet_boundary_size;
        auto buffer = ByteBuffer::create_zeroed(ethernet_frame_size);
        auto& eth = *(EthernetFrameHeader*)buffer.data();
        eth.set_source(mac_address());
        eth.set_destination(destination_mac);
        eth.set_ether_type(EtherType::IPv4);
        auto& ipv4 = *(IPv4Packet*)eth.payload();
        ipv4.set_version(4);
        ipv4.set_internet_header_length(5);
        ipv4.set_source(ipv4_address());
        ipv4.set_destination(destination_ipv4);
        ipv4.set_protocol((u8)protocol);
        ipv4.set_length(sizeof(IPv4Packet) + packet_payload_size);
        ipv4.set_has_more_fragments(!is_last_block);
        ipv4.set_ident(identification);
        ipv4.set_ttl(ttl);
        ipv4.set_fragment_offset(packet_index * number_of_blocks_in_fragment);
        ipv4.set_checksum(ipv4.compute_checksum());
        m_packets_out++;
        m_bytes_out += ethernet_frame_size;
        if (!payload.read(ipv4.payload(), packet_index * packet_boundary_size, packet_payload_size))
            return -EFAULT;
        send_raw({ (const u8*)&eth, ethernet_frame_size });
    }
    return 0;
}

void NetworkAdapter::did_receive(ReadonlyBytes payload)
{
    InterruptDisabler disabler;
    m_packets_in++;
    m_bytes_in += payload.size();

    Optional<KBuffer> buffer;

    if (m_unused_packet_buffers.is_empty()) {
        buffer = KBuffer::copy(payload.data(), payload.size());
    } else {
        buffer = m_unused_packet_buffers.take_first();
        --m_unused_packet_buffers_count;
        if (payload.size() <= buffer.value().size()) {
            memcpy(buffer.value().data(), payload.data(), payload.size());
            buffer.value().set_size(payload.size());
        } else {
            buffer = KBuffer::copy(payload.data(), payload.size());
        }
    }

    m_packet_queue.append({ buffer.value(), kgettimeofday() });

    if (on_receive)
        on_receive();
}

size_t NetworkAdapter::dequeue_packet(u8* buffer, size_t buffer_size, timeval& packet_timestamp)
{
    InterruptDisabler disabler;
    if (m_packet_queue.is_empty())
        return 0;
    auto packet_with_timestamp = m_packet_queue.take_first();
    packet_timestamp = packet_with_timestamp.timestamp;
    auto packet = move(packet_with_timestamp.packet);
    size_t packet_size = packet.size();
    ASSERT(packet_size <= buffer_size);
    memcpy(buffer, packet.data(), packet_size);
    if (m_unused_packet_buffers_count < 100) {
        m_unused_packet_buffers.append(packet);
        ++m_unused_packet_buffers_count;
    }
    return packet_size;
}

void NetworkAdapter::set_ipv4_address(const IPv4Address& address)
{
    m_ipv4_address = address;
}

void NetworkAdapter::set_ipv4_netmask(const IPv4Address& netmask)
{
    m_ipv4_netmask = netmask;
}

void NetworkAdapter::set_ipv4_gateway(const IPv4Address& gateway)
{
    m_ipv4_gateway = gateway;
}

void NetworkAdapter::set_interface_name(const StringView& basename)
{
    // FIXME: Find a unique name for this interface, starting with $basename.
    StringBuilder builder;
    builder.append(basename);
    builder.append('0');
    m_name = builder.to_string();
}

}
