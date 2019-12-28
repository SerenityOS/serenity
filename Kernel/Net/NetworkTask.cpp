#include <Kernel/Lock.h>
#include <Kernel/Net/ARP.h>
#include <Kernel/Net/EtherType.h>
#include <Kernel/Net/EthernetFrameHeader.h>
#include <Kernel/Net/ICMP.h>
#include <Kernel/Net/IPv4.h>
#include <Kernel/Net/IPv4Socket.h>
#include <Kernel/Net/LoopbackAdapter.h>
#include <Kernel/Net/Routing.h>
#include <Kernel/Net/TCP.h>
#include <Kernel/Net/TCPSocket.h>
#include <Kernel/Net/UDP.h>
#include <Kernel/Net/UDPSocket.h>
#include <Kernel/Process.h>

//#define NETWORK_TASK_DEBUG
//#define ETHERNET_DEBUG
//#define ETHERNET_VERY_DEBUG
//#define ARP_DEBUG
//#define IPV4_DEBUG
//#define ICMP_DEBUG
//#define UDP_DEBUG
//#define TCP_DEBUG

static void handle_arp(const EthernetFrameHeader&, size_t frame_size);
static void handle_ipv4(const EthernetFrameHeader&, size_t frame_size);
static void handle_icmp(const EthernetFrameHeader&, const IPv4Packet&);
static void handle_udp(const IPv4Packet&);
static void handle_tcp(const IPv4Packet&);

void NetworkTask_main()
{
    WaitQueue packet_wait_queue;
    u8 octet = 15;
    int pending_packets = 0;
    NetworkAdapter::for_each([&](auto& adapter) {
        if (String(adapter.class_name()) == "LoopbackAdapter") {
            adapter.set_ipv4_address({ 127, 0, 0, 1 });
            adapter.set_ipv4_netmask({ 255, 0, 0, 0 });
            adapter.set_ipv4_gateway({ 0, 0, 0, 0 });
        } else {
            adapter.set_ipv4_address({ 10, 0, 2, octet++ });
            adapter.set_ipv4_netmask({ 255, 255, 255, 0 });
            adapter.set_ipv4_gateway({ 10, 0, 2, 2 });
        }

        kprintf("NetworkTask: %s network adapter found: hw=%s address=%s netmask=%s gateway=%s\n",
            adapter.class_name(),
            adapter.mac_address().to_string().characters(),
            adapter.ipv4_address().to_string().characters(),
            adapter.ipv4_netmask().to_string().characters(),
            adapter.ipv4_gateway().to_string().characters());

        adapter.on_receive = [&]() {
            pending_packets++;
            packet_wait_queue.wake_all();
        };
    });

    auto dequeue_packet = [&pending_packets](u8* buffer, size_t buffer_size) -> size_t {
        if (pending_packets == 0)
            return 0;
        size_t packet_size = 0;
        NetworkAdapter::for_each([&](auto& adapter) {
            if (packet_size || !adapter.has_queued_packets())
                return;
            packet_size = adapter.dequeue_packet(buffer, buffer_size);
            pending_packets--;
#ifdef NETWORK_TASK_DEBUG
            kprintf("NetworkTask: Dequeued packet from %s (%d bytes)\n", adapter.name().characters(), packet_size);
#endif
        });
        return packet_size;
    };

    size_t buffer_size = 64 * KB;
    auto buffer_region = MM.allocate_kernel_region(buffer_size, "Kernel Packet Buffer", Region::Access::Read | Region::Access::Write, false, true);
    auto buffer = (u8*)buffer_region->vaddr().get();

    kprintf("NetworkTask: Enter main loop.\n");
    for (;;) {
        size_t packet_size = dequeue_packet(buffer, buffer_size);
        if (!packet_size) {
            current->wait_on(packet_wait_queue);
            continue;
        }
        if (packet_size < sizeof(EthernetFrameHeader)) {
            kprintf("NetworkTask: Packet is too small to be an Ethernet packet! (%zu)\n", packet_size);
            continue;
        }
        auto& eth = *(const EthernetFrameHeader*)buffer;
#ifdef ETHERNET_DEBUG
        kprintf("NetworkTask: From %s to %s, ether_type=%w, packet_length=%u\n",
            eth.source().to_string().characters(),
            eth.destination().to_string().characters(),
            eth.ether_type(),
            packet_size);
#endif

#ifdef ETHERNET_VERY_DEBUG
        for (size_t i = 0; i < packet_size; i++) {
            kprintf("%b", buffer[i]);

            switch (i % 16) {
            case 7:
                kprintf("  ");
                break;
            case 15:
                kprintf("\n");
                break;
            default:
                kprintf(" ");
                break;
            }
        }

        kprintf("\n");
#endif

        switch (eth.ether_type()) {
        case EtherType::ARP:
            handle_arp(eth, packet_size);
            break;
        case EtherType::IPv4:
            handle_ipv4(eth, packet_size);
            break;
        case EtherType::IPv6:
            // ignore
            break;
        default:
            kprintf("NetworkTask: Unknown ethernet type %#04x\n", eth.ether_type());
        }
    }
}

void handle_arp(const EthernetFrameHeader& eth, size_t frame_size)
{
    constexpr size_t minimum_arp_frame_size = sizeof(EthernetFrameHeader) + sizeof(ARPPacket);
    if (frame_size < minimum_arp_frame_size) {
        kprintf("handle_arp: Frame too small (%d, need %d)\n", frame_size, minimum_arp_frame_size);
        return;
    }
    auto& packet = *static_cast<const ARPPacket*>(eth.payload());
    if (packet.hardware_type() != 1 || packet.hardware_address_length() != sizeof(MACAddress)) {
        kprintf("handle_arp: Hardware type not ethernet (%w, len=%u)\n",
            packet.hardware_type(),
            packet.hardware_address_length());
        return;
    }
    if (packet.protocol_type() != EtherType::IPv4 || packet.protocol_address_length() != sizeof(IPv4Address)) {
        kprintf("handle_arp: Protocol type not IPv4 (%w, len=%u)\n",
            packet.hardware_type(),
            packet.protocol_address_length());
        return;
    }

#ifdef ARP_DEBUG
    kprintf("handle_arp: operation=%w, sender=%s/%s, target=%s/%s\n",
        packet.operation(),
        packet.sender_hardware_address().to_string().characters(),
        packet.sender_protocol_address().to_string().characters(),
        packet.target_hardware_address().to_string().characters(),
        packet.target_protocol_address().to_string().characters());
#endif

    if (!packet.sender_hardware_address().is_zero() && !packet.sender_protocol_address().is_zero()) {
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

    if (packet.operation() == ARPOperation::Request) {
        // Who has this IP address?
        if (auto adapter = NetworkAdapter::from_ipv4_address(packet.target_protocol_address())) {
            // We do!
            kprintf("handle_arp: Responding to ARP request for my IPv4 address (%s)\n",
                adapter->ipv4_address().to_string().characters());
            ARPPacket response;
            response.set_operation(ARPOperation::Response);
            response.set_target_hardware_address(packet.sender_hardware_address());
            response.set_target_protocol_address(packet.sender_protocol_address());
            response.set_sender_hardware_address(adapter->mac_address());
            response.set_sender_protocol_address(adapter->ipv4_address());

            adapter->send(packet.sender_hardware_address(), response);
        }
        return;
    }
}

void handle_ipv4(const EthernetFrameHeader& eth, size_t frame_size)
{
    constexpr size_t minimum_ipv4_frame_size = sizeof(EthernetFrameHeader) + sizeof(IPv4Packet);
    if (frame_size < minimum_ipv4_frame_size) {
        kprintf("handle_ipv4: Frame too small (%d, need %d)\n", frame_size, minimum_ipv4_frame_size);
        return;
    }
    auto& packet = *static_cast<const IPv4Packet*>(eth.payload());

    if (packet.length() < sizeof(IPv4Packet)) {
        kprintf("handle_ipv4: IPv4 packet too short (%u, need %u)\n", packet.length(), sizeof(IPv4Packet));
        return;
    }

    size_t actual_ipv4_packet_length = frame_size - sizeof(EthernetFrameHeader);
    if (packet.length() > actual_ipv4_packet_length) {
        kprintf("handle_ipv4: IPv4 packet claims to be longer than it is (%u, actually %zu)\n", packet.length(), actual_ipv4_packet_length);
        return;
    }

#ifdef IPV4_DEBUG
    kprintf("handle_ipv4: source=%s, target=%s\n",
        packet.source().to_string().characters(),
        packet.destination().to_string().characters());
#endif

    switch ((IPv4Protocol)packet.protocol()) {
    case IPv4Protocol::ICMP:
        return handle_icmp(eth, packet);
    case IPv4Protocol::UDP:
        return handle_udp(packet);
    case IPv4Protocol::TCP:
        return handle_tcp(packet);
    default:
        kprintf("handle_ipv4: Unhandled protocol %u\n", packet.protocol());
        break;
    }
}

void handle_icmp(const EthernetFrameHeader& eth, const IPv4Packet& ipv4_packet)
{
    auto& icmp_header = *static_cast<const ICMPHeader*>(ipv4_packet.payload());
#ifdef ICMP_DEBUG
    kprintf("handle_icmp: source=%s, destination=%s, type=%b, code=%b\n",
        ipv4_packet.source().to_string().characters(),
        ipv4_packet.destination().to_string().characters(),
        icmp_header.type(),
        icmp_header.code());
#endif

    {
        LOCKER(IPv4Socket::all_sockets().lock());
        for (RefPtr<IPv4Socket> socket : IPv4Socket::all_sockets().resource()) {
            LOCKER(socket->lock());
            if (socket->protocol() != (unsigned)IPv4Protocol::ICMP)
                continue;
            socket->did_receive(ipv4_packet.source(), 0, KBuffer::copy(&ipv4_packet, sizeof(IPv4Packet) + ipv4_packet.payload_size()));
        }
    }

    auto adapter = NetworkAdapter::from_ipv4_address(ipv4_packet.destination());
    if (!adapter)
        return;

    if (icmp_header.type() == ICMPType::EchoRequest) {
        auto& request = reinterpret_cast<const ICMPEchoPacket&>(icmp_header);
        kprintf("handle_icmp: EchoRequest from %s: id=%u, seq=%u\n",
            ipv4_packet.source().to_string().characters(),
            (u16)request.identifier,
            (u16)request.sequence_number);
        size_t icmp_packet_size = ipv4_packet.payload_size();
        auto buffer = ByteBuffer::create_zeroed(icmp_packet_size);
        auto& response = *(ICMPEchoPacket*)buffer.data();
        response.header.set_type(ICMPType::EchoReply);
        response.header.set_code(0);
        response.identifier = request.identifier;
        response.sequence_number = request.sequence_number;
        if (size_t icmp_payload_size = icmp_packet_size - sizeof(ICMPEchoPacket))
            memcpy(response.payload(), request.payload(), icmp_payload_size);
        response.header.set_checksum(internet_checksum(&response, icmp_packet_size));
        // FIXME: What is the right TTL value here? Is 64 ok? Should we use the same TTL as the echo request?
        adapter->send_ipv4(eth.source(), ipv4_packet.source(), IPv4Protocol::ICMP, buffer.data(), buffer.size(), 64);
    }
}

void handle_udp(const IPv4Packet& ipv4_packet)
{
    if (ipv4_packet.payload_size() < sizeof(UDPPacket)) {
        kprintf("handle_udp: Packet too small (%u, need %zu)\n", ipv4_packet.payload_size());
        return;
    }

    auto adapter = NetworkAdapter::from_ipv4_address(ipv4_packet.destination());
    if (!adapter) {
        kprintf("handle_udp: this packet is not for me, it's for %s\n", ipv4_packet.destination().to_string().characters());
        return;
    }

    auto& udp_packet = *static_cast<const UDPPacket*>(ipv4_packet.payload());
#ifdef UDP_DEBUG
    kprintf("handle_udp: source=%s:%u, destination=%s:%u length=%u\n",
        ipv4_packet.source().to_string().characters(),
        udp_packet.source_port(),
        ipv4_packet.destination().to_string().characters(),
        udp_packet.destination_port(),
        udp_packet.length());
#endif

    auto socket = UDPSocket::from_port(udp_packet.destination_port());
    if (!socket) {
        kprintf("handle_udp: No UDP socket for port %u\n", udp_packet.destination_port());
        return;
    }

    ASSERT(socket->type() == SOCK_DGRAM);
    ASSERT(socket->local_port() == udp_packet.destination_port());
    socket->did_receive(ipv4_packet.source(), udp_packet.source_port(), KBuffer::copy(&ipv4_packet, sizeof(IPv4Packet) + ipv4_packet.payload_size()));
}

void handle_tcp(const IPv4Packet& ipv4_packet)
{
    if (ipv4_packet.payload_size() < sizeof(TCPPacket)) {
        kprintf("handle_tcp: IPv4 payload is too small to be a TCP packet (%u, need %zu)\n", ipv4_packet.payload_size(), sizeof(TCPPacket));
        return;
    }

    auto& tcp_packet = *static_cast<const TCPPacket*>(ipv4_packet.payload());

    size_t minimum_tcp_header_size = 5 * sizeof(u32);
    size_t maximum_tcp_header_size = 15 * sizeof(u32);
    if (tcp_packet.header_size() < minimum_tcp_header_size || tcp_packet.header_size() > maximum_tcp_header_size) {
        kprintf("handle_tcp: TCP packet header has invalid size %zu\n", tcp_packet.header_size());
    }

    if (ipv4_packet.payload_size() < tcp_packet.header_size()) {
        kprintf("handle_tcp: IPv4 payload is smaller than TCP header claims (%u, supposedly %u)\n", ipv4_packet.payload_size(), tcp_packet.header_size());
        return;
    }

    size_t payload_size = ipv4_packet.payload_size() - tcp_packet.header_size();

#ifdef TCP_DEBUG
    kprintf("handle_tcp: source=%s:%u, destination=%s:%u seq_no=%u, ack_no=%u, flags=%w (%s%s%s%s), window_size=%u, payload_size=%u\n",
        ipv4_packet.source().to_string().characters(),
        tcp_packet.source_port(),
        ipv4_packet.destination().to_string().characters(),
        tcp_packet.destination_port(),
        tcp_packet.sequence_number(),
        tcp_packet.ack_number(),
        tcp_packet.flags(),
        tcp_packet.has_syn() ? "SYN " : "",
        tcp_packet.has_ack() ? "ACK " : "",
        tcp_packet.has_fin() ? "FIN " : "",
        tcp_packet.has_rst() ? "RST " : "",
        tcp_packet.window_size(),
        payload_size);
#endif

    auto adapter = NetworkAdapter::from_ipv4_address(ipv4_packet.destination());
    if (!adapter) {
        kprintf("handle_tcp: this packet is not for me, it's for %s\n", ipv4_packet.destination().to_string().characters());
        return;
    }

    IPv4SocketTuple tuple(ipv4_packet.destination(), tcp_packet.destination_port(), ipv4_packet.source(), tcp_packet.source_port());

#ifdef TCP_DEBUG
    kprintf("handle_tcp: looking for socket; tuple=%s\n", tuple.to_string().characters());
#endif

    auto socket = TCPSocket::from_tuple(tuple);
    if (!socket) {
        kprintf("handle_tcp: No TCP socket for tuple %s\n", tuple.to_string().characters());
        kprintf("handle_tcp: source=%s:%u, destination=%s:%u seq_no=%u, ack_no=%u, flags=%w (%s%s%s%s), window_size=%u, payload_size=%u\n",
            ipv4_packet.source().to_string().characters(),
            tcp_packet.source_port(),
            ipv4_packet.destination().to_string().characters(),
            tcp_packet.destination_port(),
            tcp_packet.sequence_number(),
            tcp_packet.ack_number(),
            tcp_packet.flags(),
            tcp_packet.has_syn() ? "SYN " : "",
            tcp_packet.has_ack() ? "ACK " : "",
            tcp_packet.has_fin() ? "FIN " : "",
            tcp_packet.has_rst() ? "RST " : "",
            tcp_packet.window_size(),
            payload_size);
        return;
    }

    ASSERT(socket->type() == SOCK_STREAM);
    ASSERT(socket->local_port() == tcp_packet.destination_port());

#ifdef TCP_DEBUG
    kprintf("handle_tcp: got socket; state=%s\n", socket->tuple().to_string().characters(), TCPSocket::to_string(socket->state()));
#endif

    socket->receive_tcp_packet(tcp_packet, ipv4_packet.payload_size());

    switch (socket->state()) {
    case TCPSocket::State::Closed:
        kprintf("handle_tcp: unexpected flags in Closed state\n");
        // TODO: we may want to send an RST here, maybe as a configurable option
        return;
    case TCPSocket::State::TimeWait:
        kprintf("handle_tcp: unexpected flags in TimeWait state\n");
        socket->send_tcp_packet(TCPFlags::RST);
        socket->set_state(TCPSocket::State::Closed);
        return;
    case TCPSocket::State::Listen:
        switch (tcp_packet.flags()) {
        case TCPFlags::SYN: {
#ifdef TCP_DEBUG
            kprintf("handle_tcp: incoming connection\n");
#endif
            auto& local_address = ipv4_packet.destination();
            auto& peer_address = ipv4_packet.source();
            auto client = socket->create_client(local_address, tcp_packet.destination_port(), peer_address, tcp_packet.source_port());
            if (!client) {
                kprintf("handle_tcp: couldn't create client socket\n");
                return;
            }
#ifdef TCP_DEBUG
            kprintf("handle_tcp: created new client socket with tuple %s\n", client->tuple().to_string().characters());
#endif
            client->set_sequence_number(1000);
            client->set_ack_number(tcp_packet.sequence_number() + payload_size + 1);
            client->send_tcp_packet(TCPFlags::SYN | TCPFlags::ACK);
            client->set_state(TCPSocket::State::SynReceived);
            return;
        }
        default:
            kprintf("handle_tcp: unexpected flags in Listen state\n");
            // socket->send_tcp_packet(TCPFlags::RST);
            return;
        }
    case TCPSocket::State::SynSent:
        switch (tcp_packet.flags()) {
        case TCPFlags::SYN:
            socket->set_ack_number(tcp_packet.sequence_number() + payload_size + 1);
            socket->send_tcp_packet(TCPFlags::ACK);
            socket->set_state(TCPSocket::State::SynReceived);
            return;
        case TCPFlags::ACK | TCPFlags::SYN:
            socket->set_ack_number(tcp_packet.sequence_number() + payload_size + 1);
            socket->send_tcp_packet(TCPFlags::ACK);
            socket->set_state(TCPSocket::State::Established);
            socket->set_setup_state(Socket::SetupState::Completed);
            socket->set_connected(true);
            return;
        case TCPFlags::ACK | TCPFlags::FIN:
            socket->set_ack_number(tcp_packet.sequence_number() + payload_size + 1);
            socket->send_tcp_packet(TCPFlags::ACK);
            socket->set_state(TCPSocket::State::Closed);
            socket->set_error(TCPSocket::Error::FINDuringConnect);
            socket->set_setup_state(Socket::SetupState::Completed);
            return;
        case TCPFlags::ACK | TCPFlags::RST:
            socket->set_ack_number(tcp_packet.sequence_number() + payload_size);
            socket->send_tcp_packet(TCPFlags::ACK);
            socket->set_state(TCPSocket::State::Closed);
            socket->set_error(TCPSocket::Error::RSTDuringConnect);
            socket->set_setup_state(Socket::SetupState::Completed);
            return;
        default:
            kprintf("handle_tcp: unexpected flags in SynSent state\n");
            socket->send_tcp_packet(TCPFlags::RST);
            socket->set_state(TCPSocket::State::Closed);
            socket->set_error(TCPSocket::Error::UnexpectedFlagsDuringConnect);
            socket->set_setup_state(Socket::SetupState::Completed);
            return;
        }
    case TCPSocket::State::SynReceived:
        switch (tcp_packet.flags()) {
        case TCPFlags::ACK:
            socket->set_ack_number(tcp_packet.sequence_number() + payload_size);

            switch (socket->direction()) {
            case TCPSocket::Direction::Incoming:
                if (!socket->has_originator()) {
                    kprintf("handle_tcp: connection doesn't have an originating socket; maybe it went away?\n");
                    socket->send_tcp_packet(TCPFlags::RST);
                    socket->set_state(TCPSocket::State::Closed);
                    return;
                }

                socket->set_state(TCPSocket::State::Established);
                socket->set_setup_state(Socket::SetupState::Completed);
                socket->release_to_originator();
                return;
            case TCPSocket::Direction::Outgoing:
                socket->set_state(TCPSocket::State::Established);
                socket->set_setup_state(Socket::SetupState::Completed);
                socket->set_connected(true);
                return;
            default:
                kprintf("handle_tcp: got ACK in SynReceived state but direction is invalid (%s)\n", TCPSocket::to_string(socket->direction()));
                socket->send_tcp_packet(TCPFlags::RST);
                socket->set_state(TCPSocket::State::Closed);
                return;
            }

            return;
        default:
            kprintf("handle_tcp: unexpected flags in SynReceived state\n");
            socket->send_tcp_packet(TCPFlags::RST);
            socket->set_state(TCPSocket::State::Closed);
            return;
        }
    case TCPSocket::State::CloseWait:
        switch (tcp_packet.flags()) {
        default:
            kprintf("handle_tcp: unexpected flags in CloseWait state\n");
            socket->send_tcp_packet(TCPFlags::RST);
            socket->set_state(TCPSocket::State::Closed);
            return;
        }
    case TCPSocket::State::LastAck:
        switch (tcp_packet.flags()) {
        case TCPFlags::ACK:
            socket->set_ack_number(tcp_packet.sequence_number() + payload_size);
            socket->set_state(TCPSocket::State::Closed);
            return;
        default:
            kprintf("handle_tcp: unexpected flags in LastAck state\n");
            socket->send_tcp_packet(TCPFlags::RST);
            socket->set_state(TCPSocket::State::Closed);
            return;
        }
    case TCPSocket::State::FinWait1:
        switch (tcp_packet.flags()) {
        case TCPFlags::ACK:
            socket->set_ack_number(tcp_packet.sequence_number() + payload_size);
            socket->set_state(TCPSocket::State::FinWait2);
            return;
        case TCPFlags::FIN:
            socket->set_ack_number(tcp_packet.sequence_number() + payload_size + 1);
            socket->set_state(TCPSocket::State::Closing);
            return;
        default:
            kprintf("handle_tcp: unexpected flags in FinWait1 state\n");
            socket->send_tcp_packet(TCPFlags::RST);
            socket->set_state(TCPSocket::State::Closed);
            return;
        }
    case TCPSocket::State::FinWait2:
        switch (tcp_packet.flags()) {
        case TCPFlags::FIN:
            socket->set_ack_number(tcp_packet.sequence_number() + payload_size + 1);
            socket->set_state(TCPSocket::State::TimeWait);
            return;
        default:
            kprintf("handle_tcp: unexpected flags in FinWait2 state\n");
            socket->send_tcp_packet(TCPFlags::RST);
            socket->set_state(TCPSocket::State::Closed);
            return;
        }
    case TCPSocket::State::Closing:
        switch (tcp_packet.flags()) {
        case TCPFlags::ACK:
            socket->set_ack_number(tcp_packet.sequence_number() + payload_size);
            socket->set_state(TCPSocket::State::TimeWait);
            return;
        default:
            kprintf("handle_tcp: unexpected flags in Closing state\n");
            socket->send_tcp_packet(TCPFlags::RST);
            socket->set_state(TCPSocket::State::Closed);
            return;
        }
    case TCPSocket::State::Established:
        if (tcp_packet.has_fin()) {
            if (payload_size != 0)
                socket->did_receive(ipv4_packet.source(), tcp_packet.source_port(), KBuffer::copy(&ipv4_packet, sizeof(IPv4Packet) + ipv4_packet.payload_size()));

            socket->set_ack_number(tcp_packet.sequence_number() + payload_size + 1);
            // TODO: We should only send a FIN packet out once we're shutting
            // down our side of the socket, so we should change this back to
            // just being an ACK and a transition to CloseWait once we have a
            // shutdown() implementation.
            socket->send_tcp_packet(TCPFlags::FIN | TCPFlags::ACK);
            socket->set_state(TCPSocket::State::Closing);
            socket->set_connected(false);
            return;
        }

        socket->set_ack_number(tcp_packet.sequence_number() + payload_size);

#ifdef TCP_DEBUG
        kprintf("Got packet with ack_no=%u, seq_no=%u, payload_size=%u, acking it with new ack_no=%u, seq_no=%u\n",
            tcp_packet.ack_number(),
            tcp_packet.sequence_number(),
            payload_size,
            socket->ack_number(),
            socket->sequence_number());
#endif

        bool should_ack = true;
        if (payload_size != 0) {
            should_ack = socket->did_receive(ipv4_packet.source(), tcp_packet.source_port(), KBuffer::copy(&ipv4_packet, sizeof(IPv4Packet) + ipv4_packet.payload_size()));
        }

        if (should_ack)
            socket->send_tcp_packet(TCPFlags::ACK);
    }
}
