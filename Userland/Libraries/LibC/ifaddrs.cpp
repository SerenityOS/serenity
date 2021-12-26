/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibC/errno.h>
#include <LibC/ifaddrs.h>

int getifaddrs(struct ifaddrs**)
{
    errno = ENOSYS;
    return -1;
}

void freeifaddrs(struct ifaddrs*)
{
}
