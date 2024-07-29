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

extern "C" {

int main(int, char**, char**);

// Tell the compiler that this may be called from somewhere else.
int _entry(int argc, char** argv) __attribute__((used));
void _start(int, char**, char**) __attribute__((used));

NAKED void _start(int, char**, char**)
{
#if ARCH(AARCH64)
    asm(
        "mov x29, 0\n"
        "mov x30, 0\n"
        "bl _entry\n");
#elif ARCH(RISCV64)
    asm(
        "li fp, 0\n"
        "li ra, 0\n"
        "tail _entry@plt\n");
#elif ARCH(X86_64)
    asm(
        "push $0\n"
        "jmp _entry@plt\n");
#else
#    error "Unknown architecture"
#endif
}

int _entry(int argc, char** argv)
{
    // In the original crt0.cpp, we didn't need this line, but we don't
    // run with libc as **defined** dependency (i.e. shared object),
    // therefore the expected initialization of the dynamic libc.so that
    // should done by the DynamicLinker is never actually done,
    // specifically at DynamicLinker::initialize_libc method.
    // Thus, let's initialize our own "copy" of libc in this binary so
    // everything could work as expected.
    __libc_init();

    __begin_atexit_locking();

    int status = main(argc, argv, environ);

    exit(status);

    return 20150614;
}
}
