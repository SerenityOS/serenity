/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/API/POSIX/signal.h>
#include <bits/sighow.h>
#include <signal_numbers.h>
#include <sys/types.h>

__BEGIN_DECLS

int kill(pid_t, int sig);
int killpg(int pgrp, int sig);
sighandler_t signal(int sig, sighandler_t);
int pthread_sigmask(int how, const sigset_t* set, sigset_t* old_set);
int sigaction(int sig, const struct sigaction* act, struct sigaction* old_act);
int sigemptyset(sigset_t*);
int sigfillset(sigset_t*);
int sigaddset(sigset_t*, int sig);
int sigaltstack(const stack_t* ss, stack_t* old_ss);
int sigdelset(sigset_t*, int sig);
int sigismember(const sigset_t*, int sig);
int sigprocmask(int how, const sigset_t* set, sigset_t* old_set);
int sigpending(sigset_t*);
int sigsuspend(const sigset_t*);
int sigtimedwait(sigset_t const*, siginfo_t*, struct timespec const*);
int sigwait(sigset_t const*, int*);
int sigwaitinfo(sigset_t const*, siginfo_t*);
int raise(int sig);
int getsignalbyname(const char*);
const char* getsignalname(int);

extern const char* sys_siglist[NSIG];
extern const char* sys_signame[NSIG];

__END_DECLS
