/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/API/POSIX/sys/types.h>
#include <Kernel/API/POSIX/sys/uio.h>
#include <Kernel/API/POSIX/sys/un.h>

#ifdef __cplusplus
extern "C" {
#endif

#define AF_MASK 0xff
#define AF_UNSPEC 0
#define AF_LOCAL 1
#define AF_UNIX AF_LOCAL
#define AF_INET 2
#define AF_INET6 3
#define AF_MAX 4
#define PF_LOCAL AF_LOCAL
#define PF_UNIX PF_LOCAL
#define PF_INET AF_INET
#define PF_INET6 AF_INET6
#define PF_UNSPEC AF_UNSPEC
#define PF_MAX AF_MAX

#define SOCK_TYPE_MASK 0xff
#define SOCK_STREAM 1
#define SOCK_DGRAM 2
#define SOCK_RAW 3
#define SOCK_RDM 4
#define SOCK_SEQPACKET 5
#define SOCK_NONBLOCK 04000
#define SOCK_CLOEXEC 02000000

#define SHUT_RD 0
#define SHUT_WR 1
#define SHUT_RDWR 2

#define IPPROTO_IP 0
#define IPPROTO_ICMP 1
#define IPPROTO_IGMP 2
#define IPPROTO_IPIP 4
#define IPPROTO_TCP 6
#define IPPROTO_UDP 17
#define IPPROTO_IPV6 41
#define IPPROTO_ESP 50
#define IPPROTO_AH 51
#define IPPROTO_ICMPV6 58
#define IPPROTO_RAW 255

#define MSG_TRUNC 0x1
#define MSG_CTRUNC 0x2
#define MSG_PEEK 0x4
#define MSG_OOB 0x8
#define MSG_DONTROUTE 0x10
#define MSG_WAITALL 0x20
#define MSG_DONTWAIT 0x40
#define MSG_NOSIGNAL 0x80
#define MSG_EOR 0x100

typedef uint16_t sa_family_t;

struct cmsghdr {
    socklen_t cmsg_len;
    int cmsg_level;
    int cmsg_type;
};

struct msghdr {
    void* msg_name;
    socklen_t msg_namelen;
    struct iovec* msg_iov;
    int msg_iovlen;
    void* msg_control;
    socklen_t msg_controllen;
    int msg_flags;
};

// These three are non-POSIX, but common:
#define CMSG_ALIGN(x) (((x) + sizeof(void*) - 1) & ~(sizeof(void*) - 1))
#define CMSG_SPACE(x) (CMSG_ALIGN(sizeof(struct cmsghdr)) + CMSG_ALIGN(x))
#define CMSG_LEN(x) (CMSG_ALIGN(sizeof(struct cmsghdr)) + (x))

static inline struct cmsghdr* CMSG_FIRSTHDR(struct msghdr* msg)
{
    if (msg->msg_controllen < sizeof(struct cmsghdr))
        return (struct cmsghdr*)0;
    return (struct cmsghdr*)msg->msg_control;
}

static inline struct cmsghdr* CMSG_NXTHDR(struct msghdr* msg, struct cmsghdr* cmsg)
{
    struct cmsghdr* next = (struct cmsghdr*)((char*)cmsg + CMSG_ALIGN(cmsg->cmsg_len));
    unsigned offset = (char*)next - (char*)msg->msg_control;
    if (msg->msg_controllen < offset + sizeof(struct cmsghdr))
        return (struct cmsghdr*)0;
    return next;
}

static inline void* CMSG_DATA(struct cmsghdr* cmsg)
{
    return (void*)(cmsg + 1);
}

struct sockaddr {
    sa_family_t sa_family;
    // For network interface ioctl(), this needs to fit all sockaddr_* structures (excluding Unix domain sockets).
    char sa_data[26];
};

struct ucred {
    pid_t pid;
    uid_t uid;
    gid_t gid;
};

struct linger {
    int l_onoff;
    int l_linger;
};

#define SOL_SOCKET 1
#define SOMAXCONN 128

enum {
    SO_RCVTIMEO,
    SO_SNDTIMEO,
    SO_TYPE,
    SO_ERROR,
    SO_PEERCRED,
    SO_RCVBUF,
    SO_SNDBUF,
    SO_DEBUG,
    SO_REUSEADDR,
    SO_BINDTODEVICE,
    SO_KEEPALIVE,
    SO_TIMESTAMP,
    SO_BROADCAST,
    SO_LINGER,
    SO_ACCEPTCONN,
    SO_DONTROUTE,
    SO_OOBINLINE,
    SO_SNDLOWAT,
    SO_RCVLOWAT,
};
#define SO_RCVTIMEO SO_RCVTIMEO
#define SO_SNDTIMEO SO_SNDTIMEO
#define SO_TYPE SO_TYPE
#define SO_ERROR SO_ERROR
#define SO_PEERCRED SO_PEERCRED
#define SO_DEBUG SO_DEBUG
#define SO_REUSEADDR SO_REUSEADDR
#define SO_BINDTODEVICE SO_BINDTODEVICE
#define SO_KEEPALIVE SO_KEEPALIVE
#define SO_TIMESTAMP SO_TIMESTAMP
#define SO_BROADCAST SO_BROADCAST
#define SO_SNDBUF SO_SNDBUF
#define SO_RCVBUF SO_RCVBUF
#define SO_LINGER SO_LINGER
#define SO_ACCEPTCONN SO_ACCEPTCONN
#define SO_DONTROUTE SO_DONTROUTE
#define SO_OOBINLINE SO_OOBINLINE
#define SO_SNDLOWAT SO_SNDLOWAT
#define SO_RCVLOWAT SO_RCVLOWAT

enum {
    SCM_TIMESTAMP,
    SCM_RIGHTS,
};
#define SCM_TIMESTAMP SCM_TIMESTAMP
#define SCM_RIGHTS SCM_RIGHTS

struct sockaddr_storage {
    sa_family_t ss_family;
    union {
        char data[sizeof(struct sockaddr_un)];
        void* alignment;
    };
};

#ifdef __cplusplus
}
#endif
