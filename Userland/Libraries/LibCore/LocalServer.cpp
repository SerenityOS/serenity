/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/LocalServer.h>
#include <LibCore/LocalSocket.h>
#include <LibCore/Notifier.h>
#include <LibCore/System.h>
#include <fcntl.h>
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

ErrorOr<void> LocalServer::take_over_from_system_server(String const& socket_path)
{
    if (m_listening)
        return Error::from_string_literal("Core::LocalServer: Can't perform socket takeover when already listening"sv);

    if (!LocalSocket::s_overtaken_sockets_parsed)
        LocalSocket::parse_sockets_from_system_server();

    int fd = -1;
    if (socket_path.is_null()) {
        // We want the first (and only) socket.
        if (LocalSocket::s_overtaken_sockets.size() == 1) {
            fd = LocalSocket::s_overtaken_sockets.begin()->value;
        }
    } else {
        auto it = LocalSocket::s_overtaken_sockets.find(socket_path);
        if (it != LocalSocket::s_overtaken_sockets.end()) {
            fd = it->value;
        }
    }

    if (fd < 0)
        return Error::from_string_literal("Core::LocalServer: No file descriptor for socket takeover"sv);

    // Sanity check: it has to be a socket.
    auto stat = TRY(Core::System::fstat(fd));

    if (!S_ISSOCK(stat.st_mode))
        return Error::from_string_literal("Core::LocalServer: Attempted socket takeover with non-socket file descriptor"sv);

    // It had to be !CLOEXEC for obvious reasons, but we
    // don't need it to be !CLOEXEC anymore, so set the
    // CLOEXEC flag now.
    TRY(Core::System::fcntl(fd, F_SETFD, FD_CLOEXEC));

    // The SystemServer has passed us the socket, so use that instead of
    // creating our own.
    m_fd = fd;

    m_listening = true;
    setup_notifier();
    return {};
}

void LocalServer::setup_notifier()
{
    m_notifier = Notifier::construct(m_fd, Notifier::Event::Read, this);
    m_notifier->on_ready_to_read = [this] {
        if (on_accept) {
            if (auto client_socket = accept())
                on_accept(client_socket.release_nonnull());
        }
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
    VERIFY(m_fd >= 0);
#ifndef AK_OS_MACOS
    rc = fchmod(m_fd, 0600);
    if (rc < 0) {
        perror("fchmod");
        VERIFY_NOT_REACHED();
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
    VERIFY(m_listening);
    sockaddr_un un;
    socklen_t un_size = sizeof(un);
#ifndef AK_OS_MACOS
    int accepted_fd = ::accept4(m_fd, (sockaddr*)&un, &un_size, SOCK_NONBLOCK | SOCK_CLOEXEC);
#else
    int accepted_fd = ::accept(m_fd, (sockaddr*)&un, &un_size);
#endif
    if (accepted_fd < 0) {
        perror("accept");
        return nullptr;
    }

#ifdef AK_OS_MACOS
    int option = 1;
    ioctl(m_fd, FIONBIO, &option);
    (void)fcntl(accepted_fd, F_SETFD, FD_CLOEXEC);
#endif

    return LocalSocket::construct(accepted_fd);
}

}
