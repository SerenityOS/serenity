/*
 * Copyright (c) 2021, Brian Gianforcaro <bgianf@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <AK/Types.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/internals.h>
#include <unistd.h>

extern "C" [[gnu::noreturn, gnu::no_stack_protector]] void __stack_chk_fail()
{
    dbgln("Error: USERSPACE({}) Stack protector failure, stack smashing detected!", getpid());
    if (__stdio_is_initialized)
        warnln("Error: Stack protector failure, stack smashing detected!");
    abort();
}
