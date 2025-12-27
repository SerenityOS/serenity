/*
 * Copyright (c) 2025, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Array.h>
#include <AK/InternetChecksum.h>
#include <LibTest/TestCase.h>
#include <netinet/in.h>

TEST_CASE(test_internetchecksum)
{
    auto do_test = [](ReadonlyBytes input, u16 expected_result) {
        auto digest = InternetChecksum(input).digest();
        EXPECT_EQ(digest, expected_result);
    };

    do_test(to_readonly_bytes(Array<u16, 3> { htons(0b0110'0110'0110'0000), htons(0b0101'0101'0101'0101), htons(0b1000'1111'0000'1100) }.span()), 0b1011'0101'0011'1101);

    // Test case from RFC1071.
    // The specified result doesn't include the final conversion from one's complement,
    // hence the bitwise negation.
    do_test(to_readonly_bytes(Array<u16, 4> { 0x0100, 0x03f2, 0xf5f4, 0xf7f6 }.span()), static_cast<u16>(~0xddf2));

    // Variation of the above (with an uneven payload).
    do_test(to_readonly_bytes(Array<u8, 3> { 0x01, 0x00, 0x03 }.span()), htons(static_cast<u16>(~0x4)));
}
