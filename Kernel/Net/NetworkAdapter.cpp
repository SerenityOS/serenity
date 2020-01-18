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
#include <AK/StringBuilder.h>
#include <Kernel/Heap/kmalloc.h>
#include <Kernel/Lock.h>
#include <Kernel/Net/EtherType.h>
#include <Kernel/Net/EthernetFrameHeader.h>
#include <Kernel/Net/LoopbackAdapter.h>
#include <Kernel/Net/NetworkAdapter.h>
#include <Kernel/StdLib.h>

static Lockable<HashTable<NetworkAdapter*>>& all_adapters()
{
    static Lockable<HashTable<NetworkAdapter*>>* table;
    if (!table)
        table = new Lockable<HashTable<NetworkAdapter*>>;
    return *table;
}

void NetworkAdapter::for_each(Function<void(NetworkAdapter&)> callback)
{
    LOCKER(all_adapters().lock());
    for (auto& it : all_adapters().resource())
        callback(*it);
}

WeakPtr<NetworkAdapter> NetworkAdapter::from_ipv4_address(const IPv4Address& address)
{
    LOCKER(all_adapters().lock());
    for (auto* adapter : all_adapters().resource()) {
        if (adapter->ipv4_address() == address)
            return adapter->make_weak_ptr();
    }
    if (address[0] == 127)
        return LoopbackAdapter::the().make_weak_ptr();
    return nullptr;
}

WeakPtr<NetworkAdapter> NetworkAdapter::lookup_by_name(const StringView& name)
{
    NetworkAdapter* found_adapter = nullptr;
    for_each([&](auto& adapter) {
        if (adapter.name() == name)
            found_adapter = &adapter;
    });
    return found_adapter ? found_adapter->make_weak_ptr() : nullptr;
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
    int size_in_bytes = sizeof(EthernetFrameHeader) + sizeof(ARPPacket);
    auto buffer = ByteBuffer::create_zeroed(size_in_bytes);
    auto* eth = (EthernetFrameHeader*)buffer.data();
    eth->set_source(mac_address());
    eth->set_destination(destination);
    eth->set_ether_type(EtherType::ARP);
    m_packets_out++;
    m_bytes_out += size_in_bytes;
    memcpy(eth->payload(), &packet, sizeof(ARPPacket));
    send_raw((const u8*)eth, size_in_bytes);
}

void NetworkAdapter::send_ipv4(const MACAddress& destination_mac, const IPv4Address& destination_ipv4, IPv4Protocol protocol, const u8* payload, size_t payload_size, u8 ttl)
{
    size_t ipv4_packet_size = sizeof(IPv4Packet) + payload_size;
    if (ipv4_packet_size > mtu()) {
        // FIXME: Implement IP fragmentation.
        ASSERT_NOT_REACHED();
    }

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
    memcpy(ipv4.payload(), payload, payload_size);
    send_raw((const u8*)&eth, ethernet_frame_size);
}

void NetworkAdapter::did_receive(const u8* data, int length)
{
    InterruptDisabler disabler;
    m_packets_in++;
    m_bytes_in += length;

    Optional<KBuffer> buffer;

    if (m_unused_packet_buffers.is_empty()) {
        buffer = KBuffer::copy(data, length);
    } else {
        buffer = m_unused_packet_buffers.take_first();
        --m_unused_packet_buffers_count;
        if ((size_t)length <= buffer.value().size()) {
            memcpy(buffer.value().data(), data, length);
            buffer.value().set_size(length);
        } else {
            buffer = KBuffer::copy(data, length);
        }
    }

    m_packet_queue.append(buffer.value());

    if (on_receive)
        on_receive();
}

size_t NetworkAdapter::dequeue_packet(u8* buffer, size_t buffer_size)
{
    InterruptDisabler disabler;
    if (m_packet_queue.is_empty())
        return 0;
    auto packet = m_packet_queue.take_first();
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
