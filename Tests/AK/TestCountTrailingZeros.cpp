/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/CountTrailingZeros.h>

TEST_CASE(CountTrailingZeros)
{
    {
        static_assert(count_trailing_zeros<u8>(0) == 8);
        EXPECT_EQ(count_trailing_zeros<u8>(0), 8u);

        static_assert(count_trailing_zeros<u8>(1) == 0);
        EXPECT_EQ(count_trailing_zeros<u8>(1), 0u);

        static_assert(count_trailing_zeros<u8>(1 << 7) == 7);
        EXPECT_EQ(count_trailing_zeros<u8>(1 << 7), 7u);
    }

    {
        static_assert(count_trailing_zeros<u16>(0) == 16);
        EXPECT_EQ(count_trailing_zeros<u16>(0), 16u);

        static_assert(count_trailing_zeros<u16>(1) == 0);
        EXPECT_EQ(count_trailing_zeros<u16>(1), 0u);

        static_assert(count_trailing_zeros<u16>(1 << 15) == 15);
        EXPECT_EQ(count_trailing_zeros<u16>(1 << 15), 15u);
    }

    {
        static_assert(count_trailing_zeros<u32>(0) == 32);
        EXPECT_EQ(count_trailing_zeros<u32>(0), 32u);

        static_assert(count_trailing_zeros<u32>(1) == 0);
        EXPECT_EQ(count_trailing_zeros<u32>(1), 0u);

        static_assert(count_trailing_zeros<u32>(u32(1) << 31) == 31);
        EXPECT_EQ(count_trailing_zeros<u32>(u32(1) << 31), 31u);
    }

    {
        static_assert(count_trailing_zeros<u64>(0) == 64);
        EXPECT_EQ(count_trailing_zeros<u64>(0), 64u);

        static_assert(count_trailing_zeros<u64>(1) == 0);
        EXPECT_EQ(count_trailing_zeros<u64>(1), 0u);

        static_assert(count_trailing_zeros<u64>(u64(1) << 63) == 63);
        EXPECT_EQ(count_trailing_zeros<u64>(u64(1) << 63), 63u);
    }
}
