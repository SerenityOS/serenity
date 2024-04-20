/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Types.h>
#include <assert.h>
#include <errno.h>
#include <sys/auxv.h>
#include <sys/internals.h>
#include <unistd.h>

[[gnu::weak]] char** __environ_value() asm("__environ_value");

extern "C" {

#ifdef NO_TLS
int errno_storage;
#else
__thread int errno_storage;
#endif
bool __environ_is_malloced = false;
bool __stdio_is_initialized = false;
void* __auxiliary_vector = reinterpret_cast<void*>(explode_byte(0xe1));

#ifndef _DYNAMIC_LOADER
char** environ = reinterpret_cast<char**>(explode_byte(0xe2));
uintptr_t __stack_chk_guard;
#endif

int* __errno_location()
{
    return &errno_storage;
}

void __libc_init()
{
#ifndef _DYNAMIC_LOADER
    // We can only call magic functions until __stack_chk_guard is initialized.
    environ = __environ_value();
#endif

    char** env;
    for (env = environ; *env; ++env)
        ;
    __auxiliary_vector = (void*)++env;

#ifndef _DYNAMIC_LOADER
    for (auxv_t* entry = reinterpret_cast<auxv_t*>(__auxiliary_vector); entry->a_type != AT_NULL; ++entry)
        if (entry->a_type == AT_RANDOM)
            __stack_chk_guard = *(reinterpret_cast<u64*>(entry->a_un.a_ptr) + 1);

    // We include an additional hardening: zero the first byte of the stack guard to avoid leaking
    // or overwriting the stack guard with C-style string functions.
    __stack_chk_guard &= ~0xffULL;
#endif
    __malloc_init();
    __stdio_init();
}
}
