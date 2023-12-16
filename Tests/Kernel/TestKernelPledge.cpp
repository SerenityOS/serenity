/*
 * Copyright (c) 2018-2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteString.h>
#include <LibTest/TestCase.h>
#include <errno.h>
#include <unistd.h>

TEST_CASE(test_nonexistent_pledge)
{
    auto res = pledge("testing123", "notthere");
    if (res >= 0)
        FAIL("Pledging on existent promises should fail.");
}

TEST_CASE(test_pledge_argument_validation)
{
    auto const long_argument = ByteString::repeated('a', 2048);

    auto res = pledge(long_argument.characters(), "stdio");
    EXPECT_EQ(res, -1);
    EXPECT_EQ(errno, E2BIG);

    res = pledge("stdio", long_argument.characters());
    EXPECT_EQ(res, -1);
    EXPECT_EQ(errno, E2BIG);

    res = pledge(long_argument.characters(), long_argument.characters());
    EXPECT_EQ(res, -1);
    EXPECT_EQ(errno, E2BIG);

    res = pledge("fake", "stdio");
    EXPECT_EQ(res, -1);
    EXPECT_EQ(errno, EINVAL);

    res = pledge("stdio", "fake");
    EXPECT_EQ(res, -1);
    EXPECT_EQ(errno, EINVAL);
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
