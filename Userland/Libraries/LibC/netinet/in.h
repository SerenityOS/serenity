/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/API/POSIX/netinet/in.h>
#include <endian.h>

__BEGIN_DECLS

in_addr_t inet_addr(char const*);

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
    ((addr)->s6_addr[0] == 0 && (addr)->s6_addr[1] == 0 && (addr)->s6_addr[2] == 0 && (addr)->s6_addr[3] == 0 && (addr)->s6_addr[4] == 0 && (addr)->s6_addr[5] == 0 && (addr)->s6_addr[6] == 0 && (addr)->s6_addr[7] == 0 && (addr)->s6_addr[8] == 0 && (addr)->s6_addr[9] == 0 && (addr)->s6_addr[10] == 0 && (addr)->s6_addr[11] == 0 && (addr)->s6_addr[12] == 0 && (addr)->s6_addr[13] == 0 && (addr)->s6_addr[14] == 0 && (addr)->s6_addr[15] == 1)

#define IN6_IS_ADDR_V4MAPPED(addr) \
    ((((addr)->s6_addr[0]) == 0) && (((addr)->s6_addr[1]) == 0) && (((addr)->s6_addr[2]) == 0) && (((addr)->s6_addr[3]) == 0) && (((addr)->s6_addr[4]) == 0) && (((addr)->s6_addr[5]) == 0) && (((addr)->s6_addr[6]) == 0) && (((addr)->s6_addr[7]) == 0) && (((addr)->s6_addr[8]) == 0) && (((addr)->s6_addr[9]) == 0) && (((addr)->s6_addr[10]) == 0xFF) && (((addr)->s6_addr[11]) == 0xFF))

__END_DECLS
