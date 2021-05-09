/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <bits/stdint.h>
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

struct in6_addr {
    uint8_t s6_addr[16];
};

struct sockaddr_in6 {
    sa_family_t sin6_family;   // AF_INET6.
    in_port_t sin6_port;       // Port number.
    uint32_t sin6_flowinfo;    // IPv6 traffic class and flow information.
    struct in6_addr sin6_addr; // IPv6 address.
    uint32_t sin6_scope_id;    // Set of interfaces for a scop
};

__END_DECLS
