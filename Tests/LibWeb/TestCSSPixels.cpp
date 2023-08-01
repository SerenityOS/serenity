/*
 * Copyright (c) 2023, Aliaksandr Kalenik <kalenik.aliaksandr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>
#include <LibWeb/PixelUnits.h>

namespace Web {

TEST_CASE(addition1)
{
    CSSPixels a(10);
    CSSPixels b(20);
    CSSPixels c = a + b;
    EXPECT_EQ(c, CSSPixels(30));
}

TEST_CASE(subtraction1)
{
    CSSPixels a(30);
    CSSPixels b(10);
    CSSPixels c = a - b;
    EXPECT_EQ(c, CSSPixels(20));
}

TEST_CASE(division1)
{
    CSSPixels a(10);
    CSSPixels b(5);
    CSSPixels c = a / b;
    EXPECT_EQ(c, CSSPixels(2));
}

TEST_CASE(multiplication1)
{
    CSSPixels a(3);
    CSSPixels b(4);
    CSSPixels c = a * b;
    EXPECT_EQ(c, CSSPixels(12));
}

TEST_CASE(addition2)
{
    CSSPixels a(3);
    a += CSSPixels(2);
    EXPECT_EQ(a, CSSPixels(5));
}

TEST_CASE(to_double)
{
    CSSPixels a(10);
    EXPECT_EQ(a.to_double(), 10);
}

TEST_CASE(to_float)
{
    CSSPixels a(11);
    EXPECT_EQ(a.to_float(), 11);
}

TEST_CASE(to_int)
{
    CSSPixels b(12);
    EXPECT_EQ(b.to_int(), 12);
}

TEST_CASE(comparison1)
{
    EXPECT_EQ(CSSPixels(1) < CSSPixels(2), true);
}

TEST_CASE(comparison2)
{
    EXPECT_EQ(CSSPixels(123) == CSSPixels(123), true);
}

TEST_CASE(saturated_addition)
{
    EXPECT_EQ(CSSPixels(INFINITY), CSSPixels(INFINITY) + 1);
}

TEST_CASE(saturated_subtraction)
{
    auto value = CSSPixels(INFINITY);
    EXPECT_EQ(value - -1, CSSPixels(INFINITY));
}

}
