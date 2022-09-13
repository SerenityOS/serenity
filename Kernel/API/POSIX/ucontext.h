/*
 * Copyright (c) 2022, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/API/POSIX/signal.h>
#include <Kernel/API/POSIX/sys/types.h>
#include <Kernel/Arch/mcontext.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef struct __mcontext mcontext_t;

typedef struct __ucontext {
    struct __ucontext* uc_link;
    sigset_t uc_sigmask;
    stack_t uc_stack;
    mcontext_t uc_mcontext;
} ucontext_t;

#define ILL_ILLOPC 0
#define ILL_ILLOPN 1
#define ILL_ILLADR 2
#define ILL_ILLTRP 3
#define ILL_PRVOPC 4
#define ILL_PRVREG 5
#define ILL_COPROC 6
#define ILL_BADSTK 7

#define FPE_INTDIV 0
#define FPE_INTOVF 1
#define FPE_FLTDIV 2
#define FPE_FLTOVF 3
#define FPE_FLTUND 4
#define FPE_FLTRES 5
#define FPE_FLTINV 6

#define SEGV_MAPERR 0
#define SEGV_ACCERR 1

#define BUS_ADRALN 0
#define BUS_ADRERR 1
#define BUS_OBJERR 2

#define TRAP_BRKPT 0
#define TRAP_TRACE 1

#define SI_USER 0x40000000
#define SI_QUEUE 0x40000001
#define SI_TIMER 0x40000002
#define SI_ASYNCIO 0x40000003
#define SI_MESGQ 0x40000004
#define SI_NOINFO 0x40000042

#ifdef __cplusplus
}
#endif
