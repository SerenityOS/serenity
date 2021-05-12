/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/LocalSocket.h>
#include <errno.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/socket.h>
#include <sys/stat.h>

#ifndef SOCK_NONBLOCK
#    include <sys/ioctl.h>
#endif

namespace Core {

LocalSocket::LocalSocket(int fd, Object* parent)
    : Socket(Socket::Type::Local, parent)
{
    // NOTE: This constructor is used by LocalServer::accept(), so the socket is already connected.
    m_connected = true;
    set_fd(fd);
    set_mode(OpenMode::ReadWrite);
    set_error(0);
}

LocalSocket::LocalSocket(Object* parent)
    : Socket(Socket::Type::Local, parent)
{
#ifdef SOCK_NONBLOCK
    int fd = socket(AF_LOCAL, SOCK_STREAM | SOCK_NONBLOCK | SOCK_CLOEXEC, 0);
#else
    int fd = socket(AF_LOCAL, SOCK_STREAM, 0);
    int option = 1;
    ioctl(fd, FIONBIO, &option);
    fcntl(fd, F_SETFD, FD_CLOEXEC);
#endif

    if (fd < 0) {
        set_error(errno);
    } else {
        set_fd(fd);
        set_mode(OpenMode::ReadWrite);
        set_error(0);
    }
}

LocalSocket::~LocalSocket()
{
}

HashMap<String, int> LocalSocket::s_overtaken_sockets {};
bool LocalSocket::s_overtaken_sockets_parsed { false };

void LocalSocket::parse_sockets_from_system_server()
{
    VERIFY(!s_overtaken_sockets_parsed);

    constexpr auto socket_takeover = "SOCKET_TAKEOVER";
    const char* sockets = getenv(socket_takeover);
    if (!sockets) {
        s_overtaken_sockets_parsed = true;
        return;
    }

    for (auto& socket : StringView(sockets).split_view(' ')) {
        auto params = socket.split_view(':');
        s_overtaken_sockets.set(params[0].to_string(), strtol(params[1].to_string().characters(), nullptr, 10));
    }

    s_overtaken_sockets_parsed = true;
    // We wouldn't want our children to think we're passing
    // them a socket either, so unset the env variable.
    unsetenv(socket_takeover);
}

RefPtr<LocalSocket> LocalSocket::take_over_accepted_socket_from_system_server(String const& socket_path)
{
    if (!s_overtaken_sockets_parsed)
        parse_sockets_from_system_server();

    int fd;
    if (socket_path.is_null()) {
        // We want the first (and only) socket.
        VERIFY(s_overtaken_sockets.size() == 1);
        fd = s_overtaken_sockets.begin()->value;
    } else {
        auto it = s_overtaken_sockets.find(socket_path);
        if (it == s_overtaken_sockets.end()) {
            dbgln("Non-existent socket requested");
            return nullptr;
        }
        fd = it->value;
    }

    // Sanity check: it has to be a socket.
    struct stat stat;
    int rc = fstat(fd, &stat);
    if (rc < 0 || !S_ISSOCK(stat.st_mode)) {
        if (rc != 0)
            perror("fstat");
        dbgln("ERROR: The fd we got from SystemServer is not a socket");
        return nullptr;
    }

    auto socket = LocalSocket::construct(fd);

    // It had to be !CLOEXEC for obvious reasons, but we
    // don't need it to be !CLOEXEC anymore, so set the
    // CLOEXEC flag now.
    fcntl(fd, F_SETFD, FD_CLOEXEC);
    return socket;
}

}
