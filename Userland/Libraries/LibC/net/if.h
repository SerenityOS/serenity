/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Panagiotis Vasilopoulos <hello@alwayslivid.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <sys/cdefs.h>
#include <sys/socket.h>

__BEGIN_DECLS

/* Inteface flag definitions - Work in progress */
#define IFF_UP		0x1
#define IFF_BROADCAST		0x2
#define IFF_DEBUG		0x4
#define IFF_LOOPBACK		0x8
#define IFF_POINTOPOINT	0x10
#define IFF_NOTRAILERS	0x20
#define IFF_RUNNING		0x40
#define IFF_NOARP		0x80
#define IFF_PROMISC		0x100
#define IFF_ALLMULTI		0x200

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

__END_DECLS
