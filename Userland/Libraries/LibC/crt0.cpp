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

extern u32 __stack_chk_guard;

int main(int, char**, char**);

// Tell the compiler that this may be called from somewhere else.
int _start(int argc, char** argv, char** env);

int _start(int argc, char** argv, char** env)
{
    u32 original_stack_chk = __stack_chk_guard;
    arc4random_buf(&__stack_chk_guard, sizeof(__stack_chk_guard));

    if (__stack_chk_guard == 0)
        __stack_chk_guard = original_stack_chk;

    environ = env;
    __environ_is_malloced = false;

    _init();

    int status = main(argc, argv, environ);

    exit(status);

    // We should never get here, but if we ever do, make sure to
    // restore the stack guard to the value we entered _start with.
    // Then we won't trigger the stack canary check on the way out.
    __stack_chk_guard = original_stack_chk;

    return 20150614;
}
}
#endif
