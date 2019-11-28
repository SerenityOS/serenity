#pragma once

#include <AK/ByteBuffer.h>
#include <AK/Function.h>
#include <AK/SinglyLinkedList.h>
#include <AK/Types.h>
#include <AK/Weakable.h>
#include <AK/WeakPtr.h>
#include <Kernel/KBuffer.h>
#include <Kernel/Net/ARP.h>
#include <Kernel/Net/ICMP.h>
#include <Kernel/Net/IPv4.h>
#include <Kernel/Net/MACAddress.h>

class NetworkAdapter;

class NetworkAdapter : public Weakable<NetworkAdapter> {
public:
    static void for_each(Function<void(NetworkAdapter&)>);
    static WeakPtr<NetworkAdapter> from_ipv4_address(const IPv4Address&);
    static WeakPtr<NetworkAdapter> lookup_by_name(const StringView&);
    virtual ~NetworkAdapter();

    virtual const char* class_name() const = 0;

    const String& name() const { return m_name; }
    MACAddress mac_address() { return m_mac_address; }
    IPv4Address ipv4_address() const { return m_ipv4_address; }
    IPv4Address ipv4_netmask() const { return m_ipv4_netmask; }
    IPv4Address ipv4_gateway() const { return m_ipv4_gateway; }
    virtual bool link_up() { return false; }

    void set_ipv4_address(const IPv4Address&);
    void set_ipv4_netmask(const IPv4Address&);
    void set_ipv4_gateway(const IPv4Address&);

    void send(const MACAddress&, const ARPPacket&);
    void send_ipv4(const MACAddress&, const IPv4Address&, IPv4Protocol, const u8* payload, size_t payload_size, u8 ttl);

    Optional<KBuffer> dequeue_packet();

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
    virtual void send_raw(const u8*, int) = 0;
    void did_receive(const u8*, int);

private:
    MACAddress m_mac_address;
    IPv4Address m_ipv4_address;
    IPv4Address m_ipv4_netmask;
    IPv4Address m_ipv4_gateway;
    SinglyLinkedList<KBuffer> m_packet_queue;
    String m_name;
    u32 m_packets_in { 0 };
    u32 m_bytes_in { 0 };
    u32 m_packets_out { 0 };
    u32 m_bytes_out { 0 };
    u32 m_mtu { 1500 };
};
