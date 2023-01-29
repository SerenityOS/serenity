/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/API/POSIX/sys/uio.h>
#include <sys/cdefs.h>

__BEGIN_DECLS

ssize_t writev(int fd, const struct iovec*, int iov_count);
ssize_t readv(int fd, const struct iovec*, int iov_count);
ssize_t pwritev(int fd, const struct iovec*, int iov_count, off_t);

__END_DECLS
