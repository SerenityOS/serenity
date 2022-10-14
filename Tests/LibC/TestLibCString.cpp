/*
 * Copyright (c) 2021, Gunnar Beutner <gbeutner@serenityos.org>
 * Copyright (c) 2022, Daniel Bertalan <dani@danielbertalan.dev>
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

TEST_CASE(strtok_r_delimiters_only)
{
    char dummy[] = "a;";
    char input[] = ";;;;;;";
    char* saved_str = dummy;

    EXPECT_EQ(strtok_r(input, ";", &saved_str), nullptr);
    EXPECT_EQ(strtok_r(nullptr, ";", &saved_str), nullptr);
    // The string to which `saved_str` initially points to shouldn't be modified.
    EXPECT_EQ(strcmp(dummy, "a;"), 0);
}
