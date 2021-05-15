/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/Net/LocalSocket.h>
#include <Kernel/Process.h>

namespace Kernel {

KResultOr<int> Process::sys$sendfd(int sockfd, int fd)
{
    REQUIRE_PROMISE(sendfd);
    auto socket_description = file_description(sockfd);
    if (!socket_description)
        return EBADF;
    if (!socket_description->is_socket())
        return ENOTSOCK;
    auto& socket = *socket_description->socket();
    if (!socket.is_local())
        return EAFNOSUPPORT;
    if (!socket.is_connected())
        return ENOTCONN;

    auto passing_descriptor = file_description(fd);
    if (!passing_descriptor)
        return EBADF;

    auto& local_socket = static_cast<LocalSocket&>(socket);
    return local_socket.sendfd(*socket_description, *passing_descriptor);
}

KResultOr<int> Process::sys$recvfd(int sockfd, int options)
{
    REQUIRE_PROMISE(recvfd);
    auto socket_description = file_description(sockfd);
    if (!socket_description)
        return EBADF;
    if (!socket_description->is_socket())
        return ENOTSOCK;
    auto& socket = *socket_description->socket();
    if (!socket.is_local())
        return EAFNOSUPPORT;

    int new_fd = alloc_fd();
    if (new_fd < 0)
        return new_fd;

    auto& local_socket = static_cast<LocalSocket&>(socket);
    auto received_descriptor_or_error = local_socket.recvfd(*socket_description);

    if (received_descriptor_or_error.is_error())
        return received_descriptor_or_error.error();

    u32 fd_flags = 0;
    if (options & O_CLOEXEC)
        fd_flags |= FD_CLOEXEC;

    m_fds[new_fd].set(*received_descriptor_or_error.value(), fd_flags);
    return new_fd;
}

}
