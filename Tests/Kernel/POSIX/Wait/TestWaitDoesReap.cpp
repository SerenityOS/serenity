/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/System.h>
#include <LibTest/TestCase.h>

enum class TestNoWait : u8 {
    No = 0,
    Yes,
};

static void run_test(TestNoWait test_no_wait, auto callback)
{
    auto pid = TRY_OR_FAIL(Core::System::fork());

    if (pid == 0)
        _exit(42);

    if (test_no_wait == TestNoWait::Yes) {
        // With WNOWAIT, waitid should return the info without reaping the child.
        siginfo_t siginfo {};
        auto return_value = callback(&siginfo, TestNoWait::Yes);
        EXPECT_EQ(return_value, 0);
        EXPECT_EQ(siginfo.si_pid, pid);
    }

    siginfo_t siginfo {};
    auto return_value = callback(&siginfo, TestNoWait::No);
    EXPECT_EQ(return_value, 0);
    EXPECT_EQ(siginfo.si_pid, pid);

    // The child should have been reaped, second call to wait should return ECHILD.
    siginfo = {};
    return_value = callback(&siginfo, TestNoWait::No);
    EXPECT_EQ(return_value, -1);
    EXPECT_EQ(errno, ECHILD);
}

TEST_CASE(waitid_reaps)
{
    // Try to avoid "lucky" false positive by doing multiple repetitions.
    for (u32 _ = 0; _ < 10; ++_)
        run_test(TestNoWait::No, [](auto* siginfo, TestNoWait) { return waitid(P_ALL, -1, siginfo, WEXITED); });
}

TEST_CASE(waitid_nowait)
{
    run_test(TestNoWait::Yes,
        [](auto* siginfo, TestNoWait test_no_wait) {
            auto options = WEXITED;
            if (test_no_wait == TestNoWait::Yes)
                options |= WNOWAIT;
            return waitid(P_ALL, -1, siginfo, options);
        });
}
