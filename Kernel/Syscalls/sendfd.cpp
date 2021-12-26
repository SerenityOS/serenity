/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/Net/LocalSocket.h>
#include <Kernel/Process.h>

namespace Kernel {

KResultOr<FlatPtr> Process::sys$sendfd(int sockfd, int fd)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    REQUIRE_PROMISE(sendfd);
    auto socket_description = TRY(fds().open_file_description(sockfd));
    if (!socket_description->is_socket())
        return ENOTSOCK;
    auto& socket = *socket_description->socket();
    if (!socket.is_local())
        return EAFNOSUPPORT;
    if (!socket.is_connected())
        return ENOTCONN;

    auto passing_description = TRY(fds().open_file_description(fd));
    auto& local_socket = static_cast<LocalSocket&>(socket);
    return local_socket.sendfd(*socket_description, move(passing_description));
}

KResultOr<FlatPtr> Process::sys$recvfd(int sockfd, int options)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    REQUIRE_PROMISE(recvfd);
    auto socket_description = TRY(fds().open_file_description(sockfd));
    if (!socket_description->is_socket())
        return ENOTSOCK;
    auto& socket = *socket_description->socket();
    if (!socket.is_local())
        return EAFNOSUPPORT;

    auto fd_allocation = TRY(m_fds.allocate());

    auto& local_socket = static_cast<LocalSocket&>(socket);
    auto received_description = TRY(local_socket.recvfd(*socket_description));

    u32 fd_flags = 0;
    if (options & O_CLOEXEC)
        fd_flags |= FD_CLOEXEC;

    m_fds[fd_allocation.fd].set(move(received_description), fd_flags);
    return fd_allocation.fd;
}

}
