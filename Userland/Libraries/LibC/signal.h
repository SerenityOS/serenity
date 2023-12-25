/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

// Includes essentially mandated by POSIX:
// https://pubs.opengroup.org/onlinepubs/9699919799/basedefs/signal.h.html
#include <time.h>

#include <Kernel/API/POSIX/signal.h>
#include <Kernel/API/POSIX/ucontext.h>
#include <bits/sighow.h>
#include <sys/cdefs.h>
#include <sys/types.h>

__BEGIN_DECLS

int kill(pid_t, int sig);
int killpg(int pgrp, int sig);
sighandler_t signal(int sig, sighandler_t);
int pthread_sigmask(int how, sigset_t const* set, sigset_t* old_set);
int sigaction(int sig, const struct sigaction* act, struct sigaction* old_act);
int sigemptyset(sigset_t*);
int sigfillset(sigset_t*);
int sigaddset(sigset_t*, int sig);
int sigaltstack(stack_t const* ss, stack_t* old_ss);
int sigdelset(sigset_t*, int sig);
int siginterrupt(int sig, int flag);
int sigismember(sigset_t const*, int sig);
int sigprocmask(int how, sigset_t const* set, sigset_t* old_set);
int sigpending(sigset_t*);
int sigsuspend(sigset_t const*);
int sigtimedwait(sigset_t const*, siginfo_t*, struct timespec const*);
int sigwait(sigset_t const*, int*);
int sigwaitinfo(sigset_t const*, siginfo_t*);
int raise(int sig);
int getsignalbyname(char const*);
char const* getsignalname(int);

extern char const* sys_siglist[NSIG];
extern char const* sys_signame[NSIG];

__END_DECLS
