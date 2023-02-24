/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/Net/LocalSocket.h>
#include <Kernel/Tasks/Process.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$sendfd(int sockfd, int fd)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::sendfd));
    auto socket_description = TRY(open_file_description(sockfd));
    if (!socket_description->is_socket())
        return ENOTSOCK;
    auto& socket = *socket_description->socket();
    if (!socket.is_local())
        return EAFNOSUPPORT;
    if (!socket.is_connected())
        return ENOTCONN;

    auto passing_description = TRY(open_file_description(fd));
    auto& local_socket = static_cast<LocalSocket&>(socket);
    TRY(local_socket.sendfd(*socket_description, move(passing_description)));
    return 0;
}

ErrorOr<FlatPtr> Process::sys$recvfd(int sockfd, int options)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::recvfd));
    auto socket_description = TRY(open_file_description(sockfd));
    if (!socket_description->is_socket())
        return ENOTSOCK;
    auto& socket = *socket_description->socket();
    if (!socket.is_local())
        return EAFNOSUPPORT;

    auto fd_allocation = TRY(m_fds.with_exclusive([](auto& fds) { return fds.allocate(); }));

    auto& local_socket = static_cast<LocalSocket&>(socket);
    auto received_description = TRY(local_socket.recvfd(*socket_description));

    u32 fd_flags = 0;
    if (options & O_CLOEXEC)
        fd_flags |= FD_CLOEXEC;

    m_fds.with_exclusive([&](auto& fds) { fds[fd_allocation.fd].set(move(received_description), fd_flags); });
    return fd_allocation.fd;
}

}
