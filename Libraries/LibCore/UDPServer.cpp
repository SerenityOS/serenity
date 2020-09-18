/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/IPv4Address.h>
#include <AK/Types.h>
#include <LibCore/Notifier.h>
#include <LibCore/UDPServer.h>
#include <LibCore/UDPSocket.h>
#include <stdio.h>
#include <unistd.h>

#ifndef SOCK_NONBLOCK
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
    ASSERT(m_fd >= 0);
}

UDPServer::~UDPServer()
{
    ::close(m_fd);
}

bool UDPServer::bind(const IPv4Address& address, u16 port)
{
    if (m_bound)
        return false;

    int rc;
    auto saddr = SocketAddress(address, port);
    auto in = saddr.to_sockaddr_in();

    rc = ::bind(m_fd, (const sockaddr*)&in, sizeof(in));
    ASSERT(rc == 0);
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
    auto buf = ByteBuffer::create_uninitialized(size);
    socklen_t in_len = sizeof(in);
    ssize_t rlen = ::recvfrom(m_fd, buf.data(), size, 0, (sockaddr*)&in, &in_len);
    if (rlen < 0) {
        dbg() << "recvfrom: " << strerror(errno);
        return {};
    }

    buf.trim(rlen);
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
