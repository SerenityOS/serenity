#pragma once

#include <Kernel/Net/Socket.h>
#include <Kernel/DoubleBuffer.h>
#include <Kernel/Net/IPv4.h>
#include <AK/HashMap.h>
#include <Kernel/Lock.h>
#include <AK/SinglyLinkedList.h>

class IPv4SocketHandle;
class TCPSocketHandle;
class NetworkAdapter;
class TCPPacket;
class TCPSocket;

class IPv4Socket : public Socket {
public:
    static Retained<IPv4Socket> create(int type, int protocol);
    virtual ~IPv4Socket() override;

    static Lockable<HashTable<IPv4Socket*>>& all_sockets();

    virtual KResult bind(const sockaddr*, socklen_t) override;
    virtual KResult connect(FileDescriptor&, const sockaddr*, socklen_t, ShouldBlock = ShouldBlock::Yes) override;
    virtual bool get_address(sockaddr*, socklen_t*) override;
    virtual void attach(FileDescriptor&) override;
    virtual void detach(FileDescriptor&) override;
    virtual bool can_read(FileDescriptor&) const override;
    virtual ssize_t read(FileDescriptor&, byte*, ssize_t) override;
    virtual ssize_t write(FileDescriptor&, const byte*, ssize_t) override;
    virtual bool can_write(FileDescriptor&) const override;
    virtual ssize_t sendto(FileDescriptor&, const void*, size_t, int, const sockaddr*, socklen_t) override;
    virtual ssize_t recvfrom(FileDescriptor&, void*, size_t, int flags, sockaddr*, socklen_t*) override;

    void did_receive(const IPv4Address& source_address, word source_port, ByteBuffer&&);

    const IPv4Address& source_address() const;
    word source_port() const { return m_source_port; }
    void set_source_port(word port) { m_source_port = port; }

    const IPv4Address& destination_address() const { return m_destination_address; }
    word destination_port() const { return m_destination_port; }
    void set_destination_port(word port) { m_destination_port = port; }

protected:
    IPv4Socket(int type, int protocol);
    virtual const char* class_name() const override { return "IPv4Socket"; }

    int allocate_source_port_if_needed();

    virtual KResult protocol_bind() { return KSuccess; }
    virtual int protocol_receive(const ByteBuffer&, void*, size_t, int, sockaddr*, socklen_t*) { return -ENOTIMPL; }
    virtual int protocol_send(const void*, int) { return -ENOTIMPL; }
    virtual KResult protocol_connect(FileDescriptor&, ShouldBlock) { return KSuccess; }
    virtual int protocol_allocate_source_port() { return 0; }
    virtual bool protocol_is_disconnected() const { return false; }

private:
    virtual bool is_ipv4() const override { return true; }

    bool m_bound { false };
    int m_attached_fds { 0 };

    IPv4Address m_source_address;
    IPv4Address m_destination_address;

    DoubleBuffer m_for_client;
    DoubleBuffer m_for_server;

    struct ReceivedPacket {
        IPv4Address source_address;
        word source_port;
        ByteBuffer data;
    };

    SinglyLinkedList<ReceivedPacket> m_receive_queue;

    word m_source_port { 0 };
    word m_destination_port { 0 };

    dword m_bytes_received { 0 };

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
