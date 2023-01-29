/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <errno_codes.h>
#include <sys/cdefs.h>

#define __RETURN_WITH_ERRNO(rc, good_ret, bad_ret) \
    do {                                           \
        if (rc < 0) {                              \
            errno = -rc;                           \
            return (bad_ret);                      \
        }                                          \
        return (good_ret);                         \
    } while (0)

__BEGIN_DECLS

extern char const* const sys_errlist[];
extern int sys_nerr;

#ifdef NO_TLS
extern int errno;
#else
extern __thread int errno;
#endif

int* __errno_location() __attribute__((const));
#define errno (*__errno_location())

__END_DECLS
