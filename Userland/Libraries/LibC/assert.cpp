/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <Kernel/API/prctl_numbers.h>
#include <assert.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/internals.h>
#include <syscall.h>
#include <unistd.h>

extern "C" {

extern bool __stdio_is_initialized;

void __assertion_failed(char const* msg)
{
    if (__heap_is_stable) {
        dbgln("ASSERTION FAILED: {}", msg);
        if (__stdio_is_initialized)
            warnln("ASSERTION FAILED: {}", msg);
    }

    Syscall::SC_set_coredump_metadata_params params {
        { "assertion", strlen("assertion") },
        { msg, strlen(msg) },
    };
    syscall(SC_prctl, PR_SET_COREDUMP_METADATA_VALUE, &params, nullptr, nullptr);
    abort();
}
}
