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

int main(int, char**, char**);

// Tell the compiler that this may be called from somewhere else.
int _entry(int argc, char** argv) __attribute__((used));
void _start(int, char**, char**) __attribute__((used));

NAKED void _start(int, char**, char**)
{
#    if ARCH(AARCH64)
    asm(
        "mov x29, 0\n"
        "mov x30, 0\n"
        "bl _entry\n");
#    else
    asm(
        "push $0\n"
        "jmp _entry@plt\n");
#    endif
}

int _entry(int argc, char** argv)
{
    __begin_atexit_locking();

    int status = main(argc, argv, environ);

    exit(status);

    return 20150614;
}
}
#endif
