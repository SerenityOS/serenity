/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Types.h>
#include <assert.h>
#include <errno.h>
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

static void __auxiliary_vector_init();

int* __errno_location()
{
    return &errno_storage;
}

void __libc_init([[maybe_unused]] uintptr_t cookie)
{
#ifndef _DYNAMIC_LOADER
    __stack_chk_guard = cookie;
    environ = __environ_value();
#endif
    __auxiliary_vector_init();
    __malloc_init();
    __stdio_init();
}

static void __auxiliary_vector_init()
{
    char** env;
    for (env = environ; *env; ++env) {
    }

    __auxiliary_vector = (void*)++env;
}
}
