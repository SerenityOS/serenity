/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <sys/cdefs.h>
#include <stdarg.h>

__BEGIN_DECLS

void warn (const char *__format, ...)
     __attribute__ ((__format__ (__printf__, 1, 2)));
void vwarn (const char *__format, va_list)
     __attribute__ ((__format__ (__printf__, 1, 0)));

void warnx (const char *__format, ...)
     __attribute__ ((__format__ (__printf__, 1, 2)));
void vwarnx (const char *__format, va_list)
     __attribute__ ((__format__ (__printf__, 1, 0)));

void err (int __status, const char *__format, ...)
     __attribute__ ((noreturn, __format__ (__printf__, 2, 3)));
void verr (int __status, const char *__format, va_list)
     __attribute__ ((noreturn, __format__ (__printf__, 2, 0)));
void errx (int __status, const char *__format, ...)
     __attribute__ ((noreturn, __format__ (__printf__, 2, 3)));
void verrx (int __status, const char *, va_list)
     __attribute__ ((noreturn, __format__ (__printf__, 2, 0)));

__END_DECLS
