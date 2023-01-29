/*
 * Copyright (c) 2018-2020, Nick Johnson <sylvyrfysh@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/BuiltinWrappers.h>
#include <AK/Types.h>

TEST_CASE(wrapped_popcount)
{
    EXPECT_EQ(popcount(NumericLimits<u8>::max()), 8);
    EXPECT_EQ(popcount(NumericLimits<u16>::max()), 16);
    EXPECT_EQ(popcount(NumericLimits<u32>::max()), 32);
    EXPECT_EQ(popcount(NumericLimits<u64>::max()), 64);
    EXPECT_EQ(popcount(NumericLimits<size_t>::max()), static_cast<int>(8 * sizeof(size_t)));
    EXPECT_EQ(popcount(0u), 0);
    EXPECT_EQ(popcount(0b01010101ULL), 4);
}

TEST_CASE(wrapped_count_leading_zeroes)
{
    EXPECT_EQ(count_leading_zeroes(NumericLimits<u8>::max()), 0);
    EXPECT_EQ(count_leading_zeroes(static_cast<u8>(0x20)), 2);
    EXPECT_EQ(count_leading_zeroes_safe(static_cast<u8>(0)), 8);
    EXPECT_EQ(count_leading_zeroes(NumericLimits<u16>::max()), 0);
    EXPECT_EQ(count_leading_zeroes(static_cast<u16>(0x20)), 10);
    EXPECT_EQ(count_leading_zeroes_safe(static_cast<u16>(0)), 16);
    EXPECT_EQ(count_leading_zeroes(NumericLimits<u32>::max()), 0);
    EXPECT_EQ(count_leading_zeroes(static_cast<u32>(0x20)), 26);
    EXPECT_EQ(count_leading_zeroes_safe(static_cast<u32>(0)), 32);
    EXPECT_EQ(count_leading_zeroes(NumericLimits<u64>::max()), 0);
}

TEST_CASE(wrapped_count_trailing_zeroes)
{
    EXPECT_EQ(count_trailing_zeroes(NumericLimits<u8>::max()), 0);
    EXPECT_EQ(count_trailing_zeroes(static_cast<u8>(1)), 0);
    EXPECT_EQ(count_trailing_zeroes(static_cast<u8>(2)), 1);
    EXPECT_EQ(count_trailing_zeroes_safe(static_cast<u8>(0)), 8);
    EXPECT_EQ(count_trailing_zeroes(NumericLimits<u16>::max()), 0);
    EXPECT_EQ(count_trailing_zeroes(static_cast<u16>(1)), 0);
    EXPECT_EQ(count_trailing_zeroes(static_cast<u16>(2)), 1);
    EXPECT_EQ(count_trailing_zeroes_safe(static_cast<u16>(0)), 16);
    EXPECT_EQ(count_trailing_zeroes(NumericLimits<u32>::max()), 0);
    EXPECT_EQ(count_trailing_zeroes(static_cast<u32>(1)), 0);
    EXPECT_EQ(count_trailing_zeroes(static_cast<u32>(2)), 1);
    EXPECT_EQ(count_trailing_zeroes_safe(static_cast<u32>(0)), 32);
    EXPECT_EQ(count_trailing_zeroes(NumericLimits<u64>::max()), 0);
    EXPECT_EQ(count_trailing_zeroes(static_cast<u64>(1)), 0);
    EXPECT_EQ(count_trailing_zeroes(static_cast<u64>(2)), 1);
}
