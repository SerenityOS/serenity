/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/API/POSIX/signal_numbers.h>
#include <Kernel/API/POSIX/sys/types.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef void (*__sighandler_t)(int);
typedef __sighandler_t sighandler_t;

typedef uint32_t sigset_t;
typedef uint32_t sig_atomic_t;

union sigval {
    int sival_int;
    void* sival_ptr;
};

typedef struct siginfo {
    int si_signo;
    int si_code;
    int si_errno;
    pid_t si_pid;
    uid_t si_uid;
    void* si_addr;
    int si_status;
    int si_band;
    union sigval si_value;
} siginfo_t;

struct sigaction {
    union {
        void (*sa_handler)(int);
        void (*sa_sigaction)(int, siginfo_t*, void*);
    };
    sigset_t sa_mask;
    int sa_flags;
};

typedef struct {
    void* ss_sp;
    int ss_flags;
    size_t ss_size;
} stack_t;

#define SS_ONSTACK 1
#define SS_DISABLE 2

// FIXME: These values are arbitrary, and might be platform dependent
#define MINSIGSTKSZ 4096 // Minimum allowed
#define SIGSTKSZ 32768   // Recommended size

#define SIG_DFL ((__sighandler_t)0)
#define SIG_ERR ((__sighandler_t)(-1))
#define SIG_IGN ((__sighandler_t)1)

#define SA_NOCLDSTOP 1
#define SA_NOCLDWAIT 2
#define SA_SIGINFO 4
#define SA_ONSTACK 0x08000000
#define SA_RESTART 0x10000000
#define SA_NODEFER 0x40000000
#define SA_RESETHAND 0x80000000

#define SA_NOMASK SA_NODEFER
#define SA_ONESHOT SA_RESETHAND

#define SIG_BLOCK 0
#define SIG_UNBLOCK 1
#define SIG_SETMASK 2

#define CLD_EXITED 0
#define CLD_KILLED 1
#define CLD_DUMPED 2
#define CLD_TRAPPED 3
#define CLD_STOPPED 4
#define CLD_CONTINUED 5

#define FPE_INTDIV 0
#define FPE_INTOVF 1
#define FPE_FLTDIV 2
#define FPE_FLTOVF 3
#define FPE_FLTUND 4
#define FPE_FLTRES 5
#define FPE_FLTINV 6
#define FPE_FLTSUB 7

#ifdef __cplusplus
}
#endif
