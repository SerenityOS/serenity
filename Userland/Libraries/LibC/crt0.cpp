/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Types.h>
#include <assert.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/internals.h>
#include <unistd.h>

#ifndef _DYNAMIC_LOADER
extern "C" {

extern size_t __stack_chk_guard;
extern bool s_global_initializers_ran;

int main(int, char**, char**);

// Tell the compiler that this may be called from somewhere else.
int _entry(int argc, char** argv, char** env) __attribute__((used));
void _start(int, char**, char**) __attribute__((used));

NAKED void _start(int, char**, char**)
{
#    ifdef AK_ARCH_AARCH64
    asm(
        "bl _entry\n");
#    else
    asm(
        "push $0\n"
        "jmp _entry@plt\n");
#    endif
}

int _entry(int argc, char** argv, char** env)
{
    environ = env;
    __environ_is_malloced = false;
    __begin_atexit_locking();

    s_global_initializers_ran = true;

    _init();

    int status = main(argc, argv, environ);

    exit(status);

    return 20150614;
}
}
#endif
