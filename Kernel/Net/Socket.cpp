/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <Kernel/Debug.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/Net/IPv4Socket.h>
#include <Kernel/Net/LocalSocket.h>
#include <Kernel/Net/NetworkingManagement.h>
#include <Kernel/Net/Socket.h>
#include <Kernel/Process.h>
#include <Kernel/UnixTypes.h>
#include <LibC/errno_numbers.h>

namespace Kernel {

ErrorOr<NonnullRefPtr<Socket>> Socket::create(int domain, int type, int protocol)
{
    switch (domain) {
    case AF_LOCAL:
        return TRY(LocalSocket::try_create(type & SOCK_TYPE_MASK));
    case AF_INET:
        return IPv4Socket::create(type & SOCK_TYPE_MASK, protocol);
    default:
        return EAFNOSUPPORT;
    }
}

Socket::Socket(int domain, int type, int protocol)
    : m_domain(domain)
    , m_type(type)
    , m_protocol(protocol)
{
    set_origin(Process::current());
}

Socket::~Socket()
{
}

void Socket::set_setup_state(SetupState new_setup_state)
{
    dbgln_if(SOCKET_DEBUG, "Socket({}) setup state moving from {} to {}", this, to_string(m_setup_state), to_string(new_setup_state));
    m_setup_state = new_setup_state;
    evaluate_block_conditions();
}

RefPtr<Socket> Socket::accept()
{
    MutexLocker locker(mutex());
    if (m_pending.is_empty())
        return nullptr;
    dbgln_if(SOCKET_DEBUG, "Socket({}) de-queueing connection", this);
    auto client = m_pending.take_first();
    VERIFY(!client->is_connected());
    auto& process = Process::current();
    client->set_acceptor(process);
    client->m_connected = true;
    client->set_role(Role::Accepted);
    if (!m_pending.is_empty())
        evaluate_block_conditions();
    return client;
}

ErrorOr<void> Socket::queue_connection_from(NonnullRefPtr<Socket> peer)
{
    dbgln_if(SOCKET_DEBUG, "Socket({}) queueing connection", this);
    MutexLocker locker(mutex());
    if (m_pending.size() >= m_backlog)
        return set_so_error(ECONNREFUSED);
    SOCKET_TRY(m_pending.try_append(peer));
    evaluate_block_conditions();
    return {};
}

ErrorOr<void> Socket::setsockopt(int level, int option, Userspace<const void*> user_value, socklen_t user_value_size)
{
    if (level != SOL_SOCKET)
        return ENOPROTOOPT;
    VERIFY(level == SOL_SOCKET);
    switch (option) {
    case SO_SNDTIMEO:
        if (user_value_size != sizeof(timeval))
            return EINVAL;
        m_send_timeout = TRY(copy_time_from_user(static_ptr_cast<timeval const*>(user_value)));
        return {};
    case SO_RCVTIMEO:
        if (user_value_size != sizeof(timeval))
            return EINVAL;
        m_receive_timeout = TRY(copy_time_from_user(static_ptr_cast<timeval const*>(user_value)));
        return {};
    case SO_BINDTODEVICE: {
        if (user_value_size != IFNAMSIZ)
            return EINVAL;
        auto user_string = static_ptr_cast<const char*>(user_value);
        auto ifname = TRY(try_copy_kstring_from_user(user_string, user_value_size));
        auto device = NetworkingManagement::the().lookup_by_name(ifname->view());
        if (!device)
            return ENODEV;
        m_bound_interface = move(device);
        return {};
    }
    case SO_DEBUG:
        // NOTE: This is supposed to toggle collection of debugging information on/off, we don't have any right now, so this is a no-op.
        return {};
    case SO_KEEPALIVE:
        // FIXME: Obviously, this is not a real keepalive.
        return {};
    case SO_TIMESTAMP:
        if (user_value_size != sizeof(int))
            return EINVAL;
        {
            int timestamp;
            TRY(copy_from_user(&timestamp, static_ptr_cast<const int*>(user_value)));
            m_timestamp = timestamp;
        }
        if (m_timestamp && (domain() != AF_INET || type() == SOCK_STREAM)) {
            // FIXME: Support SO_TIMESTAMP for more protocols?
            m_timestamp = 0;
            return ENOTSUP;
        }
        return {};
    case SO_DONTROUTE: {
        int routing_disabled;
        if (user_value_size != sizeof(routing_disabled))
            return EINVAL;
        TRY(copy_from_user(&routing_disabled, static_ptr_cast<const int*>(user_value)));
        m_routing_disabled = routing_disabled != 0;
        return {};
    }
    default:
        dbgln("setsockopt({}) at SOL_SOCKET not implemented.", option);
        return ENOPROTOOPT;
    }
}

ErrorOr<void> Socket::getsockopt(OpenFileDescription&, int level, int option, Userspace<void*> value, Userspace<socklen_t*> value_size)
{
    socklen_t size;
    TRY(copy_from_user(&size, value_size.unsafe_userspace_ptr()));

    // FIXME: Add TCP_NODELAY, IPPROTO_TCP and IPPROTO_IP (used in OpenSSH)
    if (level != SOL_SOCKET) {
        // Not sure if this is the correct error code, but it's only temporary until other levels are implemented.
        return ENOPROTOOPT;
    }

    switch (option) {
    case SO_SNDTIMEO:
        if (size < sizeof(timeval))
            return EINVAL;
        {
            timeval tv = m_send_timeout.to_timeval();
            TRY(copy_to_user(static_ptr_cast<timeval*>(value), &tv));
        }
        size = sizeof(timeval);
        return copy_to_user(value_size, &size);
    case SO_RCVTIMEO:
        if (size < sizeof(timeval))
            return EINVAL;
        {
            timeval tv = m_send_timeout.to_timeval();
            TRY(copy_to_user(static_ptr_cast<timeval*>(value), &tv));
        }
        size = sizeof(timeval);
        return copy_to_user(value_size, &size);
    case SO_ERROR: {
        if (size < sizeof(int))
            return EINVAL;
        int errno;
        if (so_error().is_error())
            errno = so_error().error().code();
        else
            errno = 0;
        TRY(copy_to_user(static_ptr_cast<int*>(value), &errno));
        size = sizeof(int);
        TRY(copy_to_user(value_size, &size));
        clear_so_error();
        return {};
    }
    case SO_BINDTODEVICE:
        if (size < IFNAMSIZ)
            return EINVAL;
        if (m_bound_interface) {
            auto name = m_bound_interface->name();
            auto length = name.length() + 1;
            auto characters = name.characters_without_null_termination();
            TRY(copy_to_user(static_ptr_cast<char*>(value), characters, length));
            size = length;
            return copy_to_user(value_size, &size);
        } else {
            size = 0;
            TRY(copy_to_user(value_size, &size));
            // FIXME: This return value looks suspicious.
            return EFAULT;
        }
    case SO_TIMESTAMP:
        if (size < sizeof(int))
            return EINVAL;
        TRY(copy_to_user(static_ptr_cast<int*>(value), &m_timestamp));
        size = sizeof(int);
        return copy_to_user(value_size, &size);
    case SO_TYPE:
        if (size < sizeof(int))
            return EINVAL;
        TRY(copy_to_user(static_ptr_cast<int*>(value), &m_type));
        size = sizeof(int);
        return copy_to_user(value_size, &size);
    case SO_DEBUG:
        // NOTE: This is supposed to toggle collection of debugging information on/off, we don't have any right now, so we just claim it's always off.
        if (size < sizeof(int))
            return EINVAL;
        TRY(memset_user(value.unsafe_userspace_ptr(), 0, sizeof(int)));
        size = sizeof(int);
        return copy_to_user(value_size, &size);
    case SO_ACCEPTCONN: {
        int accepting_connections = (m_role == Role::Listener) ? 1 : 0;
        if (size < sizeof(accepting_connections))
            return EINVAL;
        TRY(copy_to_user(static_ptr_cast<int*>(value), &accepting_connections));
        size = sizeof(accepting_connections);
        return copy_to_user(value_size, &size);
    }
    case SO_DONTROUTE: {
        int routing_disabled = m_routing_disabled ? 1 : 0;
        if (size < sizeof(routing_disabled))
            return EINVAL;
        TRY(copy_to_user(static_ptr_cast<int*>(value), &routing_disabled));
        size = sizeof(routing_disabled);
        return copy_to_user(value_size, &size);
    }
    default:
        dbgln("setsockopt({}) at SOL_SOCKET not implemented.", option);
        return ENOPROTOOPT;
    }
}

ErrorOr<size_t> Socket::read(OpenFileDescription& description, u64, UserOrKernelBuffer& buffer, size_t size)
{
    if (is_shut_down_for_reading())
        return 0;
    Time t {};
    return recvfrom(description, buffer, size, 0, {}, 0, t);
}

ErrorOr<size_t> Socket::write(OpenFileDescription& description, u64, const UserOrKernelBuffer& data, size_t size)
{
    if (is_shut_down_for_writing())
        return set_so_error(EPIPE);
    return sendto(description, data, size, 0, {}, 0);
}

ErrorOr<void> Socket::shutdown(int how)
{
    MutexLocker locker(mutex());
    if (type() == SOCK_STREAM && !is_connected())
        return set_so_error(ENOTCONN);
    if (m_role == Role::Listener)
        return set_so_error(ENOTCONN);
    if (!m_shut_down_for_writing && (how & SHUT_WR))
        shut_down_for_writing();
    if (!m_shut_down_for_reading && (how & SHUT_RD))
        shut_down_for_reading();
    m_shut_down_for_reading |= (how & SHUT_RD) != 0;
    m_shut_down_for_writing |= (how & SHUT_WR) != 0;
    return {};
}

ErrorOr<void> Socket::stat(::stat& st) const
{
    memset(&st, 0, sizeof(st));
    st.st_mode = S_IFSOCK;
    return {};
}

void Socket::set_connected(bool connected)
{
    MutexLocker locker(mutex());
    if (m_connected == connected)
        return;
    m_connected = connected;
    evaluate_block_conditions();
}

void Socket::set_origin(Process const& process)
{
    m_origin = { process.pid().value(), process.uid().value(), process.gid().value() };
}

void Socket::set_acceptor(Process const& process)
{
    m_acceptor = { process.pid().value(), process.uid().value(), process.gid().value() };
}

}
