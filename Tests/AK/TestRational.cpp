/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>
#include <AK/Rational.h>

TEST_CASE(rational_construct_is_zero)
{
    Rational<u32> item;
    EXPECT_EQ(item.numerator(), 0U);
    EXPECT_EQ(item.denominator(), 1U);
    EXPECT_APPROXIMATE(item.to_double(), 0);
}

TEST_CASE(rational_0_over_0_todouble_isnan)
{
    Rational<u32> item(0, 0);
    const auto result = item.to_double();
    EXPECT(result != result);
}


TEST_CASE(rational_3_over_2_todouble_is_1p5)
{
    Rational<u32> item(3, 2);
    EXPECT_APPROXIMATE(item.to_double(), 1.5);
}

TEST_CASE(rational_1_over_1_to_string)
{
    Rational<u32> item(1, 1);
    EXPECT_EQ(item.to_string(), String("1/1"));
}

