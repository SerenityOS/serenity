/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/Function.h>
#include <AK/MACAddress.h>
#include <AK/SinglyLinkedList.h>
#include <AK/Types.h>
#include <AK/WeakPtr.h>
#include <AK/Weakable.h>
#include <Kernel/KBuffer.h>
#include <Kernel/Net/ARP.h>
#include <Kernel/Net/ICMP.h>
#include <Kernel/Net/IPv4.h>
#include <Kernel/UserOrKernelBuffer.h>

namespace Kernel {

class NetworkAdapter;

class NetworkAdapter : public RefCounted<NetworkAdapter> {
public:
    static void for_each(Function<void(NetworkAdapter&)>);
    static RefPtr<NetworkAdapter> from_ipv4_address(const IPv4Address&);
    static RefPtr<NetworkAdapter> lookup_by_name(const StringView&);
    virtual ~NetworkAdapter();

    virtual const char* class_name() const = 0;

    const String& name() const { return m_name; }
    MACAddress mac_address() { return m_mac_address; }
    IPv4Address ipv4_address() const { return m_ipv4_address; }
    IPv4Address ipv4_netmask() const { return m_ipv4_netmask; }
    IPv4Address ipv4_broadcast() const { return IPv4Address { (m_ipv4_address.to_u32() & m_ipv4_netmask.to_u32()) | ~m_ipv4_netmask.to_u32() }; }
    IPv4Address ipv4_gateway() const { return m_ipv4_gateway; }
    virtual bool link_up() { return false; }

    void set_ipv4_address(const IPv4Address&);
    void set_ipv4_netmask(const IPv4Address&);
    void set_ipv4_gateway(const IPv4Address&);

    void send(const MACAddress&, const ARPPacket&);
    KResult send_ipv4(const IPv4Address& source_ipv4, const MACAddress&, const IPv4Address&, IPv4Protocol, const UserOrKernelBuffer& payload, size_t payload_size, u8 ttl);
    KResult send_ipv4_fragmented(const IPv4Address& source_ipv4, const MACAddress&, const IPv4Address&, IPv4Protocol, const UserOrKernelBuffer& payload, size_t payload_size, u8 ttl);

    size_t dequeue_packet(u8* buffer, size_t buffer_size, Time& packet_timestamp);

    bool has_queued_packets() const { return !m_packet_queue.is_empty(); }

    u32 mtu() const { return m_mtu; }
    void set_mtu(u32 mtu) { m_mtu = mtu; }

    u32 packets_in() const { return m_packets_in; }
    u32 bytes_in() const { return m_bytes_in; }
    u32 packets_out() const { return m_packets_out; }
    u32 bytes_out() const { return m_bytes_out; }

    Function<void()> on_receive;

protected:
    NetworkAdapter();
    void set_interface_name(const StringView& basename);
    void set_mac_address(const MACAddress& mac_address) { m_mac_address = mac_address; }
    virtual void send_raw(ReadonlyBytes) = 0;
    void did_receive(ReadonlyBytes);

private:
    MACAddress m_mac_address;
    IPv4Address m_ipv4_address;
    IPv4Address m_ipv4_netmask;
    IPv4Address m_ipv4_gateway;

    struct PacketWithTimestamp {
        KBuffer packet;
        Time timestamp;
    };

    // FIXME: Make this configurable
    static constexpr size_t max_packet_buffers = 1024;

    SinglyLinkedList<PacketWithTimestamp> m_packet_queue;
    size_t m_packet_queue_size { 0 };
    SinglyLinkedList<KBuffer> m_unused_packet_buffers;
    size_t m_unused_packet_buffers_count { 0 };
    String m_name;
    u32 m_packets_in { 0 };
    u32 m_bytes_in { 0 };
    u32 m_packets_out { 0 };
    u32 m_bytes_out { 0 };
    u32 m_mtu { 1500 };
};

}
