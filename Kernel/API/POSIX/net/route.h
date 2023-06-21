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
    unsigned long rt_pad1;
    struct sockaddr rt_dst;     /* the target address */
    struct sockaddr rt_gateway; /* the gateway address */
    struct sockaddr rt_genmask; /* the target network mask */
    unsigned short int rt_flags;
    short rt_pad2;
    unsigned long rt_pad3;
    void* rt_pad4;
    short rt_metric;         /* route metric */
    char* rt_dev;            /* tie route to specific device */
    unsigned long rt_mtu;    /* Define MTU for this route */
    unsigned long rt_window; /* Window clamping for this route */
    unsigned short rt_irtt;  /* Initial round trip time */
};

#define RTF_UP 0x1        /* do not delete the route */
#define RTF_GATEWAY 0x2   /* the route is a gateway and not an end host */
#define RTF_HOST 0x4      /* host entry (net otherwise) */
#define RTF_REINSTATE 0x8 /* Reinstate route after a timeout */
#define RTF_DYNAMIC 0x10  /* Route created dynamically */
#define RTF_MODIFIED 0x20 /* Route modified dynamically */
#define RTF_MTU 0x40      /* Route has specific MTU */
#define RTF_WINDOW 0x80   /* Route has window clamping */
#define RTF_IRTT 0x100    /* Initial round trip time */
#define RTF_REJECT 0x200  /* Reject the route */

#ifdef __cplusplus
}
#endif
