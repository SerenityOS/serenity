/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#define FD_SETSIZE 1024
#define FD_ZERO(set) memset((set), 0, sizeof(fd_set));
#define FD_CLR(fd, set) ((set)->fds_bits[(fd / 8)] &= ~(1 << (fd) % 8))
#define FD_SET(fd, set) ((set)->fds_bits[(fd / 8)] |= (1 << (fd) % 8))
#define FD_ISSET(fd, set) ((set)->fds_bits[(fd / 8)] & (1 << (fd) % 8))

struct __fd_set {
    unsigned char fds_bits[FD_SETSIZE / 8];
};

typedef struct __fd_set fd_set;
