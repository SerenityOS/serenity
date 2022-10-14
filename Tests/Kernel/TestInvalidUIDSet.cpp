/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>
#include <errno.h>
#include <unistd.h>

TEST_CASE(test_invalid_set_uid_parameters)
{
    auto res = setuid(-1);
    EXPECT_EQ(res, -1);
    EXPECT_EQ(errno, EINVAL);

    res = seteuid(-1);
    EXPECT_EQ(res, -1);
    EXPECT_EQ(errno, EINVAL);

    res = setgid(-1);
    EXPECT_EQ(res, -1);
    EXPECT_EQ(errno, EINVAL);

    res = setegid(-1);
    EXPECT_EQ(res, -1);
    EXPECT_EQ(errno, EINVAL);
}
