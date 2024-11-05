/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Heap/kmalloc.h>
#include <Kernel/Interrupts/InterruptDisabler.h>
#include <Kernel/Library/StdLib.h>
#include <Kernel/Net/EtherType.h>
#include <Kernel/Net/NetworkAdapter.h>
#include <Kernel/Net/NetworkingManagement.h>
#include <Kernel/Tasks/Process.h>

namespace Kernel {

NetworkAdapter::NetworkAdapter(StringView interface_name)
{
    m_name.store_characters(interface_name);
}

NetworkAdapter::~NetworkAdapter() = default;

void NetworkAdapter::send_packet(ReadonlyBytes packet)
{
    m_packets_out++;
    m_bytes_out += packet.size();
    send_raw(packet);
}

void NetworkAdapter::send(MACAddress const& destination, ARPPacket const& packet)
{
    size_t size_in_bytes = sizeof(EthernetFrameHeader) + sizeof(ARPPacket);
    auto buffer_result = NetworkByteBuffer::create_zeroed(size_in_bytes);
    if (buffer_result.is_error()) {
        dbgln("Dropping ARP packet targeted at {} as there is not enough memory to buffer it", packet.target_hardware_address().to_string());
        return;
    }
    auto* eth = (EthernetFrameHeader*)buffer_result.value().data();
    eth->set_source(mac_address());
    eth->set_destination(destination);
    eth->set_ether_type(EtherType::ARP);
    memcpy(eth->payload(), &packet, sizeof(ARPPacket));
    send_packet({ (u8 const*)eth, size_in_bytes });
}

void NetworkAdapter::fill_in_ipv4_header(PacketWithTimestamp& packet, IPv4Address const& source_ipv4, MACAddress const& destination_mac, IPv4Address const& destination_ipv4, TransportProtocol protocol, size_t payload_size, u8 type_of_service, u8 ttl)
{
    size_t ipv4_packet_size = sizeof(IPv4Packet) + payload_size;
    VERIFY(ipv4_packet_size <= mtu());

    size_t ethernet_frame_size = ipv4_payload_offset() + payload_size;
    VERIFY(packet.buffer->size() == ethernet_frame_size);
    memset(packet.buffer->data(), 0, ipv4_payload_offset());

    auto& eth = *bit_cast<EthernetFrameHeader*>(packet.buffer->data());
    eth.set_source(mac_address());
    eth.set_destination(destination_mac);
    eth.set_ether_type(EtherType::IPv4);

    auto& ipv4 = *bit_cast<IPv4Packet*>(eth.payload());
    ipv4.set_version(4);
    ipv4.set_internet_header_length(5);
    ipv4.set_dscp_and_ecn(type_of_service);
    ipv4.set_source(source_ipv4);
    ipv4.set_destination(destination_ipv4);
    ipv4.set_protocol((u8)protocol);
    ipv4.set_length(ipv4_packet_size);
    ipv4.set_ident(1);
    ipv4.set_ttl(ttl);
    ipv4.set_checksum(ipv4.compute_checksum());
}

void NetworkAdapter::fill_in_ipv6_header(PacketWithTimestamp& packet, IPv6Address const& source_ipv6, MACAddress const& destination_mac, IPv6Address const& destination_ipv6, TransportProtocol protocol, size_t payload_size, u8 hop_limit)
{
    size_t ipv6_packet_size = sizeof(IPv6PacketHeader) + payload_size;
    VERIFY(ipv6_packet_size <= mtu());

    size_t ethernet_frame_size = ipv6_payload_offset() + payload_size;
    VERIFY(packet.buffer->size() == ethernet_frame_size);
    memset(packet.buffer->data(), 0, ipv6_payload_offset());

    auto& eth = *bit_cast<EthernetFrameHeader*>(packet.buffer->data());
    eth.set_source(mac_address());
    eth.set_destination(destination_mac);
    eth.set_ether_type(EtherType::IPv6);

    auto& ipv6 = *bit_cast<IPv6PacketHeader*>(eth.payload());
    ipv6.set_version(6);
    ipv6.set_destination(destination_ipv6);
    ipv6.set_source(source_ipv6);

    switch (protocol) {
    case TransportProtocol::ICMPv6:
        ipv6.set_next_header(static_cast<u8>(TransportProtocol::ICMPv6));
        break;
    default:
        dbgln("fill_in_ipv6_header: Unknown TransportProtocol, setting NoNextHeader");
        ipv6.set_next_header(static_cast<u8>(IPv6NextHeader::NoNextHeader));
    }

    ipv6.set_length(payload_size);
    ipv6.set_hop_limit(hop_limit);
}

void NetworkAdapter::did_receive(ReadonlyBytes payload)
{
    InterruptDisabler disabler;
    m_packets_in++;
    m_bytes_in += payload.size();

    if (m_packet_queue_size == max_packet_buffers) {
        m_packets_dropped++;
        return;
    }

    auto packet = acquire_packet_buffer(payload.size());
    if (!packet) {
        dbgln("Discarding packet because we're out of memory");
        return;
    }

    memcpy(packet->buffer->data(), payload.data(), payload.size());

    m_packet_queue.append(*packet);
    m_packet_queue_size++;

    if (on_receive)
        on_receive();
}

size_t NetworkAdapter::dequeue_packet(u8* buffer, size_t buffer_size, UnixDateTime& packet_timestamp)
{
    InterruptDisabler disabler;
    if (m_packet_queue.is_empty())
        return 0;
    auto packet_with_timestamp = m_packet_queue.take_first();
    m_packet_queue_size--;
    packet_timestamp = packet_with_timestamp->timestamp;
    auto& packet_buffer = packet_with_timestamp->buffer;
    size_t packet_size = packet_buffer->size();
    VERIFY(packet_size <= buffer_size);
    memcpy(buffer, packet_buffer->data(), packet_size);
    release_packet_buffer(*packet_with_timestamp);
    return packet_size;
}

RefPtr<PacketWithTimestamp> NetworkAdapter::acquire_packet_buffer(size_t size)
{
    auto packet = m_unused_packets.with([size](auto& unused_packets) -> RefPtr<PacketWithTimestamp> {
        if (unused_packets.is_empty())
            return nullptr;

        auto unused_packet = unused_packets.take_first();

        if (unused_packet->buffer->capacity() >= size)
            return unused_packet;

        unused_packets.append(*unused_packet);
        return nullptr;
    });

    if (packet) {
        packet->timestamp = kgettimeofday();
        packet->buffer->set_size(size);
        return packet;
    }

    auto buffer_or_error = KBuffer::try_create_with_size("NetworkAdapter: Packet buffer"sv, size, Memory::Region::Access::ReadWrite, AllocationStrategy::AllocateNow);
    if (buffer_or_error.is_error())
        return {};
    packet = adopt_ref_if_nonnull(new (nothrow) PacketWithTimestamp { buffer_or_error.release_value(), kgettimeofday() });
    if (!packet)
        return {};
    packet->buffer->set_size(size);
    return packet;
}

void NetworkAdapter::release_packet_buffer(PacketWithTimestamp& packet)
{
    m_unused_packets.with([&packet](auto& unused_packets) {
        unused_packets.append(packet);
    });
}

void NetworkAdapter::set_ipv4_address(IPv4Address const& address)
{
    m_ipv4_address = address;
}

void NetworkAdapter::set_ipv4_netmask(IPv4Address const& netmask)
{
    m_ipv4_netmask = netmask;
}

void NetworkAdapter::set_ipv6_address(IPv6Address const& address)
{
    m_ipv6_address = address;
}

void NetworkAdapter::set_ipv6_netmask(IPv6Address const& netmask)
{
    m_ipv6_netmask = netmask;
}

void NetworkAdapter::autoconfigure_link_local_ipv6()
{
    auto mac = mac_address();
    if (mac.is_zero() || !link_up())
        return;

    // TODO: other IPv6 autoconf modes
    // TODO: duplicate address detection as mandated by RFC 4862, this is only a very naive implementation of autoconf
    auto ipv6_ll = IPv6Address({ 0xfe,
        0x80,
        0,
        0,
        0,
        0,
        0,
        0,
        static_cast<u8>(mac[0] ^ 0b00000010),
        mac[1],
        mac[2],
        0xff,
        0xfe,
        mac[3],
        mac[4],
        mac[5] });
    auto netmask = IPv6Address({ 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0, 0, 0, 0, 0, 0, 0, 0 });
    set_ipv6_address(ipv6_ll);
    set_ipv6_netmask(netmask);
    dbgln("autoconfigured link-local address {}", ipv6_ll.to_string());
}

}
