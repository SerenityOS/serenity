/*
 * Copyright (c) 2021, Peter Bocan  <me@pbocan.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCrypto/Checksum/Adler32.h>
#include <LibCrypto/Checksum/CRC32.h>
#include <LibTest/TestCase.h>

TEST_CASE(test_adler32)
{
    auto do_test = [](ReadonlyBytes input, u32 expected_result) {
        auto digest = Crypto::Checksum::Adler32(input).digest();
        EXPECT_EQ(digest, expected_result);
    };

    do_test(String("").bytes(), 0x1);
    do_test(String("a").bytes(), 0x00620062);
    do_test(String("abc").bytes(), 0x024d0127);
    do_test(String("message digest").bytes(), 0x29750586);
    do_test(String("abcdefghijklmnopqrstuvwxyz").bytes(), 0x90860b20);
}

TEST_CASE(test_crc32)
{
    auto do_test = [](ReadonlyBytes input, u32 expected_result) {
        auto digest = Crypto::Checksum::CRC32(input).digest();
        EXPECT_EQ(digest, expected_result);
    };

    do_test(String("").bytes(), 0x0);
    do_test(String("The quick brown fox jumps over the lazy dog").bytes(), 0x414FA339);
    do_test(String("various CRC algorithms input data").bytes(), 0x9BD366AE);
}
