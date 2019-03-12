#include <Kernel/E1000NetworkAdapter.h>
#include <Kernel/EthernetFrameHeader.h>
#include <Kernel/ARPPacket.h>
#include <Kernel/ICMP.h>
#include <Kernel/IPv4Packet.h>
#include <Kernel/Process.h>
#include <Kernel/EtherType.h>
#include <AK/Lock.h>

//#define ETHERNET_DEBUG
//#define IPV4_DEBUG

static void handle_arp(const EthernetFrameHeader&, int frame_size);
static void handle_ipv4(const EthernetFrameHeader&, int frame_size);
static void handle_icmp(const EthernetFrameHeader&, int frame_size);

Lockable<HashMap<IPv4Address, MACAddress>>& arp_table()
{
    static Lockable<HashMap<IPv4Address, MACAddress>>* the;
    if (!the)
        the = new Lockable<HashMap<IPv4Address, MACAddress>>;
    return *the;
}

void NetworkTask_main()
{
    auto* e1000_ptr = E1000NetworkAdapter::the();
    ASSERT(e1000_ptr);
    auto& e1000 = *e1000_ptr;
    e1000.set_ipv4_address(IPv4Address(192, 168, 5, 2));

    kprintf("NetworkTask: Enter main loop.\n");
    for (;;) {
        auto packet = e1000.dequeue_packet();
        if (packet.is_null()) {
            sleep(100);
            continue;
        }
        if (packet.size() < (int)(sizeof(EthernetFrameHeader) + sizeof(EthernetFrameCheckSequence))) {
            kprintf("NetworkTask: Packet is too small to be an Ethernet packet! (%d)\n", packet.size());
            continue;
        }
        auto& eth = *(const EthernetFrameHeader*)packet.pointer();
#ifdef ETHERNET_DEBUG
        kprintf("NetworkTask: From %s to %s, ether_type=%w, packet_length=%u\n",
            eth.source().to_string().characters(),
            eth.destination().to_string().characters(),
            eth.ether_type(),
            packet.size()
        );
#endif

        switch (eth.ether_type()) {
        case EtherType::ARP:
            handle_arp(eth, packet.size());
            break;
        case EtherType::IPv4:
            handle_ipv4(eth, packet.size());
            break;
        }
    }
}

void handle_arp(const EthernetFrameHeader& eth, int frame_size)
{
    constexpr int minimum_arp_frame_size = sizeof(EthernetFrameHeader) + sizeof(ARPPacket) + sizeof(EthernetFrameCheckSequence);
    if (frame_size < minimum_arp_frame_size) {
        kprintf("handle_arp: Frame too small (%d, need %d)\n", frame_size, minimum_arp_frame_size);
        return;
    }
    auto& packet = *static_cast<const ARPPacket*>(eth.payload());
    if (packet.hardware_type() != 1 || packet.hardware_address_length() != sizeof(MACAddress)) {
        kprintf("handle_arp: Hardware type not ethernet (%w, len=%u)\n",
            packet.hardware_type(),
            packet.hardware_address_length()
        );
        return;
    }
    if (packet.protocol_type() != EtherType::IPv4 || packet.protocol_address_length() != sizeof(IPv4Address)) {
        kprintf("handle_arp: Protocol type not IPv4 (%w, len=%u)\n",
            packet.hardware_type(),
            packet.protocol_address_length()
        );
        return;
    }

#ifdef ARP_DEBUG
    kprintf("handle_arp: operation=%w, sender=%s/%s, target=%s/%s\n",
        packet.operation(),
        packet.sender_hardware_address().to_string().characters(),
        packet.sender_protocol_address().to_string().characters(),
        packet.target_hardware_address().to_string().characters(),
        packet.target_protocol_address().to_string().characters()
    );
#endif

    // FIXME: Get the adapter through some kind of lookup by IPv4 address.
    auto& e1000 = *E1000NetworkAdapter::the();

    if (packet.operation() == ARPOperation::Request) {
        // Who has this IP address?
        if (e1000.ipv4_address() == packet.target_protocol_address()) {
            // We do!
            kprintf("handle_arp: Responding to ARP request for my IPv4 address (%s)\n",
                    e1000.ipv4_address().to_string().characters());
            ARPPacket response;
            response.set_operation(ARPOperation::Response);
            response.set_target_hardware_address(packet.sender_hardware_address());
            response.set_target_protocol_address(packet.sender_protocol_address());
            response.set_sender_hardware_address(e1000.mac_address());
            response.set_sender_protocol_address(e1000.ipv4_address());

            e1000.send(packet.sender_hardware_address(), response);
        }
        return;
    }

    if (packet.operation() == ARPOperation::Response) {
        // Someone has this IPv4 address. I guess we can try to remember that.
        // FIXME: Protect against ARP spamming.
        // FIXME: Support static ARP table entries.
        LOCKER(arp_table().lock());
        arp_table().resource().set(packet.sender_protocol_address(), packet.sender_hardware_address());

        kprintf("ARP table (%d entries):\n", arp_table().resource().size());
        for (auto& it : arp_table().resource()) {
            kprintf("%s :: %s\n", it.value.to_string().characters(), it.key.to_string().characters());
        }
    }
}

void handle_ipv4(const EthernetFrameHeader& eth, int frame_size)
{
    constexpr int minimum_ipv4_frame_size = sizeof(EthernetFrameHeader) + sizeof(IPv4Packet) + sizeof(EthernetFrameCheckSequence);
    if (frame_size < minimum_ipv4_frame_size) {
        kprintf("handle_ipv4: Frame too small (%d, need %d)\n", frame_size, minimum_ipv4_frame_size);
        return;
    }
    auto& packet = *static_cast<const IPv4Packet*>(eth.payload());

#ifdef IPV4_DEBUG
    kprintf("handle_ipv4: source=%s, target=%s\n",
        packet.source().to_string().characters(),
        packet.destination().to_string().characters()
    );
#endif

    switch (packet.protocol()) {
    case IPv4Protocol::ICMP:
        return handle_icmp(eth, frame_size);
    default:
        kprintf("handle_ipv4: Unhandled protocol %u\n", packet.protocol());
        break;
    }
}

void handle_icmp(const EthernetFrameHeader& eth, int frame_size)
{
    auto& ipv4_packet = *static_cast<const IPv4Packet*>(eth.payload());
    auto& icmp_header = *static_cast<const ICMPHeader*>(ipv4_packet.payload());
    kprintf("handle_icmp: type=%b, code=%b\n", icmp_header.type(), icmp_header.code());

    auto& e1000 = *E1000NetworkAdapter::the();
    if (ipv4_packet.destination() == e1000.ipv4_address()) {
        // It's for me!
        if (icmp_header.type() == ICMPType::EchoRequest) {
            auto& request = reinterpret_cast<const ICMPEchoPacket&>(icmp_header);
            kprintf("ICMP echo request: id=%u, seq=%u\n", (int)request.identifier, (int)request.sequence_number);
            byte* response_buffer = (byte*)kmalloc(ipv4_packet.length());
            memset(response_buffer, 0, ipv4_packet.length());
            struct [[gnu::packed]] EchoResponse {
                IPv4Packet ipv4;
                ICMPEchoPacket icmp_echo;
            };
            auto& response = *(EchoResponse*)response_buffer;
            response.ipv4.set_version(4);
            response.ipv4.set_internet_header_length(5);
            response.ipv4.set_source(e1000.ipv4_address());
            response.ipv4.set_destination(ipv4_packet.source());
            response.ipv4.set_protocol(IPv4Protocol::ICMP);
            response.ipv4.set_length(sizeof(IPv4ICMPPacket));
            response.icmp_echo.header.set_type(ICMPType::EchoReply);
            response.icmp_echo.header.set_code(0);
            response.icmp_echo.identifier = request.identifier;
            response.icmp_echo.sequence_number = request.sequence_number;
            memcpy(response.icmp_echo.payload, request.payload, ipv4_packet.length() - sizeof(ICMPEchoPacket));
            e1000.send_ipv4(eth.source(), &response, ipv4_packet.length());
            kfree(response_buffer);
        }
    }
}
