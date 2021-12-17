/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <signal.h>

TEST_CASE(raise)
{
    EXPECT_CRASH("This should raise a SIGUSR1 signal", [] {
        raise(SIGUSR1);
        return Test::Crash::Failure::DidNotCrash;
    });
}
