/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/CountOnes.h>

TEST_CASE(CountTrailingZeros)
{
    {
        static_assert(count_ones<u8>(0) == 0);
        EXPECT_EQ(count_ones<u8>(0), 0u);

        static_assert(count_ones<u8>(1) == 1);
        EXPECT_EQ(count_ones<u8>(1), 1u);

        static_assert(count_ones<u8>(1 << 7) == 1);
        EXPECT_EQ(count_ones<u8>(1 << 7), 1u);
    }

    {
        static_assert(count_ones<u16>(0) == 0);
        EXPECT_EQ(count_ones<u16>(0), 0u);

        static_assert(count_ones<u16>(1) == 1);
        EXPECT_EQ(count_ones<u16>(1), 1u);

        static_assert(count_ones<u16>(1 << 15) == 1);
        EXPECT_EQ(count_ones<u16>(1 << 15), 1u);
    }

    {
        static_assert(count_ones<u32>(0) == 0);
        EXPECT_EQ(count_ones<u32>(0), 0u);

        static_assert(count_ones<u32>(1) == 1);
        EXPECT_EQ(count_ones<u32>(1), 1u);

        static_assert(count_ones<u32>(u32(1) << 31) == 1);
        EXPECT_EQ(count_ones<u32>(u32(1) << 31), 1u);
    }

    {
        static_assert(count_ones<u64>(0) == 0);
        EXPECT_EQ(count_ones<u64>(0), 0u);

        static_assert(count_ones<u64>(1) == 1);
        EXPECT_EQ(count_ones<u64>(1), 1u);

        static_assert(count_ones<u64>(u64(1) << 63) == 1);
        EXPECT_EQ(count_ones<u64>(u64(1) << 63), 1u);
    }
}
