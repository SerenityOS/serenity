/*
 * Copyright (c) 2023, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

static int putenv_from_stack(char const* environment_variable)
{
    char environment_buffer[32];
    auto result = snprintf(environment_buffer, 31, "%s", environment_variable);
    VERIFY(result > 0);
    return putenv(environment_buffer);
}

static char const* getenv_with_overwritten_stack(char const* environment_variable_name)
{
    char environment_buffer[32];
    memset(environment_buffer, ' ', 31);
    environment_buffer[31] = 0;
    return getenv(environment_variable_name);
}

TEST_CASE(putenv_overwrite_invalid_stack_value)
{
    // Write an environment variable using a stack value
    auto result = putenv_from_stack("TESTVAR=123");
    EXPECT_EQ(result, 0);

    // Try to retrieve the variable after overwriting the stack
    auto environment_variable = getenv_with_overwritten_stack("TESTVAR");
    EXPECT_EQ(environment_variable, nullptr);

    // Try to overwrite the variable now that it's zeroed out
    char new_environment_value[32];
    result = snprintf(new_environment_value, 31, "%s", "TESTVAR=456");
    VERIFY(result > 0);
    result = putenv(new_environment_value);
    EXPECT_EQ(result, 0);

    // Retrieve the variable and verify that it's set correctly
    environment_variable = getenv("TESTVAR");
    EXPECT_NE(environment_variable, nullptr);
    EXPECT_EQ(strcmp(environment_variable, "456"), 0);

    // Overwrite and retrieve it again to test correct search behavior for '='
    char final_environment_value[32];
    result = snprintf(final_environment_value, 31, "%s", "TESTVAR=789");
    VERIFY(result > 0);
    result = putenv(final_environment_value);
    EXPECT_EQ(result, 0);
    environment_variable = getenv("TESTVAR");
    EXPECT_NE(environment_variable, nullptr);
    EXPECT_EQ(strcmp(environment_variable, "789"), 0);
}
