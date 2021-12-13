/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/CountLeadingZeros.h>

TEST_CASE(CountTrailingZeros)
{
    {
        static_assert(count_leading_zeros<u8>(0) == 8);
        EXPECT_EQ(count_leading_zeros<u8>(0), 8u);

        static_assert(count_leading_zeros<u8>(1) == 7);
        EXPECT_EQ(count_leading_zeros<u8>(1), 7u);

        static_assert(count_leading_zeros<u8>(1 << 7) == 0);
        EXPECT_EQ(count_leading_zeros<u8>(1 << 7), 0u);
    }

    {
        static_assert(count_leading_zeros<u16>(0) == 16);
        EXPECT_EQ(count_leading_zeros<u16>(0), 16u);

        static_assert(count_leading_zeros<u16>(1) == 15);
        EXPECT_EQ(count_leading_zeros<u16>(1), 15u);

        static_assert(count_leading_zeros<u16>(1 << 15) == 0);
        EXPECT_EQ(count_leading_zeros<u16>(1 << 15), 0u);
    }

    {
        static_assert(count_leading_zeros<u32>(0) == 32);
        EXPECT_EQ(count_leading_zeros<u32>(0), 32u);

        static_assert(count_leading_zeros<u32>(1) == 31);
        EXPECT_EQ(count_leading_zeros<u32>(1), 31u);

        static_assert(count_leading_zeros<u32>(u32(1) << 31) == 0);
        EXPECT_EQ(count_leading_zeros<u32>(u32(1) << 31), 0u);
    }

    {
        static_assert(count_leading_zeros<u64>(0) == 64);
        EXPECT_EQ(count_leading_zeros<u64>(0), 64u);

        static_assert(count_leading_zeros<u64>(1) == 63);
        EXPECT_EQ(count_leading_zeros<u64>(1), 63u);

        static_assert(count_leading_zeros<u64>(u64(1) << 63) == 0);
        EXPECT_EQ(count_leading_zeros<u64>(u64(1) << 63), 0u);
    }
}
