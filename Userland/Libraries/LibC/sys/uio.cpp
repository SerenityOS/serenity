/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <bits/pthread_cancel.h>
#include <errno.h>
#include <sys/uio.h>
#include <syscall.h>

extern "C" {

ssize_t writev(int fd, const struct iovec* iov, int iov_count)
{
    return pwritev(fd, iov, iov_count, -1);
}

ssize_t readv(int fd, const struct iovec* iov, int iov_count)
{
    __pthread_maybe_cancel();

    int rc = syscall(SC_readv, fd, iov, iov_count);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

ssize_t pwritev(int fd, struct iovec const* iov, int iov_count, off_t offset)
{
    __pthread_maybe_cancel();

    int rc = syscall(SC_pwritev, fd, iov, iov_count, offset);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}
}
