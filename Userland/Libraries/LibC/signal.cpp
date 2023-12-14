/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <assert.h>
#include <bits/pthread_cancel.h>
#include <errno.h>
#include <setjmp.h>
#include <signal.h>
#include <string.h>
#include <sys/select.h>
#include <syscall.h>
#include <unistd.h>

extern "C" {

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/kill.html
int kill(pid_t pid, int sig)
{
    int rc = syscall(SC_kill, pid, sig);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/killpg.html
int killpg(int pgrp, int sig)
{
    int rc = syscall(SC_killpg, pgrp, sig);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/raise.html
int raise(int sig)
{
    // FIXME: Support multi-threaded programs.
    return kill(getpid(), sig);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/signal.html
sighandler_t signal(int signum, sighandler_t handler)
{
    struct sigaction new_act;
    struct sigaction old_act;
    new_act.sa_handler = handler;
    new_act.sa_flags = 0;
    new_act.sa_mask = 0;
    int rc = sigaction(signum, &new_act, &old_act);
    if (rc < 0)
        return SIG_ERR;
    return old_act.sa_handler;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/sigaction.html
int sigaction(int signum, const struct sigaction* act, struct sigaction* old_act)
{
    int rc = syscall(SC_sigaction, signum, act, old_act);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/sigemptyset.html
int sigemptyset(sigset_t* set)
{
    *set = 0;
    return 0;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/sigfillset.html
int sigfillset(sigset_t* set)
{
    *set = 0xffffffff;
    return 0;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/sigaddset.html
int sigaddset(sigset_t* set, int sig)
{
    if (sig < 1 || sig > 32) {
        errno = EINVAL;
        return -1;
    }
    *set |= 1 << (sig - 1);
    return 0;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/sigaltstack.html
int sigaltstack(stack_t const* ss, stack_t* old_ss)
{
    int rc = syscall(SC_sigaltstack, ss, old_ss);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/sigdelset.html
int sigdelset(sigset_t* set, int sig)
{
    if (sig < 1 || sig > 32) {
        errno = EINVAL;
        return -1;
    }
    *set &= ~(1 << (sig - 1));
    return 0;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/sigismember.html
int sigismember(sigset_t const* set, int sig)
{
    if (sig < 1 || sig > 32) {
        errno = EINVAL;
        return -1;
    }
    if (*set & (1 << (sig - 1)))
        return 1;
    return 0;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/sigprocmask.html
int sigprocmask(int how, sigset_t const* set, sigset_t* old_set)
{
    int rc = syscall(SC_sigprocmask, how, set, old_set);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/sigpending.html
int sigpending(sigset_t* set)
{
    int rc = syscall(SC_sigpending, set);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

char const* sys_signame[NSIG] = {
#define NAME(name, description) name,
    __ENUMERATE_SIGNALS(NAME)
#undef NAME
};

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/siglongjmp.html
void siglongjmp(jmp_buf env, int val)
{
    if (env->did_save_signal_mask) {
        int rc = sigprocmask(SIG_SETMASK, &env->saved_signal_mask, nullptr);
        assert(rc == 0);
    }
    longjmp(env, val);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/sigsuspend.html
int sigsuspend(sigset_t const* set)
{
    __pthread_maybe_cancel();

    int rc = syscall(SC_sigsuspend, set);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

// https://pubs.opengroup.org/onlinepubs/009604499/functions/sigwait.html
int sigwait(sigset_t const* set, int* sig)
{
    __pthread_maybe_cancel();

    int rc = syscall(Syscall::SC_sigtimedwait, set, nullptr, nullptr);
    VERIFY(rc != 0);
    if (rc < 0)
        return -rc;
    *sig = rc;
    return 0;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/sigwaitinfo.html
int sigwaitinfo(sigset_t const* set, siginfo_t* info)
{
    return sigtimedwait(set, info, nullptr);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/sigtimedwait.html
int sigtimedwait(sigset_t const* set, siginfo_t* info, struct timespec const* timeout)
{
    __pthread_maybe_cancel();

    int rc = syscall(Syscall::SC_sigtimedwait, set, info, timeout);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int getsignalbyname(char const* name)
{
    VERIFY(name);
    StringView name_sv { name, strlen(name) };
    for (size_t i = 1; i < NSIG; ++i) {
        if (!sys_signame[i])
            continue;

        StringView signal_name { sys_signame[i], strlen(sys_signame[i]) };
        if (signal_name == name_sv || (name_sv.starts_with("SIG"sv) && signal_name == name_sv.substring_view(3)))
            return i;
    }
    errno = EINVAL;
    return -1;
}

char const* getsignalname(int signal)
{
    if (signal <= 0 || signal >= NSIG) {
        errno = EINVAL;
        return nullptr;
    }

    auto const* result = sys_signame[signal];
    if (!result)
        errno = EINVAL;

    return result;
}
}
