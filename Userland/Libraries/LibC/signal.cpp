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

// https://pubs.opengroup.org/onlinepubs/009696699/functions/siginterrupt.html
int siginterrupt(int sig, int flag)
{
    struct sigaction act;
    int rc = sigaction(sig, nullptr, &act);
    if (rc < 0)
        return rc;
    if (flag)
        act.sa_flags &= ~SA_RESTART;
    else
        act.sa_flags |= SA_RESTART;
    return sigaction(sig, &act, nullptr);
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

// Signal 0 (the null signal) and Signal 32 (SIGCANCEL) are deliberately set to null here.
// They are not intended to be resolved by strsignal(), sig2str() or str2sig().
#define ENUMERATE_SIGNALS                                  \
    __ENUMERATE_SIGNAL(nullptr, nullptr)                   \
    __ENUMERATE_SIGNAL("HUP", "Hangup")                    \
    __ENUMERATE_SIGNAL("INT", "Interrupt")                 \
    __ENUMERATE_SIGNAL("QUIT", "Quit")                     \
    __ENUMERATE_SIGNAL("ILL", "Illegal instruction")       \
    __ENUMERATE_SIGNAL("TRAP", "Trap")                     \
    __ENUMERATE_SIGNAL("ABRT", "Aborted")                  \
    __ENUMERATE_SIGNAL("BUS", "Bus error")                 \
    __ENUMERATE_SIGNAL("FPE", "Division by zero")          \
    __ENUMERATE_SIGNAL("KILL", "Killed")                   \
    __ENUMERATE_SIGNAL("USR1", "User signal 1")            \
    __ENUMERATE_SIGNAL("SEGV", "Segmentation violation")   \
    __ENUMERATE_SIGNAL("USR2", "User signal 2")            \
    __ENUMERATE_SIGNAL("PIPE", "Broken pipe")              \
    __ENUMERATE_SIGNAL("ALRM", "Alarm clock")              \
    __ENUMERATE_SIGNAL("TERM", "Terminated")               \
    __ENUMERATE_SIGNAL("STKFLT", "Stack fault")            \
    __ENUMERATE_SIGNAL("CHLD", "Child exited")             \
    __ENUMERATE_SIGNAL("CONT", "Continued")                \
    __ENUMERATE_SIGNAL("STOP", "Stopped (signal)")         \
    __ENUMERATE_SIGNAL("TSTP", "Stopped")                  \
    __ENUMERATE_SIGNAL("TTIN", "Stopped (tty input)")      \
    __ENUMERATE_SIGNAL("TTOU", "Stopped (tty output)")     \
    __ENUMERATE_SIGNAL("URG", "Urgent I/O condition)")     \
    __ENUMERATE_SIGNAL("XCPU", "CPU limit exceeded")       \
    __ENUMERATE_SIGNAL("XFSZ", "File size limit exceeded") \
    __ENUMERATE_SIGNAL("VTALRM", "Virtual timer expired")  \
    __ENUMERATE_SIGNAL("PROF", "Profiling timer expired")  \
    __ENUMERATE_SIGNAL("WINCH", "Window changed")          \
    __ENUMERATE_SIGNAL("IO", "I/O possible")               \
    __ENUMERATE_SIGNAL("INFO", "Power failure")            \
    __ENUMERATE_SIGNAL("SYS", "Bad system call")           \
    __ENUMERATE_SIGNAL(nullptr, nullptr)

char const* sys_siglist[NSIG] = {
#define __ENUMERATE_SIGNAL(name, description) description,
    ENUMERATE_SIGNALS
#undef __ENUMERATE_SIGNAL
};

char const* sys_signame[NSIG] = {
#define __ENUMERATE_SIGNAL(name, description) name,
    ENUMERATE_SIGNALS
#undef __ENUMERATE_SIGNAL
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

// https://pubs.opengroup.org/onlinepubs/9799919799/functions/sig2str.html
int sig2str(int signum, char* str)
{
    VERIFY(str);

    // If signum is equal to 0, the behavior is unspecified.
    if (signum <= 0)
        return -1;

    // If signum is a valid, supported signal number (...), the sig2str()
    // function shall return 0; otherwise, if signum is not equal to 0, it shall
    // return -1.
    if (signum < 0 || signum >= NSIG)
        return -1;

    // If signum is equal to one of the symbolic constants listed in the table
    // of signal numbers in <signal.h>, the stored signal name shall be the
    // name of the symbolic constant without the SIG prefix.
    if (sys_signame[signum]) {
        size_t signal_string_length = strlen(sys_signame[signum]);
        // SIG2STR_MAX includes the null terminator while strlen does not,
        // so the length must be at most SIG2STR_MAX - 1.
        VERIFY(signal_string_length < SIG2STR_MAX);
        memcpy(str, sys_signame[signum], signal_string_length);
        str[signal_string_length] = 0;
        return 0;
    }

    // FIXME: Handle realtime signals.

    return -1;
}

// https://pubs.opengroup.org/onlinepubs/9799919799/functions/str2sig.html
int str2sig(char const* __restrict__ str, int* __restrict__ pnum)
{
    VERIFY(str);
    VERIFY(pnum);

    // If str points to a string containing the name of one of the symbolic
    // constants listed in the table of signal numbers in <signal.h>, without
    // the SIG prefix, the stored signal number shall be equal to the value of
    // the symbolic constant.
    for (int i = 1; i < NSIG; ++i) {
        if (!sys_signame[i])
            continue;
        if (strcmp(str, sys_signame[i]) == 0) {
            *pnum = i;
            return 0;
        }
    }

    // FIXME: Handle realtime signals.

    // If str points to a string containing a decimal representation of a valid,
    // supported signal number, the value stored in the location pointed to by
    // pnum shall be equal to that number.
    int parsed_number = 0;
    for (size_t i = 0; str[i]; ++i) {
        if (str[i] < '0' || str[i] > '9')
            return -1;
        parsed_number = (parsed_number * 10) + (str[i] - '0');
    }

    // If str points to a string containing a decimal representation of the
    // value 0 and the string was not returned by a previous successful call
    // to sig2str() with a signum argument of 0, the behavior is unspecified.
    if (parsed_number <= 0)
        return -1;

    if (parsed_number >= NSIG || !sys_signame[parsed_number])
        return -1;

    *pnum = parsed_number;
    return 0;
}
}
