#include <AK/HashTable.h>
#include <AK/StringBuilder.h>
#include <Kernel/Lock.h>
#include <Kernel/Net/EtherType.h>
#include <Kernel/Net/EthernetFrameHeader.h>
#include <Kernel/Net/NetworkAdapter.h>
#include <Kernel/StdLib.h>
#include <Kernel/kmalloc.h>

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

NetworkAdapter* NetworkAdapter::from_ipv4_address(const IPv4Address& address)
{
    LOCKER(all_adapters().lock());
    for (auto* adapter : all_adapters().resource()) {
        if (adapter->ipv4_address() == address)
            return adapter;
    }
    return nullptr;
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
    auto* eth = (EthernetFrameHeader*)buffer.pointer();
    eth->set_source(mac_address());
    eth->set_destination(destination);
    eth->set_ether_type(EtherType::ARP);
    memcpy(eth->payload(), &packet, sizeof(ARPPacket));
    send_raw((u8*)eth, size_in_bytes);
}

void NetworkAdapter::send_ipv4(const MACAddress& destination_mac, const IPv4Address& destination_ipv4, IPv4Protocol protocol, ByteBuffer&& payload)
{
    size_t size_in_bytes = sizeof(EthernetFrameHeader) + sizeof(IPv4Packet) + payload.size();
    auto buffer = ByteBuffer::create_zeroed(size_in_bytes);
    auto& eth = *(EthernetFrameHeader*)buffer.pointer();
    eth.set_source(mac_address());
    eth.set_destination(destination_mac);
    eth.set_ether_type(EtherType::IPv4);
    auto& ipv4 = *(IPv4Packet*)eth.payload();
    ipv4.set_version(4);
    ipv4.set_internet_header_length(5);
    ipv4.set_source(ipv4_address());
    ipv4.set_destination(destination_ipv4);
    ipv4.set_protocol((u8)protocol);
    ipv4.set_length(sizeof(IPv4Packet) + payload.size());
    ipv4.set_ident(1);
    ipv4.set_ttl(64);
    ipv4.set_checksum(ipv4.compute_checksum());
    memcpy(ipv4.payload(), payload.pointer(), payload.size());
    send_raw((const u8*)&eth, size_in_bytes);
}

void NetworkAdapter::did_receive(const u8* data, int length)
{
    InterruptDisabler disabler;
    m_packet_queue.append(ByteBuffer::copy(data, length));
}

ByteBuffer NetworkAdapter::dequeue_packet()
{
    InterruptDisabler disabler;
    if (m_packet_queue.is_empty())
        return {};
    return m_packet_queue.take_first();
}

void NetworkAdapter::set_ipv4_address(const IPv4Address& address)
{
    m_ipv4_address = address;
}

void NetworkAdapter::set_interface_name(const StringView& basename)
{
    // FIXME: Find a unique name for this interface, starting with $basename.
    StringBuilder builder;
    builder.append(basename);
    builder.append('0');
    m_name = builder.to_string();
}
