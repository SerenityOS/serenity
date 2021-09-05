/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/IPv4Address.h>
#include <AK/Types.h>
#include <LibCore/Notifier.h>
#include <LibCore/UDPServer.h>
#include <errno.h>
#include <stdio.h>
#include <unistd.h>

#ifndef SOCK_NONBLOCK
#    include <fcntl.h>
#    include <sys/ioctl.h>
#endif

namespace Core {

UDPServer::UDPServer(Object* parent)
    : Object(parent)
{
#ifdef SOCK_NONBLOCK
    m_fd = socket(AF_INET, SOCK_DGRAM | SOCK_NONBLOCK | SOCK_CLOEXEC, 0);
#else
    m_fd = socket(AF_INET, SOCK_DGRAM, 0);
    int option = 1;
    ioctl(m_fd, FIONBIO, &option);
    fcntl(m_fd, F_SETFD, FD_CLOEXEC);
#endif
    VERIFY(m_fd >= 0);
}

UDPServer::~UDPServer()
{
    ::close(m_fd);
}

bool UDPServer::bind(const IPv4Address& address, u16 port)
{
    if (m_bound)
        return false;

    auto saddr = SocketAddress(address, port);
    auto in = saddr.to_sockaddr_in();

    if (::bind(m_fd, (const sockaddr*)&in, sizeof(in)) != 0) {
        perror("UDPServer::bind");
        return false;
    }

    m_bound = true;

    m_notifier = Notifier::construct(m_fd, Notifier::Event::Read, this);
    m_notifier->on_ready_to_read = [this] {
        if (on_ready_to_receive)
            on_ready_to_receive();
    };
    return true;
}

ByteBuffer UDPServer::receive(size_t size, sockaddr_in& in)
{
    // FIXME: Handle possible OOM situation.
    auto buf = ByteBuffer::create_uninitialized(size).release_value();
    socklen_t in_len = sizeof(in);
    ssize_t rlen = ::recvfrom(m_fd, buf.data(), size, 0, (sockaddr*)&in, &in_len);
    if (rlen < 0) {
        dbgln("recvfrom: {}", strerror(errno));
        return {};
    }

    buf.resize(rlen);
    return buf;
}

Optional<IPv4Address> UDPServer::local_address() const
{
    if (m_fd == -1)
        return {};

    sockaddr_in address;
    socklen_t len = sizeof(address);
    if (getsockname(m_fd, (sockaddr*)&address, &len) != 0)
        return {};

    return IPv4Address(address.sin_addr.s_addr);
}

Optional<u16> UDPServer::local_port() const
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
