/*
 * Copyright (c) 2021, Justin Mietzner <sw1tchbl4d3@sw1tchbl4d3.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/API/POSIX/sys/statvfs.h>
#include <sys/cdefs.h>

__BEGIN_DECLS

int statvfs(char const* path, struct statvfs* buf);
int fstatvfs(int fd, struct statvfs* buf);

__END_DECLS
