/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/API/POSIX/sys/socket.h>
#include <Kernel/API/POSIX/sys/types.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef uint32_t in_addr_t;

#define INADDR_ANY ((in_addr_t)0)
#define INADDR_NONE ((in_addr_t)(-1))
#define INADDR_LOOPBACK 0x7f000001
#define INADDR_BROADCAST 0xffffffff

#define IN_CLASSA_NET 0xff000000
#define IN_CLASSB_NET 0xffff0000
#define IN_CLASSC_NET 0xffffff00

#define IN_LOOPBACKNET 127

#define IP_TOS 1
#define IP_TTL 2
#define IP_MULTICAST_LOOP 3
#define IP_ADD_MEMBERSHIP 4
#define IP_DROP_MEMBERSHIP 5
#define IP_MULTICAST_IF 6
#define IP_MULTICAST_TTL 7
#define IP_BLOCK_SOURCE 8
#define IP_UNBLOCK_SOURCE 9
#define IP_OPTIONS 10

#define IPTOS_LOWDELAY 16
#define IPTOS_THROUGHPUT 8
#define IPTOS_RELIABILITY 4

/* Make sure these don't overlap with any other IPv4 and IPv6 options */
#define MCAST_JOIN_SOURCE_GROUP 100
#define MCAST_LEAVE_SOURCE_GROUP 101

#define IPPORT_RESERVED 1024
#define IPPORT_USERRESERVED 5000

typedef uint16_t in_port_t;

struct in_addr {
    uint32_t s_addr;
};

struct sockaddr_in {
    sa_family_t sin_family;
    in_port_t sin_port;
    struct in_addr sin_addr;
    char sin_zero[8];
};

struct ip_mreq {
    struct in_addr imr_multiaddr;
    struct in_addr imr_interface;
};

struct group_source_req {
    uint32_t gsr_interface;
    struct sockaddr_storage gsr_group;
    struct sockaddr_storage gsr_source;
};

struct ip_mreq_source {
    struct in_addr imr_multiaddr;
    struct in_addr imr_sourceaddr;
    struct in_addr imr_interface;
};

#define IPV6_UNICAST_HOPS 1
#define IPV6_MULTICAST_HOPS 2
#define IPV6_MULTICAST_LOOP 3
#define IPV6_MULTICAST_IF 4
#define IPV6_ADD_MEMBERSHIP 5
#define IPV6_DROP_MEMBERSHIP 6
#define IP_ADD_SOURCE_MEMBERSHIP 7
#define IP_DROP_SOURCE_MEMBERSHIP 8
#define IPV6_V6ONLY 9
#define IPV6_JOIN_GROUP 5
#define IPV6_LEAVE_GROUP 6
#define IPV6_RECVPKTINFO 10
#define IPV6_PKTINFO 11
#define IPV6_RECVHOPLIMIT 12
#define IPV6_HOPLIMIT 13

struct in6_addr {
    union {
        uint8_t s6_addr[16];
        uint32_t s6_addr32[4];
    };
};

struct in6_pktinfo {
    struct in6_addr ipi6_addr;
    uint32_t ipi6_ifindex;
};

/* clang-format off */
#define IN6ADDR_ANY_INIT      { { { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 } } }
#define IN6ADDR_LOOPBACK_INIT { { { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 } } }
/* clang-format on */

extern const struct in6_addr in6addr_any;
extern const struct in6_addr in6addr_loopback;

struct sockaddr_in6 {
    sa_family_t sin6_family;   // AF_INET6.
    in_port_t sin6_port;       // Port number.
    uint32_t sin6_flowinfo;    // IPv6 traffic class and flow information.
    struct in6_addr sin6_addr; // IPv6 address.
    uint32_t sin6_scope_id;    // Set of interfaces for a scop
};

struct ipv6_mreq {
    struct in6_addr ipv6mr_multiaddr;
    uint32_t ipv6mr_interface;
};

#ifdef __cplusplus
}
#endif
