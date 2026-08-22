/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/System.h>
#include <LibTest/TestCase.h>
#include <sys/wait.h>
#include <unistd.h>

static Function<int(int)> g_wait_caller = nullptr;
static pid_t volatile g_expected_pid = 0;

static void sigchld_handler(int, siginfo_t* siginfo, void*)
{
    EXPECT_EQ(siginfo->si_signo, SIGCHLD);
    EXPECT_EQ(siginfo->si_pid, g_expected_pid);

    auto return_value = g_wait_caller(g_expected_pid);
    EXPECT_EQ(return_value, g_expected_pid);
}

static void run_test(auto callback)
{
    struct sigaction s;
    s.sa_sigaction = sigchld_handler;
    sigemptyset(&s.sa_mask);
    s.sa_flags = SA_SIGINFO;
    TRY_OR_FAIL(Core::System::sigaction(SIGCHLD, &s, nullptr));

    g_wait_caller = move(callback);

    // Block SIGCHLD until we are ready.
    sigset_t blocked, prev;
    sigemptyset(&blocked);
    sigaddset(&blocked, SIGCHLD);
    EXPECT_EQ(sigprocmask(SIG_BLOCK, &blocked, &prev), 0);

    auto pid = TRY_OR_FAIL(Core::System::fork());

    if (pid == 0)
        _exit(42);

    g_expected_pid = pid;

    sigsuspend(&prev);
    EXPECT_EQ(sigprocmask(SIG_SETMASK, &prev, nullptr), 0);
}

TEST_CASE(waitid)
{
    auto waitid_wrapper = [](auto p1, auto p2, auto p4) {
        siginfo_t siginfo {};
        int result = waitid(p1, p2, &siginfo, p4);
        EXPECT_EQ(result, 0);
        return siginfo.si_pid;
    };

    for (auto option : { WEXITED, WEXITED | WNOHANG }) {
        run_test([=](int) mutable { return waitid_wrapper(P_ALL, -1, option); });
        run_test([=](int child_pid) mutable { return waitid_wrapper(P_PID, child_pid, option); });
        run_test([=](int) mutable { return waitid_wrapper(P_PGID, getpgid(getpid()), option); });
    }
}

TEST_CASE(waitpid)
{
    for (auto option : { 0, WNOHANG }) {
        run_test([=](int) { return waitpid(-1, nullptr, option); });
        run_test([=](int child_pid) { return waitpid(child_pid, nullptr, option); });
        run_test([=](int) { return waitpid(0, nullptr, option); });
        run_test([=](int) { return waitpid(-getpgid(getpid()), nullptr, option); });
    }
}
