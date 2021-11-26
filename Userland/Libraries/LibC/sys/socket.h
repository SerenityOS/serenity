/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/API/POSIX/sys/socket.h>
#include <sys/un.h>

__BEGIN_DECLS

int socket(int domain, int type, int protocol);
int bind(int sockfd, const struct sockaddr* addr, socklen_t);
int listen(int sockfd, int backlog);
int accept(int sockfd, struct sockaddr*, socklen_t*);
int accept4(int sockfd, struct sockaddr*, socklen_t*, int);
int connect(int sockfd, const struct sockaddr*, socklen_t);
int shutdown(int sockfd, int how);
ssize_t send(int sockfd, const void*, size_t, int flags);
ssize_t sendmsg(int sockfd, const struct msghdr*, int flags);
ssize_t sendto(int sockfd, const void*, size_t, int flags, const struct sockaddr*, socklen_t);
ssize_t recv(int sockfd, void*, size_t, int flags);
ssize_t recvmsg(int sockfd, struct msghdr*, int flags);
ssize_t recvfrom(int sockfd, void*, size_t, int flags, struct sockaddr*, socklen_t*);
int getsockopt(int sockfd, int level, int option, void*, socklen_t*);
int setsockopt(int sockfd, int level, int option, const void*, socklen_t);
int getsockname(int sockfd, struct sockaddr*, socklen_t*);
int getpeername(int sockfd, struct sockaddr*, socklen_t*);
int socketpair(int domain, int type, int protocol, int sv[2]);
int sendfd(int sockfd, int fd);
int recvfd(int sockfd, int options);

// These three are non-POSIX, but common:
#define CMSG_ALIGN(x) (((x) + sizeof(void*) - 1) & ~(sizeof(void*) - 1))
#define CMSG_SPACE(x) (CMSG_ALIGN(sizeof(struct cmsghdr)) + CMSG_ALIGN(x))
#define CMSG_LEN(x) (CMSG_ALIGN(sizeof(struct cmsghdr)) + (x))

static inline struct cmsghdr* CMSG_FIRSTHDR(struct msghdr* msg)
{
    if (msg->msg_controllen < sizeof(struct cmsghdr))
        return 0;
    return (struct cmsghdr*)msg->msg_control;
}

static inline struct cmsghdr* CMSG_NXTHDR(struct msghdr* msg, struct cmsghdr* cmsg)
{
    struct cmsghdr* next = (struct cmsghdr*)((char*)cmsg + CMSG_ALIGN(cmsg->cmsg_len));
    unsigned offset = (char*)next - (char*)msg->msg_control;
    if (msg->msg_controllen < offset + sizeof(struct cmsghdr))
        return NULL;
    return next;
}

static inline void* CMSG_DATA(struct cmsghdr* cmsg)
{
    return (void*)(cmsg + 1);
}

__END_DECLS
