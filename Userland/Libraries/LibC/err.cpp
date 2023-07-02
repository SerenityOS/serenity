/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <err.h>
#include <AK/Assertions.h>

void warn (const char*, ...)
{
}

void vwarn (const char*, va_list)
{
}

void warnx (const char*, ...)
{
}
void vwarnx (const char* , va_list)
{
}

void err (int, const char* , ...)
{
     VERIFY_NOT_REACHED();
}

void verr (int, const char*, va_list)
{
     VERIFY_NOT_REACHED();
}

void errx (int, const char*, ...)
{
     VERIFY_NOT_REACHED();
}

void verrx (int, const char*, va_list)
{
     VERIFY_NOT_REACHED();
}
