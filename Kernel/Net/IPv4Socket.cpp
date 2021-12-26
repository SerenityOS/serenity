/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <AK/StringBuilder.h>
#include <Kernel/Debug.h>
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/Net/ARP.h>
#include <Kernel/Net/ICMP.h>
#include <Kernel/Net/IPv4.h>
#include <Kernel/Net/IPv4Socket.h>
#include <Kernel/Net/NetworkAdapter.h>
#include <Kernel/Net/Routing.h>
#include <Kernel/Net/TCP.h>
#include <Kernel/Net/TCPSocket.h>
#include <Kernel/Net/UDP.h>
#include <Kernel/Net/UDPSocket.h>
#include <Kernel/Process.h>
#include <Kernel/UnixTypes.h>
#include <LibC/errno_numbers.h>
#include <LibC/sys/ioctl_numbers.h>

namespace Kernel {

static AK::Singleton<Lockable<HashTable<IPv4Socket*>>> s_table;

using BlockFlags = Thread::FileDescriptionBlocker::BlockFlags;

Lockable<HashTable<IPv4Socket*>>& IPv4Socket::all_sockets()
{
    return *s_table;
}

KResultOr<NonnullRefPtr<Socket>> IPv4Socket::create(int type, int protocol)
{
    if (type == SOCK_STREAM) {
        auto tcp_socket = TCPSocket::create(protocol);
        if (tcp_socket.is_error())
            return tcp_socket.error();
        return tcp_socket.release_value();
    }
    if (type == SOCK_DGRAM) {
        auto udp_socket = UDPSocket::create(protocol);
        if (udp_socket.is_error())
            return udp_socket.error();
        return udp_socket.release_value();
    }
    if (type == SOCK_RAW) {
        auto raw_socket = adopt_ref_if_nonnull(new IPv4Socket(type, protocol));
        if (raw_socket)
            return raw_socket.release_nonnull();
        return ENOMEM;
    }
    return EINVAL;
}

IPv4Socket::IPv4Socket(int type, int protocol)
    : Socket(AF_INET, type, protocol)
{
    dbgln_if(IPV4_SOCKET_DEBUG, "IPv4Socket({}) created with type={}, protocol={}", this, type, protocol);
    m_buffer_mode = type == SOCK_STREAM ? BufferMode::Bytes : BufferMode::Packets;
    if (m_buffer_mode == BufferMode::Bytes) {
        m_scratch_buffer = KBuffer::create_with_size(65536);
    }
    Locker locker(all_sockets().lock());
    all_sockets().resource().set(this);
}

IPv4Socket::~IPv4Socket()
{
    Locker locker(all_sockets().lock());
    all_sockets().resource().remove(this);
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

KResult IPv4Socket::bind(Userspace<const sockaddr*> user_address, socklen_t address_size)
{
    VERIFY(setup_state() == SetupState::Unstarted);
    if (address_size != sizeof(sockaddr_in))
        return EINVAL;

    sockaddr_in address;
    if (!copy_from_user(&address, user_address, sizeof(sockaddr_in)))
        return EFAULT;

    if (address.sin_family != AF_INET)
        return EINVAL;

    auto requested_local_port = ntohs(address.sin_port);
    if (!Process::current()->is_superuser()) {
        if (requested_local_port > 0 && requested_local_port < 1024) {
            dbgln("UID {} attempted to bind {} to port {}", Process::current()->uid(), class_name(), requested_local_port);
            return EACCES;
        }
    }

    m_local_address = IPv4Address((const u8*)&address.sin_addr.s_addr);
    m_local_port = requested_local_port;

    dbgln_if(IPV4_SOCKET_DEBUG, "IPv4Socket::bind {}({}) to {}:{}", class_name(), this, m_local_address, m_local_port);

    return protocol_bind();
}

KResult IPv4Socket::listen(size_t backlog)
{
    Locker locker(lock());
    auto result = allocate_local_port_if_needed();
    if (result.error_or_port.is_error() && result.error_or_port.error() != -ENOPROTOOPT)
        return result.error_or_port.error();

    set_backlog(backlog);
    m_role = Role::Listener;
    evaluate_block_conditions();

    dbgln_if(IPV4_SOCKET_DEBUG, "IPv4Socket({}) listening with backlog={}", this, backlog);

    return protocol_listen(result.did_allocate);
}

KResult IPv4Socket::connect(FileDescription& description, Userspace<const sockaddr*> address, socklen_t address_size, ShouldBlock should_block)
{
    if (address_size != sizeof(sockaddr_in))
        return EINVAL;
    u16 sa_family_copy;
    auto* user_address = reinterpret_cast<const sockaddr*>(address.unsafe_userspace_ptr());
    if (!copy_from_user(&sa_family_copy, &user_address->sa_family, sizeof(u16)))
        return EFAULT;
    if (sa_family_copy != AF_INET)
        return EINVAL;
    if (m_role == Role::Connected)
        return EISCONN;

    sockaddr_in safe_address;
    if (!copy_from_user(&safe_address, (const sockaddr_in*)user_address, sizeof(sockaddr_in)))
        return EFAULT;

    m_peer_address = IPv4Address((const u8*)&safe_address.sin_addr.s_addr);
    if (m_peer_address == IPv4Address { 0, 0, 0, 0 })
        m_peer_address = IPv4Address { 127, 0, 0, 1 };
    m_peer_port = ntohs(safe_address.sin_port);

    return protocol_connect(description, should_block);
}

bool IPv4Socket::can_read(const FileDescription&, size_t) const
{
    if (m_role == Role::Listener)
        return can_accept();
    if (protocol_is_disconnected())
        return true;
    return m_can_read;
}

bool IPv4Socket::can_write(const FileDescription&, size_t) const
{
    return true;
}

PortAllocationResult IPv4Socket::allocate_local_port_if_needed()
{
    Locker locker(lock());
    if (m_local_port)
        return { m_local_port, false };
    auto port_or_error = protocol_allocate_local_port();
    if (port_or_error.is_error())
        return { port_or_error.error(), false };
    m_local_port = port_or_error.value();
    return { m_local_port, true };
}

KResultOr<size_t> IPv4Socket::sendto(FileDescription&, const UserOrKernelBuffer& data, size_t data_length, [[maybe_unused]] int flags, Userspace<const sockaddr*> addr, socklen_t addr_length)
{
    Locker locker(lock());

    if (addr && addr_length != sizeof(sockaddr_in))
        return EINVAL;

    if (addr) {
        sockaddr_in ia;
        if (!copy_from_user(&ia, Userspace<const sockaddr_in*>(addr.ptr())))
            return EFAULT;

        if (ia.sin_family != AF_INET) {
            dmesgln("sendto: Bad address family: {} is not AF_INET", ia.sin_family);
            return EAFNOSUPPORT;
        }

        m_peer_address = IPv4Address((const u8*)&ia.sin_addr.s_addr);
        m_peer_port = ntohs(ia.sin_port);
    }

    if (!is_connected() && m_peer_address.is_zero())
        return EPIPE;

    auto routing_decision = route_to(m_peer_address, m_local_address, bound_interface());
    if (routing_decision.is_zero())
        return EHOSTUNREACH;

    if (m_local_address.to_u32() == 0)
        m_local_address = routing_decision.adapter->ipv4_address();

    if (auto result = allocate_local_port_if_needed(); result.error_or_port.is_error() && result.error_or_port.error() != -ENOPROTOOPT)
        return result.error_or_port.error();

    dbgln_if(IPV4_SOCKET_DEBUG, "sendto: destination={}:{}", m_peer_address, m_peer_port);

    if (type() == SOCK_RAW) {
        auto ipv4_payload_offset = routing_decision.adapter->ipv4_payload_offset();
        data_length = min(data_length, routing_decision.adapter->mtu() - ipv4_payload_offset);
        auto packet = routing_decision.adapter->acquire_packet_buffer(ipv4_payload_offset + data_length);
        if (!packet)
            return ENOMEM;
        routing_decision.adapter->fill_in_ipv4_header(*packet, local_address(), routing_decision.next_hop,
            m_peer_address, (IPv4Protocol)protocol(), data_length, m_ttl);
        if (!data.read(packet->buffer.data() + ipv4_payload_offset, data_length)) {
            routing_decision.adapter->release_packet_buffer(*packet);
            return EFAULT;
        }
        routing_decision.adapter->send_packet({ packet->buffer.data(), packet->buffer.size() });
        routing_decision.adapter->release_packet_buffer(*packet);
        return data_length;
    }

    auto nsent_or_error = protocol_send(data, data_length);
    if (!nsent_or_error.is_error())
        Thread::current()->did_ipv4_socket_write(nsent_or_error.value());
    return nsent_or_error;
}

KResultOr<size_t> IPv4Socket::receive_byte_buffered(FileDescription& description, UserOrKernelBuffer& buffer, size_t buffer_length, int flags, Userspace<sockaddr*>, Userspace<socklen_t*>)
{
    Locker locker(lock());
    if (m_receive_buffer.is_empty()) {
        if (protocol_is_disconnected())
            return 0;
        if (!description.is_blocking())
            return EAGAIN;

        locker.unlock();
        auto unblocked_flags = BlockFlags::None;
        auto res = Thread::current()->block<Thread::ReadBlocker>({}, description, unblocked_flags);
        locker.lock();

        if (!has_flag(unblocked_flags, BlockFlags::Read)) {
            if (res.was_interrupted())
                return EINTR;

            // Unblocked due to timeout.
            return EAGAIN;
        }
    }

    VERIFY(!m_receive_buffer.is_empty());

    int nreceived;
    if (flags & MSG_PEEK)
        nreceived = m_receive_buffer.peek(buffer, buffer_length);
    else
        nreceived = m_receive_buffer.read(buffer, buffer_length);

    if (nreceived > 0 && !(flags & MSG_PEEK))
        Thread::current()->did_ipv4_socket_read((size_t)nreceived);

    set_can_read(!m_receive_buffer.is_empty());
    return nreceived;
}

KResultOr<size_t> IPv4Socket::receive_packet_buffered(FileDescription& description, UserOrKernelBuffer& buffer, size_t buffer_length, int flags, Userspace<sockaddr*> addr, Userspace<socklen_t*> addr_length, Time& packet_timestamp)
{
    Locker locker(lock());
    ReceivedPacket packet;
    {
        if (m_receive_queue.is_empty()) {
            // FIXME: Shouldn't this return -ENOTCONN instead of EOF?
            //        But if so, we still need to deliver at least one EOF read to userspace.. right?
            if (protocol_is_disconnected())
                return 0;
            if (!description.is_blocking())
                return EAGAIN;
        }

        if (!m_receive_queue.is_empty()) {
            if (flags & MSG_PEEK)
                packet = m_receive_queue.first();
            else
                packet = m_receive_queue.take_first();

            set_can_read(!m_receive_queue.is_empty());

            dbgln_if(IPV4_SOCKET_DEBUG, "IPv4Socket({}): recvfrom without blocking {} bytes, packets in queue: {}",
                this,
                packet.data.value().size(),
                m_receive_queue.size());
        }
    }
    if (!packet.data.has_value()) {
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
                return EINTR;

            // Unblocked due to timeout.
            return EAGAIN;
        }
        VERIFY(m_can_read);
        VERIFY(!m_receive_queue.is_empty());

        if (flags & MSG_PEEK)
            packet = m_receive_queue.first();
        else
            packet = m_receive_queue.take_first();

        set_can_read(!m_receive_queue.is_empty());

        dbgln_if(IPV4_SOCKET_DEBUG, "IPv4Socket({}): recvfrom with blocking {} bytes, packets in queue: {}",
            this,
            packet.data.value().size(),
            m_receive_queue.size());
    }
    VERIFY(packet.data.has_value());

    packet_timestamp = packet.timestamp;

    if (addr) {
        dbgln_if(IPV4_SOCKET_DEBUG, "Incoming packet is from: {}:{}", packet.peer_address, packet.peer_port);

        sockaddr_in out_addr {};
        memcpy(&out_addr.sin_addr, &packet.peer_address, sizeof(IPv4Address));
        out_addr.sin_port = htons(packet.peer_port);
        out_addr.sin_family = AF_INET;
        Userspace<sockaddr_in*> dest_addr = addr.ptr();
        if (!copy_to_user(dest_addr, &out_addr))
            return EFAULT;

        socklen_t out_length = sizeof(sockaddr_in);
        VERIFY(addr_length);
        if (!copy_to_user(addr_length, &out_length))
            return EFAULT;
    }

    if (type() == SOCK_RAW) {
        size_t bytes_written = min(packet.data.value().size(), buffer_length);
        if (!buffer.write(packet.data.value().data(), bytes_written))
            return EFAULT;
        return bytes_written;
    }

    return protocol_receive(ReadonlyBytes { packet.data.value().data(), packet.data.value().size() }, buffer, buffer_length, flags);
}

KResultOr<size_t> IPv4Socket::recvfrom(FileDescription& description, UserOrKernelBuffer& buffer, size_t buffer_length, int flags, Userspace<sockaddr*> user_addr, Userspace<socklen_t*> user_addr_length, Time& packet_timestamp)
{
    if (user_addr_length) {
        socklen_t addr_length;
        if (!copy_from_user(&addr_length, user_addr_length.unsafe_userspace_ptr()))
            return EFAULT;
        if (addr_length < sizeof(sockaddr_in))
            return EINVAL;
    }

    dbgln_if(IPV4_SOCKET_DEBUG, "recvfrom: type={}, local_port={}", type(), local_port());

    KResultOr<size_t> nreceived = 0;
    if (buffer_mode() == BufferMode::Bytes)
        nreceived = receive_byte_buffered(description, buffer, buffer_length, flags, user_addr, user_addr_length);
    else
        nreceived = receive_packet_buffered(description, buffer, buffer_length, flags, user_addr, user_addr_length, packet_timestamp);

    if (!nreceived.is_error())
        Thread::current()->did_ipv4_socket_read(nreceived.value());
    return nreceived;
}

bool IPv4Socket::did_receive(const IPv4Address& source_address, u16 source_port, ReadonlyBytes packet, const Time& packet_timestamp)
{
    Locker locker(lock());

    if (is_shut_down_for_reading())
        return false;

    auto packet_size = packet.size();

    if (buffer_mode() == BufferMode::Bytes) {
        size_t space_in_receive_buffer = m_receive_buffer.space_for_writing();
        if (packet_size > space_in_receive_buffer) {
            dbgln("IPv4Socket({}): did_receive refusing packet since buffer is full.", this);
            VERIFY(m_can_read);
            return false;
        }
        auto scratch_buffer = UserOrKernelBuffer::for_kernel_buffer(m_scratch_buffer.value().data());
        auto nreceived_or_error = protocol_receive(ReadonlyBytes { packet.data(), packet.size() }, scratch_buffer, m_scratch_buffer.value().size(), 0);
        if (nreceived_or_error.is_error())
            return false;
        ssize_t nwritten = m_receive_buffer.write(scratch_buffer, nreceived_or_error.value());
        if (nwritten < 0)
            return false;
        set_can_read(!m_receive_buffer.is_empty());
    } else {
        if (m_receive_queue.size() > 2000) {
            dbgln("IPv4Socket({}): did_receive refusing packet since queue is full.", this);
            return false;
        }
        m_receive_queue.append({ source_address, source_port, packet_timestamp, KBuffer::copy(packet.data(), packet.size()) });
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

String IPv4Socket::absolute_path(const FileDescription&) const
{
    if (m_role == Role::None)
        return "socket";

    StringBuilder builder;
    builder.append("socket:");

    builder.appendff("{}:{}", m_local_address.to_string(), m_local_port);
    if (m_role == Role::Accepted || m_role == Role::Connected)
        builder.appendff(" / {}:{}", m_peer_address.to_string(), m_peer_port);

    switch (m_role) {
    case Role::Listener:
        builder.append(" (listening)");
        break;
    case Role::Accepted:
        builder.append(" (accepted)");
        break;
    case Role::Connected:
        builder.append(" (connected)");
        break;
    case Role::Connecting:
        builder.append(" (connecting)");
        break;
    default:
        VERIFY_NOT_REACHED();
    }

    return builder.to_string();
}

KResult IPv4Socket::setsockopt(int level, int option, Userspace<const void*> user_value, socklen_t user_value_size)
{
    if (level != IPPROTO_IP)
        return Socket::setsockopt(level, option, user_value, user_value_size);

    switch (option) {
    case IP_TTL: {
        if (user_value_size < sizeof(int))
            return EINVAL;
        int value;
        if (!copy_from_user(&value, static_ptr_cast<const int*>(user_value)))
            return EFAULT;
        if (value < 0 || value > 255)
            return EINVAL;
        m_ttl = value;
        return KSuccess;
    }
    case IP_MULTICAST_LOOP: {
        if (user_value_size != 1)
            return EINVAL;
        u8 value;
        if (!copy_from_user(&value, static_ptr_cast<const u8*>(user_value)))
            return EFAULT;
        if (value != 0 && value != 1)
            return EINVAL;
        m_multicast_loop = value;
        return KSuccess;
    }
    case IP_ADD_MEMBERSHIP: {
        if (user_value_size != sizeof(ip_mreq))
            return EINVAL;
        ip_mreq mreq;
        if (!copy_from_user(&mreq, static_ptr_cast<const ip_mreq*>(user_value)))
            return EFAULT;
        if (mreq.imr_interface.s_addr != INADDR_ANY)
            return ENOTSUP;
        IPv4Address address { (const u8*)&mreq.imr_multiaddr.s_addr };
        if (!m_multicast_memberships.contains_slow(address))
            m_multicast_memberships.append(address);
        return KSuccess;
    }
    case IP_DROP_MEMBERSHIP: {
        if (user_value_size != sizeof(ip_mreq))
            return EINVAL;
        ip_mreq mreq;
        if (!copy_from_user(&mreq, static_ptr_cast<const ip_mreq*>(user_value)))
            return EFAULT;
        if (mreq.imr_interface.s_addr != INADDR_ANY)
            return ENOTSUP;
        IPv4Address address { (const u8*)&mreq.imr_multiaddr.s_addr };
        m_multicast_memberships.remove_first_matching([&address](auto& a) { return a == address; });
        return KSuccess;
    }
    default:
        return ENOPROTOOPT;
    }
}

KResult IPv4Socket::getsockopt(FileDescription& description, int level, int option, Userspace<void*> value, Userspace<socklen_t*> value_size)
{
    if (level != IPPROTO_IP)
        return Socket::getsockopt(description, level, option, value, value_size);

    socklen_t size;
    if (!copy_from_user(&size, value_size.unsafe_userspace_ptr()))
        return EFAULT;

    switch (option) {
    case IP_TTL:
        if (size < sizeof(int))
            return EINVAL;
        if (!copy_to_user(static_ptr_cast<int*>(value), (int*)&m_ttl))
            return EFAULT;
        size = sizeof(int);
        if (!copy_to_user(value_size, &size))
            return EFAULT;
        return KSuccess;
    case IP_MULTICAST_LOOP: {
        if (size < 1)
            return EINVAL;
        if (!copy_to_user(static_ptr_cast<u8*>(value), (const u8*)&m_multicast_loop))
            return EFAULT;
        size = 1;
        if (!copy_to_user(value_size, &size))
            return EFAULT;
        return KSuccess;
    }
    default:
        return ENOPROTOOPT;
    }
}

int IPv4Socket::ioctl(FileDescription&, unsigned request, FlatPtr arg)
{
    REQUIRE_PROMISE(inet);

    auto ioctl_route = [request, arg]() {
        rtentry route;
        if (!copy_from_user(&route, (rtentry*)arg))
            return -EFAULT;

        auto copied_ifname = copy_string_from_user(route.rt_dev, IFNAMSIZ);
        if (copied_ifname.is_null())
            return -EFAULT;

        auto adapter = NetworkAdapter::lookup_by_name(copied_ifname);
        if (!adapter)
            return -ENODEV;

        switch (request) {
        case SIOCADDRT:
            if (!Process::current()->is_superuser())
                return -EPERM;
            if (route.rt_gateway.sa_family != AF_INET)
                return -EAFNOSUPPORT;
            if ((route.rt_flags & (RTF_UP | RTF_GATEWAY)) != (RTF_UP | RTF_GATEWAY))
                return -EINVAL; // FIXME: Find the correct value to return
            adapter->set_ipv4_gateway(IPv4Address(((sockaddr_in&)route.rt_gateway).sin_addr.s_addr));
            return 0;

        case SIOCDELRT:
            // FIXME: Support gateway deletion
            return 0;
        }

        return -EINVAL;
    };

    auto ioctl_interface = [request, arg]() {
        ifreq* user_ifr = (ifreq*)arg;
        ifreq ifr;
        if (!copy_from_user(&ifr, user_ifr))
            return -EFAULT;

        char namebuf[IFNAMSIZ + 1];
        memcpy(namebuf, ifr.ifr_name, IFNAMSIZ);
        namebuf[sizeof(namebuf) - 1] = '\0';

        auto adapter = NetworkAdapter::lookup_by_name(namebuf);
        if (!adapter)
            return -ENODEV;

        switch (request) {
        case SIOCSIFADDR:
            if (!Process::current()->is_superuser())
                return -EPERM;
            if (ifr.ifr_addr.sa_family != AF_INET)
                return -EAFNOSUPPORT;
            adapter->set_ipv4_address(IPv4Address(((sockaddr_in&)ifr.ifr_addr).sin_addr.s_addr));
            return 0;

        case SIOCSIFNETMASK:
            if (!Process::current()->is_superuser())
                return -EPERM;
            if (ifr.ifr_addr.sa_family != AF_INET)
                return -EAFNOSUPPORT;
            adapter->set_ipv4_netmask(IPv4Address(((sockaddr_in&)ifr.ifr_netmask).sin_addr.s_addr));
            return 0;

        case SIOCGIFADDR: {
            u16 sa_family = AF_INET;
            if (!copy_to_user(&user_ifr->ifr_addr.sa_family, &sa_family))
                return -EFAULT;
            auto ip4_addr = adapter->ipv4_address().to_u32();
            if (!copy_to_user(&((sockaddr_in&)user_ifr->ifr_addr).sin_addr.s_addr, &ip4_addr, sizeof(ip4_addr)))
                return -EFAULT;
            return 0;
        }

        case SIOCGIFNETMASK: {
            u16 sa_family = AF_INET;
            if (!copy_to_user(&user_ifr->ifr_addr.sa_family, &sa_family))
                return -EFAULT;
            auto ip4_netmask = adapter->ipv4_netmask().to_u32();
            // NOTE: NOT ifr_netmask.
            if (!copy_to_user(&((sockaddr_in&)user_ifr->ifr_addr).sin_addr.s_addr, &ip4_netmask, sizeof(ip4_netmask)))
                return -EFAULT;
            return 0;
        }

        case SIOCGIFHWADDR: {
            u16 sa_family = AF_INET;
            if (!copy_to_user(&user_ifr->ifr_hwaddr.sa_family, &sa_family))
                return -EFAULT;
            auto mac_address = adapter->mac_address();
            if (!copy_to_user(ifr.ifr_hwaddr.sa_data, &mac_address, sizeof(MACAddress)))
                return -EFAULT;
            return 0;
        }

        case SIOCGIFBRDADDR: {
            u16 sa_family = AF_INET;
            if (!copy_to_user(&user_ifr->ifr_addr.sa_family, &sa_family))
                return -EFAULT;

            // Broadcast address is basically the reverse of the netmask, i.e.
            // instead of zeroing out the end, you OR with 1 instead.
            auto ip4_netmask = adapter->ipv4_netmask().to_u32();
            auto broadcast_addr = adapter->ipv4_address().to_u32() | ~ip4_netmask;

            if (!copy_to_user(&((sockaddr_in&)user_ifr->ifr_addr).sin_addr.s_addr, &broadcast_addr, sizeof(broadcast_addr)))
                return -EFAULT;
            return 0;
        }

        case SIOCGIFMTU: {
            u16 sa_family = AF_INET;
            if (!copy_to_user(&user_ifr->ifr_addr.sa_family, &sa_family))
                return -EFAULT;

            auto ip4_metric = adapter->mtu();

            if (!copy_to_user(&user_ifr->ifr_metric, &ip4_metric, sizeof(ip4_metric)))
                return -EFAULT;
            return 0;
        }

        case SIOCGIFFLAGS: {
            u16 sa_family = AF_INET;
            if (!copy_to_user(&user_ifr->ifr_addr.sa_family, &sa_family))
                return -EFAULT;

            // FIXME: stub!
            short flags = 1;

            if (!copy_to_user(&user_ifr->ifr_flags, &flags, sizeof(flags)))
                return -EFAULT;
            return 0;
        }

        case SIOCGIFCONF: {
            // FIXME: stub!
            return -EINVAL;
        }
        }

        return -EINVAL;
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
    }

    return -EINVAL;
}

KResult IPv4Socket::close()
{
    [[maybe_unused]] auto rc = shutdown(SHUT_RDWR);
    return KSuccess;
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

}
