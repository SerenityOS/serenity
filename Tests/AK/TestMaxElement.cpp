/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/Array.h>
#include <AK/MaxElement.h>

TEST_CASE(should_return_end_for_empty_container)
{
    constexpr AK::Array<int, 0> v {};
    EXPECT_EQ(v.end(), AK::max_element(v));
    static_assert(v.end() == AK::max_element(v));
}

TEST_CASE(should_return_begin_for_1_element_in_container)
{
    constexpr AK::Array v { 42 };
    EXPECT_EQ(v.begin(), AK::max_element(v));
    static_assert(v.begin() == AK::max_element(v));
}

TEST_CASE(should_return_begin_for_same_elements_in_container)
{
    constexpr AK::Array v { 42, 42 };
    EXPECT_EQ(v.begin(), AK::max_element(v));
    static_assert(v.begin() == AK::max_element(v));
}

TEST_CASE(should_return_largest_element_first)
{
    constexpr AK::Array v { 42, 5, 17 };
    EXPECT_EQ(v.begin(), AK::max_element(v));
    static_assert(v.begin() == AK::max_element(v));
}

TEST_CASE(should_return_largest_element_last)
{
    constexpr AK::Array v { 1, 2, 3 };
    EXPECT_EQ(3, *AK::max_element(v));
    static_assert(3 == *AK::max_element(v));
}

TEST_CASE(should_return_largest_element_middle)
{
    constexpr AK::Array v { 1, 42, 3 };
    EXPECT_EQ(42, *AK::max_element(v));
    static_assert(42 == *AK::max_element(v));
}
