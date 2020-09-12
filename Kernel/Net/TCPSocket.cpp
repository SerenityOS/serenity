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

#include <AK/Singleton.h>
#include <AK/Time.h>
#include <Kernel/Devices/RandomDevice.h>
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/Net/NetworkAdapter.h>
#include <Kernel/Net/Routing.h>
#include <Kernel/Net/TCP.h>
#include <Kernel/Net/TCPSocket.h>
#include <Kernel/Process.h>
#include <Kernel/Random.h>

//#define TCP_SOCKET_DEBUG

namespace Kernel {

void TCPSocket::for_each(Function<void(const TCPSocket&)> callback)
{
    LOCKER(sockets_by_tuple().lock(), Lock::Mode::Shared);
    for (auto& it : sockets_by_tuple().resource())
        callback(*it.value);
}

void TCPSocket::set_state(State new_state)
{
#ifdef TCP_SOCKET_DEBUG
    dbg() << "TCPSocket{" << this << "} state moving from " << to_string(m_state) << " to " << to_string(new_state);
#endif

    m_state = new_state;

    if (new_state == State::Established && m_direction == Direction::Outgoing)
        m_role = Role::Connected;

    if (new_state == State::Closed) {
        LOCKER(closing_sockets().lock());
        closing_sockets().resource().remove(tuple());
    }
}

static AK::Singleton<Lockable<HashMap<IPv4SocketTuple, RefPtr<TCPSocket>>>> s_socket_closing;

Lockable<HashMap<IPv4SocketTuple, RefPtr<TCPSocket>>>& TCPSocket::closing_sockets()
{
    return *s_socket_closing;
}

static AK::Singleton<Lockable<HashMap<IPv4SocketTuple, TCPSocket*>>> s_socket_tuples;

Lockable<HashMap<IPv4SocketTuple, TCPSocket*>>& TCPSocket::sockets_by_tuple()
{
    return *s_socket_tuples;
}

RefPtr<TCPSocket> TCPSocket::from_tuple(const IPv4SocketTuple& tuple)
{
    LOCKER(sockets_by_tuple().lock(), Lock::Mode::Shared);

    auto exact_match = sockets_by_tuple().resource().get(tuple);
    if (exact_match.has_value())
        return { *exact_match.value() };

    auto address_tuple = IPv4SocketTuple(tuple.local_address(), tuple.local_port(), IPv4Address(), 0);
    auto address_match = sockets_by_tuple().resource().get(address_tuple);
    if (address_match.has_value())
        return { *address_match.value() };

    auto wildcard_tuple = IPv4SocketTuple(IPv4Address(), tuple.local_port(), IPv4Address(), 0);
    auto wildcard_match = sockets_by_tuple().resource().get(wildcard_tuple);
    if (wildcard_match.has_value())
        return { *wildcard_match.value() };

    return {};
}

RefPtr<TCPSocket> TCPSocket::from_endpoints(const IPv4Address& local_address, u16 local_port, const IPv4Address& peer_address, u16 peer_port)
{
    return from_tuple(IPv4SocketTuple(local_address, local_port, peer_address, peer_port));
}

RefPtr<TCPSocket> TCPSocket::create_client(const IPv4Address& new_local_address, u16 new_local_port, const IPv4Address& new_peer_address, u16 new_peer_port)
{
    auto tuple = IPv4SocketTuple(new_local_address, new_local_port, new_peer_address, new_peer_port);

    LOCKER(sockets_by_tuple().lock());
    if (sockets_by_tuple().resource().contains(tuple))
        return {};

    auto client = TCPSocket::create(protocol());

    client->set_setup_state(SetupState::InProgress);
    client->set_local_address(new_local_address);
    client->set_local_port(new_local_port);
    client->set_peer_address(new_peer_address);
    client->set_peer_port(new_peer_port);
    client->set_direction(Direction::Incoming);
    client->set_originator(*this);

    m_pending_release_for_accept.set(tuple, client);
    sockets_by_tuple().resource().set(tuple, client);

    return from_tuple(tuple);
}

void TCPSocket::release_to_originator()
{
    ASSERT(!!m_originator);
    m_originator->release_for_accept(this);
}

void TCPSocket::release_for_accept(RefPtr<TCPSocket> socket)
{
    ASSERT(m_pending_release_for_accept.contains(socket->tuple()));
    m_pending_release_for_accept.remove(socket->tuple());
    // FIXME: Should we observe this error somehow?
    (void)queue_connection_from(*socket);
}

TCPSocket::TCPSocket(int protocol)
    : IPv4Socket(SOCK_STREAM, protocol)
{
}

TCPSocket::~TCPSocket()
{
    LOCKER(sockets_by_tuple().lock());
    sockets_by_tuple().resource().remove(tuple());

#ifdef TCP_SOCKET_DEBUG
    dbg() << "~TCPSocket in state " << to_string(state());
#endif
}

NonnullRefPtr<TCPSocket> TCPSocket::create(int protocol)
{
    return adopt(*new TCPSocket(protocol));
}

KResultOr<size_t> TCPSocket::protocol_receive(const KBuffer& packet_buffer, UserOrKernelBuffer& buffer, size_t buffer_size, int flags)
{
    (void)flags;
    auto& ipv4_packet = *(const IPv4Packet*)(packet_buffer.data());
    auto& tcp_packet = *static_cast<const TCPPacket*>(ipv4_packet.payload());
    size_t payload_size = packet_buffer.size() - sizeof(IPv4Packet) - tcp_packet.header_size();
#ifdef TCP_SOCKET_DEBUG
    klog() << "payload_size " << payload_size << ", will it fit in " << buffer_size << "?";
#endif
    ASSERT(buffer_size >= payload_size);
    if (!buffer.write(tcp_packet.payload(), payload_size))
        return KResult(-EFAULT);
    return payload_size;
}

KResultOr<size_t> TCPSocket::protocol_send(const UserOrKernelBuffer& data, size_t data_length)
{
    int err = send_tcp_packet(TCPFlags::PUSH | TCPFlags::ACK, &data, data_length);
    if (err < 0)
        return KResult(err);
    return data_length;
}

int TCPSocket::send_tcp_packet(u16 flags, const UserOrKernelBuffer* payload, size_t payload_size)
{
    auto buffer = ByteBuffer::create_zeroed(sizeof(TCPPacket) + payload_size);
    auto& tcp_packet = *(TCPPacket*)(buffer.data());
    ASSERT(local_port());
    tcp_packet.set_source_port(local_port());
    tcp_packet.set_destination_port(peer_port());
    tcp_packet.set_window_size(1024);
    tcp_packet.set_sequence_number(m_sequence_number);
    tcp_packet.set_data_offset(sizeof(TCPPacket) / sizeof(u32));
    tcp_packet.set_flags(flags);

    if (flags & TCPFlags::ACK)
        tcp_packet.set_ack_number(m_ack_number);

    if (payload && !payload->read(tcp_packet.payload(), payload_size))
        return -EFAULT;

    if (flags & TCPFlags::SYN) {
        ++m_sequence_number;
    } else {
        m_sequence_number += payload_size;
    }

    tcp_packet.set_checksum(compute_tcp_checksum(local_address(), peer_address(), tcp_packet, payload_size));

    if (tcp_packet.has_syn() || payload_size > 0) {
        LOCKER(m_not_acked_lock);
        m_not_acked.append({ m_sequence_number, move(buffer) });
        send_outgoing_packets();
        return 0;
    }

    auto routing_decision = route_to(peer_address(), local_address(), bound_interface());
    ASSERT(!routing_decision.is_zero());

    auto packet_buffer = UserOrKernelBuffer::for_kernel_buffer(buffer.data());
    int err = routing_decision.adapter->send_ipv4(
        routing_decision.next_hop, peer_address(), IPv4Protocol::TCP,
        packet_buffer, buffer.size(), ttl());
    if (err < 0)
        return err;

    m_packets_out++;
    m_bytes_out += buffer.size();
    return 0;
}

void TCPSocket::send_outgoing_packets()
{
    auto routing_decision = route_to(peer_address(), local_address(), bound_interface());
    ASSERT(!routing_decision.is_zero());

    auto now = kgettimeofday();

    LOCKER(m_not_acked_lock, Lock::Mode::Shared);
    for (auto& packet : m_not_acked) {
        timeval diff;
        timeval_sub(packet.tx_time, now, diff);
        if (diff.tv_sec == 0 && diff.tv_usec <= 500000)
            continue;
        packet.tx_time = now;
        packet.tx_counter++;

#ifdef TCP_SOCKET_DEBUG
        auto& tcp_packet = *(TCPPacket*)(packet.buffer.data());
        klog() << "sending tcp packet from " << local_address().to_string().characters() << ":" << local_port() << " to " << peer_address().to_string().characters() << ":" << peer_port() << " with (" << (tcp_packet.has_syn() ? "SYN " : "") << (tcp_packet.has_ack() ? "ACK " : "") << (tcp_packet.has_fin() ? "FIN " : "") << (tcp_packet.has_rst() ? "RST " : "") << ") seq_no=" << tcp_packet.sequence_number() << ", ack_no=" << tcp_packet.ack_number() << ", tx_counter=" << packet.tx_counter;
#endif
        auto packet_buffer = UserOrKernelBuffer::for_kernel_buffer(packet.buffer.data());
        int err = routing_decision.adapter->send_ipv4(
            routing_decision.next_hop, peer_address(), IPv4Protocol::TCP,
            packet_buffer, packet.buffer.size(), ttl());
        if (err < 0) {
            auto& tcp_packet = *(TCPPacket*)(packet.buffer.data());
            klog() << "Error (" << err << ") sending tcp packet from " << local_address().to_string().characters() << ":" << local_port() << " to " << peer_address().to_string().characters() << ":" << peer_port() << " with (" << (tcp_packet.has_syn() ? "SYN " : "") << (tcp_packet.has_ack() ? "ACK " : "") << (tcp_packet.has_fin() ? "FIN " : "") << (tcp_packet.has_rst() ? "RST " : "") << ") seq_no=" << tcp_packet.sequence_number() << ", ack_no=" << tcp_packet.ack_number() << ", tx_counter=" << packet.tx_counter;
        } else {
            m_packets_out++;
            m_bytes_out += packet.buffer.size();
        }
    }
}

void TCPSocket::receive_tcp_packet(const TCPPacket& packet, u16 size)
{
    if (packet.has_ack()) {
        u32 ack_number = packet.ack_number();

#ifdef TCP_SOCKET_DEBUG
        dbg() << "TCPSocket: receive_tcp_packet: " << ack_number;
#endif

        int removed = 0;
        LOCKER(m_not_acked_lock);
        while (!m_not_acked.is_empty()) {
            auto& packet = m_not_acked.first();

#ifdef TCP_SOCKET_DEBUG
            dbg() << "TCPSocket: iterate: " << packet.ack_number;
#endif

            if (packet.ack_number <= ack_number) {
                m_not_acked.take_first();
                removed++;
            } else {
                break;
            }
        }

#ifdef TCP_SOCKET_DEBUG
        dbg() << "TCPSocket: receive_tcp_packet acknowledged " << removed << " packets";
#endif
    }

    m_packets_in++;
    m_bytes_in += packet.header_size() + size;
}

NetworkOrdered<u16> TCPSocket::compute_tcp_checksum(const IPv4Address& source, const IPv4Address& destination, const TCPPacket& packet, u16 payload_size)
{
    struct [[gnu::packed]] PseudoHeader
    {
        IPv4Address source;
        IPv4Address destination;
        u8 zero;
        u8 protocol;
        NetworkOrdered<u16> payload_size;
    };

    PseudoHeader pseudo_header { source, destination, 0, (u8)IPv4Protocol::TCP, sizeof(TCPPacket) + payload_size };

    u32 checksum = 0;
    auto* w = (const NetworkOrdered<u16>*)&pseudo_header;
    for (size_t i = 0; i < sizeof(pseudo_header) / sizeof(u16); ++i) {
        checksum += w[i];
        if (checksum > 0xffff)
            checksum = (checksum >> 16) + (checksum & 0xffff);
    }
    w = (const NetworkOrdered<u16>*)&packet;
    for (size_t i = 0; i < sizeof(packet) / sizeof(u16); ++i) {
        checksum += w[i];
        if (checksum > 0xffff)
            checksum = (checksum >> 16) + (checksum & 0xffff);
    }
    ASSERT(packet.data_offset() * 4 == sizeof(TCPPacket));
    w = (const NetworkOrdered<u16>*)packet.payload();
    for (size_t i = 0; i < payload_size / sizeof(u16); ++i) {
        checksum += w[i];
        if (checksum > 0xffff)
            checksum = (checksum >> 16) + (checksum & 0xffff);
    }
    if (payload_size & 1) {
        u16 expanded_byte = ((const u8*)packet.payload())[payload_size - 1] << 8;
        checksum += expanded_byte;
        if (checksum > 0xffff)
            checksum = (checksum >> 16) + (checksum & 0xffff);
    }
    return ~(checksum & 0xffff);
}

KResult TCPSocket::protocol_bind()
{
    if (has_specific_local_address() && !m_adapter) {
        m_adapter = NetworkAdapter::from_ipv4_address(local_address());
        if (!m_adapter)
            return KResult(-EADDRNOTAVAIL);
    }

    return KSuccess;
}

KResult TCPSocket::protocol_listen()
{
    LOCKER(sockets_by_tuple().lock());
    if (sockets_by_tuple().resource().contains(tuple()))
        return KResult(-EADDRINUSE);
    sockets_by_tuple().resource().set(tuple(), this);
    set_direction(Direction::Passive);
    set_state(State::Listen);
    set_setup_state(SetupState::Completed);
    return KSuccess;
}

KResult TCPSocket::protocol_connect(FileDescription& description, ShouldBlock should_block)
{
    auto routing_decision = route_to(peer_address(), local_address());
    if (routing_decision.is_zero())
        return KResult(-EHOSTUNREACH);
    if (!has_specific_local_address())
        set_local_address(routing_decision.adapter->ipv4_address());

    allocate_local_port_if_needed();

    m_sequence_number = get_good_random<u32>();
    m_ack_number = 0;

    set_setup_state(SetupState::InProgress);
    int err = send_tcp_packet(TCPFlags::SYN);
    if (err < 0)
        return KResult(err);
    m_state = State::SynSent;
    m_role = Role::Connecting;
    m_direction = Direction::Outgoing;

    if (should_block == ShouldBlock::Yes) {
        if (Thread::current()->block<Thread::ConnectBlocker>(nullptr, description).was_interrupted())
            return KResult(-EINTR);
        ASSERT(setup_state() == SetupState::Completed);
        if (has_error()) {
            m_role = Role::None;
            return KResult(-ECONNREFUSED);
        }
        return KSuccess;
    }

    return KResult(-EINPROGRESS);
}

int TCPSocket::protocol_allocate_local_port()
{
    static const u16 first_ephemeral_port = 32768;
    static const u16 last_ephemeral_port = 60999;
    static const u16 ephemeral_port_range_size = last_ephemeral_port - first_ephemeral_port;
    u16 first_scan_port = first_ephemeral_port + get_good_random<u16>() % ephemeral_port_range_size;

    LOCKER(sockets_by_tuple().lock());
    for (u16 port = first_scan_port;;) {
        IPv4SocketTuple proposed_tuple(local_address(), port, peer_address(), peer_port());

        auto it = sockets_by_tuple().resource().find(proposed_tuple);
        if (it == sockets_by_tuple().resource().end()) {
            set_local_port(port);
            sockets_by_tuple().resource().set(proposed_tuple, this);
            return port;
        }
        ++port;
        if (port > last_ephemeral_port)
            port = first_ephemeral_port;
        if (port == first_scan_port)
            break;
    }
    return -EADDRINUSE;
}

bool TCPSocket::protocol_is_disconnected() const
{
    switch (m_state) {
    case State::Closed:
    case State::CloseWait:
    case State::LastAck:
    case State::FinWait1:
    case State::FinWait2:
    case State::Closing:
    case State::TimeWait:
        return true;
    default:
        return false;
    }
}

void TCPSocket::shut_down_for_writing()
{
    if (state() == State::Established) {
#ifdef TCP_SOCKET_DEBUG
        dbg() << " Sending FIN/ACK from Established and moving into FinWait1";
#endif
        (void)send_tcp_packet(TCPFlags::FIN | TCPFlags::ACK);
        set_state(State::FinWait1);
    } else {
        dbg() << " Shutting down TCPSocket for writing but not moving to FinWait1 since state is " << to_string(state());
    }
}

KResult TCPSocket::close()
{
    auto result = IPv4Socket::close();
    if (state() == State::CloseWait) {
#ifdef TCP_SOCKET_DEBUG
        dbg() << " Sending FIN from CloseWait and moving into LastAck";
#endif
        (void)send_tcp_packet(TCPFlags::FIN | TCPFlags::ACK);
        set_state(State::LastAck);
    }

    LOCKER(closing_sockets().lock());
    closing_sockets().resource().set(tuple(), *this);
    return result;
}

}
