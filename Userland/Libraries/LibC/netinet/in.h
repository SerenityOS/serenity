/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <bits/stdint.h>
#include <endian.h>
#include <sys/cdefs.h>
#include <sys/socket.h>

__BEGIN_DECLS

typedef uint32_t in_addr_t;
in_addr_t inet_addr(const char*);

#define INADDR_ANY ((in_addr_t)0)
#define INADDR_NONE ((in_addr_t)-1)
#define INADDR_LOOPBACK 0x7f000001

#define IN_LOOPBACKNET 127

#define IP_TTL 2
#define IP_MULTICAST_LOOP 3
#define IP_ADD_MEMBERSHIP 4
#define IP_DROP_MEMBERSHIP 5
#define IP_MULTICAST_IF 6
#define IP_MULTICAST_TTL 7

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

struct in6_addr {
    uint8_t s6_addr[16];
};

#define IN6ADDR_ANY_INIT                               \
    {                                                  \
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 \
    }

extern struct in6_addr in6addr_any;

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

static inline uint16_t htons(uint16_t value)
{
#if __BYTE_ORDER == __LITTLE_ENDIAN
    return __builtin_bswap16(value);
#else
    return value;
#endif
}

static inline uint16_t ntohs(uint16_t value)
{
    return htons(value);
}

static inline uint32_t htonl(uint32_t value)
{
#if __BYTE_ORDER == __LITTLE_ENDIAN
    return __builtin_bswap32(value);
#else
    return value;
#endif
}

static inline uint32_t ntohl(uint32_t value)
{
    return htonl(value);
}

#define IN6_IS_ADDR_LOOPBACK(addr) \
    (addr->s6_addr[0] == 0 && addr->s6_addr[1] == 0 && addr->s6_addr[2] == 0 && addr->s6_addr[3] == 0 && addr->s6_addr[4] == 0 && addr->s6_addr[5] == 0 && addr->s6_addr[6] == 0 && addr->s6_addr[7] == 0 && addr->s6_addr[8] == 0 && addr->s6_addr[9] == 0 && addr->s6_addr[10] == 0 && addr->s6_addr[11] == 0 && addr->s6_addr[12] == 0 && addr->s6_addr[13] == 0 && addr->s6_addr[14] == 0 && addr->s6_addr[15] == 1)

#define IN6_IS_ADDR_V4MAPPED(addr) \
    (addr->s6_addr[0] == 0 && addr->s6_addr[1] == 0 && addr->s6_addr[2] == 0 && addr->s6_addr[3] == 0 && addr->s6_addr[4] == 0 && addr->s6_addr[5] == 0 && addr->s6_addr[6] == 0 && addr->s6_addr[7] == 0 && addr->s6_addr[8] == 0xff && addr->s6_addr[9] == 0xff && addr->s6_addr[10] == 0xff && addr->s6_addr[11] == 0xff)

__END_DECLS
