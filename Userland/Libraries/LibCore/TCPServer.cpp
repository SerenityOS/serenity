/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/IPv4Address.h>
#include <AK/Types.h>
#include <LibCore/Notifier.h>
#include <LibCore/TCPServer.h>
#include <LibCore/TCPSocket.h>
#include <fcntl.h>
#include <stdio.h>
#include <sys/socket.h>
#include <unistd.h>

#ifndef SOCK_NONBLOCK
#    include <sys/ioctl.h>
#endif
namespace Core {

TCPServer::TCPServer(Object* parent)
    : Object(parent)
{
#ifdef SOCK_NONBLOCK
    m_fd = socket(AF_INET, SOCK_STREAM | SOCK_NONBLOCK | SOCK_CLOEXEC, 0);
#else
    m_fd = socket(AF_INET, SOCK_STREAM, 0);
    int option = 1;
    ioctl(m_fd, FIONBIO, &option);
    fcntl(m_fd, F_SETFD, FD_CLOEXEC);
#endif
    VERIFY(m_fd >= 0);
}

TCPServer::~TCPServer()
{
    ::close(m_fd);
}

bool TCPServer::listen(const IPv4Address& address, u16 port)
{
    if (m_listening)
        return false;

    auto socket_address = SocketAddress(address, port);
    auto in = socket_address.to_sockaddr_in();
    if (::bind(m_fd, (const sockaddr*)&in, sizeof(in)) < 0) {
        perror("TCPServer::listen: bind");
        return false;
    }

    if (::listen(m_fd, 5) < 0) {
        perror("TCPServer::listen: listen");
        return false;
    }
    m_listening = true;

    m_notifier = Notifier::construct(m_fd, Notifier::Event::Read, this);
    m_notifier->on_ready_to_read = [this] {
        if (on_ready_to_accept)
            on_ready_to_accept();
    };
    return true;
}

void TCPServer::set_blocking(bool blocking)
{
    int flags = fcntl(m_fd, F_GETFL, 0);
    VERIFY(flags >= 0);
    if (blocking)
        flags = fcntl(m_fd, F_SETFL, flags & ~O_NONBLOCK);
    else
        flags = fcntl(m_fd, F_SETFL, flags | O_NONBLOCK);
    VERIFY(flags == 0);
}

RefPtr<TCPSocket> TCPServer::accept()
{
    VERIFY(m_listening);
    sockaddr_in in;
    socklen_t in_size = sizeof(in);
#ifndef AK_OS_MACOS
    int accepted_fd = ::accept4(m_fd, (sockaddr*)&in, &in_size, SOCK_NONBLOCK | SOCK_CLOEXEC);
#else
    int accepted_fd = ::accept(m_fd, (sockaddr*)&in, &in_size);
#endif

    if (accepted_fd < 0) {
        perror("accept");
        return nullptr;
    }

#ifdef AK_OS_MACOS
    int option = 1;
    (void)ioctl(m_fd, FIONBIO, &option);
    (void)fcntl(accepted_fd, F_SETFD, FD_CLOEXEC);
#endif

    return TCPSocket::construct(accepted_fd);
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
