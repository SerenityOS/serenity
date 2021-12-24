/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <bits/stdint.h>
#include <signal.h>
#include <stdbool.h>
#include <sys/cdefs.h>
#include <sys/types.h>

__BEGIN_DECLS

//
// /!\ This structure is accessed inside setjmp.S, keep both files in sync!
//

struct __jmp_buf {
#ifdef __i386__
    uint32_t ebx;
    uint32_t esi;
    uint32_t edi;
    uint32_t ebp;
    uint32_t esp;
    uint32_t eip;
#elif __x86_64__
    uint64_t rbx;
    uint64_t r12;
    uint64_t r13;
    uint64_t r14;
    uint64_t r15;
    uint64_t rbp;
    uint64_t rsp;
    uint64_t rip;
#elif __aarch64__
    // FIXME: This is likely incorrect.
    uint64_t regs[22];
#else
#    error
#endif
    int did_save_signal_mask;
    sigset_t saved_signal_mask;
};

typedef struct __jmp_buf jmp_buf[1];
typedef struct __jmp_buf sigjmp_buf[1];

/**
 * Since setjmp.h may be included by ports written in C, we need to guard this.
 */
#ifdef __cplusplus
#    ifdef __i386__
static_assert(sizeof(struct __jmp_buf) == 32, "struct __jmp_buf unsynchronized with i386/setjmp.S");
#    elif __x86_64__
static_assert(sizeof(struct __jmp_buf) == 72, "struct __jmp_buf unsynchronized with x86_64/setjmp.S");
#    elif __aarch64__
static_assert(sizeof(struct __jmp_buf) == 184, "struct __jmp_buf unsynchronized with aarch64/setjmp.S");
#    else
#        error
#    endif
#endif

/**
 * Calling conventions mandates that sigsetjmp() cannot call setjmp(),
 * otherwise the restored calling environment will not be the original caller's
 * but sigsetjmp()'s and we'll return to the wrong call site on siglongjmp().
 *
 * The setjmp(), sigsetjmp() and longjmp() functions have to be implemented in
 * assembly because they touch the call stack and registers in non-portable
 * ways. However, we *can* implement siglongjmp() as a standard C function.
 */

int setjmp(jmp_buf);
__attribute__((noreturn)) void longjmp(jmp_buf, int val);

int sigsetjmp(sigjmp_buf, int savesigs);
__attribute__((noreturn)) void siglongjmp(sigjmp_buf, int val);

/**
 * _setjmp() and _longjmp() are specified as behaving the exactly the same as
 * setjmp() and longjmp(), except they are not supposed to modify the signal mask.
 *
 * Our implementations already follow this restriction, so we just map them directly
 * to the same functions.
 *
 * https://pubs.opengroup.org/onlinepubs/9699969599/functions/_setjmp.html
 */
int _setjmp(jmp_buf);
__attribute__((noreturn)) void _longjmp(jmp_buf, int val);

__END_DECLS
