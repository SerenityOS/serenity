/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <AK/Time.h>
#include <Kernel/Debug.h>
#include <Kernel/Devices/Generic/RandomDevice.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/Locking/MutexProtected.h>
#include <Kernel/Net/EthernetFrameHeader.h>
#include <Kernel/Net/IP/IP.h>
#include <Kernel/Net/IP/IPv4.h>
#include <Kernel/Net/NetworkAdapter.h>
#include <Kernel/Net/NetworkingManagement.h>
#include <Kernel/Net/Routing.h>
#include <Kernel/Net/TCP.h>
#include <Kernel/Net/TCPSocket.h>
#include <Kernel/Security/Random.h>
#include <Kernel/Tasks/Process.h>
#include <Kernel/Time/TimeManagement.h>

namespace Kernel {

void TCPSocket::for_each(Function<void(TCPSocket const&)> callback)
{
    sockets_by_tuple().for_each_shared([&](auto const& it) {
        callback(*it.value);
    });
}

ErrorOr<void> TCPSocket::try_for_each(Function<ErrorOr<void>(TCPSocket const&)> callback)
{
    return sockets_by_tuple().with_shared([&](auto const& sockets) -> ErrorOr<void> {
        for (auto& it : sockets)
            TRY(callback(*it.value));
        return {};
    });
}

bool TCPSocket::unref() const
{
    bool did_hit_zero = sockets_by_tuple().with_exclusive([&](auto& table) {
        if (deref_base())
            return false;
        table.remove(tuple());
        const_cast<TCPSocket&>(*this).revoke_weak_ptrs();
        return true;
    });
    if (did_hit_zero) {
        const_cast<TCPSocket&>(*this).will_be_destroyed();
        delete this;
    }
    return did_hit_zero;
}

void TCPSocket::set_state(State new_state)
{
    dbgln_if(TCP_SOCKET_DEBUG, "TCPSocket({}) state moving from {} to {}", this, to_string(m_state), to_string(new_state));

    auto was_disconnected = protocol_is_disconnected();
    auto previous_role = m_role;

    m_state = new_state;

    if (new_state == State::Established && m_direction == Direction::Outgoing) {
        set_role(Role::Connected);
        clear_so_error();
    }

    if (new_state == State::TimeWait) {
        // Once we hit TimeWait, we are only holding the socket in case there
        // are packets on the way which we wouldn't want a new socket to get hit
        // with, so there's no point in keeping the receive buffer around.
        drop_receive_buffer();

        auto deadline = TimeManagement::the().current_time(CLOCK_MONOTONIC_COARSE) + maximum_segment_lifetime;
        auto timer_was_added = TimerQueue::the().add_timer_without_id(*m_timer, CLOCK_MONOTONIC_COARSE, deadline, [&]() {
            dbgln_if(TCP_SOCKET_DEBUG, "TCPSocket({}) TimeWait timer elpased", this);
            if (m_state == State::TimeWait) {
                m_state = State::Closed;
                do_state_closed();
            }
        });

        if (!timer_was_added) [[unlikely]] {
            dbgln_if(TCP_SOCKET_DEBUG, "TCPSocket({}) TimeWait timer deadline is in the past", this);
            m_state = State::Closed;
            new_state = State::Closed;
        }
    }

    if (new_state == State::Closed)
        do_state_closed();

    if (previous_role != m_role || was_disconnected != protocol_is_disconnected())
        evaluate_block_conditions();
}

void TCPSocket::do_state_closed()
{
    if (m_originator)
        release_to_originator();

    closing_sockets().with_exclusive([&](auto& table) {
        table.remove(tuple());
    });
}

static Singleton<MutexProtected<HashMap<IPv4SocketTuple, RefPtr<TCPSocket>>>> s_socket_closing;

MutexProtected<HashMap<IPv4SocketTuple, RefPtr<TCPSocket>>>& TCPSocket::closing_sockets()
{
    return *s_socket_closing;
}

static Singleton<MutexProtected<HashMap<IPv4SocketTuple, TCPSocket*>>> s_socket_tuples;

MutexProtected<HashMap<IPv4SocketTuple, TCPSocket*>>& TCPSocket::sockets_by_tuple()
{
    return *s_socket_tuples;
}

RefPtr<TCPSocket> TCPSocket::from_tuple(IPv4SocketTuple const& tuple)
{
    return sockets_by_tuple().with_shared([&](auto const& table) -> RefPtr<TCPSocket> {
        auto exact_match = table.get(tuple);
        if (exact_match.has_value())
            return { *exact_match.value() };

        auto address_tuple = IPv4SocketTuple(tuple.local_address(), tuple.local_port(), IPv4Address(), 0);
        auto address_match = table.get(address_tuple);
        if (address_match.has_value())
            return { *address_match.value() };

        auto wildcard_tuple = IPv4SocketTuple(IPv4Address(), tuple.local_port(), IPv4Address(), 0);
        auto wildcard_match = table.get(wildcard_tuple);
        if (wildcard_match.has_value())
            return { *wildcard_match.value() };

        return {};
    });
}
ErrorOr<NonnullRefPtr<TCPSocket>> TCPSocket::try_create_client(IPv4Address const& new_local_address, u16 new_local_port, IPv4Address const& new_peer_address, u16 new_peer_port)
{
    auto tuple = IPv4SocketTuple(new_local_address, new_local_port, new_peer_address, new_peer_port);
    return sockets_by_tuple().with_exclusive([&](auto& table) -> ErrorOr<NonnullRefPtr<TCPSocket>> {
        if (table.contains(tuple))
            return EEXIST;

        auto receive_buffer = TRY(try_create_receive_buffer());
        auto client = TRY(TCPSocket::try_create(protocol(), move(receive_buffer)));

        client->set_setup_state(SetupState::InProgress);
        client->set_local_address(new_local_address);
        client->set_local_port(new_local_port);
        client->set_peer_address(new_peer_address);
        client->set_peer_port(new_peer_port);
        client->set_bound();
        client->set_direction(Direction::Incoming);
        client->set_originator(*this);

        m_pending_release_for_accept.set(tuple, client);
        client->m_registered_socket_tuple = tuple;
        table.set(tuple, client);

        return { move(client) };
    });
}

void TCPSocket::release_to_originator()
{
    VERIFY(!!m_originator);
    m_originator.strong_ref()->release_for_accept(*this);
    m_originator.clear();
}

void TCPSocket::release_for_accept(NonnullRefPtr<TCPSocket> socket)
{
    VERIFY(m_pending_release_for_accept.contains(socket->tuple()));
    m_pending_release_for_accept.remove(socket->tuple());
    // FIXME: Should we observe this error somehow?
    [[maybe_unused]] auto rc = queue_connection_from(move(socket));
}

TCPSocket::TCPSocket(int protocol, NonnullOwnPtr<DoubleBuffer> receive_buffer, NonnullOwnPtr<KBuffer> scratch_buffer, NonnullRefPtr<Timer> timer)
    : IPv4Socket(SOCK_STREAM, protocol, move(receive_buffer), move(scratch_buffer))
    , m_last_ack_sent_time(TimeManagement::the().monotonic_time())
    , m_last_retransmit_time(TimeManagement::the().monotonic_time())
    , m_timer(timer)
{
}

TCPSocket::~TCPSocket()
{
    dequeue_for_retransmit();

    dbgln_if(TCP_SOCKET_DEBUG, "~TCPSocket in state {}", to_string(state()));
}

ErrorOr<NonnullRefPtr<TCPSocket>> TCPSocket::try_create(int protocol, NonnullOwnPtr<DoubleBuffer> receive_buffer)
{
    // Note: Scratch buffer is only used for SOCK_STREAM sockets.
    auto scratch_buffer = TRY(KBuffer::try_create_with_size("TCPSocket: Scratch buffer"sv, 65536));
    auto timer = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) Timer));
    return adopt_nonnull_ref_or_enomem(new (nothrow) TCPSocket(protocol, move(receive_buffer), move(scratch_buffer), timer));
}

ErrorOr<size_t> TCPSocket::protocol_size(ReadonlyBytes raw_ipv4_packet)
{
    auto& ipv4_packet = *reinterpret_cast<IPv4Packet const*>(raw_ipv4_packet.data());
    auto& tcp_packet = *static_cast<TCPPacket const*>(ipv4_packet.payload());
    return raw_ipv4_packet.size() - sizeof(IPv4Packet) - tcp_packet.header_size();
}

ErrorOr<size_t> TCPSocket::protocol_receive(ReadonlyBytes raw_ipv4_packet, UserOrKernelBuffer& buffer, size_t buffer_size, [[maybe_unused]] int flags)
{
    auto& ipv4_packet = *reinterpret_cast<IPv4Packet const*>(raw_ipv4_packet.data());
    auto& tcp_packet = *static_cast<TCPPacket const*>(ipv4_packet.payload());
    size_t payload_size = raw_ipv4_packet.size() - sizeof(IPv4Packet) - tcp_packet.header_size();
    dbgln_if(TCP_SOCKET_DEBUG, "payload_size {}, will it fit in {}?", payload_size, buffer_size);
    VERIFY(buffer_size >= payload_size);
    SOCKET_TRY(buffer.write(tcp_packet.payload(), payload_size));
    return payload_size;
}

ErrorOr<size_t> TCPSocket::protocol_send(UserOrKernelBuffer const& data, size_t data_length)
{
    auto adapter = bound_interface().with([](auto& bound_device) -> RefPtr<NetworkAdapter> { return bound_device; });
    RoutingDecision routing_decision = route_to(peer_address(), local_address(), adapter);
    if (routing_decision.is_zero())
        return set_so_error(EHOSTUNREACH);
    size_t mss = routing_decision.adapter->mtu() - sizeof(IPv4Packet) - sizeof(TCPPacket);

    if (!m_no_delay) {
        // RFC 896 (Nagle’s algorithm): https://www.ietf.org/rfc/rfc0896
        // "The solution is to inhibit the sending of new TCP  segments when
        //  new  outgoing  data  arrives  from  the  user  if  any previously
        //  transmitted data on the connection remains unacknowledged.   This
        //  inhibition  is  to be unconditional; no timers, tests for size of
        //  data received, or other conditions are required."
        auto has_unacked_data = m_unacked_packets.with_shared([&](auto const& packets) { return packets.size > 0; });
        if (has_unacked_data && data_length < mss)
            return set_so_error(EAGAIN);
    }

    data_length = min(data_length, mss);
    TRY(send_tcp_packet(TCPFlags::PSH | TCPFlags::ACK, &data, data_length, &routing_decision));
    return data_length;
}

ErrorOr<void> TCPSocket::send_ack(bool allow_duplicate)
{
    if (!allow_duplicate && m_last_ack_number_sent == m_ack_number)
        return {};
    return send_tcp_packet(TCPFlags::ACK);
}

ErrorOr<void> TCPSocket::send_tcp_packet(u16 flags, UserOrKernelBuffer const* payload, size_t payload_size, RoutingDecision* user_routing_decision)
{
    auto adapter = bound_interface().with([](auto& bound_device) -> RefPtr<NetworkAdapter> { return bound_device; });
    RoutingDecision routing_decision = user_routing_decision ? *user_routing_decision : route_to(peer_address(), local_address(), adapter);
    if (routing_decision.is_zero())
        return set_so_error(EHOSTUNREACH);

    auto ipv4_payload_offset = routing_decision.adapter->ipv4_payload_offset();

    bool const has_mss_option = flags & TCPFlags::SYN;
    bool const has_window_scale_option = flags & TCPFlags::SYN;
    size_t const options_size = (has_mss_option ? sizeof(TCPOptionMSS) : 0) + (has_window_scale_option ? sizeof(TCPOptionWindowScale) : 0);
    size_t const tcp_header_size = sizeof(TCPPacket) + align_up_to(options_size, 4);
    size_t const buffer_size = ipv4_payload_offset + tcp_header_size + payload_size;
    auto packet = routing_decision.adapter->acquire_packet_buffer(buffer_size);
    if (!packet)
        return set_so_error(ENOMEM);
    routing_decision.adapter->fill_in_ipv4_header(*packet, local_address(),
        routing_decision.next_hop, peer_address(), TransportProtocol::TCP,
        buffer_size - ipv4_payload_offset, type_of_service(), ttl());
    memset(packet->buffer->data() + ipv4_payload_offset, 0, sizeof(TCPPacket));
    auto& tcp_packet = *(TCPPacket*)(packet->buffer->data() + ipv4_payload_offset);
    VERIFY(local_port());
    tcp_packet.set_source_port(local_port());
    tcp_packet.set_destination_port(peer_port());
    auto window_size = available_space_in_receive_buffer();
    if ((flags & TCPFlags::SYN) == 0 && m_window_scaling_supported)
        window_size >>= receive_window_scale();
    tcp_packet.set_window_size(min(window_size, NumericLimits<u16>::max()));
    tcp_packet.set_sequence_number(m_sequence_number);
    tcp_packet.set_data_offset(tcp_header_size / sizeof(u32));
    tcp_packet.set_flags(flags);

    if (payload) {
        if (auto result = payload->read(tcp_packet.payload(), payload_size); result.is_error()) {
            routing_decision.adapter->release_packet_buffer(*packet);
            return set_so_error(result.release_error());
        }
    }

    if (flags & TCPFlags::ACK) {
        m_last_ack_number_sent = m_ack_number;
        m_last_ack_sent_time = TimeManagement::the().monotonic_time();
        tcp_packet.set_ack_number(m_ack_number);
    }

    if (flags & TCPFlags::SYN) {
        ++m_sequence_number;
    } else {
        m_sequence_number += payload_size;
    }

    u8* next_option = packet->buffer->data() + ipv4_payload_offset + sizeof(TCPPacket);
    if (has_mss_option) {
        u16 mss = routing_decision.adapter->mtu() - sizeof(IPv4Packet) - sizeof(TCPPacket);
        TCPOptionMSS mss_option { mss };
        memcpy(next_option, &mss_option, sizeof(mss_option));
        next_option += sizeof(mss_option);
    }
    if (has_window_scale_option) {
        TCPOptionWindowScale window_scale_option { receive_window_scale() };
        memcpy(next_option, &window_scale_option, sizeof(window_scale_option));
        next_option += sizeof(window_scale_option);
    }
    if ((options_size % 4) != 0)
        *next_option = to_underlying(TCPOptionKind::End);

    tcp_packet.set_checksum(compute_tcp_checksum(local_address(), peer_address(), tcp_packet, payload_size));

    bool expect_ack { tcp_packet.has_syn() || payload_size > 0 };
    if (expect_ack) {
        bool append_failed { false };
        m_unacked_packets.with_exclusive([&](auto& unacked_packets) {
            auto result = unacked_packets.packets.try_append({ m_sequence_number, packet, ipv4_payload_offset, *routing_decision.adapter });
            if (result.is_error()) {
                dbgln("TCPSocket: Dropped outbound packet because try_append() failed");
                append_failed = true;
                return;
            }
            unacked_packets.size += payload_size;
            enqueue_for_retransmit();
        });
        if (append_failed)
            return set_so_error(ENOMEM);
    }

    m_packets_out++;
    m_bytes_out += buffer_size;
    routing_decision.adapter->send_packet(packet->bytes());
    if (!expect_ack)
        routing_decision.adapter->release_packet_buffer(*packet);

    return {};
}

void TCPSocket::receive_tcp_packet(TCPPacket const& packet, u16 size)
{
    if (packet.has_ack()) {
        u32 ack_number = packet.ack_number();

        dbgln_if(TCP_SOCKET_DEBUG, "TCPSocket: receive_tcp_packet: {}", ack_number);

        int removed = 0;
        m_unacked_packets.with_exclusive([&](auto& unacked_packets) {
            while (!unacked_packets.packets.is_empty()) {
                auto& packet = unacked_packets.packets.first();

                dbgln_if(TCP_SOCKET_DEBUG, "TCPSocket: iterate: {}", packet.ack_number);

                if (packet.ack_number <= ack_number) {
                    auto old_adapter = packet.adapter.strong_ref();
                    if (old_adapter)
                        old_adapter->release_packet_buffer(*packet.buffer);
                    TCPPacket& tcp_packet = *(TCPPacket*)(packet.buffer->buffer->data() + packet.ipv4_payload_offset);
                    if (m_send_window_size != tcp_packet.window_size()) {
                        m_send_window_size = tcp_packet.window_size() << m_send_window_scale;
                    }
                    auto payload_size = packet.buffer->buffer->data() + packet.buffer->buffer->size() - (u8*)tcp_packet.payload();
                    unacked_packets.size -= payload_size;
                    evaluate_block_conditions();
                    unacked_packets.packets.take_first();
                    removed++;
                } else {
                    break;
                }
            }

            if (unacked_packets.packets.is_empty()) {
                m_retransmit_attempts = 0;
                dequeue_for_retransmit();
            }

            dbgln_if(TCP_SOCKET_DEBUG, "TCPSocket: receive_tcp_packet acknowledged {} packets", removed);
        });
    }

    m_packets_in++;
    m_bytes_in += packet.header_size() + size;
}

bool TCPSocket::should_delay_next_ack() const
{
    // FIXME: We don't know the MSS here so make a reasonable guess.
    size_t const mss = 1500;

    // RFC 1122 says we should send an ACK for every two full-sized segments.
    if (m_ack_number >= m_last_ack_number_sent + 2 * mss)
        return false;

    // RFC 1122 says we should not delay ACKs for more than 500 milliseconds.
    if (TimeManagement::the().monotonic_time(TimePrecision::Precise) >= m_last_ack_sent_time + Duration::from_milliseconds(500))
        return false;

    return true;
}

NetworkOrdered<u16> TCPSocket::compute_tcp_checksum(IPv4Address const& source, IPv4Address const& destination, TCPPacket const& packet, u16 payload_size)
{
    union PseudoHeader {
        struct [[gnu::packed]] {
            IPv4Address source;
            IPv4Address destination;
            u8 zero;
            u8 protocol;
            NetworkOrdered<u16> payload_size;
        } header;
        u16 raw[6];
    };
    static_assert(sizeof(PseudoHeader) == 12);

    Checked<u16> packet_size = packet.header_size();
    packet_size += payload_size;
    VERIFY(!packet_size.has_overflow());

    PseudoHeader pseudo_header { .header = { source, destination, 0, (u8)TransportProtocol::TCP, packet_size.value() } };

    u32 checksum = 0;
    auto* raw_pseudo_header = pseudo_header.raw;
    for (size_t i = 0; i < sizeof(pseudo_header) / sizeof(u16); ++i) {
        checksum += AK::convert_between_host_and_network_endian(raw_pseudo_header[i]);
        if (checksum > 0xffff)
            checksum = (checksum >> 16) + (checksum & 0xffff);
    }
    auto* raw_packet = bit_cast<u16*>(&packet);
    for (size_t i = 0; i < packet.header_size() / sizeof(u16); ++i) {
        checksum += AK::convert_between_host_and_network_endian(raw_packet[i]);
        if (checksum > 0xffff)
            checksum = (checksum >> 16) + (checksum & 0xffff);
    }
    VERIFY(packet.data_offset() * 4 == packet.header_size());
    auto* raw_payload = bit_cast<u16*>(packet.payload());
    for (size_t i = 0; i < payload_size / sizeof(u16); ++i) {
        checksum += AK::convert_between_host_and_network_endian(raw_payload[i]);
        if (checksum > 0xffff)
            checksum = (checksum >> 16) + (checksum & 0xffff);
    }
    if (payload_size & 1) {
        u16 expanded_byte = ((u8 const*)packet.payload())[payload_size - 1] << 8;
        checksum += expanded_byte;
        if (checksum > 0xffff)
            checksum = (checksum >> 16) + (checksum & 0xffff);
    }
    return ~(checksum & 0xffff);
}

ErrorOr<void> TCPSocket::setsockopt(int level, int option, Userspace<void const*> user_value, socklen_t user_value_size)
{
    if (level != IPPROTO_TCP)
        return IPv4Socket::setsockopt(level, option, user_value, user_value_size);

    MutexLocker locker(mutex());

    switch (option) {
    case TCP_NODELAY:
        if (user_value_size < sizeof(int))
            return EINVAL;
        int value;
        TRY(copy_from_user(&value, static_ptr_cast<int const*>(user_value)));
        if (value != 0 && value != 1)
            return EINVAL;
        m_no_delay = value;
        return {};
    default:
        dbgln("setsockopt({}) at IPPROTO_TCP not implemented.", option);
        return ENOPROTOOPT;
    }
}

ErrorOr<void> TCPSocket::getsockopt(OpenFileDescription& description, int level, int option, Userspace<void*> value, Userspace<socklen_t*> value_size)
{
    if (level != IPPROTO_TCP)
        return IPv4Socket::getsockopt(description, level, option, value, value_size);

    MutexLocker locker(mutex());

    socklen_t size;
    TRY(copy_from_user(&size, value_size.unsafe_userspace_ptr()));

    switch (option) {
    case TCP_NODELAY: {
        int nodelay = m_no_delay ? 1 : 0;
        if (size < sizeof(nodelay))
            return EINVAL;
        TRY(copy_to_user(static_ptr_cast<int*>(value), &nodelay));
        size = sizeof(nodelay);
        return copy_to_user(value_size, &size);
    }
    default:
        dbgln("getsockopt({}) at IPPROTO_TCP not implemented.", option);
        return ENOPROTOOPT;
    }
}

ErrorOr<void> TCPSocket::protocol_bind()
{
    dbgln_if(TCP_SOCKET_DEBUG, "TCPSocket::protocol_bind(), local_port() is {}", local_port());
    // Check that we do have the address we're trying to bind to.
    TRY(m_adapter.with([this](auto& adapter) -> ErrorOr<void> {
        if (has_specific_local_address() && !adapter) {
            adapter = NetworkingManagement::the().from_ipv4_address(local_address());
            if (!adapter)
                return set_so_error(EADDRNOTAVAIL);
        }
        return {};
    }));

    if (local_port() == 0) {
        // Allocate an unused ephemeral port.
        constexpr u16 first_ephemeral_port = 32768;
        constexpr u16 last_ephemeral_port = 60999;
        constexpr u16 ephemeral_port_range_size = last_ephemeral_port - first_ephemeral_port;
        u16 first_scan_port = first_ephemeral_port + get_good_random<u16>() % ephemeral_port_range_size;

        return sockets_by_tuple().with_exclusive([&](auto& table) -> ErrorOr<void> {
            u16 port = first_scan_port;
            while (true) {
                IPv4SocketTuple proposed_tuple(local_address(), port, peer_address(), peer_port());

                auto it = table.find(proposed_tuple);
                if (it == table.end()) {
                    set_local_port(port);
                    m_registered_socket_tuple = proposed_tuple;
                    table.set(proposed_tuple, this);
                    dbgln_if(TCP_SOCKET_DEBUG, "...allocated port {}, tuple {}", port, proposed_tuple.to_string());
                    return {};
                }
                ++port;
                if (port > last_ephemeral_port)
                    port = first_ephemeral_port;
                if (port == first_scan_port)
                    break;
            }
            return set_so_error(EADDRINUSE);
        });
    } else {
        // Verify that the user-supplied port is not already used by someone else.
        bool ok = sockets_by_tuple().with_exclusive([&](auto& table) -> bool {
            if (table.contains(tuple()))
                return false;
            auto socket_tuple = tuple();
            m_registered_socket_tuple = socket_tuple;
            table.set(socket_tuple, this);
            return true;
        });
        if (!ok)
            return set_so_error(EADDRINUSE);
        return {};
    }
}

ErrorOr<void> TCPSocket::protocol_listen()
{
    set_direction(Direction::Passive);
    set_state(State::Listen);
    set_setup_state(SetupState::Completed);
    return {};
}

ErrorOr<void> TCPSocket::protocol_connect(OpenFileDescription& description)
{
    MutexLocker locker(mutex());

    auto routing_decision = route_to(peer_address(), local_address());
    if (routing_decision.is_zero())
        return set_so_error(EHOSTUNREACH);
    if (!has_specific_local_address())
        set_local_address(routing_decision.adapter->ipv4_address());

    TRY(ensure_bound());
    if (m_registered_socket_tuple.has_value() && m_registered_socket_tuple != tuple()) {
        // If the socket was manually bound (using bind(2)) instead of implicitly using connect,
        // it will already be registered in the TCPSocket sockets_by_tuple table, under the previous
        // socket tuple. We replace the entry in the table to ensure it is also properly removed on
        // socket deletion, to prevent a dangling reference.
        TRY(sockets_by_tuple().with_exclusive([this](auto& table) -> ErrorOr<void> {
            auto removed = table.remove(*m_registered_socket_tuple);
            VERIFY(removed);
            if (table.contains(tuple()))
                return set_so_error(EADDRINUSE);
            table.set(tuple(), this);
            return {};
        }));
        m_registered_socket_tuple = tuple();
    }

    m_sequence_number = get_good_random<u32>();
    m_ack_number = 0;

    set_setup_state(SetupState::InProgress);
    TRY(send_tcp_packet(TCPFlags::SYN));
    m_state = State::SynSent;
    set_role(Role::Connecting);
    m_direction = Direction::Outgoing;

    evaluate_block_conditions();

    if (description.is_blocking()) {
        locker.unlock();
        auto unblock_flags = Thread::FileBlocker::BlockFlags::None;
        if (Thread::current()->block<Thread::ConnectBlocker>({}, description, unblock_flags).was_interrupted())
            return set_so_error(EINTR);
        locker.lock();
        VERIFY(setup_state() == SetupState::Completed);
        if (has_error()) { // TODO: check unblock_flags
            set_role(Role::None);
            if (error() == TCPSocket::Error::RetransmitTimeout)
                return set_so_error(ETIMEDOUT);
            else
                return set_so_error(ECONNREFUSED);
        }
        return {};
    }

    return set_so_error(EINPROGRESS);
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
        dbgln_if(TCP_SOCKET_DEBUG, " Sending FIN from Established and moving into FinWait1");
        (void)send_tcp_packet(TCPFlags::FIN | TCPFlags::ACK);
        set_state(State::FinWait1);
    } else {
        dbgln(" Shutting down TCPSocket for writing but not moving to FinWait1 since state is {}", to_string(state()));
    }
}

ErrorOr<void> TCPSocket::close()
{
    MutexLocker locker(mutex());
    auto result = IPv4Socket::close();
    if (state() == State::CloseWait) {
        dbgln_if(TCP_SOCKET_DEBUG, " Sending FIN from CloseWait and moving into LastAck");
        [[maybe_unused]] auto rc = send_tcp_packet(TCPFlags::FIN | TCPFlags::ACK);
        set_state(State::LastAck);
    }

    if (state() != State::Closed && state() != State::Listen)
        closing_sockets().with_exclusive([&](auto& table) {
            table.set(tuple(), *this);
        });
    return result;
}

static Singleton<MutexProtected<TCPSocket::RetransmitList>> s_sockets_for_retransmit;

MutexProtected<TCPSocket::RetransmitList>& TCPSocket::sockets_for_retransmit()
{
    return *s_sockets_for_retransmit;
}

void TCPSocket::enqueue_for_retransmit()
{
    sockets_for_retransmit().with_exclusive([&](auto& list) {
        list.append(*this);
    });
}

void TCPSocket::dequeue_for_retransmit()
{
    sockets_for_retransmit().with_exclusive([&](auto& list) {
        list.remove(*this);
    });
}

void TCPSocket::retransmit_packets()
{
    auto now = TimeManagement::the().monotonic_time();

    // RFC6298 says we should have at least one second between retransmits. According to
    // RFC1122 we must do exponential backoff - even for SYN packets.
    i64 retransmit_interval = 1;
    for (decltype(m_retransmit_attempts) i = 0; i < m_retransmit_attempts; i++)
        retransmit_interval *= 2;

    if (m_last_retransmit_time > now - Duration::from_seconds(retransmit_interval))
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

    auto adapter = bound_interface().with([](auto& bound_device) -> RefPtr<NetworkAdapter> { return bound_device; });
    auto routing_decision = route_to(peer_address(), local_address(), adapter);
    if (routing_decision.is_zero())
        return;

    m_unacked_packets.with_exclusive([&](auto& unacked_packets) {
        for (auto& packet : unacked_packets.packets) {
            packet.tx_counter++;

            if constexpr (TCP_SOCKET_DEBUG) {
                auto& tcp_packet = *(TCPPacket const*)(packet.buffer->buffer->data() + packet.ipv4_payload_offset);
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

            auto packet_buffer = packet.buffer->bytes();

            routing_decision.adapter->fill_in_ipv4_header(*packet.buffer,
                local_address(), routing_decision.next_hop, peer_address(),
                TransportProtocol::TCP, packet_buffer.size() - ipv4_payload_offset, type_of_service(), ttl());
            routing_decision.adapter->send_packet(packet_buffer);
            m_packets_out++;
            m_bytes_out += packet_buffer.size();
        }
    });
}

bool TCPSocket::can_write(OpenFileDescription const& file_description, u64 size) const
{
    if (!IPv4Socket::can_write(file_description, size))
        return false;

    if (m_state == State::SynSent || m_state == State::SynReceived)
        return false;

    if (!file_description.is_blocking())
        return true;

    return m_unacked_packets.with_shared([&](auto& unacked_packets) {
        return unacked_packets.size + size <= m_send_window_size;
    });
}
}
