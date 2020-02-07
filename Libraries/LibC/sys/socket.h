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

#pragma once

#include <bits/stdint.h>
#include <sys/cdefs.h>
#include <sys/types.h>
#include <sys/un.h>

__BEGIN_DECLS

#define AF_MASK 0xff
#define AF_UNSPEC 0
#define AF_LOCAL 1
#define AF_UNIX AF_LOCAL
#define AF_INET 2
#define PF_LOCAL AF_LOCAL
#define PF_UNIX PF_LOCAL
#define PF_INET AF_INET

#define SOCK_TYPE_MASK 0xff
#define SOCK_STREAM 1
#define SOCK_DGRAM 2
#define SOCK_RAW 3
#define SOCK_NONBLOCK 04000
#define SOCK_CLOEXEC 02000000

#define SHUT_RD 1
#define SHUT_WR 2
#define SHUT_RDWR 3

#define IPPROTO_IP 0
#define IPPROTO_ICMP 1
#define IPPROTO_TCP 6
#define IPPROTO_UDP 17

#define MSG_DONTWAIT 0x40

struct sockaddr {
    uint16_t sa_family;
    char sa_data[14];
};

struct ucred {
    pid_t pid;
    uid_t uid;
    gid_t gid;
};

#define SOL_SOCKET 1
#define SOMAXCONN 128

#define SO_RCVTIMEO 1
#define SO_SNDTIMEO 2
#define SO_KEEPALIVE 3
#define SO_ERROR 4
#define SO_PEERCRED 5

int socket(int domain, int type, int protocol);
int bind(int sockfd, const struct sockaddr* addr, socklen_t);
int listen(int sockfd, int backlog);
int accept(int sockfd, struct sockaddr*, socklen_t*);
int connect(int sockfd, const struct sockaddr*, socklen_t);
int shutdown(int sockfd, int how);
ssize_t send(int sockfd, const void*, size_t, int flags);
ssize_t sendto(int sockfd, const void*, size_t, int flags, const struct sockaddr*, socklen_t);
ssize_t recv(int sockfd, void*, size_t, int flags);
ssize_t recvfrom(int sockfd, void*, size_t, int flags, struct sockaddr*, socklen_t*);
int getsockopt(int sockfd, int level, int option, void*, socklen_t*);
int setsockopt(int sockfd, int level, int option, const void*, socklen_t);
int getsockname(int sockfd, struct sockaddr*, socklen_t*);
int getpeername(int sockfd, struct sockaddr*, socklen_t*);

__END_DECLS
