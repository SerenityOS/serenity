/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <stdlib.h>

TEST_CASE(weird_wrong_division)
{

    dbgln("{:016x}", bit_cast<u64>(1e-22));
    dbgln("{:016x}", bit_cast<u64>(89255.));
    dbgln("{:016x}", bit_cast<u64>(89255. * 1e-22));
    dbgln("{:016x}", bit_cast<u64>(89255. / 1e22));
    dbgln("{:016x}", bit_cast<u64>(1e-22 * 89255.));
    dbgln("{:016x}", bit_cast<u64>(89255. * strtod("1e-22", nullptr)));
    dbgln("{:016x}", bit_cast<u64>(89255. / strtod("1e22", nullptr)));
    dbgln("{:016x}", bit_cast<u64>(strtod("89255", nullptr) * strtod("1e-22", nullptr)));
    dbgln("{:016x}", bit_cast<u64>(strtod("89255", nullptr) / strtod("1e22", nullptr)));

    // Just to make sure we get the exact double we mean
    u64 denom_in_bits = 0x4480f0cf064dd592ULL; // 1e22
    u64 numer_in_bits = 0x40f5ca7000000000ULL;

    double denominator = bit_cast<double>(denom_in_bits);
    double numerator = bit_cast<double>(numer_in_bits);

    double result = numerator / denominator;

    dbgln("got {} --> [{}]", result, bit_cast<u64>(result));

    EXPECT_EQ(89255e-22, result);
    EXPECT_EQ(bit_cast<u64>(result), 0x3c6494af6ce5221fULL);
}
