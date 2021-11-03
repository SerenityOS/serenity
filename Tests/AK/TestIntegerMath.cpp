/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/IntegralMath.h>

TEST_CASE(IntegerExponentialsAndLogs)
{
    EXPECT_EQ(AK::exp2<i32>(0), 1);
    EXPECT_EQ(AK::exp2<i32>(1), 2);
    EXPECT_EQ(AK::exp2<i32>(2), 4);
    EXPECT_EQ(AK::exp2<i32>(5), 32);
    EXPECT_EQ(AK::exp2<i32>(30), 1 << 30);

    EXPECT_EQ(AK::log2<i32>(0), AK::NumericLimits<i32>::min());
    EXPECT_EQ(AK::log2<i32>(1), 0);
    EXPECT_EQ(AK::log2<i32>(2), 1);
    EXPECT_EQ(AK::log2<i32>(3), 1);
    EXPECT_EQ(AK::log2<i32>(32), 5);

    EXPECT_EQ(AK::log2<i32>(AK::exp2<i32>(1)), 1);
    EXPECT_EQ(AK::log2<i32>(AK::exp2<i32>(2)), 2);
    EXPECT_EQ(AK::log2<i32>(AK::exp2<i32>(3)), 3);
    EXPECT_EQ(AK::log2<i32>(AK::exp2<i32>(4)), 4);
    EXPECT_EQ(AK::log2<i32>(AK::exp2<i32>(6)), 6);
    EXPECT_EQ(AK::log2<i32>(AK::exp2<i32>(20)), 20);
}

TEST_CASE(pow)
{
    EXPECT_EQ(AK::pow<u64>(10, 0), 1ull);
    EXPECT_EQ(AK::pow<u64>(10, 1), 10ull);
    EXPECT_EQ(AK::pow<u64>(10, 2), 100ull);
    EXPECT_EQ(AK::pow<u64>(10, 3), 1'000ull);
    EXPECT_EQ(AK::pow<u64>(10, 4), 10'000ull);
    EXPECT_EQ(AK::pow<u64>(10, 5), 100'000ull);
    EXPECT_EQ(AK::pow<u64>(10, 6), 1'000'000ull);
}
