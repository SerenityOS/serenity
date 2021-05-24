/*
 * Copyright (c) 2021, Gunnar Beutner <gbeutner@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>
#include <errno.h>
#include <string.h>

TEST_CASE(strerror_r_basic)
{
    EXPECT_EQ(strerror_r(1000, nullptr, 0), EINVAL);
    EXPECT_EQ(strerror_r(EFAULT, nullptr, 0), ERANGE);
    char buf[64];
    EXPECT_EQ(strerror_r(EFAULT, buf, sizeof(buf)), 0);
    EXPECT_EQ(strcmp(buf, "Bad address"), 0);
}
