/*
 * Copyright (c) 2021, Brian Gianforcaro <bgianf@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <sys/internals.h>

#if defined __SSP__ || defined __SSP_ALL__
#    error "file must not be compiled with stack protection enabled on it. Use -fno-stack-protector"
#endif

extern "C" {

__attribute__((noreturn)) void __stack_chk_fail_local()
{
    __stack_chk_fail();
}

} // extern "C"
