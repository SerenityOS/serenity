#pragma once

#include <Kernel/Net/IPv4Socket.h>

class TCPSocket final : public IPv4Socket {
public:
    static NonnullRefPtr<TCPSocket> create(int protocol);
    virtual ~TCPSocket() override;

    enum class State {
        Disconnected,
        Connecting,
        Connected,
        Disconnecting,
    };

    State state() const { return m_state; }
    void set_state(State state) { m_state = state; }

    void set_ack_number(dword n) { m_ack_number = n; }
    void set_sequence_number(dword n) { m_sequence_number = n; }
    dword ack_number() const { return m_ack_number; }
    dword sequence_number() const { return m_sequence_number; }

    void send_tcp_packet(word flags, const void* = nullptr, int = 0);

    static Lockable<HashMap<word, TCPSocket*>>& sockets_by_port();
    static TCPSocketHandle from_port(word);

private:
    explicit TCPSocket(int protocol);
    virtual const char* class_name() const override { return "TCPSocket"; }

    static NetworkOrdered<word> compute_tcp_checksum(const IPv4Address& source, const IPv4Address& destination, const TCPPacket&, word payload_size);

    virtual int protocol_receive(const ByteBuffer&, void* buffer, size_t buffer_size, int flags, sockaddr* addr, socklen_t* addr_length) override;
    virtual int protocol_send(const void*, int) override;
    virtual KResult protocol_connect(FileDescription&, ShouldBlock) override;
    virtual int protocol_allocate_local_port() override;
    virtual bool protocol_is_disconnected() const override;
    virtual KResult protocol_bind() override;

    dword m_sequence_number { 0 };
    dword m_ack_number { 0 };
    State m_state { State::Disconnected };
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
