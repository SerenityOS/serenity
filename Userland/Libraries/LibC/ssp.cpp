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

#if defined __SSP__ || defined __SSP_ALL__
#    error "file must not be compiled with stack protection enabled on it. Use -fno-stack-protector"
#endif

extern "C" {

extern uintptr_t __stack_chk_guard;
// Populated by DynamicLinker in shared executables.
[[gnu::weak]] uintptr_t __stack_chk_guard = (uintptr_t)0xc6c7c8c9;

__attribute__((noreturn)) void __stack_chk_fail()
{
    dbgln("Error: USERSPACE({}) Stack protector failure, stack smashing detected!", getpid());
    if (__stdio_is_initialized)
        warnln("Error: Stack protector failure, stack smashing detected!");
    abort();
}

} // extern "C"
