/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, sin-ack <sin-ack@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/Socket.h>
#include <LibCore/System.h>

namespace Core {

ErrorOr<int> Socket::create_fd(SocketDomain domain, SocketType type)
{
    int socket_domain;
    switch (domain) {
    case SocketDomain::Inet:
        socket_domain = AF_INET;
        break;
    case SocketDomain::Local:
        socket_domain = AF_LOCAL;
        break;
    default:
        VERIFY_NOT_REACHED();
    }

    int socket_type;
    switch (type) {
    case SocketType::Stream:
        socket_type = SOCK_STREAM;
        break;
    case SocketType::Datagram:
        socket_type = SOCK_DGRAM;
        break;
    default:
        VERIFY_NOT_REACHED();
    }

    // Let's have a safe default of CLOEXEC. :^)
#ifdef SOCK_CLOEXEC
    return System::socket(socket_domain, socket_type | SOCK_CLOEXEC, 0);
#else
    auto fd = TRY(System::socket(socket_domain, socket_type, 0));
    TRY(System::fcntl(fd, F_SETFD, FD_CLOEXEC));
    return fd;
#endif
}

ErrorOr<IPv4Address> Socket::resolve_host(DeprecatedString const& host, SocketType type)
{
    int socket_type;
    switch (type) {
    case SocketType::Stream:
        socket_type = SOCK_STREAM;
        break;
    case SocketType::Datagram:
        socket_type = SOCK_DGRAM;
        break;
    default:
        VERIFY_NOT_REACHED();
    }

    struct addrinfo hints = {};
    hints.ai_family = AF_UNSPEC;
    hints.ai_socktype = socket_type;
    hints.ai_flags = 0;
    hints.ai_protocol = 0;

    auto const results = TRY(Core::System::getaddrinfo(host.characters(), nullptr, hints));

    for (auto const& result : results.addresses()) {
        if (result.ai_family == AF_INET) {
            auto* socket_address = bit_cast<struct sockaddr_in*>(result.ai_addr);
            NetworkOrdered<u32> const network_ordered_address { socket_address->sin_addr.s_addr };
            return IPv4Address { network_ordered_address };
        }
    }

    return Error::from_string_literal("Could not resolve to IPv4 address");
}

ErrorOr<void> Socket::connect_local(int fd, DeprecatedString const& path)
{
    auto address = SocketAddress::local(path);
    auto maybe_sockaddr = address.to_sockaddr_un();
    if (!maybe_sockaddr.has_value()) {
        dbgln("Core::Socket::connect_local: Could not obtain a sockaddr_un");
        return Error::from_errno(EINVAL);
    }

    auto addr = maybe_sockaddr.release_value();
    return System::connect(fd, bit_cast<struct sockaddr*>(&addr), sizeof(addr));
}

ErrorOr<void> Socket::connect_inet(int fd, SocketAddress const& address)
{
    auto addr = address.to_sockaddr_in();
    return System::connect(fd, bit_cast<struct sockaddr*>(&addr), sizeof(addr));
}

ErrorOr<Bytes> PosixSocketHelper::read(Bytes buffer, int flags)
{
    if (!is_open()) {
        return Error::from_errno(ENOTCONN);
    }

    ssize_t nread = TRY(System::recv(m_fd, buffer.data(), buffer.size(), flags));
    m_last_read_was_eof = nread == 0;

    // If a socket read is EOF, then no more data can be read from it because
    // the protocol has disconnected. In this case, we can just disable the
    // notifier if we have one.
    if (m_last_read_was_eof && m_notifier)
        m_notifier->set_enabled(false);

    return buffer.trim(nread);
}

ErrorOr<size_t> PosixSocketHelper::write(ReadonlyBytes buffer, int flags)
{
    if (!is_open()) {
        return Error::from_errno(ENOTCONN);
    }

    return TRY(System::send(m_fd, buffer.data(), buffer.size(), flags));
}

void PosixSocketHelper::close()
{
    if (!is_open()) {
        return;
    }

    if (m_notifier)
        m_notifier->set_enabled(false);

    ErrorOr<void> result;
    do {
        result = System::close(m_fd);
    } while (result.is_error() && result.error().code() == EINTR);

    VERIFY(!result.is_error());
    m_fd = -1;
}

ErrorOr<bool> PosixSocketHelper::can_read_without_blocking(int timeout) const
{
    struct pollfd the_fd = { .fd = m_fd, .events = POLLIN, .revents = 0 };

    ErrorOr<int> result { 0 };
    do {
        result = Core::System::poll({ &the_fd, 1 }, timeout);
    } while (result.is_error() && result.error().code() == EINTR);

    if (result.is_error())
        return result.release_error();

    return (the_fd.revents & POLLIN) > 0;
}

ErrorOr<void> PosixSocketHelper::set_blocking(bool enabled)
{
    int value = enabled ? 0 : 1;
    return System::ioctl(m_fd, FIONBIO, &value);
}

ErrorOr<void> PosixSocketHelper::set_close_on_exec(bool enabled)
{
    int flags = TRY(System::fcntl(m_fd, F_GETFD));

    if (enabled)
        flags |= FD_CLOEXEC;
    else
        flags &= ~FD_CLOEXEC;

    TRY(System::fcntl(m_fd, F_SETFD, flags));
    return {};
}

ErrorOr<void> PosixSocketHelper::set_receive_timeout(Duration timeout)
{
    auto timeout_spec = timeout.to_timespec();
    return System::setsockopt(m_fd, SOL_SOCKET, SO_RCVTIMEO, &timeout_spec, sizeof(timeout_spec));
}

void PosixSocketHelper::setup_notifier()
{
    if (!m_notifier)
        m_notifier = Core::Notifier::construct(m_fd, Core::Notifier::Type::Read);
}

ErrorOr<NonnullOwnPtr<TCPSocket>> TCPSocket::connect(DeprecatedString const& host, u16 port)
{
    auto ip_address = TRY(resolve_host(host, SocketType::Stream));
    return connect(SocketAddress { ip_address, port });
}

ErrorOr<NonnullOwnPtr<TCPSocket>> TCPSocket::connect(SocketAddress const& address)
{
    auto socket = TRY(adopt_nonnull_own_or_enomem(new (nothrow) TCPSocket()));

    auto fd = TRY(create_fd(SocketDomain::Inet, SocketType::Stream));
    socket->m_helper.set_fd(fd);

    TRY(connect_inet(fd, address));

    socket->setup_notifier();
    return socket;
}

ErrorOr<NonnullOwnPtr<TCPSocket>> TCPSocket::adopt_fd(int fd)
{
    if (fd < 0) {
        return Error::from_errno(EBADF);
    }

    auto socket = TRY(adopt_nonnull_own_or_enomem(new (nothrow) TCPSocket()));
    socket->m_helper.set_fd(fd);
    socket->setup_notifier();
    return socket;
}

ErrorOr<size_t> PosixSocketHelper::pending_bytes() const
{
    if (!is_open()) {
        return Error::from_errno(ENOTCONN);
    }

    int value;
    TRY(System::ioctl(m_fd, FIONREAD, &value));
    return static_cast<size_t>(value);
}

ErrorOr<NonnullOwnPtr<UDPSocket>> UDPSocket::connect(DeprecatedString const& host, u16 port, Optional<Duration> timeout)
{
    auto ip_address = TRY(resolve_host(host, SocketType::Datagram));
    return connect(SocketAddress { ip_address, port }, timeout);
}

ErrorOr<NonnullOwnPtr<UDPSocket>> UDPSocket::connect(SocketAddress const& address, Optional<Duration> timeout)
{
    auto socket = TRY(adopt_nonnull_own_or_enomem(new (nothrow) UDPSocket()));

    auto fd = TRY(create_fd(SocketDomain::Inet, SocketType::Datagram));
    socket->m_helper.set_fd(fd);
    if (timeout.has_value()) {
        TRY(socket->m_helper.set_receive_timeout(timeout.value()));
    }

    TRY(connect_inet(fd, address));

    socket->setup_notifier();
    return socket;
}

ErrorOr<NonnullOwnPtr<LocalSocket>> LocalSocket::connect(DeprecatedString const& path, PreventSIGPIPE prevent_sigpipe)
{
    auto socket = TRY(adopt_nonnull_own_or_enomem(new (nothrow) LocalSocket(prevent_sigpipe)));

    auto fd = TRY(create_fd(SocketDomain::Local, SocketType::Stream));
    socket->m_helper.set_fd(fd);

    TRY(connect_local(fd, path));

    socket->setup_notifier();
    return socket;
}

ErrorOr<NonnullOwnPtr<LocalSocket>> LocalSocket::adopt_fd(int fd, PreventSIGPIPE prevent_sigpipe)
{
    if (fd < 0) {
        return Error::from_errno(EBADF);
    }

    auto socket = TRY(adopt_nonnull_own_or_enomem(new (nothrow) LocalSocket(prevent_sigpipe)));
    socket->m_helper.set_fd(fd);
    socket->setup_notifier();
    return socket;
}

ErrorOr<int> LocalSocket::receive_fd(int flags)
{
#if defined(AK_OS_SERENITY)
    return Core::System::recvfd(m_helper.fd(), flags);
#elif defined(AK_OS_LINUX) || defined(AK_OS_GNU_HURD) || defined(AK_OS_BSD_GENERIC) || defined(AK_OS_HAIKU)
    union {
        struct cmsghdr cmsghdr;
        char control[CMSG_SPACE(sizeof(int))];
    } cmsgu {};
    char c = 0;
    struct iovec iov {
        .iov_base = &c,
        .iov_len = 1,
    };
    struct msghdr msg = {};
    msg.msg_iov = &iov;
    msg.msg_iovlen = 1;
    msg.msg_control = cmsgu.control;
    msg.msg_controllen = sizeof(cmsgu.control);
    TRY(Core::System::recvmsg(m_helper.fd(), &msg, 0));

    struct cmsghdr* cmsg = CMSG_FIRSTHDR(&msg);
    if (!cmsg || cmsg->cmsg_len != CMSG_LEN(sizeof(int)))
        return Error::from_string_literal("Malformed message when receiving file descriptor");

    VERIFY(cmsg->cmsg_level == SOL_SOCKET);
    VERIFY(cmsg->cmsg_type == SCM_RIGHTS);
    int fd = *((int*)CMSG_DATA(cmsg));

    if (flags & O_CLOEXEC) {
        auto fd_flags = TRY(Core::System::fcntl(fd, F_GETFD));
        TRY(Core::System::fcntl(fd, F_SETFD, fd_flags | FD_CLOEXEC));
    }

    return fd;
#else
    (void)flags;
    return Error::from_string_literal("File descriptor passing not supported on this platform");
#endif
}

ErrorOr<void> LocalSocket::send_fd(int fd)
{
#if defined(AK_OS_SERENITY)
    return Core::System::sendfd(m_helper.fd(), fd);
#elif defined(AK_OS_LINUX) || defined(AK_OS_GNU_HURD) || defined(AK_OS_BSD_GENERIC) || defined(AK_OS_HAIKU)
    char c = 'F';
    struct iovec iov {
        .iov_base = &c,
        .iov_len = sizeof(c)
    };

    union {
        struct cmsghdr cmsghdr;
        char control[CMSG_SPACE(sizeof(int))];
    } cmsgu {};

    struct msghdr msg = {};
    msg.msg_iov = &iov;
    msg.msg_iovlen = 1;
    msg.msg_control = cmsgu.control;
    msg.msg_controllen = sizeof(cmsgu.control);

    struct cmsghdr* cmsg = CMSG_FIRSTHDR(&msg);
    cmsg->cmsg_len = CMSG_LEN(sizeof(int));
    cmsg->cmsg_level = SOL_SOCKET;
    cmsg->cmsg_type = SCM_RIGHTS;

    *((int*)CMSG_DATA(cmsg)) = fd;

    TRY(Core::System::sendmsg(m_helper.fd(), &msg, 0));
    return {};
#else
    (void)fd;
    return Error::from_string_literal("File descriptor passing not supported on this platform");
#endif
}

ErrorOr<pid_t> LocalSocket::peer_pid() const
{
#ifdef AK_OS_MACOS
    pid_t pid;
    socklen_t pid_size = sizeof(pid);
#elif defined(AK_OS_FREEBSD)
    struct xucred creds = {};
    socklen_t creds_size = sizeof(creds);
#elif defined(AK_OS_OPENBSD)
    struct sockpeercred creds = {};
    socklen_t creds_size = sizeof(creds);
#elif defined(AK_OS_NETBSD)
    struct sockcred creds = {};
    socklen_t creds_size = sizeof(creds);
#elif defined(AK_OS_SOLARIS)
    ucred_t* creds = NULL;
    socklen_t creds_size = sizeof(creds);
#elif defined(AK_OS_GNU_HURD)
    return Error::from_errno(ENOTSUP);
#else
    struct ucred creds = {};
    socklen_t creds_size = sizeof(creds);
#endif

#ifdef AK_OS_MACOS
    TRY(System::getsockopt(m_helper.fd(), SOL_LOCAL, LOCAL_PEERPID, &pid, &pid_size));
    return pid;
#elif defined(AK_OS_FREEBSD)
    TRY(System::getsockopt(m_helper.fd(), SOL_LOCAL, LOCAL_PEERCRED, &creds, &creds_size));
    return creds.cr_pid;
#elif defined(AK_OS_NETBSD)
    TRY(System::getsockopt(m_helper.fd(), SOL_SOCKET, SCM_CREDS, &creds, &creds_size));
    return creds.sc_pid;
#elif defined(AK_OS_SOLARIS)
    TRY(System::getsockopt(m_helper.fd(), SOL_SOCKET, SO_RECVUCRED, &creds, &creds_size));
    return ucred_getpid(creds);
#elif !defined(AK_OS_GNU_HURD)
    TRY(System::getsockopt(m_helper.fd(), SOL_SOCKET, SO_PEERCRED, &creds, &creds_size));
    return creds.pid;
#endif
}

ErrorOr<Bytes> LocalSocket::read_without_waiting(Bytes buffer)
{
    return m_helper.read(buffer, MSG_DONTWAIT);
}

Optional<int> LocalSocket::fd() const
{
    if (!is_open())
        return {};
    return m_helper.fd();
}

ErrorOr<int> LocalSocket::release_fd()
{
    if (!is_open()) {
        return Error::from_errno(ENOTCONN);
    }

    auto fd = m_helper.fd();
    m_helper.set_fd(-1);
    return fd;
}

}
