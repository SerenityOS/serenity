/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#ifdef __cplusplus
extern "C" {
#endif

#include <Kernel/API/POSIX/sys/socket.h>
#include <Kernel/API/POSIX/sys/types.h>

struct arpreq {
    struct sockaddr arp_pa;      /* protocol address */
    struct sockaddr arp_ha;      /* hardware address */
    struct sockaddr arp_netmask; /* netmask of protocol address */
    int arp_flags;               /* flags */
    char arp_dev[16];
};

#define ARPHRD_ETHER 1
#define ARPHRD_IEEE802 6
#define ARPHRD_SLIP 256
#define ARPHRD_PPP 512
#define ARPHRD_LOOPBACK 772
#define ARPHRD_FDDI 774
#define ARPHRD_IEEE802_TR 800

#ifdef __cplusplus
}
#endif
