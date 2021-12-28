/*
 * Copyright (c) 2021, Gunnar Beutner <gbeutner@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibC/errno.h>
#include <LibC/net/if.h>
#include <LibC/netinet/in.h>

const in6_addr in6addr_any = IN6ADDR_ANY_INIT;
const in6_addr in6addr_loopback = IN6ADDR_LOOPBACK_INIT;

unsigned int if_nametoindex([[maybe_unused]] const char* ifname)
{
    errno = ENODEV;
    return -1;
}

char* if_indextoname([[maybe_unused]] unsigned int ifindex, [[maybe_unused]] char* ifname)
{
    errno = ENXIO;
    return nullptr;
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
