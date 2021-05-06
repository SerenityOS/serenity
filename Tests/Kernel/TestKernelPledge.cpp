/*
 * Copyright (c) 2018-2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>
#include <unistd.h>

TEST_CASE(test_nonexistent_pledge)
{
    auto res = pledge("testing123", "notthere");
    if (res >= 0)
        FAIL("Pledging on existent promises should fail.");
}

TEST_CASE(test_pledge_failures)
{
    auto res = pledge("stdio unix rpath", "stdio");
    if (res < 0)
        FAIL("Initial pledge is expected to work.");

    res = pledge("stdio unix", "stdio unix");
    if (res >= 0)
        FAIL("Additional execpromise \"unix\" should have failed");

    res = pledge("stdio", "stdio");
    if (res < 0)
        FAIL("Reducing promises is expected to work.");
}
