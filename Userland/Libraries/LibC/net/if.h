/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/API/POSIX/net/if.h>
#include <sys/cdefs.h>

__BEGIN_DECLS

unsigned int if_nametoindex(char const* ifname);
char* if_indextoname(unsigned int ifindex, char* ifname);
struct if_nameindex* if_nameindex(void);
void if_freenameindex(struct if_nameindex* ptr);

__END_DECLS
