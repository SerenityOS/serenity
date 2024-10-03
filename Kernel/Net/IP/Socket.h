/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Format.h>
#include <AK/HashMap.h>
#include <AK/IPAddress.h>
#include <AK/SetOnce.h>
#include <AK/SinglyLinkedList.h>
#include <Kernel/Library/DoubleBuffer.h>
#include <Kernel/Library/KBuffer.h>
#include <Kernel/Locking/MutexProtected.h>
#include <Kernel/Net/IP/IPv4.h>
#include <Kernel/Net/IP/SocketTuple.h>
#include <Kernel/Net/Routing.h>
#include <Kernel/Net/Socket.h>

namespace Kernel {

class NetworkAdapter;
class TCPPacket;
class TCPSocket;
class IPv4Socket;
class IPv6Socket;

struct ReceivedPacket {
    IPAddress peer_address { IPv4Address {} };
    u16 peer_port;
    UnixDateTime timestamp;
    OwnPtr<KBuffer> data;
};

class IPSocketDelegate {
public:
    virtual ~IPSocketDelegate() = default;

    virtual IPVersion ip_version() const = 0;
    int domain() const { return ip_version() == IPVersion::IPv4 ? AF_INET : AF_INET6; }
    virtual IPAddress local_address() const = 0;
    virtual IPAddress peer_address() const = 0;
    virtual void get_local_address(sockaddr*, socklen_t*, u16 local_port) = 0;
    virtual void get_peer_address(sockaddr*, socklen_t*, u16 peer_port) = 0;
    virtual void clear_addresses() = 0;
    virtual ErrorOr<void> set_local_address(Userspace<sockaddr const*> user_address, socklen_t address_size) = 0;
    virtual ErrorOr<void> set_local_address(IPAddress) = 0;
    virtual void fill_empty_local_address(RefPtr<NetworkAdapter> adapter) = 0;
    virtual ErrorOr<void> set_peer_address(Userspace<sockaddr const*> user_address, socklen_t address_size) = 0;
    virtual ErrorOr<void> set_peer_address(IPAddress) = 0;
    virtual ErrorOr<void> copy_address_to_user(ReceivedPacket const& packet, Userspace<sockaddr*> user_address, Userspace<socklen_t*> address_size) = 0;
    virtual ErrorOr<void> add_multicast_membership(Userspace<void const*> user_value, socklen_t user_value_size) = 0;
    virtual ErrorOr<void> remove_multicast_membership(Userspace<void const*> user_value, socklen_t user_value_size) = 0;

    virtual ErrorOr<size_t> send_raw_packet(RoutingDecision routing_decision, UserOrKernelBuffer const& data, size_t data_length, int protocol, int type_of_service, int ttl) = 0;
};

class IPSocket : public Socket {
public:
    static ErrorOr<NonnullRefPtr<Socket>> create(int domain, int type, int protocol);
    virtual ~IPSocket() override;

    // Returning a certain version means it's safe to cast to the corresponding socket type.
    IPVersion ip_version() const { return m_delegate->ip_version(); }
    virtual bool is_ipv4() const override { return m_delegate->ip_version() == IPVersion::IPv4; }

    enum class BufferMode {
        Packets,
        Bytes,
    };
    BufferMode buffer_mode() const { return m_buffer_mode; }

    virtual ErrorOr<void> close() override;
    virtual ErrorOr<void> bind(Credentials const&, Userspace<sockaddr const*>, socklen_t) override;
    virtual ErrorOr<void> connect(Credentials const&, OpenFileDescription&, Userspace<sockaddr const*>, socklen_t) override;
    virtual ErrorOr<void> listen(size_t) override;
    virtual void get_local_address(sockaddr*, socklen_t*) override;
    virtual void get_peer_address(sockaddr*, socklen_t*) override;
    virtual bool can_read(OpenFileDescription const&, u64) const override;
    virtual bool can_write(OpenFileDescription const&, u64) const override;
    virtual ErrorOr<size_t> sendto(OpenFileDescription&, UserOrKernelBuffer const&, size_t, int, Userspace<sockaddr const*>, socklen_t) override;
    virtual ErrorOr<size_t> recvfrom(OpenFileDescription&, UserOrKernelBuffer&, size_t, int flags, Userspace<sockaddr*>, Userspace<socklen_t*>, UnixDateTime&, bool blocking) override;
    virtual ErrorOr<void> setsockopt(int level, int option, Userspace<void const*>, socklen_t) override;
    virtual ErrorOr<void> getsockopt(OpenFileDescription&, int level, int option, Userspace<void*>, Userspace<socklen_t*>) override;
    virtual ErrorOr<void> ioctl(OpenFileDescription&, unsigned request, Userspace<void*> arg) override;
    virtual void shut_down_for_reading() override;
    bool did_receive(IPAddress const& peer_address, u16 peer_port, ReadonlyBytes, UnixDateTime const&);

    bool is_in_multicast_group(IPAddress multicast_address) const;

    u16 local_port() const { return m_local_port; }
    void set_local_port(u16 port) { m_local_port = port; }

    u16 peer_port() const { return m_peer_port; }
    void set_peer_port(u16 port) { m_peer_port = port; }

    IPAddress local_address() const;
    IPAddress peer_address() const;

    // TODO: Make this API generic, this is a stopgap to make TCPSocket “just work” for the time being.
    IPv4SocketTuple ipv4_tuple() const;

    ErrorOr<NonnullOwnPtr<KString>> tuple_string() const;

    virtual ErrorOr<NonnullOwnPtr<KString>> pseudo_path(OpenFileDescription const& description) const override;

    u8 type_of_service() const { return m_type_of_service; }
    u8 ttl() const { return m_ttl; }

protected:
    static constexpr size_t receive_buffer_size = 256 * KiB;

    IPSocket(int type, int protocol, NonnullOwnPtr<IPSocketDelegate> delegate, NonnullOwnPtr<DoubleBuffer> receive_buffer, OwnPtr<KBuffer> optional_scratch_buffer);
    virtual StringView class_name() const override { return "IPv4Socket"sv; }

    void set_bound() { m_bound.set(); }
    ErrorOr<void> ensure_bound();

    void set_can_read(bool);

    virtual ErrorOr<void> protocol_bind() { return {}; }
    virtual ErrorOr<void> protocol_listen() { return {}; }
    // FIXME: Should only take the upper-layer packet, not the full IP packet.
    virtual ErrorOr<size_t> protocol_receive(ReadonlyBytes /* raw_ipv4_packet */, UserOrKernelBuffer&, size_t, int) { return ENOTIMPL; }
    virtual ErrorOr<size_t> protocol_send(UserOrKernelBuffer const&, size_t) { return ENOTIMPL; }
    virtual ErrorOr<void> protocol_connect(OpenFileDescription&) { return {}; }
    // FIXME: Should only take the upper-layer packet, not the full IP packet.
    virtual ErrorOr<size_t> protocol_size(ReadonlyBytes /* raw_ipv4_packet */) { return ENOTIMPL; }
    virtual bool protocol_is_disconnected() const { return false; }

    bool has_specific_local_address() const;

    static ErrorOr<NonnullOwnPtr<DoubleBuffer>> try_create_receive_buffer();
    void drop_receive_buffer();

    size_t available_space_in_receive_buffer() const { return m_receive_buffer ? m_receive_buffer->space_for_writing() : 0; }

    OwnPtr<DoubleBuffer> m_receive_buffer;
    BufferMode m_buffer_mode { BufferMode::Packets };
    OwnPtr<KBuffer> m_scratch_buffer;

    u16 m_local_port { 0 };
    u16 m_peer_port { 0 };

    u32 m_bytes_received { 0 };

    u8 m_type_of_service { IPTOS_LOWDELAY };
    u8 m_ttl { 64 };
    bool m_multicast_loop { true };

    bool m_can_read { false };
    SetOnce m_bound;

    IntrusiveListNode<IPSocket> m_list_node;

    NonnullOwnPtr<IPSocketDelegate> m_delegate;

public:
    using List = IntrusiveList<&IPSocket::m_list_node>;

    static MutexProtected<IPSocket::List>& all_sockets();

private:
    ErrorOr<size_t> receive_byte_buffered(OpenFileDescription&, UserOrKernelBuffer& buffer, size_t buffer_length, int flags, Userspace<sockaddr*>, Userspace<socklen_t*>, bool blocking);
    ErrorOr<size_t> receive_packet_buffered(OpenFileDescription&, UserOrKernelBuffer& buffer, size_t buffer_length, int flags, Userspace<sockaddr*>, Userspace<socklen_t*>, UnixDateTime&, bool blocking);

    SinglyLinkedList<ReceivedPacket, CountingSizeCalculationPolicy> m_receive_queue;
};

class IPv4Socket : public IPSocketDelegate {
public:
    IPv4Socket() = default;
    virtual ~IPv4Socket() = default;

    virtual IPVersion ip_version() const override { return IPVersion::IPv4; }

    virtual void get_local_address(sockaddr*, socklen_t*, u16 local_port) override;
    virtual void get_peer_address(sockaddr*, socklen_t*, u16 peer_port) override;

    virtual IPAddress local_address() const override { return m_local_address; }
    virtual IPAddress peer_address() const override { return m_peer_address; }

    virtual ErrorOr<void> set_local_address(Userspace<sockaddr const*> user_address, socklen_t address_size) override;
    virtual ErrorOr<void> set_local_address(IPAddress) override;
    virtual ErrorOr<void> set_peer_address(Userspace<sockaddr const*> user_address, socklen_t address_size) override;
    virtual ErrorOr<void> set_peer_address(IPAddress) override;
    virtual void clear_addresses() override
    {
        m_local_address = {};
        m_peer_address = {};
    }
    virtual ErrorOr<void> copy_address_to_user(ReceivedPacket const& packet, Userspace<sockaddr*> user_address, Userspace<socklen_t*> address_size) override;
    virtual void fill_empty_local_address(RefPtr<NetworkAdapter> adapter) override;
    virtual ErrorOr<size_t> send_raw_packet(RoutingDecision routing_decision, UserOrKernelBuffer const& data, size_t data_length, int protocol, int type_of_service, int ttl) override;

    virtual ErrorOr<void> add_multicast_membership(Userspace<void const*> user_value, socklen_t user_value_size) override;
    virtual ErrorOr<void> remove_multicast_membership(Userspace<void const*> user_value, socklen_t user_value_size) override;
    Vector<IPv4Address> const& multicast_memberships() const { return m_multicast_memberships; }

    IPv4SocketTuple tuple_with_ports(u16 local_port, u16 peer_port) const { return IPv4SocketTuple(m_local_address, local_port, m_peer_address, peer_port); }

protected:
    friend class IPSocket;

private:
    IPv4Address m_local_address;
    IPv4Address m_peer_address;

    Vector<IPv4Address> m_multicast_memberships;
};

}
