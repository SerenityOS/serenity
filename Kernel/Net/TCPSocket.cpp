/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <AK/Time.h>
#include <Kernel/Debug.h>
#include <Kernel/Devices/RandomDevice.h>
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/Net/EthernetFrameHeader.h>
#include <Kernel/Net/IPv4.h>
#include <Kernel/Net/NetworkAdapter.h>
#include <Kernel/Net/Routing.h>
#include <Kernel/Net/TCP.h>
#include <Kernel/Net/TCPSocket.h>
#include <Kernel/Process.h>
#include <Kernel/Random.h>

namespace Kernel {

void TCPSocket::for_each(Function<void(const TCPSocket&)> callback)
{
    Locker locker(sockets_by_tuple().lock(), Lock::Mode::Shared);
    for (auto& it : sockets_by_tuple().resource())
        callback(*it.value);
}

void TCPSocket::set_state(State new_state)
{
    dbgln_if(TCP_SOCKET_DEBUG, "TCPSocket({}) state moving from {} to {}", this, to_string(m_state), to_string(new_state));

    auto was_disconnected = protocol_is_disconnected();
    auto previous_role = m_role;

    m_state = new_state;

    if (new_state == State::Established && m_direction == Direction::Outgoing)
        m_role = Role::Connected;

    if (new_state == State::Closed) {
        Locker locker(closing_sockets().lock());
        closing_sockets().resource().remove(tuple());

        if (m_originator)
            release_to_originator();
    }

    if (previous_role != m_role || was_disconnected != protocol_is_disconnected())
        evaluate_block_conditions();
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
    Locker locker(sockets_by_tuple().lock(), Lock::Mode::Shared);

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
RefPtr<TCPSocket> TCPSocket::create_client(const IPv4Address& new_local_address, u16 new_local_port, const IPv4Address& new_peer_address, u16 new_peer_port)
{
    auto tuple = IPv4SocketTuple(new_local_address, new_local_port, new_peer_address, new_peer_port);

    {
        Locker locker(sockets_by_tuple().lock(), Lock::Mode::Shared);
        if (sockets_by_tuple().resource().contains(tuple))
            return {};
    }

    auto result = TCPSocket::create(protocol());
    if (result.is_error())
        return {};

    auto client = result.release_value();
    client->set_setup_state(SetupState::InProgress);
    client->set_local_address(new_local_address);
    client->set_local_port(new_local_port);
    client->set_peer_address(new_peer_address);
    client->set_peer_port(new_peer_port);
    client->set_direction(Direction::Incoming);
    client->set_originator(*this);

    Locker locker(sockets_by_tuple().lock());
    m_pending_release_for_accept.set(tuple, client);
    sockets_by_tuple().resource().set(tuple, client);

    return client;
}

void TCPSocket::release_to_originator()
{
    VERIFY(!!m_originator);
    m_originator.strong_ref()->release_for_accept(this);
    m_originator.clear();
}

void TCPSocket::release_for_accept(RefPtr<TCPSocket> socket)
{
    VERIFY(m_pending_release_for_accept.contains(socket->tuple()));
    m_pending_release_for_accept.remove(socket->tuple());
    // FIXME: Should we observe this error somehow?
    [[maybe_unused]] auto rc = queue_connection_from(*socket);
}

TCPSocket::TCPSocket(int protocol)
    : IPv4Socket(SOCK_STREAM, protocol)
{
    m_last_retransmit_time = kgettimeofday();
}

TCPSocket::~TCPSocket()
{
    Locker locker(sockets_by_tuple().lock());
    sockets_by_tuple().resource().remove(tuple());

    dequeue_for_retransmit();

    dbgln_if(TCP_SOCKET_DEBUG, "~TCPSocket in state {}", to_string(state()));
}

KResultOr<NonnullRefPtr<TCPSocket>> TCPSocket::create(int protocol)
{
    auto socket = adopt_ref_if_nonnull(new TCPSocket(protocol));
    if (socket)
        return socket.release_nonnull();
    return ENOMEM;
}

KResultOr<size_t> TCPSocket::protocol_receive(ReadonlyBytes raw_ipv4_packet, UserOrKernelBuffer& buffer, size_t buffer_size, [[maybe_unused]] int flags)
{
    auto& ipv4_packet = *reinterpret_cast<const IPv4Packet*>(raw_ipv4_packet.data());
    auto& tcp_packet = *static_cast<const TCPPacket*>(ipv4_packet.payload());
    size_t payload_size = raw_ipv4_packet.size() - sizeof(IPv4Packet) - tcp_packet.header_size();
    dbgln_if(TCP_SOCKET_DEBUG, "payload_size {}, will it fit in {}?", payload_size, buffer_size);
    VERIFY(buffer_size >= payload_size);
    if (!buffer.write(tcp_packet.payload(), payload_size))
        return EFAULT;
    return payload_size;
}

KResultOr<size_t> TCPSocket::protocol_send(const UserOrKernelBuffer& data, size_t data_length)
{
    RoutingDecision routing_decision = route_to(peer_address(), local_address(), bound_interface());
    if (routing_decision.is_zero())
        return EHOSTUNREACH;
    size_t mss = routing_decision.adapter->mtu() - sizeof(IPv4Packet) - sizeof(TCPPacket);
    data_length = min(data_length, mss);
    int err = send_tcp_packet(TCPFlags::PUSH | TCPFlags::ACK, &data, data_length, &routing_decision);
    if (err < 0)
        return KResult((ErrnoCode)-err);
    return data_length;
}

KResult TCPSocket::send_ack(bool allow_duplicate)
{
    if (!allow_duplicate && m_last_ack_number_sent == m_ack_number)
        return KSuccess;
    return send_tcp_packet(TCPFlags::ACK);
}

KResult TCPSocket::send_tcp_packet(u16 flags, const UserOrKernelBuffer* payload, size_t payload_size, RoutingDecision* user_routing_decision)
{
    RoutingDecision routing_decision = user_routing_decision ? *user_routing_decision : route_to(peer_address(), local_address(), bound_interface());
    if (routing_decision.is_zero())
        return EHOSTUNREACH;

    auto ipv4_payload_offset = routing_decision.adapter->ipv4_payload_offset();

    const bool has_mss_option = flags == TCPFlags::SYN;
    const size_t options_size = has_mss_option ? sizeof(TCPOptionMSS) : 0;
    const size_t tcp_header_size = sizeof(TCPPacket) + options_size;
    const size_t buffer_size = ipv4_payload_offset + tcp_header_size + payload_size;
    auto packet = routing_decision.adapter->acquire_packet_buffer(buffer_size);
    if (!packet)
        return ENOMEM;
    routing_decision.adapter->fill_in_ipv4_header(*packet, local_address(),
        routing_decision.next_hop, peer_address(), IPv4Protocol::TCP,
        buffer_size - ipv4_payload_offset, ttl());
    memset(packet->buffer.data() + ipv4_payload_offset, 0, sizeof(TCPPacket));
    auto& tcp_packet = *(TCPPacket*)(packet->buffer.data() + ipv4_payload_offset);
    VERIFY(local_port());
    tcp_packet.set_source_port(local_port());
    tcp_packet.set_destination_port(peer_port());
    tcp_packet.set_window_size(NumericLimits<u16>::max());
    tcp_packet.set_sequence_number(m_sequence_number);
    tcp_packet.set_data_offset(tcp_header_size / sizeof(u32));
    tcp_packet.set_flags(flags);

    if (flags & TCPFlags::ACK) {
        m_last_ack_number_sent = m_ack_number;
        m_last_ack_sent_time = kgettimeofday();
        tcp_packet.set_ack_number(m_ack_number);
    }

    if (payload && !payload->read(tcp_packet.payload(), payload_size)) {
        routing_decision.adapter->release_packet_buffer(*packet);
        return EFAULT;
    }

    if (flags & TCPFlags::SYN) {
        ++m_sequence_number;
    } else {
        m_sequence_number += payload_size;
    }

    if (has_mss_option) {
        u16 mss = routing_decision.adapter->mtu() - sizeof(IPv4Packet) - sizeof(TCPPacket);
        TCPOptionMSS mss_option { mss };
        VERIFY(packet->buffer.size() >= ipv4_payload_offset + sizeof(TCPPacket) + sizeof(mss_option));
        memcpy(packet->buffer.data() + ipv4_payload_offset + sizeof(TCPPacket), &mss_option, sizeof(mss_option));
    }

    tcp_packet.set_checksum(compute_tcp_checksum(local_address(), peer_address(), tcp_packet, payload_size));

    routing_decision.adapter->send_packet({ packet->buffer.data(), packet->buffer.size() });

    m_packets_out++;
    m_bytes_out += buffer_size;
    if (tcp_packet.has_syn() || payload_size > 0) {
        Locker locker(m_not_acked_lock);
        m_not_acked.append({ m_sequence_number, move(packet), ipv4_payload_offset, *routing_decision.adapter });
        m_not_acked_size += payload_size;
        enqueue_for_retransmit();
    } else {
        routing_decision.adapter->release_packet_buffer(*packet);
    }

    return KSuccess;
}

void TCPSocket::receive_tcp_packet(const TCPPacket& packet, u16 size)
{
    if (packet.has_ack()) {
        u32 ack_number = packet.ack_number();

        dbgln_if(TCP_SOCKET_DEBUG, "TCPSocket: receive_tcp_packet: {}", ack_number);

        int removed = 0;
        Locker locker(m_not_acked_lock);
        while (!m_not_acked.is_empty()) {
            auto& packet = m_not_acked.first();

            dbgln_if(TCP_SOCKET_DEBUG, "TCPSocket: iterate: {}", packet.ack_number);

            if (packet.ack_number <= ack_number) {
                auto old_adapter = packet.adapter.strong_ref();
                if (old_adapter)
                    old_adapter->release_packet_buffer(*packet.buffer);
                TCPPacket& tcp_packet = *(TCPPacket*)(packet.buffer->buffer.data() + packet.ipv4_payload_offset);
                auto payload_size = packet.buffer->buffer.data() + packet.buffer->buffer.size() - (u8*)tcp_packet.payload();
                m_not_acked_size -= payload_size;
                evaluate_block_conditions();
                m_not_acked.take_first();
                removed++;
            } else {
                break;
            }
        }

        if (m_not_acked.is_empty()) {
            m_retransmit_attempts = 0;
            dequeue_for_retransmit();
        }

        dbgln_if(TCP_SOCKET_DEBUG, "TCPSocket: receive_tcp_packet acknowledged {} packets", removed);
    }

    m_packets_in++;
    m_bytes_in += packet.header_size() + size;
}

bool TCPSocket::should_delay_next_ack() const
{
    // FIXME: We don't know the MSS here so make a reasonable guess.
    const size_t mss = 1500;

    // RFC 1122 says we should send an ACK for every two full-sized segments.
    if (m_ack_number >= m_last_ack_number_sent + 2 * mss)
        return false;

    // RFC 1122 says we should not delay ACKs for more than 500 milliseconds.
    if (kgettimeofday() >= m_last_ack_sent_time + Time::from_milliseconds(500))
        return false;

    return true;
}

NetworkOrdered<u16> TCPSocket::compute_tcp_checksum(const IPv4Address& source, const IPv4Address& destination, const TCPPacket& packet, u16 payload_size)
{
    struct [[gnu::packed]] PseudoHeader {
        IPv4Address source;
        IPv4Address destination;
        u8 zero;
        u8 protocol;
        NetworkOrdered<u16> payload_size;
    };

    PseudoHeader pseudo_header { source, destination, 0, (u8)IPv4Protocol::TCP, packet.header_size() + payload_size };

    u32 checksum = 0;
    auto* w = (const NetworkOrdered<u16>*)&pseudo_header;
    for (size_t i = 0; i < sizeof(pseudo_header) / sizeof(u16); ++i) {
        checksum += w[i];
        if (checksum > 0xffff)
            checksum = (checksum >> 16) + (checksum & 0xffff);
    }
    w = (const NetworkOrdered<u16>*)&packet;
    for (size_t i = 0; i < packet.header_size() / sizeof(u16); ++i) {
        checksum += w[i];
        if (checksum > 0xffff)
            checksum = (checksum >> 16) + (checksum & 0xffff);
    }
    VERIFY(packet.data_offset() * 4 == packet.header_size());
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
            return EADDRNOTAVAIL;
    }

    return KSuccess;
}

KResult TCPSocket::protocol_listen(bool did_allocate_port)
{
    if (!did_allocate_port) {
        Locker socket_locker(sockets_by_tuple().lock());
        if (sockets_by_tuple().resource().contains(tuple()))
            return EADDRINUSE;
        sockets_by_tuple().resource().set(tuple(), this);
    }

    set_direction(Direction::Passive);
    set_state(State::Listen);
    set_setup_state(SetupState::Completed);
    return KSuccess;
}

KResult TCPSocket::protocol_connect(FileDescription& description, ShouldBlock should_block)
{
    Locker locker(lock());

    auto routing_decision = route_to(peer_address(), local_address());
    if (routing_decision.is_zero())
        return EHOSTUNREACH;
    if (!has_specific_local_address())
        set_local_address(routing_decision.adapter->ipv4_address());

    if (auto result = allocate_local_port_if_needed(); result.error_or_port.is_error())
        return result.error_or_port.error();

    m_sequence_number = get_good_random<u32>();
    m_ack_number = 0;

    set_setup_state(SetupState::InProgress);
    int err = send_tcp_packet(TCPFlags::SYN);
    if (err < 0)
        return KResult((ErrnoCode)-err);
    m_state = State::SynSent;
    m_role = Role::Connecting;
    m_direction = Direction::Outgoing;

    evaluate_block_conditions();

    if (should_block == ShouldBlock::Yes) {
        locker.unlock();
        auto unblock_flags = Thread::FileBlocker::BlockFlags::None;
        if (Thread::current()->block<Thread::ConnectBlocker>({}, description, unblock_flags).was_interrupted())
            return EINTR;
        locker.lock();
        VERIFY(setup_state() == SetupState::Completed);
        if (has_error()) { // TODO: check unblock_flags
            m_role = Role::None;
            if (error() == TCPSocket::Error::RetransmitTimeout)
                return ETIMEDOUT;
            else
                return ECONNREFUSED;
        }
        return KSuccess;
    }

    return EINPROGRESS;
}

KResultOr<u16> TCPSocket::protocol_allocate_local_port()
{
    constexpr u16 first_ephemeral_port = 32768;
    constexpr u16 last_ephemeral_port = 60999;
    constexpr u16 ephemeral_port_range_size = last_ephemeral_port - first_ephemeral_port;
    u16 first_scan_port = first_ephemeral_port + get_good_random<u16>() % ephemeral_port_range_size;

    Locker locker(sockets_by_tuple().lock());
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
    return EADDRINUSE;
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
        dbgln_if(TCP_SOCKET_DEBUG, " Sending FIN/ACK from Established and moving into FinWait1");
        [[maybe_unused]] auto rc = send_tcp_packet(TCPFlags::FIN | TCPFlags::ACK);
        set_state(State::FinWait1);
    } else {
        dbgln(" Shutting down TCPSocket for writing but not moving to FinWait1 since state is {}", to_string(state()));
    }
}

KResult TCPSocket::close()
{
    Locker socket_locker(lock());
    auto result = IPv4Socket::close();
    if (state() == State::CloseWait) {
        dbgln_if(TCP_SOCKET_DEBUG, " Sending FIN from CloseWait and moving into LastAck");
        [[maybe_unused]] auto rc = send_tcp_packet(TCPFlags::FIN | TCPFlags::ACK);
        set_state(State::LastAck);
    }

    if (state() != State::Closed && state() != State::Listen) {
        Locker locker(closing_sockets().lock());
        closing_sockets().resource().set(tuple(), *this);
    }
    return result;
}

static AK::Singleton<Lockable<HashTable<TCPSocket*>>> s_sockets_for_retransmit;

Lockable<HashTable<TCPSocket*>>& TCPSocket::sockets_for_retransmit()
{
    return *s_sockets_for_retransmit;
}

void TCPSocket::enqueue_for_retransmit()
{
    Locker locker(sockets_for_retransmit().lock());
    sockets_for_retransmit().resource().set(this);
}

void TCPSocket::dequeue_for_retransmit()
{
    Locker locker(sockets_for_retransmit().lock());
    sockets_for_retransmit().resource().remove(this);
}

void TCPSocket::retransmit_packets()
{
    auto now = kgettimeofday();

    // RFC6298 says we should have at least one second between retransmits. According to
    // RFC1122 we must do exponential backoff - even for SYN packets.
    i64 retransmit_interval = 1;
    for (decltype(m_retransmit_attempts) i = 0; i < m_retransmit_attempts; i++)
        retransmit_interval *= 2;

    if (m_last_retransmit_time > now - Time::from_seconds(retransmit_interval))
        return;

    dbgln_if(TCP_SOCKET_DEBUG, "TCPSocket({}) handling retransmit", this);

    m_last_retransmit_time = now;
    ++m_retransmit_attempts;

    if (m_retransmit_attempts > maximum_retransmits) {
        set_state(TCPSocket::State::Closed);
        set_error(TCPSocket::Error::RetransmitTimeout);
        set_setup_state(Socket::SetupState::Completed);
        return;
    }

    auto routing_decision = route_to(peer_address(), local_address(), bound_interface());
    if (routing_decision.is_zero())
        return;

    Locker locker(m_not_acked_lock, Lock::Mode::Shared);
    for (auto& packet : m_not_acked) {
        packet.tx_counter++;

        if constexpr (TCP_SOCKET_DEBUG) {
            auto& tcp_packet = *(const TCPPacket*)(packet.buffer->buffer.data() + packet.ipv4_payload_offset);
            dbgln("Sending TCP packet from {}:{} to {}:{} with ({}{}{}{}) seq_no={}, ack_no={}, tx_counter={}",
                local_address(), local_port(),
                peer_address(), peer_port(),
                (tcp_packet.has_syn() ? "SYN " : ""),
                (tcp_packet.has_ack() ? "ACK " : ""),
                (tcp_packet.has_fin() ? "FIN " : ""),
                (tcp_packet.has_rst() ? "RST " : ""),
                tcp_packet.sequence_number(),
                tcp_packet.ack_number(),
                packet.tx_counter);
        }

        size_t ipv4_payload_offset = routing_decision.adapter->ipv4_payload_offset();
        if (ipv4_payload_offset != packet.ipv4_payload_offset) {
            // FIXME: Add support for this. This can happen if after a route change
            // we ended up on another adapter which doesn't have the same layer 2 type
            // like the previous adapter.
            VERIFY_NOT_REACHED();
        }
        routing_decision.adapter->fill_in_ipv4_header(*packet.buffer,
            local_address(), routing_decision.next_hop, peer_address(),
            IPv4Protocol::TCP, packet.buffer->buffer.size() - ipv4_payload_offset, ttl());
        routing_decision.adapter->send_packet({ packet.buffer->buffer.data(), packet.buffer->buffer.size() });
        m_packets_out++;
        m_bytes_out += packet.buffer->buffer.size();
    }
}

bool TCPSocket::can_write(const FileDescription& file_description, size_t size) const
{
    if (!IPv4Socket::can_write(file_description, size))
        return false;

    if (!file_description.is_blocking())
        return true;

    Locker lock(m_not_acked_lock);
    return m_not_acked_size + size <= m_send_window_size;
}

}
