/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <AK/SinglyLinkedListWithCount.h>
#include <Kernel/DoubleBuffer.h>
#include <Kernel/KBuffer.h>
#include <Kernel/Lock.h>
#include <Kernel/Net/IPv4.h>
#include <Kernel/Net/IPv4SocketTuple.h>
#include <Kernel/Net/Socket.h>

namespace Kernel {

class NetworkAdapter;
class TCPPacket;
class TCPSocket;

struct PortAllocationResult {
    KResultOr<u16> error_or_port;
    bool did_allocate;
};

class IPv4Socket : public Socket {
public:
    static KResultOr<NonnullRefPtr<Socket>> create(int type, int protocol);
    virtual ~IPv4Socket() override;

    static Lockable<HashTable<IPv4Socket*>>& all_sockets();

    virtual KResult close() override;
    virtual KResult bind(Userspace<const sockaddr*>, socklen_t) override;
    virtual KResult connect(FileDescription&, Userspace<const sockaddr*>, socklen_t, ShouldBlock = ShouldBlock::Yes) override;
    virtual KResult listen(size_t) override;
    virtual void get_local_address(sockaddr*, socklen_t*) override;
    virtual void get_peer_address(sockaddr*, socklen_t*) override;
    virtual bool can_read(const FileDescription&, size_t) const override;
    virtual bool can_write(const FileDescription&, size_t) const override;
    virtual KResultOr<size_t> sendto(FileDescription&, const UserOrKernelBuffer&, size_t, int, Userspace<const sockaddr*>, socklen_t) override;
    virtual KResultOr<size_t> recvfrom(FileDescription&, UserOrKernelBuffer&, size_t, int flags, Userspace<sockaddr*>, Userspace<socklen_t*>, Time&) override;
    virtual KResult setsockopt(int level, int option, Userspace<const void*>, socklen_t) override;
    virtual KResult getsockopt(FileDescription&, int level, int option, Userspace<void*>, Userspace<socklen_t*>) override;

    virtual int ioctl(FileDescription&, unsigned request, FlatPtr arg) override;

    bool did_receive(const IPv4Address& peer_address, u16 peer_port, ReadonlyBytes, const Time&);

    const IPv4Address& local_address() const { return m_local_address; }
    u16 local_port() const { return m_local_port; }
    void set_local_port(u16 port) { m_local_port = port; }
    bool has_specific_local_address() { return m_local_address.to_u32() != 0; }

    const IPv4Address& peer_address() const { return m_peer_address; }
    u16 peer_port() const { return m_peer_port; }
    void set_peer_port(u16 port) { m_peer_port = port; }

    const Vector<IPv4Address>& multicast_memberships() const { return m_multicast_memberships; }

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

    PortAllocationResult allocate_local_port_if_needed();

    virtual KResult protocol_bind() { return KSuccess; }
    virtual KResult protocol_listen([[maybe_unused]] bool did_allocate_port) { return KSuccess; }
    virtual KResultOr<size_t> protocol_receive(ReadonlyBytes /* raw_ipv4_packet */, UserOrKernelBuffer&, size_t, int) { return ENOTIMPL; }
    virtual KResultOr<size_t> protocol_send(const UserOrKernelBuffer&, size_t) { return ENOTIMPL; }
    virtual KResult protocol_connect(FileDescription&, ShouldBlock) { return KSuccess; }
    virtual KResultOr<u16> protocol_allocate_local_port() { return ENOPROTOOPT; }
    virtual bool protocol_is_disconnected() const { return false; }

    virtual void shut_down_for_reading() override;

    void set_local_address(IPv4Address address) { m_local_address = address; }
    void set_peer_address(IPv4Address address) { m_peer_address = address; }

private:
    virtual bool is_ipv4() const override { return true; }

    KResultOr<size_t> receive_byte_buffered(FileDescription&, UserOrKernelBuffer& buffer, size_t buffer_length, int flags, Userspace<sockaddr*>, Userspace<socklen_t*>);
    KResultOr<size_t> receive_packet_buffered(FileDescription&, UserOrKernelBuffer& buffer, size_t buffer_length, int flags, Userspace<sockaddr*>, Userspace<socklen_t*>, Time&);

    void set_can_read(bool);

    IPv4Address m_local_address;
    IPv4Address m_peer_address;

    Vector<IPv4Address> m_multicast_memberships;
    bool m_multicast_loop { true };

    struct ReceivedPacket {
        IPv4Address peer_address;
        u16 peer_port;
        Time timestamp;
        Optional<KBuffer> data;
    };

    SinglyLinkedListWithCount<ReceivedPacket> m_receive_queue;

    DoubleBuffer m_receive_buffer { 256 * KiB };

    u16 m_local_port { 0 };
    u16 m_peer_port { 0 };

    u32 m_bytes_received { 0 };

    u8 m_ttl { 64 };

    bool m_can_read { false };

    BufferMode m_buffer_mode { BufferMode::Packets };

    Optional<KBuffer> m_scratch_buffer;
};

}
