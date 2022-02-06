/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/IPv4Address.h>
#include <AK/Types.h>
#include <LibCore/Notifier.h>
#include <LibCore/System.h>
#include <LibCore/TCPServer.h>

namespace Core {

ErrorOr<NonnullRefPtr<TCPServer>> TCPServer::try_create(Object* parent)
{
#ifdef SOCK_NONBLOCK
    int fd = TRY(Core::System::socket(AF_INET, SOCK_STREAM | SOCK_NONBLOCK | SOCK_CLOEXEC, 0));
#else
    int fd = TRY(Core::System::socket(AF_INET, SOCK_STREAM, 0));
    int option = 1;
    TRY(Core::System::ioctl(fd, FIONBIO, &option));
    TRY(Core::System::fcntl(fd, F_SETFD, FD_CLOEXEC));
#endif

    return adopt_nonnull_ref_or_enomem(new (nothrow) TCPServer(fd, parent));
}

TCPServer::TCPServer(int fd, Object* parent)
    : Object(parent)
    , m_fd(fd)
{
    VERIFY(m_fd >= 0);
}

TCPServer::~TCPServer()
{
    MUST(Core::System::close(m_fd));
}

ErrorOr<void> TCPServer::listen(IPv4Address const& address, u16 port)
{
    if (m_listening)
        return Error::from_errno(EADDRINUSE);

    auto socket_address = SocketAddress(address, port);
    auto in = socket_address.to_sockaddr_in();
    TRY(Core::System::bind(m_fd, (sockaddr const*)&in, sizeof(in)));
    TRY(Core::System::listen(m_fd, 5));
    m_listening = true;

    m_notifier = Notifier::construct(m_fd, Notifier::Event::Read, this);
    m_notifier->on_ready_to_read = [this] {
        if (on_ready_to_accept)
            on_ready_to_accept();
    };
    return {};
}

ErrorOr<void> TCPServer::set_blocking(bool blocking)
{
    int flags = TRY(Core::System::fcntl(m_fd, F_GETFL, 0));
    if (blocking)
        TRY(Core::System::fcntl(m_fd, F_SETFL, flags & ~O_NONBLOCK));
    else
        TRY(Core::System::fcntl(m_fd, F_SETFL, flags | O_NONBLOCK));
    return {};
}

ErrorOr<NonnullOwnPtr<Stream::TCPSocket>> TCPServer::accept()
{
    VERIFY(m_listening);
    sockaddr_in in;
    socklen_t in_size = sizeof(in);
#ifndef AK_OS_MACOS
    int accepted_fd = TRY(Core::System::accept4(m_fd, (sockaddr*)&in, &in_size, SOCK_NONBLOCK | SOCK_CLOEXEC));
#else
    int accepted_fd = TRY(Core::System::accept(m_fd, (sockaddr*)&in, &in_size));
#endif

    auto socket = TRY(Stream::TCPSocket::adopt_fd(accepted_fd));

#ifdef AK_OS_MACOS
    // FIXME: Ideally, we should let the caller decide whether it wants the
    //        socket to be nonblocking or not, but there are currently places
    //        which depend on this.
    TRY(socket->set_blocking(false));
    TRY(socket->set_close_on_exec(true));
#endif

    return socket;
}

Optional<IPv4Address> TCPServer::local_address() const
{
    if (m_fd == -1)
        return {};

    sockaddr_in address;
    socklen_t len = sizeof(address);
    if (getsockname(m_fd, (sockaddr*)&address, &len) != 0)
        return {};

    return IPv4Address(address.sin_addr.s_addr);
}

Optional<u16> TCPServer::local_port() const
{
    if (m_fd == -1)
        return {};

    sockaddr_in address;
    socklen_t len = sizeof(address);
    if (getsockname(m_fd, (sockaddr*)&address, &len) != 0)
        return {};

    return ntohs(address.sin_port);
}

}
