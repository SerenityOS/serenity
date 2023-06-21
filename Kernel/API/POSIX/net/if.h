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

#define IF_NAMESIZE 16

enum {
    IFF_UP = 1 << 0,
    IFF_BROADCAST = 1 << 1,
    IFF_DEBUG = 1 << 2,
    IFF_LOOPBACK = 1 << 3,
    IFF_POINTOPOINT = 1 << 4,
    IFF_RUNNING = 1 << 5,
    IFF_NOARP = 1 << 6,
    IFF_PROMISC = 1 << 7,
    IFF_ALLMULTI = 1 << 8,
    IFF_MULTICAST = 1 << 9,
};
#define IFF_UP IFF_UP
#define IFF_BROADCAST IFF_BROADCAST
#define IFF_DEBUG IFF_DEBUG
#define IFF_LOOPBACK IFF_LOOPBACK
#define IFF_POINTOPOINT IFF_POINTOPOINT
#define IFF_RUNNING IFF_RUNNING
#define IFF_NOARP IFF_NOARP
#define IFF_PROMISC IFF_PROMISC
#define IFF_ALLMULTI IFF_ALLMULTI
#define IFF_MULTICAST IFF_MULTICAST

struct ifconf {
    int ifc_len;
    union {
        void* ifc_buf;
        struct ifreq* ifc_req;
    };
};

struct ifreq {
#define IFNAMSIZ 16
    char ifr_name[IFNAMSIZ];
    union {
        struct sockaddr ifru_addr;
        struct sockaddr ifru_dstaddr;
        struct sockaddr ifru_broadaddr;
        struct sockaddr ifru_netmask;
        struct sockaddr ifru_hwaddr;
        short ifru_flags;
        int ifru_metric;
        int64_t ifru_vnetid;
        uint64_t ifru_media;
        void* ifru_data;
        unsigned int ifru_index;
    } ifr_ifru;

#define ifr_addr ifr_ifru.ifru_addr           // address
#define ifr_dstaddr ifr_ifru.ifru_dstaddr     // other end of p-to-p link
#define ifr_broadaddr ifr_ifru.ifru_broadaddr // broadcast address
#define ifr_netmask ifr_ifru.ifru_netmask     // network mask
#define ifr_flags ifr_ifru.ifru_flags         // flags
#define ifr_metric ifr_ifru.ifru_metric       // metric
#define ifr_mtu ifr_ifru.ifru_metric          // mtu (overload)
#define ifr_hardmtu ifr_ifru.ifru_metric      // hardmtu (overload)
#define ifr_media ifr_ifru.ifru_media         // media options
#define ifr_rdomainid ifr_ifru.ifru_metric    // VRF instance (overload)
#define ifr_vnetid ifr_ifru.ifru_vnetid       // Virtual Net Id
#define ifr_ttl ifr_ifru.ifru_metric          // tunnel TTL (overload)
#define ifr_data ifr_ifru.ifru_data           // for use by interface
#define ifr_index ifr_ifru.ifru_index         // interface index
#define ifr_llprio ifr_ifru.ifru_metric       // link layer priority
#define ifr_hwaddr ifr_ifru.ifru_hwaddr       // MAC address
};

struct if_nameindex {
    unsigned int if_index;
    char* if_name;
};

#ifdef __cplusplus
}
#endif
