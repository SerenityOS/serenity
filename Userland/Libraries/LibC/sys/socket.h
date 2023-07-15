/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

// Includes essentially mandated by POSIX:
// https://pubs.opengroup.org/onlinepubs/9699919799/basedefs/sys_socket.h.html
#include <sys/uio.h>

#include <Kernel/API/POSIX/sys/socket.h>
#include <sys/cdefs.h>
#include <sys/un.h>

__BEGIN_DECLS

int socket(int domain, int type, int protocol);
int bind(int sockfd, const struct sockaddr* addr, socklen_t);
int listen(int sockfd, int backlog);
int accept(int sockfd, struct sockaddr*, socklen_t*);
int accept4(int sockfd, struct sockaddr*, socklen_t*, int);
int connect(int sockfd, const struct sockaddr*, socklen_t);
int shutdown(int sockfd, int how);
ssize_t send(int sockfd, void const*, size_t, int flags);
ssize_t sendmsg(int sockfd, const struct msghdr*, int flags);
ssize_t sendto(int sockfd, void const*, size_t, int flags, const struct sockaddr*, socklen_t);
ssize_t recv(int sockfd, void*, size_t, int flags);
ssize_t recvmsg(int sockfd, struct msghdr*, int flags);
ssize_t recvfrom(int sockfd, void*, size_t, int flags, struct sockaddr*, socklen_t*);
int getsockopt(int sockfd, int level, int option, void*, socklen_t*);
int setsockopt(int sockfd, int level, int option, void const*, socklen_t);
int getsockname(int sockfd, struct sockaddr*, socklen_t*);
int getpeername(int sockfd, struct sockaddr*, socklen_t*);
int socketpair(int domain, int type, int protocol, int sv[2]);
int sendfd(int sockfd, int fd);
int recvfd(int sockfd, int options);

__END_DECLS
