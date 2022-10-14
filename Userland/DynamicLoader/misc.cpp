/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "misc.h"
#include <AK/Format.h>

extern "C" {
char const* __cxa_demangle(char const*, void*, void*, int*)
{
    dbgln("WARNING: __cxa_demangle not supported");
    return "";
}

void* __dso_handle __attribute__((__weak__));
}
