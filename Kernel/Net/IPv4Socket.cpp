/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/Singleton.h>
#include <AK/StringBuilder.h>
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

//#define IPV4_SOCKET_DEBUG

namespace Kernel {

static AK::Singleton<Lockable<HashTable<IPv4Socket*>>> s_table;

Lockable<HashTable<IPv4Socket*>>& IPv4Socket::all_sockets()
{
    return *s_table;
}

KResultOr<NonnullRefPtr<Socket>> IPv4Socket::create(int type, int protocol)
{
    if (type == SOCK_STREAM)
        return TCPSocket::create(protocol);
    if (type == SOCK_DGRAM)
        return UDPSocket::create(protocol);
    if (type == SOCK_RAW)
        return adopt(*new IPv4Socket(type, protocol));
    return KResult(-EINVAL);
}

IPv4Socket::IPv4Socket(int type, int protocol)
    : Socket(AF_INET, type, protocol)
{
#ifdef IPV4_SOCKET_DEBUG
    dbg() << "IPv4Socket{" << this << "} created with type=" << type << ", protocol=" << protocol;
#endif
    m_buffer_mode = type == SOCK_STREAM ? BufferMode::Bytes : BufferMode::Packets;
    if (m_buffer_mode == BufferMode::Bytes) {
        m_scratch_buffer = KBuffer::create_with_size(65536);
    }
    LOCKER(all_sockets().lock());
    all_sockets().resource().set(this);
}

IPv4Socket::~IPv4Socket()
{
    LOCKER(all_sockets().lock());
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
    ASSERT(setup_state() == SetupState::Unstarted);
    if (address_size != sizeof(sockaddr_in))
        return KResult(-EINVAL);

    sockaddr_in address;
    if (!copy_from_user(&address, user_address, sizeof(sockaddr_in)))
        return KResult(-EFAULT);

    if (address.sin_family != AF_INET)
        return KResult(-EINVAL);

    auto requested_local_port = ntohs(address.sin_port);
    if (!Process::current()->is_superuser()) {
        if (requested_local_port < 1024) {
            dbg() << "UID " << Process::current()->uid() << " attempted to bind " << class_name() << " to port " << requested_local_port;
            return KResult(-EACCES);
        }
    }

    m_local_address = IPv4Address((const u8*)&address.sin_addr.s_addr);
    m_local_port = requested_local_port;

#ifdef IPV4_SOCKET_DEBUG
    dbg() << "IPv4Socket::bind " << class_name() << "{" << this << "} to " << m_local_address << ":" << m_local_port;
#endif

    return protocol_bind();
}

KResult IPv4Socket::listen(size_t backlog)
{
    int rc = allocate_local_port_if_needed();
    if (rc < 0)
        return KResult(-EADDRINUSE);

    set_backlog(backlog);
    m_role = Role::Listener;

#ifdef IPV4_SOCKET_DEBUG
    dbg() << "IPv4Socket{" << this << "} listening with backlog=" << backlog;
#endif

    return protocol_listen();
}

KResult IPv4Socket::connect(FileDescription& description, Userspace<const sockaddr*> address, socklen_t address_size, ShouldBlock should_block)
{
    if (address_size != sizeof(sockaddr_in))
        return KResult(-EINVAL);
    u16 sa_family_copy;
    auto* user_address = reinterpret_cast<const sockaddr*>(address.unsafe_userspace_ptr());
    if (!copy_from_user(&sa_family_copy, &user_address->sa_family, sizeof(u16)))
        return KResult(-EFAULT);
    if (sa_family_copy != AF_INET)
        return KResult(-EINVAL);
    if (m_role == Role::Connected)
        return KResult(-EISCONN);

    sockaddr_in safe_address;
    if (!copy_from_user(&safe_address, (const sockaddr_in*)user_address, sizeof(sockaddr_in)))
        return KResult(-EFAULT);

    m_peer_address = IPv4Address((const u8*)&safe_address.sin_addr.s_addr);
    m_peer_port = ntohs(safe_address.sin_port);

    return protocol_connect(description, should_block);
}

void IPv4Socket::attach(FileDescription&)
{
}

void IPv4Socket::detach(FileDescription&)
{
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
    return is_connected();
}

int IPv4Socket::allocate_local_port_if_needed()
{
    if (m_local_port)
        return m_local_port;
    int port = protocol_allocate_local_port();
    if (port < 0)
        return port;
    m_local_port = (u16)port;
    return port;
}

KResultOr<size_t> IPv4Socket::sendto(FileDescription&, const UserOrKernelBuffer& data, size_t data_length, int flags, Userspace<const sockaddr*> addr, socklen_t addr_length)
{
    (void)flags;
    if (addr && addr_length != sizeof(sockaddr_in))
        return KResult(-EINVAL);

    if (addr) {
        sockaddr_in ia;
        if (!copy_from_user(&ia, Userspace<const sockaddr_in*>(addr.ptr())))
            return KResult(-EFAULT);

        if (ia.sin_family != AF_INET) {
            klog() << "sendto: Bad address family: " << ia.sin_family << " is not AF_INET!";
            return KResult(-EAFNOSUPPORT);
        }

        m_peer_address = IPv4Address((const u8*)&ia.sin_addr.s_addr);
        m_peer_port = ntohs(ia.sin_port);
    }

    auto routing_decision = route_to(m_peer_address, m_local_address, bound_interface());
    if (routing_decision.is_zero())
        return KResult(-EHOSTUNREACH);

    if (m_local_address.to_u32() == 0)
        m_local_address = routing_decision.adapter->ipv4_address();

    int rc = allocate_local_port_if_needed();
    if (rc < 0)
        return rc;

#ifdef IPV4_SOCKET_DEBUG
    klog() << "sendto: destination=" << m_peer_address.to_string().characters() << ":" << m_peer_port;
#endif

    if (type() == SOCK_RAW) {
        int err = routing_decision.adapter->send_ipv4(routing_decision.next_hop, m_peer_address, (IPv4Protocol)protocol(), data, data_length, m_ttl);
        if (err < 0)
            return KResult(err);
        return data_length;
    }

    auto nsent_or_error = protocol_send(data, data_length);
    if (!nsent_or_error.is_error())
        Thread::current()->did_ipv4_socket_write(nsent_or_error.value());
    return nsent_or_error;
}

KResultOr<size_t> IPv4Socket::receive_byte_buffered(FileDescription& description, UserOrKernelBuffer& buffer, size_t buffer_length, int, Userspace<sockaddr*>, Userspace<socklen_t*>)
{
    Locker locker(lock());
    if (m_receive_buffer.is_empty()) {
        if (protocol_is_disconnected())
            return 0;
        if (!description.is_blocking())
            return KResult(-EAGAIN);

        locker.unlock();
        auto res = Thread::current()->block<Thread::ReadBlocker>(nullptr, description);
        locker.lock();

        if (!m_can_read) {
            if (res.was_interrupted())
                return KResult(-EINTR);

            // Unblocked due to timeout.
            return KResult(-EAGAIN);
        }
    }

    ASSERT(!m_receive_buffer.is_empty());
    int nreceived = m_receive_buffer.read(buffer, buffer_length);
    if (nreceived > 0)
        Thread::current()->did_ipv4_socket_read((size_t)nreceived);

    m_can_read = !m_receive_buffer.is_empty();
    return nreceived;
}

KResultOr<size_t> IPv4Socket::receive_packet_buffered(FileDescription& description, UserOrKernelBuffer& buffer, size_t buffer_length, int flags, Userspace<sockaddr*> addr, Userspace<socklen_t*> addr_length, timeval& packet_timestamp)
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
                return KResult(-EAGAIN);
        }

        if (!m_receive_queue.is_empty()) {
            packet = m_receive_queue.take_first();
            m_can_read = !m_receive_queue.is_empty();
#ifdef IPV4_SOCKET_DEBUG
            dbg() << "IPv4Socket(" << this << "): recvfrom without blocking " << packet.data.value().size() << " bytes, packets in queue: " << m_receive_queue.size();
#endif
        }
    }
    if (!packet.data.has_value()) {
        if (protocol_is_disconnected()) {
            dbg() << "IPv4Socket{" << this << "} is protocol-disconnected, returning 0 in recvfrom!";
            return 0;
        }

        locker.unlock();
        auto res = Thread::current()->block<Thread::ReadBlocker>(nullptr, description);
        locker.lock();

        if (!m_can_read) {
            if (res.was_interrupted())
                return KResult(-EINTR);

            // Unblocked due to timeout.
            return KResult(-EAGAIN);
        }
        ASSERT(m_can_read);
        ASSERT(!m_receive_queue.is_empty());
        packet = m_receive_queue.take_first();
        m_can_read = !m_receive_queue.is_empty();
#ifdef IPV4_SOCKET_DEBUG
        dbg() << "IPv4Socket(" << this << "): recvfrom with blocking " << packet.data.value().size() << " bytes, packets in queue: " << m_receive_queue.size();
#endif
    }
    ASSERT(packet.data.has_value());
    auto& ipv4_packet = *(const IPv4Packet*)(packet.data.value().data());

    packet_timestamp = packet.timestamp;

    if (addr) {
#ifdef IPV4_SOCKET_DEBUG
        dbg() << "Incoming packet is from: " << packet.peer_address << ":" << packet.peer_port;
#endif

        sockaddr_in out_addr {};
        memcpy(&out_addr.sin_addr, &packet.peer_address, sizeof(IPv4Address));
        out_addr.sin_port = htons(packet.peer_port);
        out_addr.sin_family = AF_INET;
        Userspace<sockaddr_in*> dest_addr = addr.ptr();
        if (!copy_to_user(dest_addr, &out_addr))
            return KResult(-EFAULT);

        socklen_t out_length = sizeof(sockaddr_in);
        ASSERT(addr_length);
        if (!copy_to_user(addr_length, &out_length))
            return KResult(-EFAULT);
    }

    if (type() == SOCK_RAW) {
        size_t bytes_written = min((size_t)ipv4_packet.payload_size(), buffer_length);
        if (!buffer.write(ipv4_packet.payload(), bytes_written))
            return KResult(-EFAULT);
        return bytes_written;
    }

    return protocol_receive(packet.data.value(), buffer, buffer_length, flags);
}

KResultOr<size_t> IPv4Socket::recvfrom(FileDescription& description, UserOrKernelBuffer& buffer, size_t buffer_length, int flags, Userspace<sockaddr*> user_addr, Userspace<socklen_t*> user_addr_length, timeval& packet_timestamp)
{
    if (user_addr_length) {
        socklen_t addr_length;
        if (!copy_from_user(&addr_length, user_addr_length.unsafe_userspace_ptr()))
            return KResult(-EFAULT);
        if (addr_length < sizeof(sockaddr_in))
            return KResult(-EINVAL);
    }

#ifdef IPV4_SOCKET_DEBUG
    klog() << "recvfrom: type=" << type() << ", local_port=" << local_port();
#endif

    KResultOr<size_t> nreceived = 0;
    if (buffer_mode() == BufferMode::Bytes)
        nreceived = receive_byte_buffered(description, buffer, buffer_length, flags, user_addr, user_addr_length);
    else
        nreceived = receive_packet_buffered(description, buffer, buffer_length, flags, user_addr, user_addr_length, packet_timestamp);

    if (!nreceived.is_error())
        Thread::current()->did_ipv4_socket_read(nreceived.value());
    return nreceived;
}

bool IPv4Socket::did_receive(const IPv4Address& source_address, u16 source_port, KBuffer&& packet, const timeval& packet_timestamp)
{
    LOCKER(lock());

    if (is_shut_down_for_reading())
        return false;

    auto packet_size = packet.size();

    if (buffer_mode() == BufferMode::Bytes) {
        size_t space_in_receive_buffer = m_receive_buffer.space_for_writing();
        if (packet_size > space_in_receive_buffer) {
            dbg() << "IPv4Socket(" << this << "): did_receive refusing packet since buffer is full.";
            ASSERT(m_can_read);
            return false;
        }
        auto scratch_buffer = UserOrKernelBuffer::for_kernel_buffer(m_scratch_buffer.value().data());
        auto nreceived_or_error = protocol_receive(packet, scratch_buffer, m_scratch_buffer.value().size(), 0);
        if (nreceived_or_error.is_error())
            return false;
        ssize_t nwritten = m_receive_buffer.write(scratch_buffer, nreceived_or_error.value());
        if (nwritten < 0)
            return false;
        m_can_read = !m_receive_buffer.is_empty();
    } else {
        if (m_receive_queue.size() > 2000) {
            dbg() << "IPv4Socket(" << this << "): did_receive refusing packet since queue is full.";
            return false;
        }
        m_receive_queue.append({ source_address, source_port, packet_timestamp, move(packet) });
        m_can_read = true;
    }
    m_bytes_received += packet_size;
#ifdef IPV4_SOCKET_DEBUG
    if (buffer_mode() == BufferMode::Bytes)
        dbg() << "IPv4Socket(" << this << "): did_receive " << packet_size << " bytes, total_received=" << m_bytes_received;
    else
        dbg() << "IPv4Socket(" << this << "): did_receive " << packet_size << " bytes, total_received=" << m_bytes_received << ", packets in queue: " << m_receive_queue.size();
#endif
    return true;
}

String IPv4Socket::absolute_path(const FileDescription&) const
{
    if (m_role == Role::None)
        return "socket";

    StringBuilder builder;
    builder.append("socket:");

    builder.appendf("%s:%d", m_local_address.to_string().characters(), m_local_port);
    if (m_role == Role::Accepted || m_role == Role::Connected)
        builder.appendf(" / %s:%d", m_peer_address.to_string().characters(), m_peer_port);

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
        ASSERT_NOT_REACHED();
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
            return KResult(-EINVAL);
        int value;
        if (!copy_from_user(&value, static_ptr_cast<const int*>(user_value)))
            return KResult(-EFAULT);
        if (value < 0 || value > 255)
            return KResult(-EINVAL);
        m_ttl = value;
        return KSuccess;
    }
    default:
        return KResult(-ENOPROTOOPT);
    }
}

KResult IPv4Socket::getsockopt(FileDescription& description, int level, int option, Userspace<void*> value, Userspace<socklen_t*> value_size)
{
    if (level != IPPROTO_IP)
        return Socket::getsockopt(description, level, option, value, value_size);

    socklen_t size;
    if (!copy_from_user(&size, value_size.unsafe_userspace_ptr()))
        return KResult(-EFAULT);

    switch (option) {
    case IP_TTL:
        if (size < sizeof(int))
            return KResult(-EINVAL);
        if (!copy_to_user(static_ptr_cast<int*>(value), (int*)&m_ttl))
            return KResult(-EFAULT);
        size = sizeof(int);
        if (!copy_to_user(value_size, &size))
            return KResult(-EFAULT);
        return KSuccess;
    default:
        return KResult(-ENOPROTOOPT);
    }
}

int IPv4Socket::ioctl(FileDescription&, unsigned request, FlatPtr arg)
{
    REQUIRE_PROMISE(inet);

    SmapDisabler disabler;

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

        case SIOCGIFHWADDR: {
            u16 sa_family = AF_INET;
            if (!copy_to_user(&user_ifr->ifr_hwaddr.sa_family, &sa_family))
                return -EFAULT;
            auto mac_address = adapter->mac_address();
            if (!copy_to_user(ifr.ifr_hwaddr.sa_data, &mac_address, sizeof(MACAddress)))
                return -EFAULT;
            return 0;
        }
        }

        return -EINVAL;
    };

    switch (request) {
    case SIOCSIFADDR:
    case SIOCSIFNETMASK:
    case SIOCGIFADDR:
    case SIOCGIFHWADDR:
        return ioctl_interface();

    case SIOCADDRT:
    case SIOCDELRT:
        return ioctl_route();
    }

    return -EINVAL;
}

KResult IPv4Socket::close()
{
    (void)shutdown(SHUT_RDWR);
    return KSuccess;
}

void IPv4Socket::shut_down_for_reading()
{
    Socket::shut_down_for_reading();
    m_can_read = true;
}

}
