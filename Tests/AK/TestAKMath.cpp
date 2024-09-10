/*
 * Copyright (c) 2023, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/Macros.h>
#include <LibTest/TestCase.h>

#include <AK/Math.h>

// Test cases for pow(2, x) generated with Python + numpy:
// echo -e "import numpy as np\nfor x in np.arange(-1020, 1020, 0.9):\n    print(f'{{ {x}, {2**x} }},')" | python3
// FIXME: Actually use these for a test of AK::pow; currently many inputs are broken due to our bad implementation.
TEST_CASE(pow)
{
    EXPECT_EQ(AK::pow(10., 0.), 1.);
    EXPECT_EQ(AK::pow(10., 1.), 10.);
    EXPECT_EQ(AK::pow(10., 2.), 100.);
    EXPECT_EQ(AK::pow(10., 3.), 1'000.);
    EXPECT_EQ(AK::pow(10., 4.), 10'000.);
    EXPECT_EQ(AK::pow(10., 5.), 100'000.);
    EXPECT_EQ(AK::pow(10., 6.), 1'000'000.);
    EXPECT_EQ(AK::pow(1.e18, 2.), 1.e36);
    EXPECT_EQ(AK::pow(1.e19, 2.), 1.e38);
    EXPECT_EQ(AK::pow(1.e20, 2.), 1.e40);
    EXPECT_EQ(AK::pow(1.e21, 2.), 1.e42);
    EXPECT_EQ(AK::pow(1.e22, 2.), 1.e44);
    // Largest 64-bit float value that fits in a 64-bit integer.
    EXPECT_EQ(AK::pow(1., 9223372036854774784.0), 1.);
    // Should not take the integer fast path anymore, since the exponent is beyond 64-bit integers.
    EXPECT_EQ(AK::pow(1., 9223372036854775808.0), 1.);
    EXPECT_EQ(AK::pow(1., 9223372036854777856.0), 1.);

    EXPECT_EQ(AK::pow(1., -9223372036854774784.0), 1.);
    EXPECT_EQ(AK::pow(1., -9223372036854775808.0), 1.);
    EXPECT_EQ(AK::pow(1., -9223372036854777856.0), 1.);
}
