/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
static void handle_ipv4(const EthernetFrameHeader&, size_t frame_size, const timeval& packet_timestamp);
static void handle_icmp(const EthernetFrameHeader&, const IPv4Packet&, const timeval& packet_timestamp);
static void handle_udp(const IPv4Packet&, const timeval& packet_timestamp);
static void handle_tcp(const IPv4Packet&, const timeval& packet_timestamp);

[[noreturn]] static void NetworkTask_main(void*);

void NetworkTask::spawn()
{
    RefPtr<Thread> thread;
    Process::create_kernel_process(thread, "NetworkTask", NetworkTask_main, nullptr);
}

void NetworkTask_main(void*)
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

        klog() << "NetworkTask: " << adapter.class_name() << " network adapter found: hw=" << adapter.mac_address().to_string().characters() << " address=" << adapter.ipv4_address().to_string().characters() << " netmask=" << adapter.ipv4_netmask().to_string().characters() << " gateway=" << adapter.ipv4_gateway().to_string().characters();

        adapter.on_receive = [&]() {
            pending_packets++;
            packet_wait_queue.wake_all();
        };
    });

    auto dequeue_packet = [&pending_packets](u8* buffer, size_t buffer_size, timeval& packet_timestamp) -> size_t {
        if (pending_packets == 0)
            return 0;
        size_t packet_size = 0;
        NetworkAdapter::for_each([&](auto& adapter) {
            if (packet_size || !adapter.has_queued_packets())
                return;
            packet_size = adapter.dequeue_packet(buffer, buffer_size, packet_timestamp);
            pending_packets--;
#if NETWORK_TASK_DEBUG
            klog() << "NetworkTask: Dequeued packet from " << adapter.name().characters() << " (" << packet_size << " bytes)";
#endif
        });
        return packet_size;
    };

    size_t buffer_size = 64 * KiB;
    auto buffer_region = MM.allocate_kernel_region(buffer_size, "Kernel Packet Buffer", Region::Access::Read | Region::Access::Write);
    auto buffer = (u8*)buffer_region->vaddr().get();
    timeval packet_timestamp;

    klog() << "NetworkTask: Enter main loop.";
    for (;;) {
        size_t packet_size = dequeue_packet(buffer, buffer_size, packet_timestamp);
        if (!packet_size) {
            packet_wait_queue.wait_forever("NetworkTask");
            continue;
        }
        if (packet_size < sizeof(EthernetFrameHeader)) {
            klog() << "NetworkTask: Packet is too small to be an Ethernet packet! (" << packet_size << ")";
            continue;
        }
        auto& eth = *(const EthernetFrameHeader*)buffer;
#if ETHERNET_DEBUG
        dbgln("NetworkTask: From {} to {}, ether_type={:#04x}, packet_size={}", eth.source().to_string(), eth.destination().to_string(), eth.ether_type(), packet_size);
#endif

#if ETHERNET_VERY_DEBUG
        for (size_t i = 0; i < packet_size; i++) {
            klog() << String::format("%#02x", buffer[i]);

            switch (i % 16) {
            case 7:
                klog() << "  ";
                break;
            case 15:
                klog() << "";
                break;
            default:
                klog() << " ";
                break;
            }
        }

        klog() << "";
#endif

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
            klog() << "NetworkTask: Unknown ethernet type 0x" << String::format("%x", eth.ether_type());
        }
    }
}

void handle_arp(const EthernetFrameHeader& eth, size_t frame_size)
{
    constexpr size_t minimum_arp_frame_size = sizeof(EthernetFrameHeader) + sizeof(ARPPacket);
    if (frame_size < minimum_arp_frame_size) {
        klog() << "handle_arp: Frame too small (" << frame_size << ", need " << minimum_arp_frame_size << ")";
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

#if ARP_DEBUG
    dbgln("handle_arp: operation={:#04x}, sender={}/{}, target={}/{}",
        packet.operation(),
        packet.sender_hardware_address().to_string(),
        packet.sender_protocol_address().to_string(),
        packet.target_hardware_address().to_string(),
        packet.target_protocol_address().to_string());
#endif

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
            klog() << "handle_arp: Responding to ARP request for my IPv4 address (" << adapter->ipv4_address().to_string().characters() << ")";
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

void handle_ipv4(const EthernetFrameHeader& eth, size_t frame_size, const timeval& packet_timestamp)
{
    constexpr size_t minimum_ipv4_frame_size = sizeof(EthernetFrameHeader) + sizeof(IPv4Packet);
    if (frame_size < minimum_ipv4_frame_size) {
        klog() << "handle_ipv4: Frame too small (" << frame_size << ", need " << minimum_ipv4_frame_size << ")";
        return;
    }
    auto& packet = *static_cast<const IPv4Packet*>(eth.payload());

    if (packet.length() < sizeof(IPv4Packet)) {
        klog() << "handle_ipv4: IPv4 packet too short (" << packet.length() << ", need " << sizeof(IPv4Packet) << ")";
        return;
    }

    size_t actual_ipv4_packet_length = frame_size - sizeof(EthernetFrameHeader);
    if (packet.length() > actual_ipv4_packet_length) {
        klog() << "handle_ipv4: IPv4 packet claims to be longer than it is (" << packet.length() << ", actually " << actual_ipv4_packet_length << ")";
        return;
    }

#if IPV4_DEBUG
    klog() << "handle_ipv4: source=" << packet.source().to_string().characters() << ", target=" << packet.destination().to_string().characters();
#endif

    switch ((IPv4Protocol)packet.protocol()) {
    case IPv4Protocol::ICMP:
        return handle_icmp(eth, packet, packet_timestamp);
    case IPv4Protocol::UDP:
        return handle_udp(packet, packet_timestamp);
    case IPv4Protocol::TCP:
        return handle_tcp(packet, packet_timestamp);
    default:
        klog() << "handle_ipv4: Unhandled protocol " << packet.protocol();
        break;
    }
}

void handle_icmp(const EthernetFrameHeader& eth, const IPv4Packet& ipv4_packet, const timeval& packet_timestamp)
{
    auto& icmp_header = *static_cast<const ICMPHeader*>(ipv4_packet.payload());
#if ICMP_DEBUG
    dbgln("handle_icmp: source={}, destination={}, type={:#02x}, code={:#02x}", ipv4_packet.source().to_string(), ipv4_packet.destination().to_string(), icmp_header.type(), icmp_header.code());
#endif

    {
        NonnullRefPtrVector<IPv4Socket> icmp_sockets;
        {
            LOCKER(IPv4Socket::all_sockets().lock(), Lock::Mode::Shared);
            for (auto* socket : IPv4Socket::all_sockets().resource()) {
                if (socket->protocol() != (unsigned)IPv4Protocol::ICMP)
                    continue;
                icmp_sockets.append(*socket);
            }
        }
        for (auto& socket : icmp_sockets)
            socket.did_receive(ipv4_packet.source(), 0, KBuffer::copy(&ipv4_packet, sizeof(IPv4Packet) + ipv4_packet.payload_size()), packet_timestamp);
    }

    auto adapter = NetworkAdapter::from_ipv4_address(ipv4_packet.destination());
    if (!adapter)
        return;

    if (icmp_header.type() == ICMPType::EchoRequest) {
        auto& request = reinterpret_cast<const ICMPEchoPacket&>(icmp_header);
        klog() << "handle_icmp: EchoRequest from " << ipv4_packet.source().to_string().characters() << ": id=" << (u16)request.identifier << ", seq=" << (u16)request.sequence_number;
        size_t icmp_packet_size = ipv4_packet.payload_size();
        if (icmp_packet_size < sizeof(ICMPEchoPacket)) {
            klog() << "handle_icmp: EchoRequest packet is too small, ignoring.";
            return;
        }
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
        auto response_buffer = UserOrKernelBuffer::for_kernel_buffer((u8*)&response);
        [[maybe_unused]] auto result = adapter->send_ipv4(eth.source(), ipv4_packet.source(), IPv4Protocol::ICMP, response_buffer, buffer.size(), 64);
    }
}

void handle_udp(const IPv4Packet& ipv4_packet, const timeval& packet_timestamp)
{
    if (ipv4_packet.payload_size() < sizeof(UDPPacket)) {
        klog() << "handle_udp: Packet too small (" << ipv4_packet.payload_size() << ", need " << sizeof(UDPPacket) << ")";
        return;
    }

    auto adapter = NetworkAdapter::from_ipv4_address(ipv4_packet.destination());
    if (!adapter && ipv4_packet.destination() != IPv4Address(255, 255, 255, 255)) {
        klog() << "handle_udp: this packet is not for me, it's for " << ipv4_packet.destination().to_string().characters();
        return;
    }

    auto& udp_packet = *static_cast<const UDPPacket*>(ipv4_packet.payload());
#if UDP_DEBUG
    klog() << "handle_udp: source=" << ipv4_packet.source().to_string().characters() << ":" << udp_packet.source_port() << ", destination=" << ipv4_packet.destination().to_string().characters() << ":" << udp_packet.destination_port() << " length=" << udp_packet.length();
#endif

    auto socket = UDPSocket::from_port(udp_packet.destination_port());
    if (!socket) {
        klog() << "handle_udp: No UDP socket for port " << udp_packet.destination_port();
        return;
    }

    VERIFY(socket->type() == SOCK_DGRAM);
    VERIFY(socket->local_port() == udp_packet.destination_port());
    socket->did_receive(ipv4_packet.source(), udp_packet.source_port(), KBuffer::copy(&ipv4_packet, sizeof(IPv4Packet) + ipv4_packet.payload_size()), packet_timestamp);
}

void handle_tcp(const IPv4Packet& ipv4_packet, const timeval& packet_timestamp)
{
    if (ipv4_packet.payload_size() < sizeof(TCPPacket)) {
        klog() << "handle_tcp: IPv4 payload is too small to be a TCP packet (" << ipv4_packet.payload_size() << ", need " << sizeof(TCPPacket) << ")";
        return;
    }

    auto& tcp_packet = *static_cast<const TCPPacket*>(ipv4_packet.payload());

    size_t minimum_tcp_header_size = 5 * sizeof(u32);
    size_t maximum_tcp_header_size = 15 * sizeof(u32);
    if (tcp_packet.header_size() < minimum_tcp_header_size || tcp_packet.header_size() > maximum_tcp_header_size) {
        klog() << "handle_tcp: TCP packet header has invalid size " << tcp_packet.header_size();
    }

    if (ipv4_packet.payload_size() < tcp_packet.header_size()) {
        klog() << "handle_tcp: IPv4 payload is smaller than TCP header claims (" << ipv4_packet.payload_size() << ", supposedly " << tcp_packet.header_size() << ")";
        return;
    }

    size_t payload_size = ipv4_packet.payload_size() - tcp_packet.header_size();

#if TCP_DEBUG
    dbgln("handle_tcp: source={}:{}, destination={}:{}, seq_no={}, ack_no={}, flags={:#04x} ({}{}{}{}), window_size={}, payload_size={}",
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
#endif

    auto adapter = NetworkAdapter::from_ipv4_address(ipv4_packet.destination());
    if (!adapter) {
        klog() << "handle_tcp: this packet is not for me, it's for " << ipv4_packet.destination().to_string().characters();
        return;
    }

    IPv4SocketTuple tuple(ipv4_packet.destination(), tcp_packet.destination_port(), ipv4_packet.source(), tcp_packet.source_port());

#if TCP_DEBUG
    klog() << "handle_tcp: looking for socket; tuple=" << tuple.to_string().characters();
#endif

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

    LOCKER(socket->lock());

    VERIFY(socket->type() == SOCK_STREAM);
    VERIFY(socket->local_port() == tcp_packet.destination_port());

#if TCP_DEBUG
    klog() << "handle_tcp: got socket; state=" << socket->tuple().to_string().characters() << " " << TCPSocket::to_string(socket->state());
#endif

    socket->receive_tcp_packet(tcp_packet, ipv4_packet.payload_size());

    [[maybe_unused]] int unused_rc {};
    switch (socket->state()) {
    case TCPSocket::State::Closed:
        klog() << "handle_tcp: unexpected flags in Closed state";
        // TODO: we may want to send an RST here, maybe as a configurable option
        return;
    case TCPSocket::State::TimeWait:
        klog() << "handle_tcp: unexpected flags in TimeWait state";
        unused_rc = socket->send_tcp_packet(TCPFlags::RST);
        socket->set_state(TCPSocket::State::Closed);
        return;
    case TCPSocket::State::Listen:
        switch (tcp_packet.flags()) {
        case TCPFlags::SYN: {
#if TCP_DEBUG
            klog() << "handle_tcp: incoming connection";
#endif
            auto& local_address = ipv4_packet.destination();
            auto& peer_address = ipv4_packet.source();
            auto client = socket->create_client(local_address, tcp_packet.destination_port(), peer_address, tcp_packet.source_port());
            if (!client) {
                klog() << "handle_tcp: couldn't create client socket";
                return;
            }
            LOCKER(client->lock());
#if TCP_DEBUG
            klog() << "handle_tcp: created new client socket with tuple " << client->tuple().to_string().characters();
#endif
            client->set_sequence_number(1000);
            client->set_ack_number(tcp_packet.sequence_number() + payload_size + 1);
            [[maybe_unused]] auto rc2 = client->send_tcp_packet(TCPFlags::SYN | TCPFlags::ACK);
            client->set_state(TCPSocket::State::SynReceived);
            return;
        }
        default:
            klog() << "handle_tcp: unexpected flags in Listen state";
            // socket->send_tcp_packet(TCPFlags::RST);
            return;
        }
    case TCPSocket::State::SynSent:
        switch (tcp_packet.flags()) {
        case TCPFlags::SYN:
            socket->set_ack_number(tcp_packet.sequence_number() + payload_size + 1);
            unused_rc = socket->send_tcp_packet(TCPFlags::ACK);
            socket->set_state(TCPSocket::State::SynReceived);
            return;
        case TCPFlags::ACK | TCPFlags::SYN:
            socket->set_ack_number(tcp_packet.sequence_number() + payload_size + 1);
            unused_rc = socket->send_tcp_packet(TCPFlags::ACK);
            socket->set_state(TCPSocket::State::Established);
            socket->set_setup_state(Socket::SetupState::Completed);
            socket->set_connected(true);
            return;
        case TCPFlags::ACK | TCPFlags::FIN:
            socket->set_ack_number(tcp_packet.sequence_number() + payload_size + 1);
            unused_rc = socket->send_tcp_packet(TCPFlags::ACK);
            socket->set_state(TCPSocket::State::Closed);
            socket->set_error(TCPSocket::Error::FINDuringConnect);
            socket->set_setup_state(Socket::SetupState::Completed);
            return;
        case TCPFlags::ACK | TCPFlags::RST:
            socket->set_ack_number(tcp_packet.sequence_number() + payload_size);
            unused_rc = socket->send_tcp_packet(TCPFlags::ACK);
            socket->set_state(TCPSocket::State::Closed);
            socket->set_error(TCPSocket::Error::RSTDuringConnect);
            socket->set_setup_state(Socket::SetupState::Completed);
            return;
        default:
            klog() << "handle_tcp: unexpected flags in SynSent state";
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
                    klog() << "handle_tcp: connection doesn't have an originating socket; maybe it went away?";
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
                klog() << "handle_tcp: got ACK in SynReceived state but direction is invalid (" << TCPSocket::to_string(socket->direction()) << ")";
                unused_rc = socket->send_tcp_packet(TCPFlags::RST);
                socket->set_state(TCPSocket::State::Closed);
                return;
            }

            return;
        default:
            klog() << "handle_tcp: unexpected flags in SynReceived state";
            unused_rc = socket->send_tcp_packet(TCPFlags::RST);
            socket->set_state(TCPSocket::State::Closed);
            return;
        }
    case TCPSocket::State::CloseWait:
        switch (tcp_packet.flags()) {
        default:
            klog() << "handle_tcp: unexpected flags in CloseWait state";
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
            klog() << "handle_tcp: unexpected flags in LastAck state";
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
            klog() << "handle_tcp: unexpected flags in FinWait1 state";
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
            klog() << "handle_tcp: unexpected flags in FinWait2 state";
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
            klog() << "handle_tcp: unexpected flags in Closing state";
            unused_rc = socket->send_tcp_packet(TCPFlags::RST);
            socket->set_state(TCPSocket::State::Closed);
            return;
        }
    case TCPSocket::State::Established:
        if (tcp_packet.has_fin()) {
            if (payload_size != 0)
                socket->did_receive(ipv4_packet.source(), tcp_packet.source_port(), KBuffer::copy(&ipv4_packet, sizeof(IPv4Packet) + ipv4_packet.payload_size()), packet_timestamp);

            socket->set_ack_number(tcp_packet.sequence_number() + payload_size + 1);
            unused_rc = socket->send_tcp_packet(TCPFlags::ACK);
            socket->set_state(TCPSocket::State::CloseWait);
            socket->set_connected(false);
            return;
        }

        socket->set_ack_number(tcp_packet.sequence_number() + payload_size);

#if TCP_DEBUG
        klog() << "Got packet with ack_no=" << tcp_packet.ack_number() << ", seq_no=" << tcp_packet.sequence_number() << ", payload_size=" << payload_size << ", acking it with new ack_no=" << socket->ack_number() << ", seq_no=" << socket->sequence_number();
#endif

        if (payload_size) {
            if (socket->did_receive(ipv4_packet.source(), tcp_packet.source_port(), KBuffer::copy(&ipv4_packet, sizeof(IPv4Packet) + ipv4_packet.payload_size()), packet_timestamp))
                unused_rc = socket->send_tcp_packet(TCPFlags::ACK);
        }
    }
}

}
