/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>
#include <sys/wait.h>

static void run_test(auto callback)
{
    errno = 0;
    auto return_value = callback();
    EXPECT_EQ(return_value, -1);
    EXPECT_EQ(errno, ECHILD);
}

TEST_CASE(wait)
{
    run_test([]() { return wait(nullptr); });
}

TEST_CASE(waitid)
{
    static constexpr auto all_options = WSTOPPED | WEXITED | WCONTINUED | WNOWAIT;

    for (auto option : { all_options, all_options | WNOHANG }) {
        siginfo_t siginfo {};
        run_test([=]() mutable { return waitid(P_ALL, -1, &siginfo, option); });
        run_test([=]() mutable { return waitid(P_PID, 42, &siginfo, option); });
        run_test([=]() mutable { return waitid(P_PGID, 42, &siginfo, option); });
    }
}

TEST_CASE(waitpid)
{
    static constexpr auto all_options = WUNTRACED | WCONTINUED;

    for (auto option : { all_options, all_options | WNOHANG, WNOHANG }) {
        run_test([=]() { return waitpid(-1, nullptr, option); });
        run_test([=]() { return waitpid(42, nullptr, option); });
        run_test([=]() { return waitpid(0, nullptr, option); });
        run_test([=]() { return waitpid(-42, nullptr, option); });
    }
}
