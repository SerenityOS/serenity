/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/Function.h>
#include <AK/HashMap.h>
#include <AK/IntegralMath.h>
#include <AK/SinglyLinkedList.h>
#include <AK/Time.h>
#include <Kernel/Library/LockWeakPtr.h>
#include <Kernel/Locking/MutexProtected.h>
#include <Kernel/Net/IP/Socket.h>
#include <Kernel/Time/TimerQueue.h>

namespace Kernel {

class TCPSocket final : public IPv4Socket {
public:
    static void for_each(Function<void(TCPSocket const&)>);
    static ErrorOr<void> try_for_each(Function<ErrorOr<void>(TCPSocket const&)>);
    static ErrorOr<NonnullRefPtr<TCPSocket>> try_create(int protocol, NonnullOwnPtr<DoubleBuffer> receive_buffer);
    virtual ~TCPSocket() override;

    virtual bool unref() const override;

    enum class Direction {
        Unspecified,
        Outgoing,
        Incoming,
        Passive,
    };

    static StringView to_string(Direction direction)
    {
        switch (direction) {
        case Direction::Unspecified:
            return "Unspecified"sv;
        case Direction::Outgoing:
            return "Outgoing"sv;
        case Direction::Incoming:
            return "Incoming"sv;
        case Direction::Passive:
            return "Passive"sv;
        default:
            return "None"sv;
        }
    }

    enum class State {
        Closed,
        Listen,
        SynSent,
        SynReceived,
        Established,
        CloseWait,
        LastAck,
        FinWait1,
        FinWait2,
        Closing,
        TimeWait,
    };

    static StringView to_string(State state)
    {
        switch (state) {
        case State::Closed:
            return "Closed"sv;
        case State::Listen:
            return "Listen"sv;
        case State::SynSent:
            return "SynSent"sv;
        case State::SynReceived:
            return "SynReceived"sv;
        case State::Established:
            return "Established"sv;
        case State::CloseWait:
            return "CloseWait"sv;
        case State::LastAck:
            return "LastAck"sv;
        case State::FinWait1:
            return "FinWait1"sv;
        case State::FinWait2:
            return "FinWait2"sv;
        case State::Closing:
            return "Closing"sv;
        case State::TimeWait:
            return "TimeWait"sv;
        default:
            return "None"sv;
        }
    }

    enum class Error {
        None,
        FINDuringConnect,
        RSTDuringConnect,
        UnexpectedFlagsDuringConnect,
        RetransmitTimeout,
    };

    static StringView to_string(Error error)
    {
        switch (error) {
        case Error::None:
            return "None"sv;
        case Error::FINDuringConnect:
            return "FINDuringConnect"sv;
        case Error::RSTDuringConnect:
            return "RSTDuringConnect"sv;
        case Error::UnexpectedFlagsDuringConnect:
            return "UnexpectedFlagsDuringConnect"sv;
        default:
            return "Invalid"sv;
        }
    }

    State state() const { return m_state; }
    void set_state(State state);

    Direction direction() const { return m_direction; }

    bool has_error() const { return m_error != Error::None; }
    Error error() const { return m_error; }
    void set_error(Error error) { m_error = error; }

    void set_ack_number(u32 n) { m_ack_number = n; }
    void set_sequence_number(u32 n) { m_sequence_number = n; }
    u32 ack_number() const { return m_ack_number; }
    u32 sequence_number() const { return m_sequence_number; }
    u32 packets_in() const { return m_packets_in; }
    u32 bytes_in() const { return m_bytes_in; }
    u32 packets_out() const { return m_packets_out; }
    u32 bytes_out() const { return m_bytes_out; }

    void set_send_window_scale(size_t scale)
    {
        m_window_scaling_supported = true;
        m_send_window_scale = scale;
    }

    // FIXME: Make this configurable?
    static constexpr u32 maximum_duplicate_acks = 5;
    void set_duplicate_acks(u32 acks) { m_duplicate_acks = acks; }
    u32 duplicate_acks() const { return m_duplicate_acks; }

    ErrorOr<void> send_ack(bool allow_duplicate = false);
    ErrorOr<void> send_tcp_packet(u16 flags, UserOrKernelBuffer const* = nullptr, size_t = 0, RoutingDecision* = nullptr);
    void receive_tcp_packet(TCPPacket const&, u16 size);

    bool should_delay_next_ack() const;

    static MutexProtected<HashMap<IPv4SocketTuple, TCPSocket*>>& sockets_by_tuple();
    static RefPtr<TCPSocket> from_tuple(IPv4SocketTuple const& tuple);

    static MutexProtected<HashMap<IPv4SocketTuple, RefPtr<TCPSocket>>>& closing_sockets();

    ErrorOr<NonnullRefPtr<TCPSocket>> try_create_client(IPv4Address const& local_address, u16 local_port, IPv4Address const& peer_address, u16 peer_port);
    void set_originator(TCPSocket& originator) { m_originator = originator; }
    bool has_originator() { return !!m_originator; }
    void release_to_originator();
    void release_for_accept(NonnullRefPtr<TCPSocket>);

    void retransmit_packets();

    virtual ErrorOr<void> close() override;

    virtual bool can_write(OpenFileDescription const&, u64) const override;

    static NetworkOrdered<u16> compute_tcp_checksum(IPv4Address const& source, IPv4Address const& destination, TCPPacket const&, u16 payload_size);

    virtual ErrorOr<void> setsockopt(int level, int option, Userspace<void const*>, socklen_t) override;
    virtual ErrorOr<void> getsockopt(OpenFileDescription&, int level, int option, Userspace<void*>, Userspace<socklen_t*>) override;

protected:
    void set_direction(Direction direction) { m_direction = direction; }

private:
    explicit TCPSocket(int protocol, NonnullOwnPtr<DoubleBuffer> receive_buffer, NonnullOwnPtr<KBuffer> scratch_buffer, NonnullRefPtr<Timer> timer);
    virtual StringView class_name() const override { return "TCPSocket"sv; }

    virtual void shut_down_for_writing() override;

    virtual ErrorOr<size_t> protocol_receive(ReadonlyBytes raw_ipv4_packet, UserOrKernelBuffer& buffer, size_t buffer_size, int flags) override;
    virtual ErrorOr<size_t> protocol_send(UserOrKernelBuffer const&, size_t) override;
    virtual ErrorOr<void> protocol_connect(OpenFileDescription&) override;
    virtual ErrorOr<size_t> protocol_size(ReadonlyBytes raw_ipv4_packet) override;
    virtual bool protocol_is_disconnected() const override;
    virtual ErrorOr<void> protocol_bind() override;
    virtual ErrorOr<void> protocol_listen() override;

    void do_state_closed();

    void enqueue_for_retransmit();
    void dequeue_for_retransmit();

    static constexpr size_t receive_window_scale()
    {
        auto buffer_size_bit_length = AK::log2(receive_buffer_size) + 1;
        if (buffer_size_bit_length < 16)
            return 0;
        return buffer_size_bit_length - 16;
    }

    LockWeakPtr<TCPSocket> m_originator;
    HashMap<IPv4SocketTuple, NonnullRefPtr<TCPSocket>> m_pending_release_for_accept;
    Direction m_direction { Direction::Unspecified };
    Error m_error { Error::None };
    SpinlockProtected<RefPtr<NetworkAdapter>, LockRank::None> m_adapter;
    u32 m_sequence_number { 0 };
    u32 m_ack_number { 0 };
    State m_state { State::Closed };
    u32 m_packets_in { 0 };
    u32 m_bytes_in { 0 };
    u32 m_packets_out { 0 };
    u32 m_bytes_out { 0 };

    struct OutgoingPacket {
        u32 ack_number { 0 };
        RefPtr<PacketWithTimestamp> buffer;
        size_t ipv4_payload_offset;
        LockWeakPtr<NetworkAdapter> adapter;
        int tx_counter { 0 };
    };

    struct UnackedPackets {
        SinglyLinkedList<OutgoingPacket> packets;
        size_t size { 0 };
    };

    MutexProtected<UnackedPackets> m_unacked_packets;

    u32 m_duplicate_acks { 0 };

    u32 m_last_ack_number_sent { 0 };
    MonotonicTime m_last_ack_sent_time;

    static constexpr Duration maximum_segment_lifetime = Duration::from_seconds(120);

    // FIXME: Make this configurable (sysctl)
    static constexpr u32 maximum_retransmits = 5;
    MonotonicTime m_last_retransmit_time;
    u32 m_retransmit_attempts { 0 };

    // Default to maximum window size. receive_tcp_packet() will update from the
    // peer's advertised window size.
    u32 m_send_window_size { 64 * KiB };
    bool m_window_scaling_supported { false };
    size_t m_send_window_scale { 0 };

    bool m_no_delay { false };

    IntrusiveListNode<TCPSocket> m_retransmit_list_node;

    Optional<IPv4SocketTuple> m_registered_socket_tuple;

    NonnullRefPtr<Timer> m_timer;

public:
    using RetransmitList = IntrusiveList<&TCPSocket::m_retransmit_list_node>;
    static MutexProtected<TCPSocket::RetransmitList>& sockets_for_retransmit();
};

}
