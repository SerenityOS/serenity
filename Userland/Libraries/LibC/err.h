/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <stdarg.h>

__BEGIN_DECLS

// NOTE: This file is for ported BSD applications and only for them.

// NOTE: The format is "program: ", __format, ": ", strerror(errno), and a newline.
// NOTE: This should be printed with stderr.
extern void warn(char const* __format, ...)
    __attribute__((__format__(__printf__, 1, 2)));
extern void vwarn(char const* __format, va_list)
    __attribute__((__format__(__printf__, 1, 0)));

// NOTE: The format is "program: ", __format and a newline.
// NOTE: This should be printed with stderr.
extern void warnx(char const* __format, ...)
    __attribute__((__format__(__printf__, 1, 2)));
extern void vwarnx(char const* __format, va_list)
    __attribute__((__format__(__printf__, 1, 0)));

// NOTE: The format is "program: ", __format, ": ", strerror(errno), and a newline.
// NOTE: This should be printed with stderr and then exit(__status)!
[[noreturn]] extern void err(int __status, char const* __format, ...)
    __attribute__((__format__(__printf__, 2, 3)));
[[noreturn]] extern void verr(int __status, char const* __format, va_list)
    __attribute__((__format__(__printf__, 2, 0)));

// NOTE: The format is "program: ", __format, and a newline.
// NOTE: This should be printed with stderr and then exit(__status)!
[[noreturn]] extern void errx(int __status, char const* __format, ...)
    __attribute__(( __format__(__printf__, 2, 3)));
[[noreturn]] extern void verrx(int __status, char const*, va_list)
    __attribute__((__format__(__printf__, 2, 0)));

__END_DECLS
