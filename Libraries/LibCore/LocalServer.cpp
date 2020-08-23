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

#include <LibCore/LocalServer.h>
#include <LibCore/LocalSocket.h>
#include <LibCore/Notifier.h>
#include <stdio.h>
#include <sys/socket.h>
#include <sys/stat.h>
#include <unistd.h>

#ifndef SOCK_NONBLOCK
#    include <sys/ioctl.h>
#endif

namespace Core {

LocalServer::LocalServer(Object* parent)
    : Object(parent)
{
}

LocalServer::~LocalServer()
{
    if (m_fd >= 0)
        ::close(m_fd);
}

bool LocalServer::take_over_from_system_server()
{
    if (m_listening)
        return false;

    constexpr auto socket_takeover = "SOCKET_TAKEOVER";

    if (getenv(socket_takeover)) {
        // Sanity check: it has to be a socket.
        struct stat stat;
        int rc = fstat(3, &stat);
        if (rc == 0 && S_ISSOCK(stat.st_mode)) {
            // The SystemServer has passed us the socket as fd 3,
            // so use that instead of creating our own.
            m_fd = 3;
            // It had to be !CLOEXEC for obvious reasons, but we
            // don't need it to be !CLOEXEC anymore, so set the
            // CLOEXEC flag now.
            fcntl(m_fd, F_SETFD, FD_CLOEXEC);
            // We wouldn't want our children to think we're passing
            // them a socket either, so unset the env variable.
            unsetenv(socket_takeover);

            m_listening = true;
            setup_notifier();
            return true;
        } else {
            if (rc != 0)
                perror("fstat");
            dbg() << "It's not a socket, what the heck??";
        }
    }

    dbg() << "Failed to take the socket over from SystemServer";

    return false;
}

void LocalServer::setup_notifier()
{
    m_notifier = Notifier::construct(m_fd, Notifier::Event::Read, this);
    m_notifier->on_ready_to_read = [this] {
        if (on_ready_to_accept)
            on_ready_to_accept();
    };
}

bool LocalServer::listen(const String& address)
{
    if (m_listening)
        return false;

    int rc;

#ifdef SOCK_NONBLOCK
    m_fd = socket(AF_LOCAL, SOCK_STREAM | SOCK_NONBLOCK | SOCK_CLOEXEC, 0);
#else
    m_fd = socket(AF_LOCAL, SOCK_STREAM, 0);
    int option = 1;
    ioctl(m_fd, FIONBIO, &option);
    fcntl(m_fd, F_SETFD, FD_CLOEXEC);
#endif
    ASSERT(m_fd >= 0);
#ifndef __APPLE__
    rc = fchmod(m_fd, 0600);
    if (rc < 0) {
        perror("fchmod");
        ASSERT_NOT_REACHED();
    }
#endif

    auto socket_address = SocketAddress::local(address);
    auto un_optional = socket_address.to_sockaddr_un();
    if (!un_optional.has_value()) {
        perror("bind");
        return false;
    }
    auto un = un_optional.value();
    rc = ::bind(m_fd, (const sockaddr*)&un, sizeof(un));
    if (rc < 0) {
        perror("bind");
        return false;
    }

    rc = ::listen(m_fd, 5);
    if (rc < 0) {
        perror("listen");
        return false;
    }

    m_listening = true;
    setup_notifier();
    return true;
}

RefPtr<LocalSocket> LocalServer::accept()
{
    ASSERT(m_listening);
    sockaddr_un un;
    socklen_t un_size = sizeof(un);
    int accepted_fd = ::accept(m_fd, (sockaddr*)&un, &un_size);
    if (accepted_fd < 0) {
        perror("accept");
        return nullptr;
    }

    return LocalSocket::construct(accepted_fd);
}

}
