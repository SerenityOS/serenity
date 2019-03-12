#include <Kernel/NetworkAdapter.h>
#include <Kernel/StdLib.h>
#include <Kernel/EthernetFrameHeader.h>
#include <Kernel/kmalloc.h>
#include <Kernel/EtherType.h>

NetworkAdapter::NetworkAdapter()
{
}

NetworkAdapter::~NetworkAdapter()
{
}

void NetworkAdapter::send(const MACAddress& destination, const ARPPacket& packet)
{
    int size_in_bytes = sizeof(EthernetFrameHeader) + sizeof(ARPPacket) + sizeof(EthernetFrameCheckSequence);
    auto* eth = (EthernetFrameHeader*)kmalloc(size_in_bytes);
    eth->set_source(mac_address());
    eth->set_destination(destination);
    eth->set_ether_type(EtherType::ARP);
    memcpy(eth->payload(), &packet, sizeof(ARPPacket));
    send_raw((byte*)eth, size_in_bytes);
    kfree(eth);
}

void NetworkAdapter::did_receive(const byte* data, int length)
{
    InterruptDisabler disabler;
    m_packet_queue.append(ByteBuffer::copy(data, length));
}

ByteBuffer NetworkAdapter::dequeue_packet()
{
    InterruptDisabler disabler;
    if (m_packet_queue.is_empty())
        return { };
    return m_packet_queue.take_first();
}

void NetworkAdapter::set_ipv4_address(const IPv4Address& address)
{
    m_ipv4_address = address;
}
