#pragma once

#include <AK/Function.h>
#include <AK/WeakPtr.h>
#include <Kernel/Net/IPv4Socket.h>

class TCPSocket final : public IPv4Socket {
public:
    static void for_each(Function<void(TCPSocket*&)>);
    static NonnullRefPtr<TCPSocket> create(int protocol);
    virtual ~TCPSocket() override;

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

    State state() const { return m_state; }
    void set_state(State state) { m_state = state; }

    void set_ack_number(u32 n) { m_ack_number = n; }
    void set_sequence_number(u32 n) { m_sequence_number = n; }
    u32 ack_number() const { return m_ack_number; }
    u32 sequence_number() const { return m_sequence_number; }
    u32 packets_in() const { return m_packets_in; }
    u32 bytes_in() const { return m_bytes_in; }
    u32 packets_out() const { return m_packets_out; }
    u32 bytes_out() const { return m_bytes_out; }

    void send_tcp_packet(u16 flags, const void* = nullptr, int = 0);
    void record_incoming_data(int);

    static Lockable<HashMap<IPv4SocketTuple, TCPSocket*>>& sockets_by_tuple();
    static TCPSocketHandle from_tuple(const IPv4SocketTuple& tuple);
    static TCPSocketHandle from_endpoints(const IPv4Address& local_address, u16 local_port, const IPv4Address& peer_address, u16 peer_port);

private:
    explicit TCPSocket(int protocol);
    virtual const char* class_name() const override { return "TCPSocket"; }

    static NetworkOrdered<u16> compute_tcp_checksum(const IPv4Address& source, const IPv4Address& destination, const TCPPacket&, u16 payload_size);

    virtual int protocol_receive(const KBuffer&, void* buffer, size_t buffer_size, int flags) override;
    virtual int protocol_send(const void*, int) override;
    virtual KResult protocol_connect(FileDescription&, ShouldBlock) override;
    virtual int protocol_allocate_local_port() override;
    virtual bool protocol_is_disconnected() const override;
    virtual KResult protocol_bind() override;
    virtual KResult protocol_listen() override;

    WeakPtr<NetworkAdapter> m_adapter;
    u32 m_sequence_number { 0 };
    u32 m_ack_number { 0 };
    State m_state { State::Closed };
    u32 m_packets_in { 0 };
    u32 m_bytes_in { 0 };
    u32 m_packets_out { 0 };
    u32 m_bytes_out { 0 };
};

class TCPSocketHandle : public SocketHandle {
public:
    TCPSocketHandle() {}

    TCPSocketHandle(RefPtr<TCPSocket>&& socket)
        : SocketHandle(move(socket))
    {
    }

    TCPSocketHandle(TCPSocketHandle&& other)
        : SocketHandle(move(other))
    {
    }

    TCPSocketHandle(const TCPSocketHandle&) = delete;
    TCPSocketHandle& operator=(const TCPSocketHandle&) = delete;

    TCPSocket* operator->() { return &socket(); }
    const TCPSocket* operator->() const { return &socket(); }

    TCPSocket& socket() { return static_cast<TCPSocket&>(SocketHandle::socket()); }
    const TCPSocket& socket() const { return static_cast<const TCPSocket&>(SocketHandle::socket()); }
};
