/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Debug.h>
#include <Kernel/Locking/Mutex.h>
#include <Kernel/Locking/MutexProtected.h>
#include <Kernel/Net/ARP.h>
#include <Kernel/Net/EtherType.h>
#include <Kernel/Net/EthernetFrameHeader.h>
#include <Kernel/Net/ICMP.h>
#include <Kernel/Net/IPv4.h>
#include <Kernel/Net/IPv4Socket.h>
#include <Kernel/Net/LoopbackAdapter.h>
#include <Kernel/Net/NetworkTask.h>
#include <Kernel/Net/NetworkingManagement.h>
#include <Kernel/Net/Routing.h>
#include <Kernel/Net/TCP.h>
#include <Kernel/Net/TCPSocket.h>
#include <Kernel/Net/UDP.h>
#include <Kernel/Net/UDPSocket.h>
#include <Kernel/Process.h>

namespace Kernel {

static void handle_arp(EthernetFrameHeader const&, size_t frame_size);
static void handle_ipv4(EthernetFrameHeader const&, size_t frame_size, Time const& packet_timestamp);
static void handle_icmp(EthernetFrameHeader const&, IPv4Packet const&, Time const& packet_timestamp);
static void handle_udp(IPv4Packet const&, Time const& packet_timestamp);
static void handle_tcp(IPv4Packet const&, Time const& packet_timestamp);
static void send_delayed_tcp_ack(RefPtr<TCPSocket> socket);
static void send_tcp_rst(IPv4Packet const& ipv4_packet, TCPPacket const& tcp_packet, RefPtr<NetworkAdapter> adapter);
static void flush_delayed_tcp_acks();
static void retransmit_tcp_packets();

static Thread* network_task = nullptr;
static HashTable<RefPtr<TCPSocket>>* delayed_ack_sockets;

[[noreturn]] static void NetworkTask_main(void*);

void NetworkTask::spawn()
{
    RefPtr<Thread> thread;
    auto name = KString::try_create("NetworkTask");
    if (name.is_error())
        TODO();
    (void)Process::create_kernel_process(thread, name.release_value(), NetworkTask_main, nullptr);
    network_task = thread;
}

bool NetworkTask::is_current()
{
    return Thread::current() == network_task;
}

void NetworkTask_main(void*)
{
    delayed_ack_sockets = new HashTable<RefPtr<TCPSocket>>;

    WaitQueue packet_wait_queue;
    int pending_packets = 0;
    NetworkingManagement::the().for_each([&](auto& adapter) {
        dmesgln("NetworkTask: {} network adapter found: hw={}", adapter.class_name(), adapter.mac_address().to_string());

        if (adapter.class_name() == "LoopbackAdapter"sv) {
            adapter.set_ipv4_address({ 127, 0, 0, 1 });
            adapter.set_ipv4_netmask({ 255, 0, 0, 0 });
        }

        adapter.on_receive = [&]() {
            pending_packets++;
            packet_wait_queue.wake_all();
        };
    });

    auto dequeue_packet = [&pending_packets](u8* buffer, size_t buffer_size, Time& packet_timestamp) -> size_t {
        if (pending_packets == 0)
            return 0;
        size_t packet_size = 0;
        NetworkingManagement::the().for_each([&](auto& adapter) {
            if (packet_size || !adapter.has_queued_packets())
                return;
            packet_size = adapter.dequeue_packet(buffer, buffer_size, packet_timestamp);
            pending_packets--;
            dbgln_if(NETWORK_TASK_DEBUG, "NetworkTask: Dequeued packet from {} ({} bytes)", adapter.name(), packet_size);
        });
        return packet_size;
    };

    size_t buffer_size = 64 * KiB;
    auto region_or_error = MM.allocate_kernel_region(buffer_size, "Kernel Packet Buffer", Memory::Region::Access::ReadWrite);
    if (region_or_error.is_error())
        TODO();
    auto buffer_region = region_or_error.release_value();
    auto buffer = (u8*)buffer_region->vaddr().get();
    Time packet_timestamp;

    for (;;) {
        flush_delayed_tcp_acks();
        retransmit_tcp_packets();
        size_t packet_size = dequeue_packet(buffer, buffer_size, packet_timestamp);
        if (!packet_size) {
            auto timeout_time = Time::from_milliseconds(500);
            auto timeout = Thread::BlockTimeout { false, &timeout_time };
            [[maybe_unused]] auto result = packet_wait_queue.wait_on(timeout, "NetworkTask");
            continue;
        }
        if (packet_size < sizeof(EthernetFrameHeader)) {
            dbgln("NetworkTask: Packet is too small to be an Ethernet packet! ({})", packet_size);
            continue;
        }
        auto& eth = *(EthernetFrameHeader const*)buffer;
        dbgln_if(ETHERNET_DEBUG, "NetworkTask: From {} to {}, ether_type={:#04x}, packet_size={}", eth.source().to_string(), eth.destination().to_string(), eth.ether_type(), packet_size);

        switch (eth.ether_type()) {
        case EtherType::ARP:
            handle_arp(eth, packet_size);
            break;
        case EtherType::IPv4:
            handle_ipv4(eth, packet_size, packet_timestamp);
            break;
        case EtherType::IPv6:
            // ignore
            break;
        default:
            dbgln_if(ETHERNET_DEBUG, "NetworkTask: Unknown ethernet type {:#04x}", eth.ether_type());
        }
    }
}

void handle_arp(EthernetFrameHeader const& eth, size_t frame_size)
{
    constexpr size_t minimum_arp_frame_size = sizeof(EthernetFrameHeader) + sizeof(ARPPacket);
    if (frame_size < minimum_arp_frame_size) {
        dbgln("handle_arp: Frame too small ({}, need {})", frame_size, minimum_arp_frame_size);
        return;
    }
    auto& packet = *static_cast<ARPPacket const*>(eth.payload());
    if (packet.hardware_type() != 1 || packet.hardware_address_length() != sizeof(MACAddress)) {
        dbgln("handle_arp: Hardware type not ethernet ({:#04x}, len={})", packet.hardware_type(), packet.hardware_address_length());
        return;
    }
    if (packet.protocol_type() != EtherType::IPv4 || packet.protocol_address_length() != sizeof(IPv4Address)) {
        dbgln("handle_arp: Protocol type not IPv4 ({:#04x}, len={})", packet.protocol_type(), packet.protocol_address_length());
        return;
    }

    dbgln_if(ARP_DEBUG, "handle_arp: operation={:#04x}, sender={}/{}, target={}/{}",
        packet.operation(),
        packet.sender_hardware_address().to_string(),
        packet.sender_protocol_address().to_string(),
        packet.target_hardware_address().to_string(),
        packet.target_protocol_address().to_string());

    if (!packet.sender_hardware_address().is_zero() && !packet.sender_protocol_address().is_zero()) {
        // Someone has this IPv4 address. I guess we can try to remember that.
        // FIXME: Protect against ARP spamming.
        update_arp_table(packet.sender_protocol_address(), packet.sender_hardware_address(), UpdateTable::Set);
    }

    if (packet.operation() == ARPOperation::Request) {
        // Who has this IP address?
        if (auto adapter = NetworkingManagement::the().from_ipv4_address(packet.target_protocol_address())) {
            // We do!
            dbgln("handle_arp: Responding to ARP request for my IPv4 address ({})", adapter->ipv4_address());
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

void handle_ipv4(EthernetFrameHeader const& eth, size_t frame_size, Time const& packet_timestamp)
{
    constexpr size_t minimum_ipv4_frame_size = sizeof(EthernetFrameHeader) + sizeof(IPv4Packet);
    if (frame_size < minimum_ipv4_frame_size) {
        dbgln("handle_ipv4: Frame too small ({}, need {})", frame_size, minimum_ipv4_frame_size);
        return;
    }
    auto& packet = *static_cast<IPv4Packet const*>(eth.payload());

    if (packet.length() < sizeof(IPv4Packet)) {
        dbgln("handle_ipv4: IPv4 packet too short ({}, need {})", packet.length(), sizeof(IPv4Packet));
        return;
    }

    size_t actual_ipv4_packet_length = frame_size - sizeof(EthernetFrameHeader);
    if (packet.length() > actual_ipv4_packet_length) {
        dbgln("handle_ipv4: IPv4 packet claims to be longer than it is ({}, actually {})", packet.length(), actual_ipv4_packet_length);
        return;
    }

    dbgln_if(IPV4_DEBUG, "handle_ipv4: source={}, destination={}", packet.source(), packet.destination());

    NetworkingManagement::the().for_each([&](auto& adapter) {
        if (adapter.link_up()) {
            auto my_net = adapter.ipv4_address().to_u32() & adapter.ipv4_netmask().to_u32();
            auto their_net = packet.source().to_u32() & adapter.ipv4_netmask().to_u32();
            if (my_net == their_net)
                update_arp_table(packet.source(), eth.source(), UpdateTable::Set);
        }
    });

    switch ((IPv4Protocol)packet.protocol()) {
    case IPv4Protocol::ICMP:
        return handle_icmp(eth, packet, packet_timestamp);
    case IPv4Protocol::UDP:
        return handle_udp(packet, packet_timestamp);
    case IPv4Protocol::TCP:
        return handle_tcp(packet, packet_timestamp);
    default:
        dbgln_if(IPV4_DEBUG, "handle_ipv4: Unhandled protocol {:#02x}", packet.protocol());
        break;
    }
}

void handle_icmp(EthernetFrameHeader const& eth, IPv4Packet const& ipv4_packet, Time const& packet_timestamp)
{
    auto& icmp_header = *static_cast<ICMPHeader const*>(ipv4_packet.payload());
    dbgln_if(ICMP_DEBUG, "handle_icmp: source={}, destination={}, type={:#02x}, code={:#02x}", ipv4_packet.source().to_string(), ipv4_packet.destination().to_string(), icmp_header.type(), icmp_header.code());

    {
        NonnullRefPtrVector<IPv4Socket> icmp_sockets;
        IPv4Socket::all_sockets().with_exclusive([&](auto& sockets) {
            for (auto& socket : sockets) {
                if (socket.protocol() == (unsigned)IPv4Protocol::ICMP)
                    icmp_sockets.append(socket);
            }
        });
        for (auto& socket : icmp_sockets)
            socket.did_receive(ipv4_packet.source(), 0, { &ipv4_packet, sizeof(IPv4Packet) + ipv4_packet.payload_size() }, packet_timestamp);
    }

    auto adapter = NetworkingManagement::the().from_ipv4_address(ipv4_packet.destination());
    if (!adapter)
        return;

    if (icmp_header.type() == ICMPType::EchoRequest) {
        auto& request = reinterpret_cast<ICMPEchoPacket const&>(icmp_header);
        dbgln("handle_icmp: EchoRequest from {}: id={}, seq={}", ipv4_packet.source(), (u16)request.identifier, (u16)request.sequence_number);
        size_t icmp_packet_size = ipv4_packet.payload_size();
        if (icmp_packet_size < sizeof(ICMPEchoPacket)) {
            dbgln("handle_icmp: EchoRequest packet is too small, ignoring.");
            return;
        }
        auto ipv4_payload_offset = adapter->ipv4_payload_offset();
        auto packet = adapter->acquire_packet_buffer(ipv4_payload_offset + icmp_packet_size);
        if (!packet) {
            dbgln("Could not allocate packet buffer while sending ICMP packet");
            return;
        }
        adapter->fill_in_ipv4_header(*packet, adapter->ipv4_address(), eth.source(), ipv4_packet.source(), IPv4Protocol::ICMP, icmp_packet_size, 0, 64);
        memset(packet->buffer->data() + ipv4_payload_offset, 0, sizeof(ICMPEchoPacket));
        auto& response = *(ICMPEchoPacket*)(packet->buffer->data() + ipv4_payload_offset);
        response.header.set_type(ICMPType::EchoReply);
        response.header.set_code(0);
        response.identifier = request.identifier;
        response.sequence_number = request.sequence_number;
        if (size_t icmp_payload_size = icmp_packet_size - sizeof(ICMPEchoPacket))
            memcpy(response.payload(), request.payload(), icmp_payload_size);
        response.header.set_checksum(internet_checksum(&response, icmp_packet_size));
        // FIXME: What is the right TTL value here? Is 64 ok? Should we use the same TTL as the echo request?
        adapter->send_packet(packet->bytes());
        adapter->release_packet_buffer(*packet);
    }
}

void handle_udp(IPv4Packet const& ipv4_packet, Time const& packet_timestamp)
{
    if (ipv4_packet.payload_size() < sizeof(UDPPacket)) {
        dbgln("handle_udp: Packet too small ({}, need {})", ipv4_packet.payload_size(), sizeof(UDPPacket));
        return;
    }

    auto& udp_packet = *static_cast<UDPPacket const*>(ipv4_packet.payload());
    dbgln_if(UDP_DEBUG, "handle_udp: source={}:{}, destination={}:{}, length={}",
        ipv4_packet.source(), udp_packet.source_port(),
        ipv4_packet.destination(), udp_packet.destination_port(),
        udp_packet.length());

    auto socket = UDPSocket::from_port(udp_packet.destination_port());
    if (!socket) {
        dbgln_if(UDP_DEBUG, "handle_udp: No local UDP socket for {}:{}", ipv4_packet.destination(), udp_packet.destination_port());
        return;
    }

    VERIFY(socket->type() == SOCK_DGRAM);
    VERIFY(socket->local_port() == udp_packet.destination_port());

    auto& destination = ipv4_packet.destination();

    if (destination == IPv4Address(255, 255, 255, 255) || NetworkingManagement::the().from_ipv4_address(destination) || socket->multicast_memberships().contains_slow(destination))
        socket->did_receive(ipv4_packet.source(), udp_packet.source_port(), { &ipv4_packet, sizeof(IPv4Packet) + ipv4_packet.payload_size() }, packet_timestamp);
}

void send_delayed_tcp_ack(RefPtr<TCPSocket> socket)
{
    VERIFY(socket->mutex().is_locked());
    if (!socket->should_delay_next_ack()) {
        [[maybe_unused]] auto result = socket->send_ack();
        return;
    }

    delayed_ack_sockets->set(move(socket));
}

void flush_delayed_tcp_acks()
{
    Vector<RefPtr<TCPSocket>, 32> remaining_sockets;
    for (auto& socket : *delayed_ack_sockets) {
        MutexLocker locker(socket->mutex());
        if (socket->should_delay_next_ack()) {
            MUST(remaining_sockets.try_append(socket));
            continue;
        }
        [[maybe_unused]] auto result = socket->send_ack();
    }

    if (remaining_sockets.size() != delayed_ack_sockets->size()) {
        delayed_ack_sockets->clear();
        if (remaining_sockets.size() > 0)
            dbgln("flush_delayed_tcp_acks: {} sockets remaining", remaining_sockets.size());
        for (auto&& socket : remaining_sockets)
            delayed_ack_sockets->set(move(socket));
    }
}

void send_tcp_rst(IPv4Packet const& ipv4_packet, TCPPacket const& tcp_packet, RefPtr<NetworkAdapter> adapter)
{
    auto routing_decision = route_to(ipv4_packet.source(), ipv4_packet.destination(), adapter);
    if (routing_decision.is_zero())
        return;

    auto ipv4_payload_offset = routing_decision.adapter->ipv4_payload_offset();

    const size_t options_size = 0;
    const size_t tcp_header_size = sizeof(TCPPacket) + options_size;
    const size_t buffer_size = ipv4_payload_offset + tcp_header_size;

    auto packet = routing_decision.adapter->acquire_packet_buffer(buffer_size);
    if (!packet)
        return;
    routing_decision.adapter->fill_in_ipv4_header(*packet, ipv4_packet.destination(),
        routing_decision.next_hop, ipv4_packet.source(), IPv4Protocol::TCP,
        buffer_size - ipv4_payload_offset, 0, 64);

    auto& rst_packet = *(TCPPacket*)(packet->buffer->data() + ipv4_payload_offset);
    rst_packet = {};
    rst_packet.set_source_port(tcp_packet.destination_port());
    rst_packet.set_destination_port(tcp_packet.source_port());
    rst_packet.set_window_size(0);
    rst_packet.set_sequence_number(0);
    rst_packet.set_ack_number(tcp_packet.sequence_number() + 1);
    rst_packet.set_data_offset(tcp_header_size / sizeof(u32));
    rst_packet.set_flags(TCPFlags::RST | TCPFlags::ACK);
    rst_packet.set_checksum(TCPSocket::compute_tcp_checksum(ipv4_packet.source(), ipv4_packet.destination(), rst_packet, 0));

    routing_decision.adapter->send_packet(packet->bytes());
    routing_decision.adapter->release_packet_buffer(*packet);
}

void handle_tcp(IPv4Packet const& ipv4_packet, Time const& packet_timestamp)
{
    if (ipv4_packet.payload_size() < sizeof(TCPPacket)) {
        dbgln("handle_tcp: IPv4 payload is too small to be a TCP packet ({}, need {})", ipv4_packet.payload_size(), sizeof(TCPPacket));
        return;
    }

    auto& tcp_packet = *static_cast<TCPPacket const*>(ipv4_packet.payload());

    size_t minimum_tcp_header_size = 5 * sizeof(u32);
    size_t maximum_tcp_header_size = 15 * sizeof(u32);
    if (tcp_packet.header_size() < minimum_tcp_header_size || tcp_packet.header_size() > maximum_tcp_header_size) {
        dbgln("handle_tcp: TCP packet header has invalid size {}", tcp_packet.header_size());
    }

    if (ipv4_packet.payload_size() < tcp_packet.header_size()) {
        dbgln("handle_tcp: IPv4 payload is smaller than TCP header claims ({}, supposedly {})", ipv4_packet.payload_size(), tcp_packet.header_size());
        return;
    }

    size_t payload_size = ipv4_packet.payload_size() - tcp_packet.header_size();

    dbgln_if(TCP_DEBUG, "handle_tcp: source={}:{}, destination={}:{}, seq_no={}, ack_no={}, flags={:#04x} ({}{}{}{}), window_size={}, payload_size={}",
        ipv4_packet.source().to_string(),
        tcp_packet.source_port(),
        ipv4_packet.destination().to_string(),
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

    auto adapter = NetworkingManagement::the().from_ipv4_address(ipv4_packet.destination());
    if (!adapter) {
        dbgln("handle_tcp: this packet is not for me, it's for {}", ipv4_packet.destination());
        return;
    }

    IPv4SocketTuple tuple(ipv4_packet.destination(), tcp_packet.destination_port(), ipv4_packet.source(), tcp_packet.source_port());

    dbgln_if(TCP_DEBUG, "handle_tcp: looking for socket; tuple={}", tuple.to_string());

    auto socket = TCPSocket::from_tuple(tuple);
    if (!socket) {
        if (!tcp_packet.has_rst()) {
            dbgln("handle_tcp: No TCP socket for tuple {}. Sending RST.", tuple.to_string());
            send_tcp_rst(ipv4_packet, tcp_packet, adapter);
        }
        return;
    }

    MutexLocker locker(socket->mutex());

    VERIFY(socket->type() == SOCK_STREAM);
    VERIFY(socket->local_port() == tcp_packet.destination_port());

    dbgln_if(TCP_DEBUG, "handle_tcp: got socket {}; state={}", socket->tuple().to_string(), TCPSocket::to_string(socket->state()));

    socket->receive_tcp_packet(tcp_packet, ipv4_packet.payload_size());

    switch (socket->state()) {
    case TCPSocket::State::Closed:
        dbgln("handle_tcp: unexpected flags in Closed state ({:x})", tcp_packet.flags());
        // TODO: we may want to send an RST here, maybe as a configurable option
        return;
    case TCPSocket::State::TimeWait:
        dbgln("handle_tcp: unexpected flags in TimeWait state ({:x})", tcp_packet.flags());
        (void)socket->send_tcp_packet(TCPFlags::RST);
        socket->set_state(TCPSocket::State::Closed);
        return;
    case TCPSocket::State::Listen:
        switch (tcp_packet.flags()) {
        case TCPFlags::SYN: {
            dbgln_if(TCP_DEBUG, "handle_tcp: incoming connection");
            auto& local_address = ipv4_packet.destination();
            auto& peer_address = ipv4_packet.source();
            auto client_or_error = socket->try_create_client(local_address, tcp_packet.destination_port(), peer_address, tcp_packet.source_port());
            if (client_or_error.is_error()) {
                dmesgln("handle_tcp: couldn't create client socket: {}", client_or_error.error());
                return;
            }
            auto client = client_or_error.release_value();
            MutexLocker locker(client->mutex());
            dbgln_if(TCP_DEBUG, "handle_tcp: created new client socket with tuple {}", client->tuple().to_string());
            client->set_sequence_number(1000);
            client->set_ack_number(tcp_packet.sequence_number() + payload_size + 1);
            [[maybe_unused]] auto rc2 = client->send_tcp_packet(TCPFlags::SYN | TCPFlags::ACK);
            client->set_state(TCPSocket::State::SynReceived);
            return;
        }
        default:
            dbgln("handle_tcp: unexpected flags in Listen state ({:x})", tcp_packet.flags());
            // socket->send_tcp_packet(TCPFlags::RST);
            return;
        }
    case TCPSocket::State::SynSent:
        switch (tcp_packet.flags()) {
        case TCPFlags::SYN:
            socket->set_ack_number(tcp_packet.sequence_number() + payload_size + 1);
            (void)socket->send_tcp_packet(TCPFlags::SYN | TCPFlags::ACK);
            socket->set_state(TCPSocket::State::SynReceived);
            return;
        case TCPFlags::ACK | TCPFlags::SYN:
            socket->set_ack_number(tcp_packet.sequence_number() + payload_size + 1);
            (void)socket->send_ack(true);
            socket->set_state(TCPSocket::State::Established);
            socket->set_setup_state(Socket::SetupState::Completed);
            socket->set_connected(true);
            return;
        case TCPFlags::ACK | TCPFlags::FIN:
            socket->set_ack_number(tcp_packet.sequence_number() + payload_size + 1);
            send_delayed_tcp_ack(socket);
            socket->set_state(TCPSocket::State::Closed);
            socket->set_error(TCPSocket::Error::FINDuringConnect);
            socket->set_setup_state(Socket::SetupState::Completed);
            return;
        case TCPFlags::ACK | TCPFlags::RST:
            socket->set_state(TCPSocket::State::Closed);
            socket->set_error(TCPSocket::Error::RSTDuringConnect);
            socket->set_setup_state(Socket::SetupState::Completed);
            return;
        default:
            dbgln("handle_tcp: unexpected flags in SynSent state ({:x})", tcp_packet.flags());
            (void)socket->send_tcp_packet(TCPFlags::RST);
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
                    dbgln("handle_tcp: connection doesn't have an originating socket; maybe it went away?");
                    (void)socket->send_tcp_packet(TCPFlags::RST);
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
                dbgln("handle_tcp: got ACK in SynReceived state but direction is invalid ({})", TCPSocket::to_string(socket->direction()));
                (void)socket->send_tcp_packet(TCPFlags::RST);
                socket->set_state(TCPSocket::State::Closed);
                return;
            }
            VERIFY_NOT_REACHED();

        case TCPFlags::SYN:
            dbgln("handle_tcp: ignoring SYN for partially established connection");
            return;
        default:
            dbgln("handle_tcp: unexpected flags in SynReceived state ({:x})", tcp_packet.flags());
            (void)socket->send_tcp_packet(TCPFlags::RST);
            socket->set_state(TCPSocket::State::Closed);
            return;
        }
    case TCPSocket::State::CloseWait:
        switch (tcp_packet.flags()) {
        default:
            dbgln("handle_tcp: unexpected flags in CloseWait state ({:x})", tcp_packet.flags());
            (void)socket->send_tcp_packet(TCPFlags::RST);
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
            dbgln("handle_tcp: unexpected flags in LastAck state ({:x})", tcp_packet.flags());
            (void)socket->send_tcp_packet(TCPFlags::RST);
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
            (void)socket->send_ack(true);
            return;
        case TCPFlags::FIN | TCPFlags::ACK:
            socket->set_ack_number(tcp_packet.sequence_number() + payload_size + 1);
            socket->set_state(TCPSocket::State::TimeWait);
            (void)socket->send_ack(true);
            return;
        default:
            dbgln("handle_tcp: unexpected flags in FinWait1 state ({:x})", tcp_packet.flags());
            (void)socket->send_tcp_packet(TCPFlags::RST);
            socket->set_state(TCPSocket::State::Closed);
            return;
        }
    case TCPSocket::State::FinWait2:
        switch (tcp_packet.flags()) {
        case TCPFlags::FIN:
            socket->set_ack_number(tcp_packet.sequence_number() + payload_size + 1);
            socket->set_state(TCPSocket::State::TimeWait);
            (void)socket->send_ack(true);
            return;
        case TCPFlags::ACK | TCPFlags::RST:
            // FIXME: Verify that this transition is legitimate.
            socket->set_state(TCPSocket::State::Closed);
            return;
        default:
            dbgln("handle_tcp: unexpected flags in FinWait2 state ({:x})", tcp_packet.flags());
            (void)socket->send_tcp_packet(TCPFlags::RST);
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
            dbgln("handle_tcp: unexpected flags in Closing state ({:x})", tcp_packet.flags());
            (void)socket->send_tcp_packet(TCPFlags::RST);
            socket->set_state(TCPSocket::State::Closed);
            return;
        }
    case TCPSocket::State::Established:
        if (tcp_packet.has_rst()) {
            socket->set_state(TCPSocket::State::Closed);
            return;
        }

        if (tcp_packet.sequence_number() != socket->ack_number()) {
            dbgln_if(TCP_DEBUG, "Discarding out of order packet: seq {} vs. ack {}", tcp_packet.sequence_number(), socket->ack_number());
            if (socket->duplicate_acks() < TCPSocket::maximum_duplicate_acks) {
                dbgln_if(TCP_DEBUG, "Sending ACK with same ack number to trigger fast retransmission");
                socket->set_duplicate_acks(socket->duplicate_acks() + 1);
                [[maybe_unused]] auto result = socket->send_ack(true);
            }
            return;
        }

        socket->set_duplicate_acks(0);

        if (tcp_packet.has_fin()) {
            if (payload_size != 0)
                socket->did_receive(ipv4_packet.source(), tcp_packet.source_port(), { &ipv4_packet, sizeof(IPv4Packet) + ipv4_packet.payload_size() }, packet_timestamp);

            socket->set_ack_number(tcp_packet.sequence_number() + payload_size + 1);
            send_delayed_tcp_ack(socket);
            socket->set_state(TCPSocket::State::CloseWait);
            socket->set_connected(false);
            return;
        }

        if (payload_size) {
            if (socket->did_receive(ipv4_packet.source(), tcp_packet.source_port(), { &ipv4_packet, sizeof(IPv4Packet) + ipv4_packet.payload_size() }, packet_timestamp)) {
                socket->set_ack_number(tcp_packet.sequence_number() + payload_size);
                dbgln_if(TCP_DEBUG, "Got packet with ack_no={}, seq_no={}, payload_size={}, acking it with new ack_no={}, seq_no={}",
                    tcp_packet.ack_number(), tcp_packet.sequence_number(), payload_size, socket->ack_number(), socket->sequence_number());
                send_delayed_tcp_ack(socket);
            }
        }
    }
}

void retransmit_tcp_packets()
{
    // We must keep the sockets alive until after we've unlocked the hash table
    // in case retransmit_packets() realizes that it wants to close the socket.
    NonnullRefPtrVector<TCPSocket, 16> sockets;
    TCPSocket::sockets_for_retransmit().for_each_shared([&](auto const& socket) {
        // We ignore allocation failures above the first 16 guaranteed socket slots, as
        // we will just retransmit their packets the next time around
        (void)sockets.try_append(socket);
    });

    for (auto& socket : sockets) {
        MutexLocker socket_locker(socket.mutex());
        socket.retransmit_packets();
    }
}

}
