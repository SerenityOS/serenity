/*
 * Copyright (c) 2021, Gunnar Beutner <gbeutner@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <errno.h>
#include <net/if.h>
#include <netinet/in.h>
#include <string.h>
#include <sys/ioctl.h>
#include <sys/socket.h>

in6_addr const in6addr_any = IN6ADDR_ANY_INIT;
in6_addr const in6addr_loopback = IN6ADDR_LOOPBACK_INIT;

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/if_nametoindex.html
unsigned int if_nametoindex([[maybe_unused]] char const* ifname)
{
    int dummy_socket = socket(AF_INET, SOCK_DGRAM, 0);
    if (dummy_socket < 0) {
        errno = -dummy_socket;
        return 0;
    }

    struct ifreq ifr;
    memcpy(ifr.ifr_name, ifname, IF_NAMESIZE);

    int rc = ioctl(dummy_socket, SIOCGIFINDEX, &ifr);
    if (rc) {
        errno = -rc;
        return 0;
    }

    return ifr.ifr_index;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/if_indextoname.html#tag_16_236
char* if_indextoname([[maybe_unused]] unsigned int ifindex, [[maybe_unused]] char* ifname)
{
    int dummy_socket = socket(AF_INET, SOCK_DGRAM, 0);
    if (dummy_socket < 0) {
        errno = -dummy_socket;
        return 0;
    }

    struct ifreq ifr;
    ifr.ifr_index = ifindex;

    int rc = ioctl(dummy_socket, SIOCGIFNAME, &ifr);
    if (rc) {
        errno = -rc;
        return nullptr;
    }

    memcpy(ifname, ifr.ifr_name, IF_NAMESIZE);
    return ifname;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/if_nameindex.html
struct if_nameindex* if_nameindex()
{
    errno = ENOSYS;
    return nullptr;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/if_freenameindex.html
void if_freenameindex(struct if_nameindex*)
{
}
