/*
 * Copyright (c) 2023, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>
#include <stdlib.h>
#include <string.h>

TEST_CASE(putenv_overwrite_invalid_value)
{
    // Write an environment variable using the heap
    auto* heap_environment_value = new char[12];
    VERIFY(snprintf(heap_environment_value, 12, "TESTVAR=123") == 11);
    auto result = putenv(heap_environment_value);
    EXPECT_EQ(result, 0);

    // Try to retrieve the variable after overwriting the heap value
    memset(heap_environment_value, 0, 12);
    auto* environment_variable = getenv("TESTVAR");
    EXPECT_EQ(environment_variable, nullptr);

    // Try to overwrite the variable now that it's zeroed out
    auto* new_environment_value = new char[12];
    VERIFY(snprintf(new_environment_value, 12, "TESTVAR=456") == 11);
    result = putenv(new_environment_value);
    EXPECT_EQ(result, 0);

    // Retrieve the variable and verify that it's set correctly
    environment_variable = getenv("TESTVAR");
    EXPECT_NE(environment_variable, nullptr);
    EXPECT_EQ(strcmp(environment_variable, "456"), 0);

    // Overwrite and retrieve it again to test correct search behavior for '='
    auto* final_environment_value = new char[12];
    VERIFY(snprintf(final_environment_value, 12, "TESTVAR=789") == 11);
    result = putenv(final_environment_value);
    EXPECT_EQ(result, 0);
    environment_variable = getenv("TESTVAR");
    EXPECT_NE(environment_variable, nullptr);
    EXPECT_EQ(strcmp(environment_variable, "789"), 0);
}

TEST_CASE(setenv_invalid_name)
{
    // Empty name
    auto result = setenv("", "test", 1);
    EXPECT_EQ(result, -1);
    auto* val = getenv("");
    EXPECT_EQ(val, nullptr);

    // Name which contains '='
    result = setenv("TEST=", "test", 1);
    EXPECT_EQ(result, -1);
    val = getenv("TEST=");
    EXPECT_EQ(val, nullptr);
}
