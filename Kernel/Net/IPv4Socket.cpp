/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <AK/StringBuilder.h>
#include <Kernel/API/POSIX/errno.h>
#include <Kernel/Debug.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/Net/ARP.h>
#include <Kernel/Net/ICMP.h>
#include <Kernel/Net/IPv4.h>
#include <Kernel/Net/IPv4Socket.h>
#include <Kernel/Net/NetworkAdapter.h>
#include <Kernel/Net/NetworkingManagement.h>
#include <Kernel/Net/Routing.h>
#include <Kernel/Net/TCP.h>
#include <Kernel/Net/TCPSocket.h>
#include <Kernel/Net/UDP.h>
#include <Kernel/Net/UDPSocket.h>
#include <Kernel/Process.h>
#include <Kernel/UnixTypes.h>
#include <LibC/sys/ioctl_numbers.h>

namespace Kernel {

static Singleton<MutexProtected<IPv4Socket::List>> s_all_sockets;

using BlockFlags = Thread::OpenFileDescriptionBlocker::BlockFlags;

MutexProtected<IPv4Socket::List>& IPv4Socket::all_sockets()
{
    return *s_all_sockets;
}

ErrorOr<NonnullOwnPtr<DoubleBuffer>> IPv4Socket::try_create_receive_buffer()
{
    return DoubleBuffer::try_create(256 * KiB);
}

ErrorOr<NonnullRefPtr<Socket>> IPv4Socket::create(int type, int protocol)
{
    auto receive_buffer = TRY(IPv4Socket::try_create_receive_buffer());

    if (type == SOCK_STREAM)
        return TRY(TCPSocket::try_create(protocol, move(receive_buffer)));
    if (type == SOCK_DGRAM)
        return TRY(UDPSocket::try_create(protocol, move(receive_buffer)));
    if (type == SOCK_RAW) {
        auto raw_socket = adopt_ref_if_nonnull(new (nothrow) IPv4Socket(type, protocol, move(receive_buffer), {}));
        if (raw_socket)
            return raw_socket.release_nonnull();
        return ENOMEM;
    }
    return EINVAL;
}

IPv4Socket::IPv4Socket(int type, int protocol, NonnullOwnPtr<DoubleBuffer> receive_buffer, OwnPtr<KBuffer> optional_scratch_buffer)
    : Socket(AF_INET, type, protocol)
    , m_receive_buffer(move(receive_buffer))
    , m_scratch_buffer(move(optional_scratch_buffer))
{
    dbgln_if(IPV4_SOCKET_DEBUG, "IPv4Socket({}) created with type={}, protocol={}", this, type, protocol);
    m_buffer_mode = type == SOCK_STREAM ? BufferMode::Bytes : BufferMode::Packets;
    if (m_buffer_mode == BufferMode::Bytes) {
        VERIFY(m_scratch_buffer);
    }

    all_sockets().with_exclusive([&](auto& table) {
        table.append(*this);
    });
}

IPv4Socket::~IPv4Socket()
{
    all_sockets().with_exclusive([&](auto& table) {
        table.remove(*this);
    });
}

void IPv4Socket::get_local_address(sockaddr* address, socklen_t* address_size)
{
    sockaddr_in local_address = { AF_INET, htons(m_local_port), { m_local_address.to_in_addr_t() }, { 0 } };
    memcpy(address, &local_address, min(static_cast<size_t>(*address_size), sizeof(sockaddr_in)));
    *address_size = sizeof(sockaddr_in);
}

void IPv4Socket::get_peer_address(sockaddr* address, socklen_t* address_size)
{
    sockaddr_in peer_address = { AF_INET, htons(m_peer_port), { m_peer_address.to_in_addr_t() }, { 0 } };
    memcpy(address, &peer_address, min(static_cast<size_t>(*address_size), sizeof(sockaddr_in)));
    *address_size = sizeof(sockaddr_in);
}

ErrorOr<void> IPv4Socket::bind(Userspace<const sockaddr*> user_address, socklen_t address_size)
{
    VERIFY(setup_state() == SetupState::Unstarted);
    if (address_size != sizeof(sockaddr_in))
        return set_so_error(EINVAL);

    sockaddr_in address {};
    SOCKET_TRY(copy_from_user(&address, user_address, sizeof(sockaddr_in)));

    if (address.sin_family != AF_INET)
        return set_so_error(EINVAL);

    auto requested_local_port = ntohs(address.sin_port);
    if (!Process::current().is_superuser()) {
        if (requested_local_port > 0 && requested_local_port < 1024) {
            dbgln("UID {} attempted to bind {} to port {}", Process::current().uid(), class_name(), requested_local_port);
            return set_so_error(EACCES);
        }
    }

    m_local_address = IPv4Address((const u8*)&address.sin_addr.s_addr);
    m_local_port = requested_local_port;

    dbgln_if(IPV4_SOCKET_DEBUG, "IPv4Socket::bind {}({}) to {}:{}", class_name(), this, m_local_address, m_local_port);

    return protocol_bind();
}

ErrorOr<void> IPv4Socket::listen(size_t backlog)
{
    MutexLocker locker(mutex());
    auto result = allocate_local_port_if_needed();
    if (result.error_or_port.is_error() && result.error_or_port.error().code() != ENOPROTOOPT)
        return result.error_or_port.release_error();

    set_backlog(backlog);
    set_role(Role::Listener);
    evaluate_block_conditions();

    dbgln_if(IPV4_SOCKET_DEBUG, "IPv4Socket({}) listening with backlog={}", this, backlog);

    return protocol_listen(result.did_allocate);
}

ErrorOr<void> IPv4Socket::connect(OpenFileDescription& description, Userspace<const sockaddr*> address, socklen_t address_size, ShouldBlock should_block)
{
    if (address_size != sizeof(sockaddr_in))
        return set_so_error(EINVAL);
    u16 sa_family_copy;
    auto* user_address = reinterpret_cast<const sockaddr*>(address.unsafe_userspace_ptr());
    SOCKET_TRY(copy_from_user(&sa_family_copy, &user_address->sa_family, sizeof(u16)));
    if (sa_family_copy != AF_INET)
        return set_so_error(EINVAL);
    if (m_role == Role::Connected)
        return set_so_error(EISCONN);

    sockaddr_in safe_address {};
    SOCKET_TRY(copy_from_user(&safe_address, (sockaddr_in const*)user_address, sizeof(sockaddr_in)));

    m_peer_address = IPv4Address((const u8*)&safe_address.sin_addr.s_addr);
    if (m_peer_address == IPv4Address { 0, 0, 0, 0 })
        m_peer_address = IPv4Address { 127, 0, 0, 1 };
    m_peer_port = ntohs(safe_address.sin_port);

    return protocol_connect(description, should_block);
}

bool IPv4Socket::can_read(const OpenFileDescription&, u64) const
{
    if (m_role == Role::Listener)
        return can_accept();
    if (protocol_is_disconnected())
        return true;
    return m_can_read;
}

bool IPv4Socket::can_write(const OpenFileDescription&, u64) const
{
    return true;
}

PortAllocationResult IPv4Socket::allocate_local_port_if_needed()
{
    MutexLocker locker(mutex());
    if (m_local_port)
        return { m_local_port, false };
    auto port_or_error = protocol_allocate_local_port();
    if (port_or_error.is_error())
        return { port_or_error.release_error(), false };
    m_local_port = port_or_error.release_value();
    return { m_local_port, true };
}

ErrorOr<size_t> IPv4Socket::sendto(OpenFileDescription&, const UserOrKernelBuffer& data, size_t data_length, [[maybe_unused]] int flags, Userspace<const sockaddr*> addr, socklen_t addr_length)
{
    MutexLocker locker(mutex());

    if (addr && addr_length != sizeof(sockaddr_in))
        return set_so_error(EINVAL);

    if (addr) {
        sockaddr_in ia {};
        SOCKET_TRY(copy_from_user(&ia, Userspace<const sockaddr_in*>(addr.ptr())));

        if (ia.sin_family != AF_INET) {
            dmesgln("sendto: Bad address family: {} is not AF_INET", ia.sin_family);
            return set_so_error(EAFNOSUPPORT);
        }

        m_peer_address = IPv4Address((const u8*)&ia.sin_addr.s_addr);
        m_peer_port = ntohs(ia.sin_port);
    }

    if (!is_connected() && m_peer_address.is_zero())
        return set_so_error(EPIPE);

    auto allow_using_gateway = ((flags & MSG_DONTROUTE) || m_routing_disabled) ? AllowUsingGateway::No : AllowUsingGateway::Yes;
    auto routing_decision = route_to(m_peer_address, m_local_address, bound_interface(), allow_using_gateway);
    if (routing_decision.is_zero())
        return set_so_error(EHOSTUNREACH);

    if (m_local_address.to_u32() == 0)
        m_local_address = routing_decision.adapter->ipv4_address();

    if (auto result = allocate_local_port_if_needed(); result.error_or_port.is_error() && result.error_or_port.error().code() != ENOPROTOOPT)
        return result.error_or_port.release_error();

    dbgln_if(IPV4_SOCKET_DEBUG, "sendto: destination={}:{}", m_peer_address, m_peer_port);

    if (type() == SOCK_RAW) {
        auto ipv4_payload_offset = routing_decision.adapter->ipv4_payload_offset();
        data_length = min(data_length, routing_decision.adapter->mtu() - ipv4_payload_offset);
        auto packet = routing_decision.adapter->acquire_packet_buffer(ipv4_payload_offset + data_length);
        if (!packet)
            return set_so_error(ENOMEM);
        routing_decision.adapter->fill_in_ipv4_header(*packet, local_address(), routing_decision.next_hop,
            m_peer_address, (IPv4Protocol)protocol(), data_length, m_type_of_service, m_ttl);
        if (auto result = data.read(packet->buffer->data() + ipv4_payload_offset, data_length); result.is_error()) {
            routing_decision.adapter->release_packet_buffer(*packet);
            return set_so_error(result.release_error());
        }
        routing_decision.adapter->send_packet(packet->bytes());
        routing_decision.adapter->release_packet_buffer(*packet);
        return data_length;
    }

    auto nsent_or_error = protocol_send(data, data_length);
    if (!nsent_or_error.is_error())
        Thread::current()->did_ipv4_socket_write(nsent_or_error.value());
    return nsent_or_error;
}

ErrorOr<size_t> IPv4Socket::receive_byte_buffered(OpenFileDescription& description, UserOrKernelBuffer& buffer, size_t buffer_length, int flags, Userspace<sockaddr*>, Userspace<socklen_t*>)
{
    MutexLocker locker(mutex());

    VERIFY(m_receive_buffer);

    if (m_receive_buffer->is_empty()) {
        if (protocol_is_disconnected())
            return 0;
        if (!description.is_blocking())
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

ErrorOr<size_t> IPv4Socket::receive_packet_buffered(OpenFileDescription& description, UserOrKernelBuffer& buffer, size_t buffer_length, int flags, Userspace<sockaddr*> addr, Userspace<socklen_t*> addr_length, Time& packet_timestamp)
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
            if (!description.is_blocking())
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

            dbgln_if(IPV4_SOCKET_DEBUG, "IPv4Socket({}): recvfrom without blocking {} bytes, packets in queue: {}",
                this,
                packet->data->size(),
                m_receive_queue.size());
        }
    }

    if (!packet) {
        if (protocol_is_disconnected()) {
            dbgln("IPv4Socket({}) is protocol-disconnected, returning 0 in recvfrom!", this);
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

        dbgln_if(IPV4_SOCKET_DEBUG, "IPv4Socket({}): recvfrom with blocking {} bytes, packets in queue: {}",
            this,
            packet->data->size(),
            m_receive_queue.size());
    }
    VERIFY(packet->data);

    packet_timestamp = packet->timestamp;

    if (addr) {
        dbgln_if(IPV4_SOCKET_DEBUG, "Incoming packet is from: {}:{}", packet->peer_address, packet->peer_port);

        sockaddr_in out_addr {};
        memcpy(&out_addr.sin_addr, &packet->peer_address, sizeof(IPv4Address));
        out_addr.sin_port = htons(packet->peer_port);
        out_addr.sin_family = AF_INET;
        Userspace<sockaddr_in*> dest_addr = addr.ptr();
        SOCKET_TRY(copy_to_user(dest_addr, &out_addr));

        socklen_t out_length = sizeof(sockaddr_in);
        VERIFY(addr_length);
        SOCKET_TRY(copy_to_user(addr_length, &out_length));
    }

    if (type() == SOCK_RAW) {
        size_t bytes_written = min(packet->data->size(), buffer_length);
        SOCKET_TRY(buffer.write(packet->data->data(), bytes_written));
        return bytes_written;
    }

    return protocol_receive(packet->data->bytes(), buffer, buffer_length, flags);
}

ErrorOr<size_t> IPv4Socket::recvfrom(OpenFileDescription& description, UserOrKernelBuffer& buffer, size_t buffer_length, int flags, Userspace<sockaddr*> user_addr, Userspace<socklen_t*> user_addr_length, Time& packet_timestamp)
{
    if (user_addr_length) {
        socklen_t addr_length;
        SOCKET_TRY(copy_from_user(&addr_length, user_addr_length.unsafe_userspace_ptr()));
        if (addr_length < sizeof(sockaddr_in))
            return set_so_error(EINVAL);
    }

    dbgln_if(IPV4_SOCKET_DEBUG, "recvfrom: type={}, local_port={}", type(), local_port());

    ErrorOr<size_t> total_nreceived = 0;
    do {
        auto offset_buffer = buffer.offset(total_nreceived.value());
        auto offset_buffer_length = buffer_length - total_nreceived.value();

        ErrorOr<size_t> nreceived = 0;
        if (buffer_mode() == BufferMode::Bytes)
            nreceived = receive_byte_buffered(description, offset_buffer, offset_buffer_length, flags, user_addr, user_addr_length);
        else
            nreceived = receive_packet_buffered(description, offset_buffer, offset_buffer_length, flags, user_addr, user_addr_length, packet_timestamp);

        if (nreceived.is_error())
            total_nreceived = nreceived;
        else
            total_nreceived.value() += nreceived.value();
    } while ((flags & MSG_WAITALL) && !total_nreceived.is_error() && total_nreceived.value() < buffer_length);

    if (!total_nreceived.is_error())
        Thread::current()->did_ipv4_socket_read(total_nreceived.value());
    return total_nreceived;
}

bool IPv4Socket::did_receive(const IPv4Address& source_address, u16 source_port, ReadonlyBytes packet, const Time& packet_timestamp)
{
    MutexLocker locker(mutex());

    if (is_shut_down_for_reading())
        return false;

    auto packet_size = packet.size();

    if (buffer_mode() == BufferMode::Bytes) {
        VERIFY(m_receive_buffer);

        size_t space_in_receive_buffer = m_receive_buffer->space_for_writing();
        if (packet_size > space_in_receive_buffer) {
            dbgln("IPv4Socket({}): did_receive refusing packet since buffer is full.", this);
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
        if (m_receive_queue.size() > 2000) {
            dbgln("IPv4Socket({}): did_receive refusing packet since queue is full.", this);
            return false;
        }
        auto data_or_error = KBuffer::try_create_with_bytes(packet);
        if (data_or_error.is_error()) {
            dbgln("IPv4Socket: did_receive unable to allocate storage for incoming packet.");
            return false;
        }
        m_receive_queue.append({ source_address, source_port, packet_timestamp, data_or_error.release_value() });
        set_can_read(true);
    }
    m_bytes_received += packet_size;

    if constexpr (IPV4_SOCKET_DEBUG) {
        if (buffer_mode() == BufferMode::Bytes)
            dbgln("IPv4Socket({}): did_receive {} bytes, total_received={}", this, packet_size, m_bytes_received);
        else
            dbgln("IPv4Socket({}): did_receive {} bytes, total_received={}, packets in queue: {}",
                this,
                packet_size,
                m_bytes_received,
                m_receive_queue.size());
    }

    return true;
}

ErrorOr<NonnullOwnPtr<KString>> IPv4Socket::pseudo_path(const OpenFileDescription&) const
{
    if (m_role == Role::None)
        return KString::try_create("socket"sv);

    StringBuilder builder;
    TRY(builder.try_append("socket:"));

    TRY(builder.try_appendff("{}:{}", m_local_address.to_string(), m_local_port));
    if (m_role == Role::Accepted || m_role == Role::Connected)
        TRY(builder.try_appendff(" / {}:{}", m_peer_address.to_string(), m_peer_port));

    switch (m_role) {
    case Role::Listener:
        TRY(builder.try_append(" (listening)"));
        break;
    case Role::Accepted:
        TRY(builder.try_append(" (accepted)"));
        break;
    case Role::Connected:
        TRY(builder.try_append(" (connected)"));
        break;
    case Role::Connecting:
        TRY(builder.try_append(" (connecting)"));
        break;
    default:
        VERIFY_NOT_REACHED();
    }

    return KString::try_create(builder.string_view());
}

ErrorOr<void> IPv4Socket::setsockopt(int level, int option, Userspace<const void*> user_value, socklen_t user_value_size)
{
    if (level != IPPROTO_IP)
        return Socket::setsockopt(level, option, user_value, user_value_size);

    MutexLocker locker(mutex());

    switch (option) {
    case IP_TTL: {
        if (user_value_size < sizeof(int))
            return EINVAL;
        int value;
        TRY(copy_from_user(&value, static_ptr_cast<const int*>(user_value)));
        if (value < 0 || value > 255)
            return EINVAL;
        m_ttl = value;
        return {};
    }
    case IP_TOS: {
        if (user_value_size < sizeof(int))
            return EINVAL;
        int value;
        TRY(copy_from_user(&value, static_ptr_cast<const int*>(user_value)));
        if (value < 0 || value > 255)
            return EINVAL;
        m_type_of_service = value;
        return {};
    }
    case IP_MULTICAST_LOOP: {
        if (user_value_size != 1)
            return EINVAL;
        u8 value;
        TRY(copy_from_user(&value, static_ptr_cast<const u8*>(user_value)));
        if (value != 0 && value != 1)
            return EINVAL;
        m_multicast_loop = value;
        return {};
    }
    case IP_ADD_MEMBERSHIP: {
        if (user_value_size != sizeof(ip_mreq))
            return EINVAL;
        ip_mreq mreq;
        TRY(copy_from_user(&mreq, static_ptr_cast<const ip_mreq*>(user_value)));
        if (mreq.imr_interface.s_addr != INADDR_ANY)
            return ENOTSUP;
        IPv4Address address { (const u8*)&mreq.imr_multiaddr.s_addr };
        if (!m_multicast_memberships.contains_slow(address))
            m_multicast_memberships.append(address);
        return {};
    }
    case IP_DROP_MEMBERSHIP: {
        if (user_value_size != sizeof(ip_mreq))
            return EINVAL;
        ip_mreq mreq;
        TRY(copy_from_user(&mreq, static_ptr_cast<const ip_mreq*>(user_value)));
        if (mreq.imr_interface.s_addr != INADDR_ANY)
            return ENOTSUP;
        IPv4Address address { (const u8*)&mreq.imr_multiaddr.s_addr };
        m_multicast_memberships.remove_first_matching([&address](auto& a) { return a == address; });
        return {};
    }
    default:
        return ENOPROTOOPT;
    }
}

ErrorOr<void> IPv4Socket::getsockopt(OpenFileDescription& description, int level, int option, Userspace<void*> value, Userspace<socklen_t*> value_size)
{
    if (level != IPPROTO_IP)
        return Socket::getsockopt(description, level, option, value, value_size);

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
        TRY(copy_to_user(static_ptr_cast<u8*>(value), (const u8*)&m_multicast_loop));
        size = 1;
        return copy_to_user(value_size, &size);
    }
    default:
        return ENOPROTOOPT;
    }
}

ErrorOr<void> IPv4Socket::ioctl(OpenFileDescription&, unsigned request, Userspace<void*> arg)
{
    TRY(Process::current().require_promise(Pledge::inet));

    auto ioctl_route = [request, arg]() -> ErrorOr<void> {
        auto user_route = static_ptr_cast<rtentry*>(arg);
        rtentry route;
        TRY(copy_from_user(&route, user_route));

        Userspace<const char*> user_rt_dev((FlatPtr)route.rt_dev);
        auto ifname = TRY(try_copy_kstring_from_user(user_rt_dev, IFNAMSIZ));

        auto adapter = NetworkingManagement::the().lookup_by_name(ifname->view());
        if (!adapter)
            return ENODEV;

        switch (request) {
        case SIOCADDRT:
            if (!Process::current().is_superuser())
                return EPERM;
            if (route.rt_gateway.sa_family != AF_INET)
                return EAFNOSUPPORT;
            if ((route.rt_flags & (RTF_UP | RTF_GATEWAY)) != (RTF_UP | RTF_GATEWAY))
                return EINVAL; // FIXME: Find the correct value to return
            adapter->set_ipv4_gateway(IPv4Address(((sockaddr_in&)route.rt_gateway).sin_addr.s_addr));
            return {};

        case SIOCDELRT:
            // FIXME: Support gateway deletion
            return {};
        }

        return EINVAL;
    };

    auto ioctl_arp = [request, arg]() -> ErrorOr<void> {
        auto user_req = static_ptr_cast<arpreq*>(arg);
        arpreq arp_req;
        TRY(copy_from_user(&arp_req, user_req));

        switch (request) {
        case SIOCSARP:
            if (!Process::current().is_superuser())
                return EPERM;
            if (arp_req.arp_pa.sa_family != AF_INET)
                return EAFNOSUPPORT;
            update_arp_table(IPv4Address(((sockaddr_in&)arp_req.arp_pa).sin_addr.s_addr), *(MACAddress*)&arp_req.arp_ha.sa_data[0], UpdateArp::Set);
            return {};

        case SIOCDARP:
            if (!Process::current().is_superuser())
                return EPERM;
            if (arp_req.arp_pa.sa_family != AF_INET)
                return EAFNOSUPPORT;
            update_arp_table(IPv4Address(((sockaddr_in&)arp_req.arp_pa).sin_addr.s_addr), *(MACAddress*)&arp_req.arp_ha.sa_data[0], UpdateArp::Delete);
            return {};
        }

        return EINVAL;
    };

    auto ioctl_interface = [request, arg]() -> ErrorOr<void> {
        auto user_ifr = static_ptr_cast<ifreq*>(arg);
        ifreq ifr;
        TRY(copy_from_user(&ifr, user_ifr));

        char namebuf[IFNAMSIZ + 1];
        memcpy(namebuf, ifr.ifr_name, IFNAMSIZ);
        namebuf[sizeof(namebuf) - 1] = '\0';

        auto adapter = NetworkingManagement::the().lookup_by_name(namebuf);
        if (!adapter)
            return ENODEV;

        switch (request) {
        case SIOCSIFADDR:
            if (!Process::current().is_superuser())
                return EPERM;
            if (ifr.ifr_addr.sa_family != AF_INET)
                return EAFNOSUPPORT;
            adapter->set_ipv4_address(IPv4Address(((sockaddr_in&)ifr.ifr_addr).sin_addr.s_addr));
            return {};

        case SIOCSIFNETMASK:
            if (!Process::current().is_superuser())
                return EPERM;
            if (ifr.ifr_addr.sa_family != AF_INET)
                return EAFNOSUPPORT;
            adapter->set_ipv4_netmask(IPv4Address(((sockaddr_in&)ifr.ifr_netmask).sin_addr.s_addr));
            return {};

        case SIOCGIFADDR: {
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
            ifr.ifr_hwaddr.sa_family = ARPHRD_ETHER; // FIXME: Query the underlying network interface for it's type
            mac_address.copy_to(Bytes { ifr.ifr_hwaddr.sa_data, sizeof(ifr.ifr_hwaddr.sa_data) });
            return copy_to_user(user_ifr, &ifr);
        }

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
            auto ip4_metric = adapter->mtu();

            ifr.ifr_addr.sa_family = AF_INET;
            ifr.ifr_metric = ip4_metric;
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

ErrorOr<void> IPv4Socket::close()
{
    [[maybe_unused]] auto rc = shutdown(SHUT_RDWR);
    return {};
}

void IPv4Socket::shut_down_for_reading()
{
    Socket::shut_down_for_reading();
    set_can_read(true);
}

void IPv4Socket::set_can_read(bool value)
{
    m_can_read = value;
    if (value)
        evaluate_block_conditions();
}

void IPv4Socket::drop_receive_buffer()
{
    m_receive_buffer = nullptr;
}

}
