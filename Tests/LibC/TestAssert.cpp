/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <assert.h>

TEST_CASE(assert)
{
    EXPECT_CRASH("This should assert", [] {
        assert(!"This should assert");
        return Test::Crash::Failure::DidNotCrash;
    });
}
