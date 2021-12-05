/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/API/POSIX/ifaddrs.h>
#include <sys/cdefs.h>

__BEGIN_DECLS

int getifaddrs(struct ifaddrs** ifap);
void freeifaddrs(struct ifaddrs* ifa);

__END_DECLS
