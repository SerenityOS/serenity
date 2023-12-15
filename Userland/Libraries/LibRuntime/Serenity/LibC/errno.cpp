/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <errno.h>

int* __errno_location()
{
#ifdef NO_TLS
    static int errno_storage = 0;
#else
    thread_local static int errno_storage = 0;
#endif
    return &errno_storage;
}
