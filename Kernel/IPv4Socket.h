#pragma once

#include <Kernel/Socket.h>
#include <Kernel/DoubleBuffer.h>
#include <Kernel/IPv4.h>
#include <AK/HashMap.h>
#include <AK/Lock.h>
#include <AK/SinglyLinkedList.h>

class IPv4SocketHandle;
class NetworkAdapter;
class TCPPacket;

enum TCPState {
    Disconnected,
    Connecting1,
    Connecting2,
    Connected,
    Disconnecting1,
    Disconnecting2,
};

class IPv4Socket final : public Socket {
public:
    static Retained<IPv4Socket> create(int type, int protocol);
    virtual ~IPv4Socket() override;

    static Lockable<HashTable<IPv4Socket*>>& all_sockets();
    static Lockable<HashMap<word, IPv4Socket*>>& sockets_by_udp_port();
    static Lockable<HashMap<word, IPv4Socket*>>& sockets_by_tcp_port();

    static IPv4SocketHandle from_tcp_port(word);
    static IPv4SocketHandle from_udp_port(word);

    virtual KResult bind(const sockaddr*, socklen_t) override;
    virtual KResult connect(const sockaddr*, socklen_t) override;
    virtual bool get_address(sockaddr*, socklen_t*) override;
    virtual void attach_fd(SocketRole) override;
    virtual void detach_fd(SocketRole) override;
    virtual bool can_read(SocketRole) const override;
    virtual ssize_t read(SocketRole, byte*, ssize_t) override;
    virtual ssize_t write(SocketRole, const byte*, ssize_t) override;
    virtual bool can_write(SocketRole) const override;
    virtual ssize_t sendto(const void*, size_t, int, const sockaddr*, socklen_t) override;
    virtual ssize_t recvfrom(void*, size_t, int flags, sockaddr*, socklen_t*) override;

    void did_receive(ByteBuffer&&);

    word source_port() const { return m_source_port; }
    word destination_port() const { return m_destination_port; }

    void send_tcp_packet(NetworkAdapter&, word flags, const void* payload = nullptr, size_t = 0);
    void set_tcp_state(TCPState state) { m_tcp_state = state; }
    TCPState tcp_state() const { return m_tcp_state; }
    void set_tcp_ack_number(dword n) { m_tcp_ack_number = n; }
    void set_tcp_sequence_number(dword n) { m_tcp_sequence_number = n; }
    dword tcp_ack_number() const { return m_tcp_ack_number; }
    dword tcp_sequence_number() const { return m_tcp_sequence_number; }

private:
    IPv4Socket(int type, int protocol);
    virtual bool is_ipv4() const override { return true; }

    void allocate_source_port_if_needed();
    NetworkOrdered<word> compute_tcp_checksum(const IPv4Address& source, const IPv4Address& destination, const TCPPacket&, word payload_size);

    bool m_bound { false };
    int m_attached_fds { 0 };
    IPv4Address m_destination_address;

    DoubleBuffer m_for_client;
    DoubleBuffer m_for_server;

    SinglyLinkedList<ByteBuffer> m_receive_queue;

    word m_source_port { 0 };
    word m_destination_port { 0 };

    dword m_tcp_sequence_number { 0 };
    dword m_tcp_ack_number { 0 };
    TCPState m_tcp_state { Disconnected };

    bool m_can_read { false };
};

class IPv4SocketHandle : public SocketHandle {
public:
    IPv4SocketHandle() { }

    IPv4SocketHandle(RetainPtr<IPv4Socket>&& socket)
        : SocketHandle(move(socket))
    {
    }

    IPv4SocketHandle(IPv4SocketHandle&& other)
        : SocketHandle(move(other))
    {
    }

    IPv4SocketHandle(const IPv4SocketHandle&) = delete;
    IPv4SocketHandle& operator=(const IPv4SocketHandle&) = delete;

    IPv4Socket* operator->() { return &socket(); }
    const IPv4Socket* operator->() const { return &socket(); }

    IPv4Socket& socket() { return static_cast<IPv4Socket&>(SocketHandle::socket()); }
    const IPv4Socket& socket() const { return static_cast<const IPv4Socket&>(SocketHandle::socket()); }
};
