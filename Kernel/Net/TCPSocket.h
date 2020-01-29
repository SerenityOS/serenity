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

#pragma once

#include <AK/Function.h>
#include <AK/HashMap.h>
#include <AK/SinglyLinkedList.h>
#include <AK/WeakPtr.h>
#include <Kernel/Net/IPv4Socket.h>

class TCPSocket final : public IPv4Socket
    , public Weakable<TCPSocket> {
public:
    static void for_each(Function<void(TCPSocket&)>);
    static NonnullRefPtr<TCPSocket> create(int protocol);
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

    void send_tcp_packet(u16 flags, const void* = nullptr, size_t = 0);
    void send_outgoing_packets();
    void receive_tcp_packet(const TCPPacket&, u16 size);

    static Lockable<HashMap<IPv4SocketTuple, TCPSocket*>>& sockets_by_tuple();
    static RefPtr<TCPSocket> from_tuple(const IPv4SocketTuple& tuple);
    static RefPtr<TCPSocket> from_endpoints(const IPv4Address& local_address, u16 local_port, const IPv4Address& peer_address, u16 peer_port);

    RefPtr<TCPSocket> create_client(const IPv4Address& local_address, u16 local_port, const IPv4Address& peer_address, u16 peer_port);
    void set_originator(TCPSocket& originator) { m_originator = originator.make_weak_ptr(); }
    bool has_originator() { return !!m_originator; }
    void release_to_originator();
    void release_for_accept(RefPtr<TCPSocket>);

protected:
    void set_direction(Direction direction) { m_direction = direction; }

private:
    explicit TCPSocket(int protocol);
    virtual const char* class_name() const override { return "TCPSocket"; }

    static NetworkOrdered<u16> compute_tcp_checksum(const IPv4Address& source, const IPv4Address& destination, const TCPPacket&, u16 payload_size);

    virtual int protocol_receive(const KBuffer&, void* buffer, size_t buffer_size, int flags) override;
    virtual int protocol_send(const void*, size_t) override;
    virtual KResult protocol_connect(FileDescription&, ShouldBlock) override;
    virtual int protocol_allocate_local_port() override;
    virtual bool protocol_is_disconnected() const override;
    virtual KResult protocol_bind() override;
    virtual KResult protocol_listen() override;

    WeakPtr<TCPSocket> m_originator;
    HashMap<IPv4SocketTuple, NonnullRefPtr<TCPSocket>> m_pending_release_for_accept;
    Direction m_direction { Direction::Unspecified };
    Error m_error { Error::None };
    WeakPtr<NetworkAdapter> m_adapter;
    u32 m_sequence_number { 0 };
    u32 m_ack_number { 0 };
    State m_state { State::Closed };
    u32 m_packets_in { 0 };
    u32 m_bytes_in { 0 };
    u32 m_packets_out { 0 };
    u32 m_bytes_out { 0 };

    struct OutgoingPacket {
        u32 ack_number;
        ByteBuffer buffer;
        int tx_counter { 0 };
        timeval tx_time;
    };

    Lock m_not_acked_lock { "TCPSocket unacked packets" };
    SinglyLinkedList<OutgoingPacket> m_not_acked;
};
