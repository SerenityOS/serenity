#include <Kernel/E1000NetworkAdapter.h>
#include <Kernel/EthernetFrameHeader.h>
#include <Kernel/ARPPacket.h>
#include <Kernel/Process.h>

void NetworkTask_main()
{
    auto* e1000_ptr = E1000NetworkAdapter::the();
    ASSERT(e1000_ptr);
    auto& e1000 = *e1000_ptr;
    ARPPacket arp;
    arp.hardware_type = 1; // Ethernet
    arp.hardware_address_length = 6; // MAC length
    arp.protocol_type = 0x0800; // IPv4
    arp.protocol_address_length = 4; // IP length
    arp.operation = 1; // 1 (request)
    e1000.send(MACAddress(), arp);

    kprintf("NetworkTask: Enter main loop.\n");
    for (;;) {
        auto packet = e1000.dequeue_packet();
        if (packet.is_null()) {
            sleep(100);
            continue;
        }
        if (packet.size() < sizeof(EthernetFrameHeader) + 4) {
            kprintf("NetworkTask: Packet is too small to be an Ethernet packet! (%d)\n", packet.size());
            continue;
        }
        auto* eth = (const EthernetFrameHeader*)packet.pointer();
        kprintf("NetworkTask: Handle packet from %s to %s\n", eth->source().to_string().characters(), eth->destination().to_string().characters());
    }
}
