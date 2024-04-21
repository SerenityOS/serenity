/*
 * Copyright (c) 2020-2023, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <sys/cdefs.h>
#include <sys/types.h>

__BEGIN_DECLS

typedef void (*AtExitFunction)(void*);

extern void __libc_init();
extern void __malloc_init(void);
extern void __stdio_init(void);
extern void __begin_atexit_locking(void);
extern void _init(void);
extern bool __environ_is_malloced;
extern bool __stdio_is_initialized;
extern bool __heap_is_stable;
extern void* __auxiliary_vector;

int __cxa_atexit(AtExitFunction exit_function, void* parameter, void* dso_handle);
void __cxa_finalize(void* dso_handle);
__attribute__((noreturn)) void __cxa_pure_virtual(void) __attribute__((weak));
__attribute__((noreturn)) void __stack_chk_fail(void);
__attribute__((noreturn)) void __stack_chk_fail_local(void);

struct __tls_index {
    size_t ti_module;
    size_t ti_offset;
};

void* __tls_get_addr(__tls_index*);

__END_DECLS
