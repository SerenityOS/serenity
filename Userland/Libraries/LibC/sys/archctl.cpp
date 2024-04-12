/*
 * Copyright (c) 2024, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <errno.h>
#include <stdarg.h>
#include <sys/archctl.h>
#include <syscall.h>

extern "C" {

int archctl(int option, ...)
{
    va_list args;
    va_start(args, option);

    uintptr_t arg1 = va_arg(args, uintptr_t);

    va_end(args);

    int rc = syscall(SC_archctl, option, arg1);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}
}
