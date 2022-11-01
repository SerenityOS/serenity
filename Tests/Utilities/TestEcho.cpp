/*
 * Copyright (c) 2022, demostanis worlds <demostanis@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/Command.h>
#include <LibTest/TestCase.h>

TEST_CASE(works)
{
    EXPECT_COMMAND_OUTPUT_EQ("echo hello", "hello\n");
    EXPECT_COMMAND_OUTPUT_EQ("echo Well Hello Friends!", "Well Hello Friends!\n");
    EXPECT_COMMAND_OUTPUT_EQ("echo", "\n");
    EXPECT_COMMAND_OUTPUT_EQ("echo -n hello", "hello");
    EXPECT_COMMAND_OUTPUT_EQ("echo -n", "");
}
