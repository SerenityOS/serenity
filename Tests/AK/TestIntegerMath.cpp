/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/IntegralMath.h>
#include <initializer_list>

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

TEST_CASE(exp2)
{
    EXPECT_EQ(AK::exp2<u64>(0), 1ull);
    EXPECT_EQ(AK::exp2<u64>(1), 2ull);
    EXPECT_EQ(AK::exp2<i8>(6), 64);
    EXPECT_EQ(AK::exp2<u8>(7), 128);
    EXPECT_EQ(AK::exp2<u16>(9), 512);
    EXPECT_EQ(AK::exp2<i16>(14), 16384);
    EXPECT_EQ(AK::exp2<u16>(15), 32768);
    EXPECT_EQ(AK::exp2<u32>(17), 131072u);
    EXPECT_EQ(AK::exp2<i32>(30), 1073741824);
    EXPECT_EQ(AK::exp2<u32>(31), 2147483648);
    EXPECT_EQ(AK::exp2<i64>(32), 4294967296);
    EXPECT_EQ(AK::exp2<u64>(33), 8589934592ull);
    EXPECT_EQ(AK::exp2<i64>(62), 4611686018427387904);
    EXPECT_EQ(AK::exp2<u64>(63), 9223372036854775808ull);
}
