#include <Kernel/NetworkAdapter.h>
#include <Kernel/StdLib.h>
#include <Kernel/EthernetFrameHeader.h>
#include <Kernel/kmalloc.h>

NetworkAdapter::NetworkAdapter()
{
}

NetworkAdapter::~NetworkAdapter()
{
}

void NetworkAdapter::send(const MACAddress& destination, const ARPPacket& packet)
{
    int size_in_bytes = sizeof(EthernetFrameHeader) + sizeof(ARPPacket) + 4;
    auto* eth = (EthernetFrameHeader*)kmalloc(size_in_bytes);
    eth->set_source(mac_address());
    eth->set_destination(destination);
    memcpy(eth->payload(), &packet, sizeof(ARPPacket));
    send_raw((byte*)eth, size_in_bytes);
    kfree(eth);
}
