/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/Format.h>
#include <err.h>
#include <stdlib.h>
#include <unistd.h>

// FIXME: Is it enough to have 4096 bytes?
static char print_buffer[4096];

void warn(char const* __format, ...)
{
    va_list ap;
    va_start(ap, __format);
    vwarn(__format, ap);
    va_end(ap);
}

void vwarn(char const* __format, va_list ap)
{
    vsprintf(print_buffer, __format, ap);
    warnln("{}: {}: {}", getprogname(), print_buffer, strerror(errno));
}

void warnx(char const* __format, ...)
{
    va_list ap;
    va_start(ap, __format);
    vsprintf(print_buffer, __format, ap);
    vwarnx(__format, ap);
    va_end(ap);
}

void vwarnx(char const* __format, va_list ap)
{
    vsprintf(print_buffer, __format, ap);
    warnln("{}: {}", getprogname(), print_buffer);
}

void err(int __status, char const* __format, ...)
{
    va_list ap;
    va_start(ap, __format);
    verr(__status, __format, ap);
    va_end(ap);
}

void verr(int __status, char const* __format, va_list ap)
{
    vsprintf(print_buffer, __format, ap);
    warnln("{}: {}: {}", getprogname(), print_buffer, strerror(errno));
    exit(__status);
    VERIFY_NOT_REACHED();
}

void errx(int __status, char const* __format, ...)
{
    va_list ap;
    va_start(ap, __format);
    vsprintf(print_buffer, __format, ap);
    verrx(__status, __format, ap);
    va_end(ap);
}

void verrx(int __status, char const* __format, va_list ap)
{
    vsprintf(print_buffer, __format, ap);
    warnln("{}: {}", getprogname(), print_buffer);
    exit(__status);
    VERIFY_NOT_REACHED();
}
