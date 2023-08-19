/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <errno.h>
#include <stdarg.h>
#include <stdio.h>
#include <sys/prctl.h>
#include <syscall.h>

extern "C" {

int prctl(int option, ...)
{
    va_list args;
    va_start(args, option);

    uintptr_t arg1 = va_arg(args, uintptr_t);
    uintptr_t arg2 = va_arg(args, uintptr_t);
    uintptr_t arg3 = va_arg(args, uintptr_t);

    va_end(args);

    int rc = syscall(SC_prctl, option, arg1, arg2, arg3);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}
}
