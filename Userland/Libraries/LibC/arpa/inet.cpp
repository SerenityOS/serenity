/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/IPv6Address.h>
#include <arpa/inet.h>
#include <errno.h>
#include <netinet/in.h>
#include <stdio.h>

extern "C" {

char const* inet_ntop(int af, void const* src, char* dst, socklen_t len)
{
    if (af == AF_INET) {
        if (len < INET_ADDRSTRLEN) {
            errno = ENOSPC;
            return nullptr;
        }
        auto* bytes = (unsigned char const*)src;
        snprintf(dst, len, "%u.%u.%u.%u", bytes[0], bytes[1], bytes[2], bytes[3]);
        return (char const*)dst;
    } else if (af == AF_INET6) {
        if (len < INET6_ADDRSTRLEN) {
            errno = ENOSPC;
            return nullptr;
        }
        auto str_or_error = IPv6Address(((in6_addr const*)src)->s6_addr).to_string();
        if (str_or_error.is_error()) {
            errno = ENOMEM;
            return nullptr;
        }
        auto str = str_or_error.release_value();
        if (!str.bytes_as_string_view().copy_characters_to_buffer(dst, len)) {
            errno = ENOSPC;
            return nullptr;
        }
        return (char const*)dst;
    }

    errno = EAFNOSUPPORT;
    return nullptr;
}

int inet_pton(int af, char const* src, void* dst)
{
    if (af == AF_INET) {
        unsigned a, b, c, d;
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
    } else if (af == AF_INET6) {
        auto addr = IPv6Address::from_string({ src, strlen(src) });
        if (!addr.has_value()) {
            errno = EINVAL;
            return 0;
        }

        memcpy(dst, addr->to_in6_addr_t(), sizeof(in6_addr));
        return 1;
    }

    errno = EAFNOSUPPORT;
    return -1;
}

in_addr_t inet_addr(char const* str)
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
