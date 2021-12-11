/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>
#include <signal.h>
#include <time.h>
#include <unistd.h>

TEST_CASE(sigwait)
{
    sigset_t mask;

    int rc = sigemptyset(&mask);
    EXPECT_EQ(rc, 0);
    rc = sigaddset(&mask, SIGUSR1);
    EXPECT_EQ(rc, 0);
    rc = sigprocmask(SIG_BLOCK, &mask, nullptr);
    EXPECT_EQ(rc, 0);

    int child_pid = fork();
    EXPECT(child_pid >= 0);
    if (child_pid == 0) {
        sleep(1);
        kill(getppid(), SIGUSR1);
        exit(EXIT_SUCCESS);
    } else {
        int sig;
        rc = sigwait(&mask, &sig);
        EXPECT_EQ(rc, 0);
        EXPECT_EQ(sig, SIGUSR1);
    }

    // cancel pending signal
    struct sigaction act_ignore = { { SIG_IGN }, 0, 0 };
    rc = sigaction(SIGUSR1, &act_ignore, nullptr);
    EXPECT_EQ(rc, 0);
    rc = sigprocmask(SIG_UNBLOCK, &mask, nullptr);
    EXPECT_EQ(rc, 0);
    struct sigaction act_default = { { SIG_DFL }, 0, 0 };
    rc = sigaction(SIGUSR1, &act_default, nullptr);
    EXPECT_EQ(rc, 0);
    sigset_t pending;
    rc = sigpending(&pending);
    EXPECT_EQ(rc, 0);
    EXPECT_EQ(pending, 0u);
}

TEST_CASE(sigwaitinfo)
{
    sigset_t mask;

    int rc = sigemptyset(&mask);
    EXPECT_EQ(rc, 0);
    rc = sigaddset(&mask, SIGUSR1);
    EXPECT_EQ(rc, 0);
    rc = sigprocmask(SIG_BLOCK, &mask, nullptr);
    EXPECT_EQ(rc, 0);

    int child_pid = fork();
    EXPECT(child_pid >= 0);
    if (child_pid == 0) {
        sleep(1);
        kill(getppid(), SIGUSR1);
        exit(EXIT_SUCCESS);
    } else {
        siginfo_t info;
        rc = sigwaitinfo(&mask, &info);
        EXPECT_EQ(rc, SIGUSR1);
        EXPECT_EQ(info.si_signo, SIGUSR1);
    }

    // cancel pending signal
    struct sigaction act_ignore = { { SIG_IGN }, 0, 0 };
    rc = sigaction(SIGUSR1, &act_ignore, nullptr);
    EXPECT_EQ(rc, 0);
    rc = sigprocmask(SIG_UNBLOCK, &mask, nullptr);
    EXPECT_EQ(rc, 0);
    struct sigaction act_default = { { SIG_DFL }, 0, 0 };
    rc = sigaction(SIGUSR1, &act_default, nullptr);
    EXPECT_EQ(rc, 0);
    sigset_t pending;
    rc = sigpending(&pending);
    EXPECT_EQ(rc, 0);
    EXPECT_EQ(pending, 0u);
}

TEST_CASE(sigtimedwait_normal)
{
    sigset_t mask;

    int rc = sigemptyset(&mask);
    EXPECT_EQ(rc, 0);
    rc = sigaddset(&mask, SIGUSR1);
    EXPECT_EQ(rc, 0);
    rc = sigprocmask(SIG_BLOCK, &mask, nullptr);
    EXPECT_EQ(rc, 0);

    int child_pid = fork();
    EXPECT(child_pid >= 0);
    if (child_pid == 0) {
        sleep(1);
        kill(getppid(), SIGUSR1);
        exit(EXIT_SUCCESS);
    } else {
        siginfo_t info;
        struct timespec timeout = { .tv_sec = 2, .tv_nsec = 0 };
        rc = sigtimedwait(&mask, &info, &timeout);
        EXPECT_EQ(rc, SIGUSR1);
        EXPECT_EQ(info.si_signo, SIGUSR1);
    }

    // cancel pending signal
    struct sigaction act_ignore = { { SIG_IGN }, 0, 0 };
    rc = sigaction(SIGUSR1, &act_ignore, nullptr);
    EXPECT_EQ(rc, 0);
    rc = sigprocmask(SIG_UNBLOCK, &mask, nullptr);
    EXPECT_EQ(rc, 0);
    struct sigaction act_default = { { SIG_DFL }, 0, 0 };
    rc = sigaction(SIGUSR1, &act_default, nullptr);
    EXPECT_EQ(rc, 0);
    sigset_t pending;
    rc = sigpending(&pending);
    EXPECT_EQ(rc, 0);
    EXPECT_EQ(pending, 0u);
}

TEST_CASE(sigtimedwait_poll)
{
    sigset_t mask;

    int rc = sigemptyset(&mask);
    EXPECT_EQ(rc, 0);
    rc = sigaddset(&mask, SIGUSR1);
    EXPECT_EQ(rc, 0);
    rc = sigprocmask(SIG_BLOCK, &mask, nullptr);
    EXPECT_EQ(rc, 0);

    struct timespec poll_timeout = { .tv_sec = 0, .tv_nsec = 0 };
    rc = sigtimedwait(&mask, nullptr, &poll_timeout);
    EXPECT_EQ(rc, -1);
    EXPECT_EQ(errno, EAGAIN);

    kill(getpid(), SIGUSR1);

    siginfo_t info;
    rc = sigtimedwait(&mask, &info, &poll_timeout);
    EXPECT_EQ(rc, SIGUSR1);
    EXPECT_EQ(info.si_signo, SIGUSR1);

    // cancel pending signal
    struct sigaction act_ignore = { { SIG_IGN }, 0, 0 };
    rc = sigaction(SIGUSR1, &act_ignore, nullptr);
    EXPECT_EQ(rc, 0);
    rc = sigprocmask(SIG_UNBLOCK, &mask, nullptr);
    EXPECT_EQ(rc, 0);
    struct sigaction act_default = { { SIG_DFL }, 0, 0 };
    rc = sigaction(SIGUSR1, &act_default, nullptr);
    EXPECT_EQ(rc, 0);
    sigset_t pending;
    rc = sigpending(&pending);
    EXPECT_EQ(rc, 0);
    EXPECT_EQ(pending, 0u);
}

TEST_CASE(sigtimedwait_timeout)
{
    sigset_t mask;
    int rc = sigemptyset(&mask);
    EXPECT_EQ(rc, 0);
    rc = sigaddset(&mask, SIGUSR1);
    EXPECT_EQ(rc, 0);
    struct timespec timeout = { .tv_sec = 1, .tv_nsec = 0 };
    rc = sigtimedwait(&mask, nullptr, &timeout);
    EXPECT_EQ(rc, -1);
    EXPECT_EQ(errno, EAGAIN);
}
