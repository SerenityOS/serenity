/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <AK/StringBuilder.h>
#include <AK/StringView.h>
#include <Kernel/API/Ioctl.h>
#include <Kernel/API/POSIX/errno.h>
#include <Kernel/Debug.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/Net/ICMP.h>
#include <Kernel/Net/IP/ARP.h>
#include <Kernel/Net/IP/IP.h>
#include <Kernel/Net/IP/IPv4.h>
#include <Kernel/Net/IP/Socket.h>
#include <Kernel/Net/NetworkAdapter.h>
#include <Kernel/Net/NetworkingManagement.h>
#include <Kernel/Net/Routing.h>
#include <Kernel/Net/TCP.h>
#include <Kernel/Net/TCPSocket.h>
#include <Kernel/Net/UDP.h>
#include <Kernel/Net/UDPSocket.h>
#include <Kernel/Tasks/Process.h>
#include <Kernel/UnixTypes.h>

namespace Kernel {

static Singleton<MutexProtected<IPSocket::List>> s_all_sockets;

using BlockFlags = Thread::OpenFileDescriptionBlocker::BlockFlags;

MutexProtected<IPSocket::List>& IPSocket::all_sockets()
{
    return *s_all_sockets;
}

ErrorOr<NonnullOwnPtr<DoubleBuffer>> IPSocket::try_create_receive_buffer()
{
    return DoubleBuffer::try_create("IPSocket: Receive buffer"sv, receive_buffer_size);
}

ErrorOr<NonnullRefPtr<Socket>> IPSocket::create(int domain, int type, int protocol)
{
    auto receive_buffer = TRY(IPSocket::try_create_receive_buffer());

    OwnPtr<IPSocketDelegate> nullable_delegate;
    switch (domain) {
    case AF_INET:
        nullable_delegate = static_cast<OwnPtr<IPSocketDelegate>>(TRY(adopt_nonnull_own_or_enomem(new (nothrow) IPv4Socket)));
        break;
    case AF_INET6:
        TODO();
    }
    NonnullOwnPtr<IPSocketDelegate> delegate = nullable_delegate.release_nonnull();

    if (type == SOCK_STREAM)
        return TRY(TCPSocket::try_create(protocol, move(delegate), move(receive_buffer)));
    if (type == SOCK_DGRAM)
        return TRY(UDPSocket::try_create(protocol, move(delegate), move(receive_buffer)));
    if (type == SOCK_RAW) {
        auto raw_socket = adopt_ref_if_nonnull(new (nothrow) IPSocket(type, protocol, move(delegate), move(receive_buffer), {}));
        if (raw_socket)
            return raw_socket.release_nonnull();
        return ENOMEM;
    }
    return EINVAL;
}

IPSocket::IPSocket(int type, int protocol, NonnullOwnPtr<IPSocketDelegate> delegate, NonnullOwnPtr<DoubleBuffer> receive_buffer, OwnPtr<KBuffer> optional_scratch_buffer)
    : Socket(delegate->domain(), type, protocol)
    , m_receive_buffer(move(receive_buffer))
    , m_scratch_buffer(move(optional_scratch_buffer))
    , m_delegate(move(delegate))
{
    dbgln_if(IP_SOCKET_DEBUG, "IPv4Socket({}) created with type={}, protocol={}", this, type, protocol);
    m_buffer_mode = type == SOCK_STREAM ? BufferMode::Bytes : BufferMode::Packets;
    if (m_buffer_mode == BufferMode::Bytes) {
        VERIFY(m_scratch_buffer);
    }

    all_sockets().with_exclusive([&](auto& table) {
        table.append(*this);
    });
}

IPSocket::~IPSocket()
{
    all_sockets().with_exclusive([&](auto& table) {
        table.remove(*this);
    });
}

IPAddress IPSocket::local_address() const
{
    switch (ip_version()) {
    case IPVersion::IPv4:
        return static_cast<IPv4Socket const&>(*m_delegate).local_address();
    case IPVersion::IPv6:
        TODO();
    }
}

IPAddress IPSocket::peer_address() const
{
    switch (ip_version()) {
    case IPVersion::IPv4:
        return static_cast<IPv4Socket const&>(*m_delegate).peer_address();
    case IPVersion::IPv6:
        TODO();
    }
}

void IPSocket::get_local_address(sockaddr* address, socklen_t* address_size)
{
    m_delegate->get_local_address(address, address_size, m_local_port);
}

void IPv4Socket::get_local_address(sockaddr* address, socklen_t* address_size, u16 local_port)
{
    sockaddr_in local_address = { AF_INET, htons(local_port), { m_local_address.to_in_addr_t() }, { 0 } };
    memcpy(address, &local_address, min(static_cast<size_t>(*address_size), sizeof(sockaddr_in)));
    *address_size = sizeof(sockaddr_in);
}

void IPSocket::get_peer_address(sockaddr* address, socklen_t* address_size)
{
    m_delegate->get_peer_address(address, address_size, m_peer_port);
}

void IPv4Socket::get_peer_address(sockaddr* address, socklen_t* address_size, u16 peer_port)
{
    sockaddr_in peer_address = { AF_INET, htons(peer_port), { m_peer_address.to_in_addr_t() }, { 0 } };
    memcpy(address, &peer_address, min(static_cast<size_t>(*address_size), sizeof(sockaddr_in)));
    *address_size = sizeof(sockaddr_in);
}

bool IPSocket::is_in_multicast_group(IPAddress multicast_address) const
{
    switch (ip_version()) {
    case IPVersion::IPv6:
        TODO();
    case IPVersion::IPv4:
        return static_cast<IPv4Socket const&>(*m_delegate).multicast_memberships().contains_slow(multicast_address.as_v4());
    }
}

ErrorOr<void> IPSocket::ensure_bound()
{
    dbgln_if(IP_SOCKET_DEBUG, "IPSocket::ensure_bound() m_bound {}", m_bound.was_set());
    if (m_bound.was_set())
        return {};

    auto result = protocol_bind();
    if (!result.is_error())
        m_bound.set();
    return result;
}

// For the generic port retrieval code to work (see below in bind()), port fields need to be identical between IP versions.
static_assert(__builtin_offsetof(sockaddr_in, sin_port) == __builtin_offsetof(sockaddr_in6, sin6_port)
    && sizeof(decltype(declval<sockaddr_in>().sin_port)) == sizeof(decltype(declval<sockaddr_in6>().sin6_port)));

static ErrorOr<in_port_t> copy_port_from_user(Userspace<sockaddr const*> user_address)
{
    in_port_t requested_local_port_network_order;
    auto* address = reinterpret_cast<sockaddr_in const*>(user_address.unsafe_userspace_ptr());
    // Since the port is in the same place for IPv4 and IPv6, this will always be correct.
    TRY(copy_from_user(&requested_local_port_network_order, &address->sin_port, sizeof(in_port_t)));
    auto requested_local_port = AK::convert_between_host_and_network_endian(requested_local_port_network_order);
    return requested_local_port;
}

ErrorOr<void> IPSocket::bind(Credentials const& credentials, Userspace<sockaddr const*> user_address, socklen_t address_size)
{
    if (m_bound.was_set())
        return set_so_error(EINVAL);

    VERIFY(setup_state() == SetupState::Unstarted);

    SOCKET_TRY(m_delegate->set_local_address(user_address, address_size));

    auto requested_local_port = SOCKET_TRY(copy_port_from_user(user_address));

    if (!credentials.is_superuser()) {
        if (requested_local_port > 0 && requested_local_port < 1024) {
            dbgln("UID {} attempted to bind {} to port {}", credentials.uid(), class_name(), requested_local_port);
            m_delegate->clear_addresses();
            return set_so_error(EACCES);
        }
    }

    m_local_port = requested_local_port;

    dbgln_if(IP_SOCKET_DEBUG, "IPSocket::bind {}({}) to {}:{}", class_name(), this, m_delegate->local_address(), m_local_port);

    // FIXME: If the bind fails, we still have an assigned local port and address!
    return ensure_bound();
}

ErrorOr<void> IPv4Socket::set_local_address(Userspace<sockaddr const*> user_address, socklen_t address_size)
{
    if (address_size != sizeof(sockaddr_in))
        return EINVAL;

    sockaddr_in address {};
    TRY(copy_from_user(&address, user_address, sizeof(sockaddr_in)));

    if (address.sin_family != AF_INET)
        return EINVAL;

    m_local_address = IPv4Address((u8 const*)&address.sin_addr.s_addr);

    return {};
}

ErrorOr<void> IPv4Socket::set_local_address(IPAddress address)
{
    if (!address.is_ipv4())
        return ENOTSUP;
    m_local_address = address.as_v4();
    return {};
}

ErrorOr<void> IPv4Socket::set_peer_address(IPAddress address)
{
    if (!address.is_ipv4())
        return ENOTSUP;
    m_peer_address = address.as_v4();
    return {};
}

IPv4SocketTuple IPSocket::ipv4_tuple() const
{
    VERIFY(ip_version() == IPVersion::IPv4);
    return static_cast<IPv4Socket const&>(*m_delegate).tuple_with_ports(m_local_port, m_peer_port);
}

bool IPSocket::has_specific_local_address() const
{
    return !local_address().is_zero();
}

ErrorOr<void> IPSocket::listen(size_t backlog)
{
    MutexLocker locker(mutex());
    TRY(ensure_bound());
    set_backlog(backlog);
    set_role(Role::Listener);
    evaluate_block_conditions();

    dbgln_if(IP_SOCKET_DEBUG, "IPSocket({}) listening with backlog={}", this, backlog);

    return protocol_listen();
}

ErrorOr<void> IPSocket::connect(Credentials const&, OpenFileDescription& description, Userspace<sockaddr const*> address, socklen_t address_size)
{
    if (address_size < sizeof(sa_family_t))
        return EINVAL;
    sa_family_t sa_family_copy;
    auto* user_address = reinterpret_cast<sockaddr const*>(address.unsafe_userspace_ptr());
    SOCKET_TRY(copy_from_user(&sa_family_copy, &user_address->sa_family, sizeof(u16)));
    if (sa_family_copy != m_delegate->domain())
        return set_so_error(EINVAL);
    if (m_role == Role::Connected)
        return set_so_error(EISCONN);

    m_peer_port = SOCKET_TRY(copy_port_from_user(address));

    SOCKET_TRY(m_delegate->set_peer_address(address, address_size));

    return protocol_connect(description);
}

ErrorOr<void> IPv4Socket::set_peer_address(Userspace<sockaddr const*> user_address, socklen_t address_size)
{
    if (address_size != sizeof(sockaddr_in))
        return EINVAL;
    sockaddr_in safe_address {};
    TRY(copy_from_user(&safe_address, bit_cast<sockaddr_in const*>(user_address.unsafe_userspace_ptr()), sizeof(sockaddr_in)));

    if (safe_address.sin_family != AF_INET)
        return EINVAL;

    m_peer_address = IPv4Address((u8 const*)&safe_address.sin_addr.s_addr);
    if (m_peer_address.is_zero())
        m_peer_address = IPv4Address { 127, 0, 0, 1 };

    return {};
}

bool IPSocket::can_read(OpenFileDescription const&, u64) const
{
    if (m_role == Role::Listener)
        return can_accept();
    if (protocol_is_disconnected())
        return true;
    return m_can_read;
}

bool IPSocket::can_write(OpenFileDescription const&, u64) const
{
    return true;
}

ErrorOr<size_t> IPSocket::sendto(OpenFileDescription&, UserOrKernelBuffer const& data, size_t data_length, [[maybe_unused]] int flags, Userspace<sockaddr const*> addr, socklen_t addr_length)
{
    MutexLocker locker(mutex());

    if (addr) {
        u16 sa_family_copy;
        auto* user_address = reinterpret_cast<sockaddr const*>(addr.unsafe_userspace_ptr());
        SOCKET_TRY(copy_from_user(&sa_family_copy, &user_address->sa_family, sizeof(u16)));
        if (sa_family_copy != m_delegate->domain()) {
            dmesgln("sendto: Bad address family: {} (should be {})", sa_family_copy, m_delegate->domain());
            return set_so_error(EAFNOSUPPORT);
        }

        if (type() != SOCK_STREAM) {
            SOCKET_TRY(m_delegate->set_peer_address(addr, addr_length));
            m_peer_port = SOCKET_TRY(copy_port_from_user(addr));
        }
    }

    if (!is_connected() && m_delegate->peer_address().is_zero())
        return set_so_error(EPIPE);

    auto allow_broadcast = m_broadcast_allowed ? AllowBroadcast::Yes : AllowBroadcast::No;
    auto allow_using_gateway = ((flags & MSG_DONTROUTE) || m_routing_disabled) ? AllowUsingGateway::No : AllowUsingGateway::Yes;
    auto adapter = bound_interface().with([](auto& bound_device) -> RefPtr<NetworkAdapter> { return bound_device; });
    RoutingDecision routing_decision;
    // TODO: Make this IPv6-capable once the routing supports it.
    if (ip_version() == IPVersion::IPv4) {
        routing_decision = route_to(peer_address().as_v4(), local_address().as_v4(), adapter, allow_broadcast, allow_using_gateway);
        if (routing_decision.is_zero())
            return set_so_error(EHOSTUNREACH);
    }
    // NOTE: Ensure not to release_nonnull the RoutingDecision’s field.
    m_delegate->fill_empty_local_address(routing_decision.adapter);

    TRY(ensure_bound());

    dbgln_if(IP_SOCKET_DEBUG, "sendto: destination={}:{}", m_delegate->peer_address(), m_peer_port);

    if (type() == SOCK_RAW)
        return SOCKET_TRY(m_delegate->send_raw_packet(routing_decision, data, data_length, protocol(), m_type_of_service, m_ttl));

    auto nsent_or_error = protocol_send(data, data_length);
    if (!nsent_or_error.is_error())
        Thread::current()->did_ipv4_socket_write(nsent_or_error.value());

    return nsent_or_error;
}

ErrorOr<size_t> IPv4Socket::send_raw_packet(RoutingDecision routing_decision, UserOrKernelBuffer const& data, size_t data_length, int protocol, int type_of_service, int ttl)
{
    auto ipv4_payload_offset = routing_decision.adapter->ipv4_payload_offset();
    data_length = min(data_length, routing_decision.adapter->mtu() - ipv4_payload_offset);
    auto packet = routing_decision.adapter->acquire_packet_buffer(ipv4_payload_offset + data_length);
    if (!packet)
        return ENOMEM;
    routing_decision.adapter->fill_in_ipv4_header(*packet, m_local_address, routing_decision.next_hop,
        m_peer_address, (TransportProtocol)protocol, data_length, type_of_service, ttl);
    if (auto result = data.read(packet->buffer->data() + ipv4_payload_offset, data_length); result.is_error()) {
        routing_decision.adapter->release_packet_buffer(*packet);
        return result.release_error();
    }
    routing_decision.adapter->send_packet(packet->bytes());
    routing_decision.adapter->release_packet_buffer(*packet);
    return data_length;
}

void IPv4Socket::fill_empty_local_address(RefPtr<NetworkAdapter> adapter)
{
    if (m_local_address.is_zero() && !adapter.is_null())
        m_local_address = adapter->ipv4_address();
}

ErrorOr<size_t> IPSocket::receive_byte_buffered(OpenFileDescription& description, UserOrKernelBuffer& buffer, size_t buffer_length, int flags, Userspace<sockaddr*>, Userspace<socklen_t*>, bool blocking)
{
    MutexLocker locker(mutex());

    VERIFY(m_receive_buffer);

    if (m_receive_buffer->is_empty()) {
        if (protocol_is_disconnected())
            return 0;
        if (!blocking)
            return set_so_error(EAGAIN);

        locker.unlock();
        auto unblocked_flags = BlockFlags::None;
        auto res = Thread::current()->block<Thread::ReadBlocker>({}, description, unblocked_flags);
        locker.lock();

        if (!has_flag(unblocked_flags, BlockFlags::Read)) {
            if (res.was_interrupted())
                return set_so_error(EINTR);

            // Unblocked due to timeout.
            return set_so_error(EAGAIN);
        }
    }

    ErrorOr<size_t> nreceived_or_error { 0 };
    if (flags & MSG_PEEK)
        nreceived_or_error = m_receive_buffer->peek(buffer, buffer_length);
    else
        nreceived_or_error = m_receive_buffer->read(buffer, buffer_length);

    if (!nreceived_or_error.is_error() && nreceived_or_error.value() > 0 && !(flags & MSG_PEEK))
        Thread::current()->did_ipv4_socket_read(nreceived_or_error.value());

    set_can_read(!m_receive_buffer->is_empty());
    return nreceived_or_error;
}

ErrorOr<size_t> IPSocket::receive_packet_buffered(OpenFileDescription& description, UserOrKernelBuffer& buffer, size_t buffer_length, int flags, Userspace<sockaddr*> addr, Userspace<socklen_t*> addr_length, UnixDateTime& packet_timestamp, bool blocking)
{
    MutexLocker locker(mutex());
    ReceivedPacket taken_packet;
    ReceivedPacket* packet { nullptr };
    {
        if (m_receive_queue.is_empty()) {
            // FIXME: Shouldn't this return ENOTCONN instead of EOF?
            //        But if so, we still need to deliver at least one EOF read to userspace.. right?
            if (protocol_is_disconnected())
                return 0;
            if (!blocking)
                return set_so_error(EAGAIN);
        }

        if (!m_receive_queue.is_empty()) {
            if (flags & MSG_PEEK) {
                packet = &m_receive_queue.first();
            } else {
                taken_packet = m_receive_queue.take_first();
                packet = &taken_packet;
            }

            set_can_read(!m_receive_queue.is_empty());

            dbgln_if(IP_SOCKET_DEBUG, "IPSocket({}): recvfrom without blocking {} bytes, packets in queue: {}",
                this,
                packet->data->size(),
                m_receive_queue.size());
        }
    }

    if (!packet) {
        if (protocol_is_disconnected()) {
            dbgln("IPSocket({}) is protocol-disconnected, returning 0 in recvfrom!", this);
            return 0;
        }

        locker.unlock();
        auto unblocked_flags = BlockFlags::None;
        auto res = Thread::current()->block<Thread::ReadBlocker>({}, description, unblocked_flags);
        locker.lock();

        if (!has_flag(unblocked_flags, BlockFlags::Read)) {
            if (res.was_interrupted())
                return set_so_error(EINTR);

            // Unblocked due to timeout.
            return set_so_error(EAGAIN);
        }
        VERIFY(m_can_read);
        VERIFY(!m_receive_queue.is_empty());

        if (flags & MSG_PEEK) {
            packet = &m_receive_queue.first();
        } else {
            taken_packet = m_receive_queue.take_first();
            packet = &taken_packet;
        }

        set_can_read(!m_receive_queue.is_empty());

        dbgln_if(IP_SOCKET_DEBUG, "IPSocket({}): recvfrom with blocking {} bytes, packets in queue: {}",
            this,
            packet->data->size(),
            m_receive_queue.size());
    }
    VERIFY(packet->data);

    packet_timestamp = packet->timestamp;

    if (addr) {
        dbgln_if(IP_SOCKET_DEBUG, "Incoming packet is from: {}:{}", packet->peer_address, packet->peer_port);
        SOCKET_TRY(m_delegate->copy_address_to_user(*packet, addr, addr_length));
    }

    if (type() == SOCK_RAW) {
        size_t bytes_written = min(packet->data->size(), buffer_length);
        SOCKET_TRY(buffer.write(packet->data->data(), bytes_written));
        return bytes_written;
    }

    return protocol_receive(packet->data->bytes(), buffer, buffer_length, flags);
}

ErrorOr<void> IPv4Socket::copy_address_to_user(ReceivedPacket const& packet, Userspace<sockaddr*> user_address, Userspace<socklen_t*> address_size)
{
    sockaddr_in out_addr {};
    memcpy(&out_addr.sin_addr, &packet.peer_address, sizeof(IPv4Address));
    out_addr.sin_port = htons(packet.peer_port);
    out_addr.sin_family = AF_INET;
    Userspace<sockaddr_in*> dest_addr = user_address.ptr();
    TRY(copy_to_user(dest_addr, &out_addr));

    socklen_t out_length = sizeof(sockaddr_in);
    VERIFY(address_size);
    TRY(copy_to_user(address_size, &out_length));
    return {};
}

ErrorOr<size_t> IPSocket::recvfrom(OpenFileDescription& description, UserOrKernelBuffer& buffer, size_t buffer_length, int flags, Userspace<sockaddr*> user_addr, Userspace<socklen_t*> user_addr_length, UnixDateTime& packet_timestamp, bool blocking)
{
    if (user_addr_length) {
        socklen_t addr_length;
        SOCKET_TRY(copy_from_user(&addr_length, user_addr_length.unsafe_userspace_ptr()));
        if (addr_length < sizeof(sockaddr_in))
            return set_so_error(EINVAL);
    }

    dbgln_if(IP_SOCKET_DEBUG, "recvfrom: type={}, local_port={}", type(), local_port());

    ErrorOr<size_t> total_nreceived = 0;
    do {
        auto offset_buffer = buffer.offset(total_nreceived.value());
        auto offset_buffer_length = buffer_length - total_nreceived.value();

        ErrorOr<size_t> nreceived = 0;
        if (buffer_mode() == BufferMode::Bytes)
            nreceived = receive_byte_buffered(description, offset_buffer, offset_buffer_length, flags, user_addr, user_addr_length, blocking);
        else
            nreceived = receive_packet_buffered(description, offset_buffer, offset_buffer_length, flags, user_addr, user_addr_length, packet_timestamp, blocking);

        if (nreceived.is_error())
            total_nreceived = move(nreceived);
        else
            total_nreceived.value() += nreceived.value();
    } while ((flags & MSG_WAITALL) && !total_nreceived.is_error() && total_nreceived.value() < buffer_length);

    if (!total_nreceived.is_error())
        Thread::current()->did_ipv4_socket_read(total_nreceived.value());
    return total_nreceived;
}

bool IPSocket::did_receive(IPAddress const& peer_address, u16 peer_port, ReadonlyBytes packet, UnixDateTime const& packet_timestamp)
{
    MutexLocker locker(mutex());

    if (is_shut_down_for_reading())
        return false;

    auto packet_size = packet.size();

    if (buffer_mode() == BufferMode::Bytes) {
        VERIFY(m_receive_buffer);

        size_t space_in_receive_buffer = m_receive_buffer->space_for_writing();
        if (packet_size > space_in_receive_buffer) {
            dbgln("IPSocket({}): did_receive refusing packet since buffer is full.", this);
            VERIFY(m_can_read);
            return false;
        }
        auto scratch_buffer = UserOrKernelBuffer::for_kernel_buffer(m_scratch_buffer->data());
        auto nreceived_or_error = protocol_receive(packet, scratch_buffer, m_scratch_buffer->size(), 0);
        if (nreceived_or_error.is_error())
            return false;
        auto nwritten_or_error = m_receive_buffer->write(scratch_buffer, nreceived_or_error.value());
        if (nwritten_or_error.is_error())
            return false;
        set_can_read(!m_receive_buffer->is_empty());
    } else {
        // FIXME: What’s with the magic constant here?
        if (m_receive_queue.size() > 2000) {
            dbgln("IPSocket({}): did_receive refusing packet since queue is full.", this);
            return false;
        }
        auto data_or_error = KBuffer::try_create_with_bytes("IPv4Socket: Packet buffer"sv, packet);
        if (data_or_error.is_error()) {
            dbgln("IPSocket: did_receive unable to allocate storage for incoming packet.");
            return false;
        }
        auto result = m_receive_queue.try_append({ peer_address, peer_port, packet_timestamp, data_or_error.release_value() });
        if (result.is_error()) {
            dbgln("IPSocket: Dropped incoming packet because appending to the receive queue failed.");
            return false;
        }
        set_can_read(true);
    }
    m_bytes_received += packet_size;

    if constexpr (IP_SOCKET_DEBUG) {
        if (buffer_mode() == BufferMode::Bytes)
            dbgln("IPSocket({}): did_receive {} bytes, total_received={}", this, packet_size, m_bytes_received);
        else
            dbgln("IPSocket({}): did_receive {} bytes, total_received={}, packets in queue: {}",
                this,
                packet_size,
                m_bytes_received,
                m_receive_queue.size());
    }

    return true;
}

ErrorOr<NonnullOwnPtr<KString>> IPSocket::pseudo_path(OpenFileDescription const&) const
{
    if (m_role == Role::None)
        return KString::try_create("socket"sv);

    StringBuilder builder;
    TRY(builder.try_append("socket:"sv));

    TRY(builder.try_appendff("{}:{}", local_address(), m_local_port));
    if (m_role == Role::Accepted || m_role == Role::Connected)
        TRY(builder.try_appendff(" / {}:{}", peer_address(), m_peer_port));

    switch (m_role) {
    case Role::Listener:
        TRY(builder.try_append(" (listening)"sv));
        break;
    case Role::Accepted:
        TRY(builder.try_append(" (accepted)"sv));
        break;
    case Role::Connected:
        TRY(builder.try_append(" (connected)"sv));
        break;
    case Role::Connecting:
        TRY(builder.try_append(" (connecting)"sv));
        break;
    default:
        VERIFY_NOT_REACHED();
    }

    return KString::try_create(builder.string_view());
}

ErrorOr<NonnullOwnPtr<KString>> IPSocket::tuple_string() const
{
    return KString::formatted(
        "{}:{} -> {}:{}",
        local_address(),
        m_local_port,
        peer_address(),
        m_peer_port);
}

ErrorOr<void> IPSocket::setsockopt(int level, int option, Userspace<void const*> user_value, socklen_t user_value_size)
{
    if ((ip_version() == IPVersion::IPv4 && level == IPPROTO_IPV6) || (ip_version() == IPVersion::IPv6 && level == IPPROTO_IP))
        return EINVAL;
    if (level != IPPROTO_IP && level != IPPROTO_IPV6)
        return Socket::setsockopt(level, option, user_value, user_value_size);

    MutexLocker locker(mutex());

    switch (option) {
    case IP_TTL: {
        if (user_value_size < sizeof(int))
            return EINVAL;
        int value;
        TRY(copy_from_user(&value, static_ptr_cast<int const*>(user_value)));
        if (value < 0 || value > 255)
            return EINVAL;
        m_ttl = value;
        return {};
    }
    case IP_TOS: {
        if (user_value_size < sizeof(int))
            return EINVAL;
        int value;
        TRY(copy_from_user(&value, static_ptr_cast<int const*>(user_value)));
        if (value < 0 || value > 255)
            return EINVAL;
        m_type_of_service = value;
        return {};
    }
    case IP_MULTICAST_LOOP: {
        if (user_value_size != 1)
            return EINVAL;
        u8 value;
        TRY(copy_from_user(&value, static_ptr_cast<u8 const*>(user_value)));
        if (value != 0 && value != 1)
            return EINVAL;
        m_multicast_loop = value == 0;
        return {};
    }
    case IP_ADD_MEMBERSHIP: {
        return m_delegate->add_multicast_membership(user_value, user_value_size);
    }
    case IP_DROP_MEMBERSHIP: {
        return m_delegate->remove_multicast_membership(user_value, user_value_size);
    }
    default:
        return ENOPROTOOPT;
    }
}

ErrorOr<void> IPv4Socket::add_multicast_membership(Userspace<void const*> user_value, socklen_t user_value_size)
{
    if (user_value_size != sizeof(ip_mreq))
        return EINVAL;
    ip_mreq mreq;
    TRY(copy_from_user(&mreq, static_ptr_cast<ip_mreq const*>(user_value)));
    if (mreq.imr_interface.s_addr != INADDR_ANY)
        return ENOTSUP;
    IPv4Address address { (u8 const*)&mreq.imr_multiaddr.s_addr };
    if (!m_multicast_memberships.contains_slow(address))
        m_multicast_memberships.append(address);
    return {};
}

ErrorOr<void> IPv4Socket::remove_multicast_membership(Userspace<void const*> user_value, socklen_t user_value_size)
{
    if (user_value_size != sizeof(ip_mreq))
        return EINVAL;
    ip_mreq mreq;
    TRY(copy_from_user(&mreq, static_ptr_cast<ip_mreq const*>(user_value)));
    if (mreq.imr_interface.s_addr != INADDR_ANY)
        return ENOTSUP;
    IPv4Address address { (u8 const*)&mreq.imr_multiaddr.s_addr };
    m_multicast_memberships.remove_first_matching([&address](auto& a) { return a == address; });
    return {};
}

ErrorOr<void> IPSocket::getsockopt(OpenFileDescription& description, int level, int option, Userspace<void*> value, Userspace<socklen_t*> value_size)
{
    if (level != IPPROTO_IP && level != IPPROTO_IPV6)
        return Socket::getsockopt(description, level, option, value, value_size);

    if ((ip_version() == IPVersion::IPv4 && level == IPPROTO_IPV6) || (ip_version() == IPVersion::IPv6 && level == IPPROTO_IP))
        return EINVAL;

    MutexLocker locker(mutex());

    socklen_t size;
    TRY(copy_from_user(&size, value_size.unsafe_userspace_ptr()));

    switch (option) {
    case IP_TTL: {
        if (size < sizeof(int))
            return EINVAL;
        int ttl = m_ttl;
        TRY(copy_to_user(static_ptr_cast<int*>(value), (int*)&ttl));
        size = sizeof(int);
        return copy_to_user(value_size, &size);
    }
    case IP_TOS: {
        if (size < sizeof(int))
            return EINVAL;
        int type_of_service = m_type_of_service;
        TRY(copy_to_user(static_ptr_cast<int*>(value), (int*)&type_of_service));
        size = sizeof(int);
        return copy_to_user(value_size, &size);
    }
    case IP_MULTICAST_LOOP: {
        if (size < 1)
            return EINVAL;
        TRY(copy_to_user(static_ptr_cast<u8*>(value), (u8 const*)&m_multicast_loop));
        size = 1;
        return copy_to_user(value_size, &size);
    }
    default:
        return ENOPROTOOPT;
    }
}

ErrorOr<void> IPSocket::ioctl(OpenFileDescription&, unsigned request, Userspace<void*> arg)
{
    TRY(Process::current().require_promise(Pledge::inet));

    MutexLocker locker(mutex());

    auto ioctl_route = [request, arg]() -> ErrorOr<void> {
        auto user_route = static_ptr_cast<rtentry*>(arg);
        rtentry route;
        TRY(copy_from_user(&route, user_route));

        Userspace<char const*> user_rt_dev((FlatPtr)route.rt_dev);
        auto ifname = TRY(Process::get_syscall_name_string_fixed_buffer<IFNAMSIZ>(user_rt_dev));
        auto adapter = NetworkingManagement::the().lookup_by_name(ifname.representable_view());
        if (!adapter)
            return ENODEV;

        // TODO: Support IPv6 in both of these once update_routing_table does.
        switch (request) {
        case SIOCADDRT: {
            auto current_process_credentials = Process::current().credentials();
            if (!current_process_credentials->is_superuser())
                return EPERM;
            if (route.rt_gateway.sa_family != AF_INET)
                return EAFNOSUPPORT;
            if (!(route.rt_flags & RTF_UP))
                return EINVAL; // FIXME: Find the correct value to return

            auto destination = IPv4Address(((sockaddr_in&)route.rt_dst).sin_addr.s_addr);
            auto gateway = IPv4Address(((sockaddr_in&)route.rt_gateway).sin_addr.s_addr);
            auto genmask = IPv4Address(((sockaddr_in&)route.rt_genmask).sin_addr.s_addr);

            return update_routing_table(destination, gateway, genmask, route.rt_flags, adapter, UpdateTable::Set);
        }
        case SIOCDELRT:
            auto current_process_credentials = Process::current().credentials();
            if (!current_process_credentials->is_superuser())
                return EPERM;
            if (route.rt_gateway.sa_family != AF_INET)
                return EAFNOSUPPORT;

            auto destination = IPv4Address(((sockaddr_in&)route.rt_dst).sin_addr.s_addr);
            auto gateway = IPv4Address(((sockaddr_in&)route.rt_gateway).sin_addr.s_addr);
            auto genmask = IPv4Address(((sockaddr_in&)route.rt_genmask).sin_addr.s_addr);

            return update_routing_table(destination, gateway, genmask, route.rt_flags, adapter, UpdateTable::Delete);
        }

        return EINVAL;
    };

    auto ioctl_arp = [request, arg]() -> ErrorOr<void> {
        auto user_req = static_ptr_cast<arpreq*>(arg);
        arpreq arp_req;
        TRY(copy_from_user(&arp_req, user_req));

        auto current_process_credentials = Process::current().credentials();

        switch (request) {
        case SIOCSARP:
            if (!current_process_credentials->is_superuser())
                return EPERM;
            if (arp_req.arp_pa.sa_family != AF_INET)
                return EAFNOSUPPORT;
            update_arp_table(IPv4Address(((sockaddr_in&)arp_req.arp_pa).sin_addr.s_addr), *(MACAddress*)&arp_req.arp_ha.sa_data[0], UpdateTable::Set);
            return {};

        case SIOCDARP:
            if (!current_process_credentials->is_superuser())
                return EPERM;
            if (arp_req.arp_pa.sa_family != AF_INET)
                return EAFNOSUPPORT;
            update_arp_table(IPv4Address(((sockaddr_in&)arp_req.arp_pa).sin_addr.s_addr), *(MACAddress*)&arp_req.arp_ha.sa_data[0], UpdateTable::Delete);
            return {};
        }

        return EINVAL;
    };

    auto ioctl_interface = [request, arg, this]() -> ErrorOr<void> {
        auto user_ifr = static_ptr_cast<ifreq*>(arg);
        ifreq ifr;
        TRY(copy_from_user(&ifr, user_ifr));

        if (request == SIOCGIFNAME) {
            // NOTE: Network devices are 1-indexed since index 0 denotes an invalid device
            if (ifr.ifr_index == 0)
                return EINVAL;

            size_t index = 1;
            Optional<StringView> result {};

            NetworkingManagement::the().for_each([&ifr, &index, &result](auto& adapter) {
                if (index == ifr.ifr_index)
                    result = adapter.name();
                ++index;
            });

            if (result.has_value()) {
                auto name = result.release_value();
                auto succ = name.copy_characters_to_buffer(ifr.ifr_name, IFNAMSIZ);
                if (!succ) {
                    return EFAULT;
                }
                return copy_to_user(user_ifr, &ifr);
            }

            return ENODEV;
        }

        FixedStringBuffer<IFNAMSIZ> namebuf;
        namebuf.store_characters({ ifr.ifr_name, IFNAMSIZ });

        if (request == SIOCGIFINDEX) {
            size_t index = 1;
            Optional<size_t> result {};

            NetworkingManagement::the().for_each([&namebuf, &index, &result](auto& adapter) {
                if (adapter.name() == namebuf.representable_view())
                    result = index;
                ++index;
            });

            if (result.has_value()) {
                ifr.ifr_index = result.release_value();
                return copy_to_user(user_ifr, &ifr);
            }

            return ENODEV;
        }

        auto adapter = NetworkingManagement::the().lookup_by_name(namebuf.representable_view());
        if (!adapter)
            return ENODEV;

        auto current_process_credentials = Process::current().credentials();

        switch (request) {
        case SIOCSIFADDR:
            if (!current_process_credentials->is_superuser())
                return EPERM;
            if (ifr.ifr_addr.sa_family == AF_INET) {
                adapter->set_ipv4_address(IPv4Address(bit_cast<sockaddr_in*>(&ifr.ifr_addr)->sin_addr.s_addr));
                return {};
            } else if (ifr.ifr_addr.sa_family == AF_INET6) {
                adapter->set_ipv6_address(IPv6Address(bit_cast<sockaddr_in6*>(&ifr.ifr_addr)->sin6_addr.s6_addr));
                return {};
            } else {
                return EAFNOSUPPORT;
            }

        case SIOCSIFNETMASK:
            if (!current_process_credentials->is_superuser())
                return EPERM;
            // TODO: Support IPv6.
            if (ifr.ifr_addr.sa_family != AF_INET)
                return EAFNOSUPPORT;
            adapter->set_ipv4_netmask(IPv4Address(bit_cast<sockaddr_in*>(&ifr.ifr_netmask)->sin_addr.s_addr));
            return {};

        case SIOCGIFADDR: {
            // TODO: Support IPv6 (use it if the socket is IPv6, or copy Linux’s behavior).
            auto ip4_addr = adapter->ipv4_address().to_u32();
            auto& socket_address_in = reinterpret_cast<sockaddr_in&>(ifr.ifr_addr);
            socket_address_in.sin_family = AF_INET;
            socket_address_in.sin_addr.s_addr = ip4_addr;
            return copy_to_user(user_ifr, &ifr);
        }

        case SIOCGIFNETMASK: {
            auto ip4_netmask = adapter->ipv4_netmask().to_u32();
            auto& socket_address_in = reinterpret_cast<sockaddr_in&>(ifr.ifr_addr);
            socket_address_in.sin_family = AF_INET;
            // NOTE: NOT ifr_netmask.
            socket_address_in.sin_addr.s_addr = ip4_netmask;

            return copy_to_user(user_ifr, &ifr);
        }

        case SIOCGIFHWADDR: {
            auto mac_address = adapter->mac_address();
            switch (adapter->adapter_type()) {
            case NetworkAdapter::Type::Loopback:
                ifr.ifr_hwaddr.sa_family = ARPHRD_LOOPBACK;
                break;
            case NetworkAdapter::Type::Ethernet:
                ifr.ifr_hwaddr.sa_family = ARPHRD_ETHER;
                break;
            default:
                VERIFY_NOT_REACHED();
            }
            mac_address.copy_to(Bytes { ifr.ifr_hwaddr.sa_data, sizeof(ifr.ifr_hwaddr.sa_data) });
            return copy_to_user(user_ifr, &ifr);
        }

        // TODO: Support IPv6.
        case SIOCGIFBRDADDR: {
            // Broadcast address is basically the reverse of the netmask, i.e.
            // instead of zeroing out the end, you OR with 1 instead.
            auto ip4_netmask = adapter->ipv4_netmask().to_u32();
            auto broadcast_addr = adapter->ipv4_address().to_u32() | ~ip4_netmask;
            auto& socket_address_in = reinterpret_cast<sockaddr_in&>(ifr.ifr_addr);
            socket_address_in.sin_family = AF_INET;
            socket_address_in.sin_addr.s_addr = broadcast_addr;
            return copy_to_user(user_ifr, &ifr);
        }

        case SIOCGIFMTU: {
            auto mtu = adapter->mtu();

            ifr.ifr_addr.sa_family = domain();
            ifr.ifr_metric = mtu;
            return copy_to_user(user_ifr, &ifr);
        }

        case SIOCGIFFLAGS: {
            // FIXME: stub!
            constexpr short flags = 1;
            ifr.ifr_addr.sa_family = AF_INET;
            ifr.ifr_flags = flags;
            return copy_to_user(user_ifr, &ifr);
        }

        case SIOCGIFCONF: {
            // FIXME: stub!
            return EINVAL;
        }
        }

        return EINVAL;
    };

    switch (request) {
    case SIOCSIFADDR:
    case SIOCSIFNETMASK:
    case SIOCGIFADDR:
    case SIOCGIFHWADDR:
    case SIOCGIFNETMASK:
    case SIOCGIFBRDADDR:
    case SIOCGIFMTU:
    case SIOCGIFFLAGS:
    case SIOCGIFCONF:
    case SIOCGIFNAME:
    case SIOCGIFINDEX:
        return ioctl_interface();

    case SIOCADDRT:
    case SIOCDELRT:
        return ioctl_route();

    case SIOCSARP:
    case SIOCDARP:
        return ioctl_arp();

    case FIONREAD: {
        int readable = 0;
        if (buffer_mode() == BufferMode::Bytes) {
            readable = static_cast<int>(m_receive_buffer->immediately_readable());
        } else {
            if (m_receive_queue.size() != 0u) {
                readable = static_cast<int>(TRY(protocol_size(m_receive_queue.first().data->bytes())));
            }
        }

        return copy_to_user(static_ptr_cast<int*>(arg), &readable);
    }
    }

    return EINVAL;
}

ErrorOr<void> IPSocket::close()
{
    [[maybe_unused]] auto rc = shutdown(SHUT_RDWR);
    return {};
}

void IPSocket::shut_down_for_reading()
{
    Socket::shut_down_for_reading();
    set_can_read(true);
}

void IPSocket::set_can_read(bool value)
{
    m_can_read = value;
    if (value)
        evaluate_block_conditions();
}

void IPSocket::drop_receive_buffer()
{
    m_receive_buffer = nullptr;
}

}
