/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

// Includes essentially mandated by POSIX:
// https://pubs.opengroup.org/onlinepubs/9699919799/basedefs/arpa_inet.h.html
#include <inttypes.h>
#include <netinet/in.h>

#include <sys/cdefs.h>
#include <sys/socket.h>

__BEGIN_DECLS

#define INET_ADDRSTRLEN 16
#define INET6_ADDRSTRLEN 46

char const* inet_ntop(int af, void const* src, char* dst, socklen_t);
int inet_pton(int af, char const* src, void* dst);

static inline int inet_aton(char const* cp, struct in_addr* inp)
{
    return inet_pton(AF_INET, cp, inp);
}

char* inet_ntoa(struct in_addr);

__END_DECLS
