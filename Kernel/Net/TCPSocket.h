/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <AK/HashMap.h>
#include <AK/SinglyLinkedList.h>
#include <AK/WeakPtr.h>
#include <Kernel/KResult.h>
#include <Kernel/Net/IPv4Socket.h>

namespace Kernel {

class TCPSocket final : public IPv4Socket {
public:
    static void for_each(Function<void(const TCPSocket&)>);
    static KResultOr<NonnullRefPtr<TCPSocket>> create(int protocol);
    virtual ~TCPSocket() override;

    enum class Direction {
        Unspecified,
        Outgoing,
        Incoming,
        Passive,
    };

    static const char* to_string(Direction direction)
    {
        switch (direction) {
        case Direction::Unspecified:
            return "Unspecified";
        case Direction::Outgoing:
            return "Outgoing";
        case Direction::Incoming:
            return "Incoming";
        case Direction::Passive:
            return "Passive";
        default:
            return "None";
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

    static const char* to_string(State state)
    {
        switch (state) {
        case State::Closed:
            return "Closed";
        case State::Listen:
            return "Listen";
        case State::SynSent:
            return "SynSent";
        case State::SynReceived:
            return "SynReceived";
        case State::Established:
            return "Established";
        case State::CloseWait:
            return "CloseWait";
        case State::LastAck:
            return "LastAck";
        case State::FinWait1:
            return "FinWait1";
        case State::FinWait2:
            return "FinWait2";
        case State::Closing:
            return "Closing";
        case State::TimeWait:
            return "TimeWait";
        default:
            return "None";
        }
    }

    enum class Error {
        None,
        FINDuringConnect,
        RSTDuringConnect,
        UnexpectedFlagsDuringConnect,
        RetransmitTimeout,
    };

    static const char* to_string(Error error)
    {
        switch (error) {
        case Error::None:
            return "None";
        case Error::FINDuringConnect:
            return "FINDuringConnect";
        case Error::RSTDuringConnect:
            return "RSTDuringConnect";
        case Error::UnexpectedFlagsDuringConnect:
            return "UnexpectedFlagsDuringConnect";
        default:
            return "Invalid";
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

    // FIXME: Make this configurable?
    static constexpr u32 maximum_duplicate_acks = 5;
    void set_duplicate_acks(u32 acks) { m_duplicate_acks = acks; }
    u32 duplicate_acks() const { return m_duplicate_acks; }

    KResult send_ack(bool allow_duplicate = false);
    KResult send_tcp_packet(u16 flags, const UserOrKernelBuffer* = nullptr, size_t = 0, RoutingDecision* = nullptr);
    void receive_tcp_packet(const TCPPacket&, u16 size);

    bool should_delay_next_ack() const;

    static Lockable<HashMap<IPv4SocketTuple, TCPSocket*>>& sockets_by_tuple();
    static RefPtr<TCPSocket> from_tuple(const IPv4SocketTuple& tuple);

    static Lockable<HashMap<IPv4SocketTuple, RefPtr<TCPSocket>>>& closing_sockets();

    RefPtr<TCPSocket> create_client(const IPv4Address& local_address, u16 local_port, const IPv4Address& peer_address, u16 peer_port);
    void set_originator(TCPSocket& originator) { m_originator = originator; }
    bool has_originator() { return !!m_originator; }
    void release_to_originator();
    void release_for_accept(RefPtr<TCPSocket>);

    static Lockable<HashTable<TCPSocket*>>& sockets_for_retransmit();
    void retransmit_packets();

    virtual KResult close() override;

    virtual bool can_write(const FileDescription&, size_t) const override;

protected:
    void set_direction(Direction direction) { m_direction = direction; }

private:
    explicit TCPSocket(int protocol);
    virtual const char* class_name() const override { return "TCPSocket"; }

    static NetworkOrdered<u16> compute_tcp_checksum(const IPv4Address& source, const IPv4Address& destination, const TCPPacket&, u16 payload_size);

    virtual void shut_down_for_writing() override;

    virtual KResultOr<size_t> protocol_receive(ReadonlyBytes raw_ipv4_packet, UserOrKernelBuffer& buffer, size_t buffer_size, int flags) override;
    virtual KResultOr<size_t> protocol_send(const UserOrKernelBuffer&, size_t) override;
    virtual KResult protocol_connect(FileDescription&, ShouldBlock) override;
    virtual KResultOr<u16> protocol_allocate_local_port() override;
    virtual bool protocol_is_disconnected() const override;
    virtual KResult protocol_bind() override;
    virtual KResult protocol_listen(bool did_allocate_port) override;

    void enqueue_for_retransmit();
    void dequeue_for_retransmit();

    WeakPtr<TCPSocket> m_originator;
    HashMap<IPv4SocketTuple, NonnullRefPtr<TCPSocket>> m_pending_release_for_accept;
    Direction m_direction { Direction::Unspecified };
    Error m_error { Error::None };
    RefPtr<NetworkAdapter> m_adapter;
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
        WeakPtr<NetworkAdapter> adapter;
        int tx_counter { 0 };
    };

    mutable Lock m_not_acked_lock { "TCPSocket unacked packets" };
    SinglyLinkedList<OutgoingPacket> m_not_acked;
    size_t m_not_acked_size { 0 };

    u32 m_duplicate_acks { 0 };

    u32 m_last_ack_number_sent { 0 };
    Time m_last_ack_sent_time;

    // FIXME: Make this configurable (sysctl)
    static constexpr u32 maximum_retransmits = 5;
    Time m_last_retransmit_time;
    u32 m_retransmit_attempts { 0 };

    // FIXME: Parse window size TCP option from the peer
    u32 m_send_window_size { 64 * KiB };
};

}
