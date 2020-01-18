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

#include <AK/Assertions.h>
#include <Kernel/Syscall.h>
#include <errno.h>
#include <stdio.h>
#include <sys/socket.h>

extern "C" {

int socket(int domain, int type, int protocol)
{
    int rc = syscall(SC_socket, domain, type, protocol);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int bind(int sockfd, const sockaddr* addr, socklen_t addrlen)
{
    int rc = syscall(SC_bind, sockfd, addr, addrlen);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int listen(int sockfd, int backlog)
{
    int rc = syscall(SC_listen, sockfd, backlog);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int accept(int sockfd, sockaddr* addr, socklen_t* addrlen)
{
    int rc = syscall(SC_accept, sockfd, addr, addrlen);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int connect(int sockfd, const sockaddr* addr, socklen_t addrlen)
{
    int rc = syscall(SC_connect, sockfd, addr, addrlen);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

ssize_t sendto(int sockfd, const void* data, size_t data_length, int flags, const struct sockaddr* addr, socklen_t addr_length)
{
    Syscall::SC_sendto_params params { sockfd, { data, data_length }, flags, addr, addr_length };
    int rc = syscall(SC_sendto, &params);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

ssize_t send(int sockfd, const void* data, size_t data_length, int flags)
{
    return sendto(sockfd, data, data_length, flags, nullptr, 0);
}

ssize_t recvfrom(int sockfd, void* buffer, size_t buffer_length, int flags, struct sockaddr* addr, socklen_t* addr_length)
{
    Syscall::SC_recvfrom_params params { sockfd, { buffer, buffer_length }, flags, addr, addr_length };
    int rc = syscall(SC_recvfrom, &params);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

ssize_t recv(int sockfd, void* buffer, size_t buffer_length, int flags)
{
    return recvfrom(sockfd, buffer, buffer_length, flags, nullptr, nullptr);
}

int getsockopt(int sockfd, int level, int option, void* value, socklen_t* value_size)
{
    Syscall::SC_getsockopt_params params { sockfd, level, option, value, value_size };
    int rc = syscall(SC_getsockopt, &params);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int setsockopt(int sockfd, int level, int option, const void* value, socklen_t value_size)
{
    Syscall::SC_setsockopt_params params { sockfd, level, option, value, value_size };
    int rc = syscall(SC_setsockopt, &params);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int getsockname(int sockfd, struct sockaddr* addr, socklen_t* addrlen)
{
    int rc = syscall(SC_getsockname, sockfd, addr, addrlen);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int getpeername(int sockfd, struct sockaddr* addr, socklen_t* addrlen)
{
    int rc = syscall(SC_getpeername, sockfd, addr, addrlen);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}
}
