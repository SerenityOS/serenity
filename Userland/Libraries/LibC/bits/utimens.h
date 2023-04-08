/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, sin-ack <sin-ack@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/API/POSIX/fcntl.h>
#include <Kernel/API/POSIX/sys/stat.h>
#include <sys/cdefs.h>

__BEGIN_DECLS

int __utimens(int fd, char const* path, struct timespec const times[2], int flag);

__END_DECLS
