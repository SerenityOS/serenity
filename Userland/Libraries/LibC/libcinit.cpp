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

extern "C" {

#ifdef NO_TLS
int errno_storage;
#else
__thread int errno_storage;
#endif
[[gnu::weak]] char** environ;
bool __environ_is_malloced = false;
bool __stdio_is_initialized;
void* __auxiliary_vector;

static void __auxiliary_vector_init();

int* __errno_location()
{
    return &errno_storage;
}

void __libc_init()
{
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
