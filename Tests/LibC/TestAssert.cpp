/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#undef NDEBUG
#include <assert.h>
#include <signal.h>

TEST_CASE(assert)
{
    EXPECT_CRASH("This should assert", [] {
        assert(!"This should assert");
        return Test::Crash::Failure::DidNotCrash;
    });
    EXPECT_CRASH_WITH_SIGNAL("This should assert with SIGABRT signal", SIGABRT, [] {
        assert(!"This should assert");
        return Test::Crash::Failure::DidNotCrash;
    });
}

#define NDEBUG
#include <assert.h>

TEST_CASE(assert_reinclude)
{
    EXPECT_NO_CRASH("This should not assert", [] {
        assert(!"This should not assert");
        return Test::Crash::Failure::DidNotCrash;
    });
}

#undef NDEBUG
#include <assert.h>

TEST_CASE(assert_rereinclude)
{
    EXPECT_CRASH("This should assert", [] {
        assert(!"This should assert");
        return Test::Crash::Failure::DidNotCrash;
    });
}
