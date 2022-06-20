/*
 * Copyright (c) 2020, Marios Prokopakis <mariosprokopakis@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/API/POSIX/sys/socket.h>
#include <Kernel/API/POSIX/sys/types.h>

#ifdef __cplusplus
extern "C" {
#endif

struct rtentry {
    struct sockaddr rt_dst;     /* the target address */
    struct sockaddr rt_gateway; /* the gateway address */
    struct sockaddr rt_genmask; /* the target network mask */
    unsigned short int rt_flags;
    char* rt_dev;
    /* FIXME: complete the struct */
};

#define RTF_UP 0x1      /* do not delete the route */
#define RTF_GATEWAY 0x2 /* the route is a gateway and not an end host */
#define RTF_HOST 0x4    /* host entry (net otherwise) */

#ifdef __cplusplus
}
#endif
