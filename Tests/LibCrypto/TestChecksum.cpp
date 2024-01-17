/*
 * Copyright (c) 2021, Peter Bocan  <me@pbocan.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCrypto/Checksum/Adler32.h>
#include <LibCrypto/Checksum/CRC32.h>
#include <LibCrypto/Checksum/cksum.h>
#include <LibTest/TestCase.h>

TEST_CASE(test_adler32)
{
    auto do_test = [](ReadonlyBytes input, u32 expected_result) {
        auto digest = Crypto::Checksum::Adler32(input).digest();
        EXPECT_EQ(digest, expected_result);
    };

    do_test(ByteString("").bytes(), 0x1);
    do_test(ByteString("a").bytes(), 0x00620062);
    do_test(ByteString("abc").bytes(), 0x024d0127);
    do_test(ByteString("message digest").bytes(), 0x29750586);
    do_test(ByteString("abcdefghijklmnopqrstuvwxyz").bytes(), 0x90860b20);
}

TEST_CASE(test_cksum)
{
    auto do_test = [](ReadonlyBytes input, u32 expected_result) {
        auto digest = Crypto::Checksum::cksum(input).digest();
        EXPECT_EQ(digest, expected_result);
    };

    do_test(ByteString("").bytes(), 0xFFFFFFFF);
    do_test(ByteString("The quick brown fox jumps over the lazy dog").bytes(), 0x7BAB9CE8);
    do_test(ByteString("various CRC algorithms input data").bytes(), 0xEFB5CA4F);
}

TEST_CASE(test_cksum_atomic_digest)
{
    auto compare = [](u32 digest, u32 expected_result) {
        EXPECT_EQ(digest, expected_result);
    };

    Crypto::Checksum::cksum cksum;

    cksum.update(ByteString("Well").bytes());
    cksum.update(ByteString(" hello ").bytes());
    cksum.digest();
    cksum.update(ByteString("friends").bytes());
    auto digest = cksum.digest();

    compare(digest, 0x2D65C7E0);
}

TEST_CASE(test_crc32)
{
    auto do_test = [](ReadonlyBytes input, u32 expected_result) {
        auto digest = Crypto::Checksum::CRC32(input).digest();
        EXPECT_EQ(digest, expected_result);
    };

    do_test(ByteString("").bytes(), 0x0);
    do_test(ByteString("The quick brown fox jumps over the lazy dog").bytes(), 0x414FA339);
    do_test(ByteString("various CRC algorithms input data").bytes(), 0x9BD366AE);
}
