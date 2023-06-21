/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/API/POSIX/sys/types.h>

#ifdef __cplusplus
extern "C" {
#endif

#define POLLIN (1u << 0)
#define POLLRDNORM POLLIN
#define POLLPRI (1u << 1)
#define POLLOUT (1u << 2)
#define POLLWRNORM POLLOUT
#define POLLERR (1u << 3)
#define POLLHUP (1u << 4)
#define POLLNVAL (1u << 5)
#define POLLWRBAND (1u << 12)
#define POLLRDHUP (1u << 13)

struct pollfd {
    int fd;
    short events;
    short revents;
};

typedef unsigned nfds_t;

#ifdef __cplusplus
}
#endif
