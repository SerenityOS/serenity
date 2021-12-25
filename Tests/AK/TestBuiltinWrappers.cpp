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
    EXPECT_EQ(popcount(NumericLimits<u8>::max()), 8u);
    EXPECT_EQ(popcount(NumericLimits<u16>::max()), 16u);
    EXPECT_EQ(popcount(NumericLimits<u32>::max()), 32u);
    EXPECT_EQ(popcount(NumericLimits<u64>::max()), 64u);
    EXPECT_EQ(popcount(NumericLimits<size_t>::max()), bit_sizeof(size_t));
    EXPECT_EQ(popcount(0u), 0u);
    EXPECT_EQ(popcount(0b01010101ULL), 4u);
}

TEST_CASE(wrapped_count_leading_zeroes)
{
    EXPECT_EQ(count_leading_zeroes(NumericLimits<u8>::max()), 0u);
    EXPECT_EQ(count_leading_zeroes(static_cast<u8>(0x20)), 2u);
    EXPECT_EQ(count_leading_zeroes(static_cast<u8>(0)), 8u);
    EXPECT_EQ(count_leading_zeroes(NumericLimits<u16>::max()), 0u);
    EXPECT_EQ(count_leading_zeroes(static_cast<u16>(0x20)), 10u);
    EXPECT_EQ(count_leading_zeroes(static_cast<u16>(0)), 16u);
    EXPECT_EQ(count_leading_zeroes(NumericLimits<u32>::max()), 0u);
    EXPECT_EQ(count_leading_zeroes(static_cast<u32>(0x20)), 26u);
    EXPECT_EQ(count_leading_zeroes(static_cast<u32>(0)), 32u);
    EXPECT_EQ(count_leading_zeroes(NumericLimits<u64>::max()), 0u);
}

TEST_CASE(wrapped_count_trailing_zeroes)
{
    EXPECT_EQ(count_trailing_zeroes(NumericLimits<u8>::max()), 0u);
    EXPECT_EQ(count_trailing_zeroes(static_cast<u8>(1)), 0u);
    EXPECT_EQ(count_trailing_zeroes(static_cast<u8>(2)), 1u);
    EXPECT_EQ(count_trailing_zeroes(static_cast<u8>(0)), 8u);
    EXPECT_EQ(count_trailing_zeroes(NumericLimits<u16>::max()), 0u);
    EXPECT_EQ(count_trailing_zeroes(static_cast<u16>(1)), 0u);
    EXPECT_EQ(count_trailing_zeroes(static_cast<u16>(2)), 1u);
    EXPECT_EQ(count_trailing_zeroes(static_cast<u16>(0)), 16u);
    EXPECT_EQ(count_trailing_zeroes(NumericLimits<u32>::max()), 0u);
    EXPECT_EQ(count_trailing_zeroes(static_cast<u32>(1)), 0u);
    EXPECT_EQ(count_trailing_zeroes(static_cast<u32>(2)), 1u);
    EXPECT_EQ(count_trailing_zeroes(static_cast<u32>(0)), 32u);
    EXPECT_EQ(count_trailing_zeroes(NumericLimits<u64>::max()), 0u);
    EXPECT_EQ(count_trailing_zeroes(static_cast<u64>(1)), 0u);
    EXPECT_EQ(count_trailing_zeroes(static_cast<u64>(2)), 1u);
}
