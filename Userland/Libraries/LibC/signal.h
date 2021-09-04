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
int pthread_sigmask(int how, sigset_t const* set, sigset_t* old_set);
int sigaction(int sig, const struct sigaction* act, struct sigaction* old_act);
int sigemptyset(sigset_t*);
int sigfillset(sigset_t*);
int sigaddset(sigset_t*, int sig);
int sigdelset(sigset_t*, int sig);
int sigismember(sigset_t const*, int sig);
int sigprocmask(int how, sigset_t const* set, sigset_t* old_set);
int sigpending(sigset_t*);
int sigsuspend(sigset_t const*);
int raise(int sig);
int getsignalbyname(char const*);
char const* getsignalname(int);

extern char const* sys_siglist[NSIG];
extern char const* sys_signame[NSIG];

__END_DECLS
