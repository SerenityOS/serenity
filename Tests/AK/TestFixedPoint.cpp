/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/FixedPoint.h>

using Type = FixedPoint<4>;

TEST_CASE(arithmetic)
{
    EXPECT_EQ(
        Type(0.5) + Type(0.5),
        Type(1));
    EXPECT_EQ(
        Type(1) + Type(0.5),
        Type(1.5));
    EXPECT_EQ(
        (float)(Type(1) + Type(0.5)),
        1.5f);
    // FIXME: Test for rounded multiply
    EXPECT_EQ(
        Type((int)1) * Type(0.5),
        Type(0.5));
    EXPECT_EQ(
        Type((int)1) / Type(0.5),
        Type(2));
}

TEST_CASE(rounding)
{
    EXPECT_EQ(Type(0.5).round(), Type(0));
    EXPECT_EQ(Type(0.5).floor(), Type(0));
    EXPECT_EQ(Type(0.5).ceil(), Type(1));
    EXPECT_EQ(Type(0.75).trunk(), Type(0));

    EXPECT_EQ(Type(1.5).round(), Type(2));
    EXPECT_EQ(Type(1.5).floor(), Type(1));
    EXPECT_EQ(Type(1.5).ceil(), Type(2));
    EXPECT_EQ(Type(1.25).trunk(), Type(1));

    EXPECT_EQ(Type(-0.5).round(), Type(0));
    EXPECT_EQ(Type(-0.5).floor(), Type(-1));
    EXPECT_EQ(Type(-0.5).ceil(), Type(0));
    EXPECT_EQ(Type(-0.75).trunk(), Type(0));

    EXPECT_EQ(Type(-1.5).round(), Type(-2));
    EXPECT_EQ(Type(-1.5).floor(), Type(-2));
    EXPECT_EQ(Type(-1.5).ceil(), Type(-1));
    EXPECT_EQ(Type(-1.25).trunk(), Type(-1));

    EXPECT_EQ(Type(0.5).lround(), 0);
    EXPECT_EQ(Type(0.5).lfloor(), 0);
    EXPECT_EQ(Type(0.5).lceil(), 1);
    EXPECT_EQ(Type(0.5).ltrunk(), 0);

    EXPECT_EQ(Type(1.5).lround(), 2);
    EXPECT_EQ(Type(1.5).lfloor(), 1);
    EXPECT_EQ(Type(1.5).lceil(), 2);
    EXPECT_EQ(Type(1.5).ltrunk(), 1);

    EXPECT_EQ(Type(-0.5).lround(), 0);
    EXPECT_EQ(Type(-0.5).lfloor(), -1);
    EXPECT_EQ(Type(-0.5).lceil(), 0);
    EXPECT_EQ(Type(-0.5).ltrunk(), 0);

    EXPECT_EQ(Type(-1.5).lround(), -2);
    EXPECT_EQ(Type(-1.5).lfloor(), -2);
    EXPECT_EQ(Type(-1.5).lceil(), -1);
    EXPECT_EQ(Type(-1.5).ltrunk(), -1);
}
