/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/AtomicRefCounted.h>
#include <AK/ByteBuffer.h>
#include <AK/Function.h>
#include <AK/IPv6Address.h>
#include <AK/IntrusiveList.h>
#include <AK/MACAddress.h>
#include <AK/Types.h>
#include <Kernel/Bus/PCI/Definitions.h>
#include <Kernel/Library/KBuffer.h>
#include <Kernel/Library/LockWeakPtr.h>
#include <Kernel/Library/LockWeakable.h>
#include <Kernel/Library/UserOrKernelBuffer.h>
#include <Kernel/Net/EthernetFrameHeader.h>
#include <Kernel/Net/ICMP.h>
#include <Kernel/Net/IP/ARP.h>
#include <Kernel/Net/IP/IP.h>
#include <Kernel/Net/IP/IPv4.h>
#include <Kernel/Net/IP/IPv6.h>

namespace Kernel {

class NetworkAdapter;

using NetworkByteBuffer = AK::Detail::ByteBuffer<1500>;

struct PacketWithTimestamp final : public AtomicRefCounted<PacketWithTimestamp> {
    PacketWithTimestamp(NonnullOwnPtr<KBuffer> buffer, UnixDateTime timestamp)
        : buffer(move(buffer))
        , timestamp(timestamp)
    {
    }

    ReadonlyBytes bytes() { return buffer->bytes(); }

    NonnullOwnPtr<KBuffer> buffer;
    UnixDateTime timestamp;
    IntrusiveListNode<PacketWithTimestamp, RefPtr<PacketWithTimestamp>> packet_node;
};

class NetworkingManagement;
class NetworkAdapter
    : public AtomicRefCounted<NetworkAdapter>
    , public LockWeakable<NetworkAdapter> {
public:
    enum class Type {
        Loopback,
        Ethernet
    };

    static constexpr i32 LINKSPEED_INVALID = -1;

    virtual ~NetworkAdapter();

    virtual StringView class_name() const = 0;
    virtual Type adapter_type() const = 0;
    virtual ErrorOr<void> initialize(Badge<NetworkingManagement>) = 0;

    StringView name() const { return m_name.representable_view(); }
    MACAddress mac_address() { return m_mac_address; }
    IPv4Address ipv4_address() const { return m_ipv4_address; }
    IPv4Address ipv4_netmask() const { return m_ipv4_netmask; }
    IPv4Address ipv4_broadcast() const { return IPv4Address { (m_ipv4_address.to_u32() & m_ipv4_netmask.to_u32()) | ~m_ipv4_netmask.to_u32() }; }

    IPv6Address ipv6_address() const { return m_ipv6_address; }
    IPv6Address ipv6_netmask() const { return m_ipv6_netmask; }
    // TODO: implement other multicast addresses
    IPv6Address ipv6_multicast() const { return IPv6Address({ 0xff, 0x01, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 }); }

    virtual bool link_up() { return false; }
    virtual i32 link_speed()
    {
        // In Mbit/sec.
        return LINKSPEED_INVALID;
    }
    virtual bool link_full_duplex() { return false; }

    void set_ipv4_address(IPv4Address const&);
    void set_ipv4_netmask(IPv4Address const&);

    void set_ipv6_address(IPv6Address const&);
    void set_ipv6_netmask(IPv6Address const&);

    void send(MACAddress const&, ARPPacket const&);
    void fill_in_ipv4_header(PacketWithTimestamp&, IPv4Address const&, MACAddress const&, IPv4Address const&, TransportProtocol, size_t, u8 type_of_service, u8 ttl);
    void fill_in_ipv6_header(PacketWithTimestamp&, IPv6Address const&, MACAddress const&, IPv6Address const&, TransportProtocol, size_t, u8 hop_limit);

    size_t dequeue_packet(u8* buffer, size_t buffer_size, UnixDateTime& packet_timestamp);

    bool has_queued_packets() const { return !m_packet_queue.is_empty(); }

    u32 mtu() const { return m_mtu; }
    void set_mtu(u32 mtu) { m_mtu = mtu; }

    u32 packets_in() const { return m_packets_in; }
    u32 bytes_in() const { return m_bytes_in; }
    u32 packets_out() const { return m_packets_out; }
    u32 bytes_out() const { return m_bytes_out; }
    u32 packets_dropped() const { return m_packets_dropped; }

    RefPtr<PacketWithTimestamp> acquire_packet_buffer(size_t);
    void release_packet_buffer(PacketWithTimestamp&);

    constexpr size_t layer3_payload_offset() const { return sizeof(EthernetFrameHeader); }
    constexpr size_t ipv4_payload_offset() const { return layer3_payload_offset() + sizeof(IPv4Packet); }
    constexpr size_t ipv6_payload_offset() const { return layer3_payload_offset() + sizeof(IPv6PacketHeader); }

    Function<void()> on_receive;

    void send_packet(ReadonlyBytes);

protected:
    NetworkAdapter(StringView);
    void set_mac_address(MACAddress const& mac_address) { m_mac_address = mac_address; }
    void did_receive(ReadonlyBytes);
    virtual void send_raw(ReadonlyBytes) = 0;
    void autoconfigure_link_local_ipv6();

private:
    MACAddress m_mac_address;
    // FIXME: Allow for more than one IPv4/IPv6 address each.
    IPv4Address m_ipv4_address;
    IPv4Address m_ipv4_netmask;
    IPv6Address m_ipv6_address;
    IPv6Address m_ipv6_netmask;

    // FIXME: Make this configurable
    static constexpr size_t max_packet_buffers = 1024;

    using PacketList = IntrusiveList<&PacketWithTimestamp::packet_node>;

    PacketList m_packet_queue;
    size_t m_packet_queue_size { 0 };
    SpinlockProtected<PacketList, LockRank::None> m_unused_packets {};
    FixedStringBuffer<IFNAMSIZ> m_name;
    u32 m_packets_in { 0 };
    u32 m_bytes_in { 0 };
    u32 m_packets_out { 0 };
    u32 m_bytes_out { 0 };
    u32 m_mtu { 1500 };
    u32 m_packets_dropped { 0 };
};

}
