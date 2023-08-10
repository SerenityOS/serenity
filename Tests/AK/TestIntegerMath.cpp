/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/IntegralMath.h>
#include <AK/NumericLimits.h>
#include <initializer_list>

TEST_CASE(exp2)
{
    EXPECT_EQ(AK::exp2<u64>(0), 1llu);
    EXPECT_EQ(AK::exp2<u64>(1), 2llu);
    EXPECT_EQ(AK::exp2<u64>(2), 4llu);

    // maximum
    EXPECT_EQ(AK::exp2<u8>(7), 128u);
    EXPECT_EQ(AK::exp2<u32>(31), 2147483648u);
    EXPECT_EQ(AK::exp2<u64>(63), 9223372036854775808llu);
    EXPECT_EQ(AK::exp2<i8>(6), 64);
    EXPECT_EQ(AK::exp2<i32>(30), 1073741824);
    EXPECT_EQ(AK::exp2<i64>(62), 4611686018427387904ll);

    // overflow
    EXPECT_EQ(AK::exp2<u8>(8), NumericLimits<u8>::max());
    EXPECT_EQ(AK::exp2<u32>(32), NumericLimits<u32>::max());
    EXPECT_EQ(AK::exp2<u64>(64), NumericLimits<u64>::max());
    EXPECT_EQ(AK::exp2<i8>(7), NumericLimits<i8>::max());
    EXPECT_EQ(AK::exp2<i32>(31), NumericLimits<i32>::max());
    EXPECT_EQ(AK::exp2<i64>(63), NumericLimits<i64>::max());

    EXPECT_EQ(AK::exp2<i64>(-1), 0ll);
}

TEST_CASE(log2)
{
    constexpr auto test_log2_for_type = []<typename IntType>() {
        EXPECT_EQ(AK::log2<IntType>(0), static_cast<IntType>(0));
        EXPECT_EQ(AK::ceil_log2<IntType>(0), static_cast<IntType>(0));

        EXPECT_EQ(AK::log2<IntType>(1), static_cast<IntType>(0));
        EXPECT_EQ(AK::ceil_log2<IntType>(1), static_cast<IntType>(0));

        EXPECT_EQ(AK::log2<IntType>(2), static_cast<IntType>(1));
        EXPECT_EQ(AK::ceil_log2<IntType>(2), static_cast<IntType>(1));

        for (IntType power = 2; power < 8 * sizeof(IntType); ++power) {
            IntType number = static_cast<IntType>(1) << power;

            EXPECT_EQ(AK::log2<IntType>(number), power);
            EXPECT_EQ(AK::ceil_log2<IntType>(number), power);

            EXPECT_EQ(AK::log2<IntType>(number - 1), power - 1);
            EXPECT_EQ(AK::ceil_log2<IntType>(number - 1), power);

            EXPECT_EQ(AK::log2<IntType>(number + 1), power);
            EXPECT_EQ(AK::ceil_log2<IntType>(number + 1), power + 1);
        }
    };

    test_log2_for_type.operator()<u8>();
    test_log2_for_type.operator()<u32>();
    test_log2_for_type.operator()<u64>();

    EXPECT_EQ(AK::log2<i32>(0), AK::NumericLimits<i32>::min());
    EXPECT_EQ(AK::ceil_log2<i32>(0), AK::NumericLimits<i32>::min());
    EXPECT_EQ(AK::log2<i32>(-1), AK::NumericLimits<i32>::min());
    EXPECT_EQ(AK::ceil_log2<i32>(-1), AK::NumericLimits<i32>::min());
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

TEST_CASE(is_power_of)
{
    constexpr auto check_prime = []<u64 prime>(u64 limit) {
        for (u64 power = 0; power < limit; ++power)
            EXPECT(AK::is_power_of<prime>(AK::pow(prime, power)));
    };

    // Limits calculated as floor( log_{prime}(2^64) ) to prevent overflows.
    check_prime.operator()<2>(64);
    check_prime.operator()<3>(40);
    check_prime.operator()<5>(27);
    check_prime.operator()<7>(20);
    check_prime.operator()<11>(18);
    check_prime.operator()<97>(9);
    check_prime.operator()<257>(7);
}
