/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <arpa/inet.h>
#include <errno.h>
#include <netinet/in.h>
#include <stdio.h>

extern "C" {

const char* inet_ntop(int af, const void* src, char* dst, socklen_t len)
{
    if (af != AF_INET) {
        errno = EAFNOSUPPORT;
        return nullptr;
    }
    if (len < 4) {
        errno = ENOSPC;
        return nullptr;
    }
    auto* bytes = (const unsigned char*)src;
    snprintf(dst, len, "%u.%u.%u.%u", bytes[0], bytes[1], bytes[2], bytes[3]);
    return (const char*)dst;
}

int inet_pton(int af, const char* src, void* dst)
{
    if (af != AF_INET) {
        errno = EAFNOSUPPORT;
        return -1;
    }
    unsigned a;
    unsigned b;
    unsigned c;
    unsigned d;
    int count = sscanf(src, "%u.%u.%u.%u", &a, &b, &c, &d);
    if (count != 4) {
        errno = EINVAL;
        return 0;
    }
    union {
        struct {
            uint8_t a;
            uint8_t b;
            uint8_t c;
            uint8_t d;
        };
        uint32_t l;
    } u;
    u.a = a;
    u.b = b;
    u.c = c;
    u.d = d;
    *(uint32_t*)dst = u.l;
    return 1;
}

in_addr_t inet_addr(const char* str)
{
    in_addr_t tmp {};
    int rc = inet_pton(AF_INET, str, &tmp);
    if (rc <= 0)
        return INADDR_NONE;
    return tmp;
}

char* inet_ntoa(struct in_addr in)
{
    static char buffer[32];
    inet_ntop(AF_INET, &in.s_addr, buffer, sizeof(buffer));
    return buffer;
}
}
