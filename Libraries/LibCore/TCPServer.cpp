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

#include <AK/IPv4Address.h>
#include <AK/Types.h>
#include <LibCore/Notifier.h>
#include <LibCore/TCPServer.h>
#include <LibCore/TCPSocket.h>
#include <stdio.h>
#include <sys/socket.h>

namespace Core {

TCPServer::TCPServer(Object* parent)
    : Object(parent)
{
    m_fd = socket(AF_INET, SOCK_STREAM | SOCK_NONBLOCK | SOCK_CLOEXEC, 0);
    ASSERT(m_fd >= 0);
}

TCPServer::~TCPServer()
{
}

bool TCPServer::listen(const IPv4Address& address, u16 port)
{
    if (m_listening)
        return false;

    int rc;
    auto socket_address = SocketAddress(address, port);
    auto in = socket_address.to_sockaddr_in();
    rc = ::bind(m_fd, (const sockaddr*)&in, sizeof(in));
    ASSERT(rc == 0);

    rc = ::listen(m_fd, 5);
    ASSERT(rc == 0);
    m_listening = true;

    m_notifier = Notifier::construct(m_fd, Notifier::Event::Read, this);
    m_notifier->on_ready_to_read = [this] {
        if (on_ready_to_accept)
            on_ready_to_accept();
    };
    return true;
}

RefPtr<TCPSocket> TCPServer::accept()
{
    ASSERT(m_listening);
    sockaddr_in in;
    socklen_t in_size = sizeof(in);
    int accepted_fd = ::accept(m_fd, (sockaddr*)&in, &in_size);
    if (accepted_fd < 0) {
        perror("accept");
        return nullptr;
    }

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
