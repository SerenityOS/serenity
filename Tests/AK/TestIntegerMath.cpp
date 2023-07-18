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
