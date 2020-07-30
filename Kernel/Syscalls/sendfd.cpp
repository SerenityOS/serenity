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

#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/Net/LocalSocket.h>
#include <Kernel/Process.h>

namespace Kernel {

int Process::sys$sendfd(int sockfd, int fd)
{
    REQUIRE_PROMISE(sendfd);
    auto socket_description = file_description(sockfd);
    if (!socket_description)
        return -EBADF;
    if (!socket_description->is_socket())
        return -ENOTSOCK;
    auto& socket = *socket_description->socket();
    if (!socket.is_local())
        return -EAFNOSUPPORT;
    if (!socket.is_connected())
        return -ENOTCONN;

    auto passing_descriptor = file_description(fd);
    if (!passing_descriptor)
        return -EBADF;

    auto& local_socket = static_cast<LocalSocket&>(socket);
    return local_socket.sendfd(*socket_description, *passing_descriptor);
}

int Process::sys$recvfd(int sockfd)
{
    REQUIRE_PROMISE(recvfd);
    auto socket_description = file_description(sockfd);
    if (!socket_description)
        return -EBADF;
    if (!socket_description->is_socket())
        return -ENOTSOCK;
    auto& socket = *socket_description->socket();
    if (!socket.is_local())
        return -EAFNOSUPPORT;

    int new_fd = alloc_fd();
    if (new_fd < 0)
        return new_fd;

    auto& local_socket = static_cast<LocalSocket&>(socket);
    auto received_descriptor_or_error = local_socket.recvfd(*socket_description);

    if (received_descriptor_or_error.is_error())
        return received_descriptor_or_error.error();

    m_fds[new_fd].set(*received_descriptor_or_error.value(), 0);
    return new_fd;
}

}
