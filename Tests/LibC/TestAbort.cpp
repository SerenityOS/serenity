/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <signal.h>
#include <stdlib.h>

TEST_CASE(_abort)
{
    EXPECT_CRASH("This should _abort", [] {
        _abort();
        return Test::Crash::Failure::DidNotCrash;
    });
}

TEST_CASE(abort)
{
    EXPECT_CRASH("This should abort", [] {
        abort();
        return Test::Crash::Failure::DidNotCrash;
    });
    EXPECT_CRASH_WITH_SIGNAL("This should abort with SIGABRT signal", SIGABRT, [] {
        abort();
        return Test::Crash::Failure::DidNotCrash;
    });
}
