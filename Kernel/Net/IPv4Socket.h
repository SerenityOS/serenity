#pragma once

#include <AK/HashMap.h>
#include <AK/SinglyLinkedList.h>
#include <Kernel/DoubleBuffer.h>
#include <Kernel/KBuffer.h>
#include <Kernel/Lock.h>
#include <Kernel/Net/IPv4.h>
#include <Kernel/Net/IPv4SocketTuple.h>
#include <Kernel/Net/Socket.h>

class NetworkAdapter;
class TCPPacket;
class TCPSocket;

class IPv4Socket : public Socket {
public:
    static NonnullRefPtr<IPv4Socket> create(int type, int protocol);
    virtual ~IPv4Socket() override;

    static Lockable<HashTable<IPv4Socket*>>& all_sockets();

    virtual KResult bind(const sockaddr*, socklen_t) override;
    virtual KResult connect(FileDescription&, const sockaddr*, socklen_t, ShouldBlock = ShouldBlock::Yes) override;
    virtual KResult listen(int) override;
    virtual bool get_local_address(sockaddr*, socklen_t*) override;
    virtual bool get_peer_address(sockaddr*, socklen_t*) override;
    virtual void attach(FileDescription&) override;
    virtual void detach(FileDescription&) override;
    virtual bool can_read(const FileDescription&) const override;
    virtual bool can_write(const FileDescription&) const override;
    virtual ssize_t sendto(FileDescription&, const void*, size_t, int, const sockaddr*, socklen_t) override;
    virtual ssize_t recvfrom(FileDescription&, void*, size_t, int flags, sockaddr*, socklen_t*) override;
    virtual KResult setsockopt(int level, int option, const void*, socklen_t) override;
    virtual KResult getsockopt(FileDescription&, int level, int option, void*, socklen_t*) override;

    virtual int ioctl(FileDescription&, unsigned request, unsigned arg) override;

    bool did_receive(const IPv4Address& peer_address, u16 peer_port, KBuffer&&);

    const IPv4Address& local_address() const { return m_local_address; }
    u16 local_port() const { return m_local_port; }
    void set_local_port(u16 port) { m_local_port = port; }
    bool has_specific_local_address() { return m_local_address.to_u32() != 0; }

    const IPv4Address& peer_address() const { return m_peer_address; }
    u16 peer_port() const { return m_peer_port; }
    void set_peer_port(u16 port) { m_peer_port = port; }

    IPv4SocketTuple tuple() const { return IPv4SocketTuple(m_local_address, m_local_port, m_peer_address, m_peer_port); }

    String absolute_path(const FileDescription& description) const override;

    u8 ttl() const { return m_ttl; }

    enum class BufferMode {
        Packets,
        Bytes,
    };
    BufferMode buffer_mode() const { return m_buffer_mode; }

protected:
    IPv4Socket(int type, int protocol);
    virtual const char* class_name() const override { return "IPv4Socket"; }

    int allocate_local_port_if_needed();

    virtual KResult protocol_bind() { return KSuccess; }
    virtual KResult protocol_listen() { return KSuccess; }
    virtual int protocol_receive(const KBuffer&, void*, size_t, int) { return -ENOTIMPL; }
    virtual int protocol_send(const void*, int) { return -ENOTIMPL; }
    virtual KResult protocol_connect(FileDescription&, ShouldBlock) { return KSuccess; }
    virtual int protocol_allocate_local_port() { return 0; }
    virtual bool protocol_is_disconnected() const { return false; }

    void set_local_address(IPv4Address address) { m_local_address = address; }
    void set_peer_address(IPv4Address address) { m_peer_address = address; }

private:
    virtual bool is_ipv4() const override { return true; }

    IPv4Address m_local_address;
    IPv4Address m_peer_address;

    struct ReceivedPacket {
        IPv4Address peer_address;
        u16 peer_port;
        Optional<KBuffer> data;
    };

    SinglyLinkedList<ReceivedPacket> m_receive_queue;

    DoubleBuffer m_receive_buffer;

    u16 m_local_port { 0 };
    u16 m_peer_port { 0 };

    u32 m_bytes_received { 0 };

    u8 m_ttl { 64 };

    bool m_can_read { false };

    BufferMode m_buffer_mode { BufferMode::Packets };

    Optional<KBuffer> m_scratch_buffer;
};
