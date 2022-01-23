/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Types.h>
#include <assert.h>
#include <sys/internals.h>
#include <unistd.h>

extern "C" {

#ifdef NO_TLS
int errno;
#else
__thread int errno;
#endif
char** environ;
bool __environ_is_malloced;
bool __stdio_is_initialized;
bool s_global_initializers_ran;
void* __auxiliary_vector;

static void __auxiliary_vector_init();

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
