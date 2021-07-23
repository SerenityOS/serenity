/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <inttypes.h>
#include <netinet/in.h>
#include <sys/cdefs.h>
#include <sys/socket.h>

__BEGIN_DECLS

#define INET_ADDRSTRLEN 16
#define INET6_ADDRSTRLEN 46

const char* inet_ntop(int af, const void* src, char* dst, socklen_t);
int inet_pton(int af, const char* src, void* dst);

static inline int inet_aton(const char* cp, struct in_addr* inp)
{
    return inet_pton(AF_INET, cp, inp);
}

char* inet_ntoa(struct in_addr);

__END_DECLS
