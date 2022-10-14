/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <errno.h>
#include <string.h>
#include <syscall.h>
#include <utime.h>

extern "C" {

int utime(char const* pathname, const struct utimbuf* buf)
{
    if (!pathname) {
        errno = EFAULT;
        return -1;
    }
    int rc = syscall(SC_utime, pathname, strlen(pathname), buf);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}
}
