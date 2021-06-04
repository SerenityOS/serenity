/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Debug.h>
#include <Kernel/Lock.h>
#include <Kernel/Net/ARP.h>
#include <Kernel/Net/EtherType.h>
#include <Kernel/Net/EthernetFrameHeader.h>
#include <Kernel/Net/ICMP.h>
#include <Kernel/Net/IPv4.h>
#include <Kernel/Net/IPv4Socket.h>
#include <Kernel/Net/LoopbackAdapter.h>
#include <Kernel/Net/NetworkTask.h>
#include <Kernel/Net/Routing.h>
#include <Kernel/Net/TCP.h>
#include <Kernel/Net/TCPSocket.h>
#include <Kernel/Net/UDP.h>
#include <Kernel/Net/UDPSocket.h>
#include <Kernel/Process.h>

namespace Kernel {

static void handle_arp(const EthernetFrameHeader&, size_t frame_size);
static void handle_ipv4(const EthernetFrameHeader&, size_t frame_size, const Time& packet_timestamp);
static void handle_icmp(const EthernetFrameHeader&, const IPv4Packet&, const Time& packet_timestamp);
static void handle_udp(const IPv4Packet&, const Time& packet_timestamp);
static void handle_tcp(const IPv4Packet&, const Time& packet_timestamp);
static void send_delayed_tcp_ack(RefPtr<TCPSocket> socket);
static void flush_delayed_tcp_acks();
static void retransmit_tcp_packets();

static Thread* network_task = nullptr;
static HashTable<RefPtr<TCPSocket>>* delayed_ack_sockets;

[[noreturn]] static void NetworkTask_main(void*);

void NetworkTask::spawn()
{
    RefPtr<Thread> thread;
    Process::create_kernel_process(thread, "NetworkTask", NetworkTask_main, nullptr);
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
    NetworkAdapter::for_each([&](auto& adapter) {
        dmesgln("NetworkTask: {} network adapter found: hw={}", adapter.class_name(), adapter.mac_address().to_string());

        if (String(adapter.class_name()) == "LoopbackAdapter") {
            adapter.set_ipv4_address({ 127, 0, 0, 1 });
            adapter.set_ipv4_netmask({ 255, 0, 0, 0 });
            adapter.set_ipv4_gateway({ 0, 0, 0, 0 });
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
        NetworkAdapter::for_each([&](auto& adapter) {
            if (packet_size || !adapter.has_queued_packets())
                return;
            packet_size = adapter.dequeue_packet(buffer, buffer_size, packet_timestamp);
            pending_packets--;
            dbgln_if(NETWORK_TASK_DEBUG, "NetworkTask: Dequeued packet from {} ({} bytes)", adapter.name(), packet_size);
        });
        return packet_size;
    };

    size_t buffer_size = 64 * KiB;
    auto buffer_region = MM.allocate_kernel_region(buffer_size, "Kernel Packet Buffer", Region::Access::Read | Region::Access::Write);
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
        auto& eth = *(const EthernetFrameHeader*)buffer;
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

void handle_arp(const EthernetFrameHeader& eth, size_t frame_size)
{
    constexpr size_t minimum_arp_frame_size = sizeof(EthernetFrameHeader) + sizeof(ARPPacket);
    if (frame_size < minimum_arp_frame_size) {
        dbgln("handle_arp: Frame too small ({}, need {})", frame_size, minimum_arp_frame_size);
        return;
    }
    auto& packet = *static_cast<const ARPPacket*>(eth.payload());
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
        // FIXME: Support static ARP table entries.
        update_arp_table(packet.sender_protocol_address(), packet.sender_hardware_address());
    }

    if (packet.operation() == ARPOperation::Request) {
        // Who has this IP address?
        if (auto adapter = NetworkAdapter::from_ipv4_address(packet.target_protocol_address())) {
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

void handle_ipv4(const EthernetFrameHeader& eth, size_t frame_size, const Time& packet_timestamp)
{
    constexpr size_t minimum_ipv4_frame_size = sizeof(EthernetFrameHeader) + sizeof(IPv4Packet);
    if (frame_size < minimum_ipv4_frame_size) {
        dbgln("handle_ipv4: Frame too small ({}, need {})", frame_size, minimum_ipv4_frame_size);
        return;
    }
    auto& packet = *static_cast<const IPv4Packet*>(eth.payload());

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

    NetworkAdapter::for_each([&](auto& adapter) {
        if (adapter.link_up()) {
            auto my_net = adapter.ipv4_address().to_u32() & adapter.ipv4_netmask().to_u32();
            auto their_net = packet.source().to_u32() & adapter.ipv4_netmask().to_u32();
            if (my_net == their_net)
                update_arp_table(packet.source(), eth.source());
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

void handle_icmp(const EthernetFrameHeader& eth, const IPv4Packet& ipv4_packet, const Time& packet_timestamp)
{
    auto& icmp_header = *static_cast<const ICMPHeader*>(ipv4_packet.payload());
    dbgln_if(ICMP_DEBUG, "handle_icmp: source={}, destination={}, type={:#02x}, code={:#02x}", ipv4_packet.source().to_string(), ipv4_packet.destination().to_string(), icmp_header.type(), icmp_header.code());

    {
        NonnullRefPtrVector<IPv4Socket> icmp_sockets;
        {
            Locker locker(IPv4Socket::all_sockets().lock(), Lock::Mode::Shared);
            for (auto* socket : IPv4Socket::all_sockets().resource()) {
                if (socket->protocol() != (unsigned)IPv4Protocol::ICMP)
                    continue;
                icmp_sockets.append(*socket);
            }
        }
        for (auto& socket : icmp_sockets)
            socket.did_receive(ipv4_packet.source(), 0, { &ipv4_packet, sizeof(IPv4Packet) + ipv4_packet.payload_size() }, packet_timestamp);
    }

    auto adapter = NetworkAdapter::from_ipv4_address(ipv4_packet.destination());
    if (!adapter)
        return;

    if (icmp_header.type() == ICMPType::EchoRequest) {
        auto& request = reinterpret_cast<const ICMPEchoPacket&>(icmp_header);
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
        adapter->fill_in_ipv4_header(*packet, adapter->ipv4_address(), eth.source(), ipv4_packet.source(), IPv4Protocol::ICMP, icmp_packet_size, 64);
        memset(packet->buffer.data() + ipv4_payload_offset, 0, sizeof(ICMPEchoPacket));
        auto& response = *(ICMPEchoPacket*)(packet->buffer.data() + ipv4_payload_offset);
        response.header.set_type(ICMPType::EchoReply);
        response.header.set_code(0);
        response.identifier = request.identifier;
        response.sequence_number = request.sequence_number;
        if (size_t icmp_payload_size = icmp_packet_size - sizeof(ICMPEchoPacket))
            memcpy(response.payload(), request.payload(), icmp_payload_size);
        response.header.set_checksum(internet_checksum(&response, icmp_packet_size));
        // FIXME: What is the right TTL value here? Is 64 ok? Should we use the same TTL as the echo request?
        adapter->send_packet({ packet->buffer.data(), packet->buffer.size() });
        adapter->release_packet_buffer(*packet);
    }
}

void handle_udp(const IPv4Packet& ipv4_packet, const Time& packet_timestamp)
{
    if (ipv4_packet.payload_size() < sizeof(UDPPacket)) {
        dbgln("handle_udp: Packet too small ({}, need {})", ipv4_packet.payload_size(), sizeof(UDPPacket));
        return;
    }

    auto& udp_packet = *static_cast<const UDPPacket*>(ipv4_packet.payload());
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

    if (destination == IPv4Address(255, 255, 255, 255) || NetworkAdapter::from_ipv4_address(destination) || socket->multicast_memberships().contains_slow(destination))
        socket->did_receive(ipv4_packet.source(), udp_packet.source_port(), { &ipv4_packet, sizeof(IPv4Packet) + ipv4_packet.payload_size() }, packet_timestamp);
}

void send_delayed_tcp_ack(RefPtr<TCPSocket> socket)
{
    VERIFY(socket->lock().is_locked());
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
        Locker locker(socket->lock());
        if (socket->should_delay_next_ack()) {
            remaining_sockets.append(socket);
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

void handle_tcp(const IPv4Packet& ipv4_packet, const Time& packet_timestamp)
{
    if (ipv4_packet.payload_size() < sizeof(TCPPacket)) {
        dbgln("handle_tcp: IPv4 payload is too small to be a TCP packet ({}, need {})", ipv4_packet.payload_size(), sizeof(TCPPacket));
        return;
    }

    auto& tcp_packet = *static_cast<const TCPPacket*>(ipv4_packet.payload());

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

    auto adapter = NetworkAdapter::from_ipv4_address(ipv4_packet.destination());
    if (!adapter) {
        dbgln("handle_tcp: this packet is not for me, it's for {}", ipv4_packet.destination());
        return;
    }

    IPv4SocketTuple tuple(ipv4_packet.destination(), tcp_packet.destination_port(), ipv4_packet.source(), tcp_packet.source_port());

    dbgln_if(TCP_DEBUG, "handle_tcp: looking for socket; tuple={}", tuple.to_string());

    auto socket = TCPSocket::from_tuple(tuple);
    if (!socket) {
        dbgln("handle_tcp: No TCP socket for tuple {}", tuple.to_string());
        dbgln("handle_tcp: source={}:{}, destination={}:{}, seq_no={}, ack_no={}, flags={:#04x} ({}{}{}{}), window_size={}, payload_size={}",
            ipv4_packet.source().to_string(), tcp_packet.source_port(),
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
        return;
    }

    Locker locker(socket->lock());

    VERIFY(socket->type() == SOCK_STREAM);
    VERIFY(socket->local_port() == tcp_packet.destination_port());

    dbgln_if(TCP_DEBUG, "handle_tcp: got socket {}; state={}", socket->tuple().to_string(), TCPSocket::to_string(socket->state()));

    socket->receive_tcp_packet(tcp_packet, ipv4_packet.payload_size());

    [[maybe_unused]] int unused_rc {};
    switch (socket->state()) {
    case TCPSocket::State::Closed:
        dbgln("handle_tcp: unexpected flags in Closed state");
        // TODO: we may want to send an RST here, maybe as a configurable option
        return;
    case TCPSocket::State::TimeWait:
        dbgln("handle_tcp: unexpected flags in TimeWait state");
        unused_rc = socket->send_tcp_packet(TCPFlags::RST);
        socket->set_state(TCPSocket::State::Closed);
        return;
    case TCPSocket::State::Listen:
        switch (tcp_packet.flags()) {
        case TCPFlags::SYN: {
            dbgln_if(TCP_DEBUG, "handle_tcp: incoming connection");
            auto& local_address = ipv4_packet.destination();
            auto& peer_address = ipv4_packet.source();
            auto client = socket->create_client(local_address, tcp_packet.destination_port(), peer_address, tcp_packet.source_port());
            if (!client) {
                dmesgln("handle_tcp: couldn't create client socket");
                return;
            }
            Locker locker(client->lock());
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
            unused_rc = socket->send_ack(true);
            socket->set_state(TCPSocket::State::SynReceived);
            return;
        case TCPFlags::ACK | TCPFlags::SYN:
            socket->set_ack_number(tcp_packet.sequence_number() + payload_size + 1);
            unused_rc = socket->send_ack(true);
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
            socket->set_ack_number(tcp_packet.sequence_number() + payload_size);
            send_delayed_tcp_ack(socket);
            socket->set_state(TCPSocket::State::Closed);
            socket->set_error(TCPSocket::Error::RSTDuringConnect);
            socket->set_setup_state(Socket::SetupState::Completed);
            return;
        default:
            dbgln("handle_tcp: unexpected flags in SynSent state ({:x})", tcp_packet.flags());
            unused_rc = socket->send_tcp_packet(TCPFlags::RST);
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
                    unused_rc = socket->send_tcp_packet(TCPFlags::RST);
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
                unused_rc = socket->send_tcp_packet(TCPFlags::RST);
                socket->set_state(TCPSocket::State::Closed);
                return;
            }

            return;
        case TCPFlags::SYN:
            dbgln("handle_tcp: ignoring SYN for partially established connection");
            return;
        default:
            dbgln("handle_tcp: unexpected flags in SynReceived state ({:x})", tcp_packet.flags());
            unused_rc = socket->send_tcp_packet(TCPFlags::RST);
            socket->set_state(TCPSocket::State::Closed);
            return;
        }
    case TCPSocket::State::CloseWait:
        switch (tcp_packet.flags()) {
        default:
            dbgln("handle_tcp: unexpected flags in CloseWait state ({:x})", tcp_packet.flags());
            unused_rc = socket->send_tcp_packet(TCPFlags::RST);
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
            unused_rc = socket->send_tcp_packet(TCPFlags::RST);
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
            dbgln("handle_tcp: unexpected flags in FinWait1 state ({:x})", tcp_packet.flags());
            unused_rc = socket->send_tcp_packet(TCPFlags::RST);
            socket->set_state(TCPSocket::State::Closed);
            return;
        }
    case TCPSocket::State::FinWait2:
        switch (tcp_packet.flags()) {
        case TCPFlags::FIN:
            socket->set_ack_number(tcp_packet.sequence_number() + payload_size + 1);
            socket->set_state(TCPSocket::State::TimeWait);
            return;
        case TCPFlags::ACK | TCPFlags::RST:
            socket->set_state(TCPSocket::State::Closed);
            return;
        default:
            dbgln("handle_tcp: unexpected flags in FinWait2 state ({:x})", tcp_packet.flags());
            unused_rc = socket->send_tcp_packet(TCPFlags::RST);
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
            unused_rc = socket->send_tcp_packet(TCPFlags::RST);
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
    {
        Locker locker(TCPSocket::sockets_for_retransmit().lock(), LockMode::Shared);
        for (auto& socket : TCPSocket::sockets_for_retransmit().resource())
            sockets.append(*socket);
    }

    for (auto& socket : sockets) {
        Locker socket_locker(socket.lock());
        socket.retransmit_packets();
    }
}

}
