/*
 * Copyright (c) 2021, Peter Bocan  <me@pbocan.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCrypto/Authentication/GHash.h>
#include <LibCrypto/Authentication/HMAC.h>
#include <LibCrypto/Hash/MD5.h>
#include <LibCrypto/Hash/SHA1.h>
#include <LibCrypto/Hash/SHA2.h>
#include <LibTest/TestCase.h>
#include <cstring>

TEST_CASE(test_MD5_name)
{
    Crypto::Hash::MD5 md5;
    EXPECT(md5.class_name() == "MD5");
}

TEST_CASE(test_MD5_hash_string)
{
    u8 result[] {
        0xaf, 0x04, 0x3a, 0x08, 0x94, 0x38, 0x6e, 0x7f, 0xbf, 0x73, 0xe4, 0xaa, 0xf0, 0x8e, 0xee, 0x4c
    };
    auto digest = Crypto::Hash::MD5::hash("Well hello friends");
    EXPECT(memcmp(result, digest.data, Crypto::Hash::MD5::digest_size()) == 0);
}

TEST_CASE(test_MD5_hash_empty_string)
{
    u8 result[] {
        0xd4, 0x1d, 0x8c, 0xd9, 0x8f, 0x00, 0xb2, 0x04, 0xe9, 0x80, 0x09, 0x98, 0xec, 0xf8, 0x42, 0x7e
    };
    auto digest = Crypto::Hash::MD5::hash("");
    EXPECT(memcmp(result, digest.data, Crypto::Hash::MD5::digest_size()) == 0);
}

TEST_CASE(test_MD5_hash_single_character)
{
    u8 result[] {
        0x0c, 0xc1, 0x75, 0xb9, 0xc0, 0xf1, 0xb6, 0xa8, 0x31, 0xc3, 0x99, 0xe2, 0x69, 0x77, 0x26, 0x61
    };
    auto digest = Crypto::Hash::MD5::hash("a");
    EXPECT(memcmp(result, digest.data, Crypto::Hash::MD5::digest_size()) == 0);
}

TEST_CASE(test_MD5_hash_alphabet)
{
    u8 result[] {
        0xc3, 0xfc, 0xd3, 0xd7, 0x61, 0x92, 0xe4, 0x00, 0x7d, 0xfb, 0x49, 0x6c, 0xca, 0x67, 0xe1, 0x3b
    };
    auto digest = Crypto::Hash::MD5::hash("abcdefghijklmnopqrstuvwxyz");
    EXPECT(memcmp(result, digest.data, Crypto::Hash::MD5::digest_size()) == 0);
}

TEST_CASE(test_MD5_hash_long_sequence)
{
    u8 result[] {
        0x57, 0xed, 0xf4, 0xa2, 0x2b, 0xe3, 0xc9, 0x55, 0xac, 0x49, 0xda, 0x2e, 0x21, 0x07, 0xb6, 0x7a
    };
    auto digest = Crypto::Hash::MD5::hash("12345678901234567890123456789012345678901234567890123456789012345678901234567890");
    EXPECT(memcmp(result, digest.data, Crypto::Hash::MD5::digest_size()) == 0);
}

TEST_CASE(test_MD5_consecutive_multiple_updates)
{
    u8 result[] {
        0xaf, 0x04, 0x3a, 0x08, 0x94, 0x38, 0x6e, 0x7f, 0xbf, 0x73, 0xe4, 0xaa, 0xf0, 0x8e, 0xee, 0x4c
    };
    Crypto::Hash::MD5 md5;

    md5.update("Well");
    md5.update(" hello ");
    md5.update("friends");
    auto digest = md5.digest();

    EXPECT(memcmp(result, digest.data, Crypto::Hash::MD5::digest_size()) == 0);
}

TEST_CASE(test_MD5_consecutive_updates_reuse)
{
    Crypto::Hash::MD5 md5;

    md5.update("Well");
    md5.update(" hello ");
    md5.update("friends");
    auto digest0 = md5.digest();

    md5.update("Well");
    md5.update(" hello ");
    md5.update("friends");
    auto digest1 = md5.digest();

    EXPECT(memcmp(digest0.data, digest1.data, Crypto::Hash::MD5::digest_size()) == 0);
}

TEST_CASE(test_SHA1_name)
{
    Crypto::Hash::SHA1 sha;
    EXPECT(sha.class_name() == "SHA1");
}

TEST_CASE(test_SHA1_hash_empty_string)
{
    u8 result[] {
        0xda, 0x39, 0xa3, 0xee, 0x5e, 0x6b, 0x4b, 0x0d, 0x32, 0x55, 0xbf, 0xef, 0x95, 0x60, 0x18, 0x90, 0xaf, 0xd8, 0x07, 0x09
    };
    auto digest = Crypto::Hash::SHA1::hash("");
    EXPECT(memcmp(result, digest.data, Crypto::Hash::SHA1::digest_size()) == 0);
}

TEST_CASE(test_SHA1_hash_long_string)
{
    u8 result[] {
        0x12, 0x15, 0x1f, 0xb1, 0x04, 0x44, 0x93, 0xcc, 0xed, 0x54, 0xa6, 0xb8, 0x7e, 0x93, 0x37, 0x7b, 0xb2, 0x13, 0x39, 0xdb
    };
    auto digest = Crypto::Hash::SHA1::hash("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
    EXPECT(memcmp(result, digest.data, Crypto::Hash::SHA1::digest_size()) == 0);
}

TEST_CASE(test_SHA1_hash_successive_updates)
{
    u8 result[] {
        0xd6, 0x6e, 0xce, 0xd1, 0xf4, 0x08, 0xc6, 0xd8, 0x35, 0xab, 0xf0, 0xc9, 0x05, 0x26, 0xa4, 0xb2, 0xb8, 0xa3, 0x7c, 0xd3
    };
    auto hasher = Crypto::Hash::SHA1 {};
    hasher.update("aaaaaaaaaaaaaaa");
    hasher.update("aaaaaaaaaaaaaaa");
    hasher.update("aaaaaaaaaaaaaaa");
    hasher.update("aaaaaaaaaaaaaaa");
    hasher.update("aaaaaaaaaaaaaaa");
    hasher.update("aaaaaaaaaaaaaaa");
    hasher.update("aaaaaaaaaaaaaaa");
    hasher.update("aaaaaaaaaaaaaaa");
    hasher.update("aaaaaaaaaaaaaaa");
    hasher.update("aaaaaaaaaaaaaaa");
    hasher.update("aaaaaaaaaaaaaaa");
    hasher.update("aaaaaaaaaaaaaaa");
    hasher.update("aaaaaaaaa");
    auto digest = hasher.digest();
    EXPECT(memcmp(result, digest.data, Crypto::Hash::SHA1::digest_size()) == 0);
}

TEST_CASE(test_SHA256_name)
{
    Crypto::Hash::SHA256 sha;
    EXPECT_EQ(sha.class_name(), "SHA256");
}

TEST_CASE(test_SHA256_hash_string)
{
    u8 result[] {
        0x9a, 0xcd, 0x50, 0xf9, 0xa2, 0xaf, 0x37, 0xe4, 0x71, 0xf7, 0x61, 0xc3, 0xfe, 0x7b, 0x8d, 0xea, 0x56, 0x17, 0xe5, 0x1d, 0xac, 0x80, 0x2f, 0xe6, 0xc1, 0x77, 0xb7, 0x4a, 0xbf, 0x0a, 0xbb, 0x5a
    };
    auto digest = Crypto::Hash::SHA256::hash("Well hello friends");
    EXPECT(memcmp(result, digest.data, Crypto::Hash::SHA256::digest_size()) == 0);
}

TEST_CASE(test_SHA256_hash_empty_string)
{
    u8 result[] {
        0xe3, 0xb0, 0xc4, 0x42, 0x98, 0xfc, 0x1c, 0x14, 0x9a, 0xfb, 0xf4, 0xc8, 0x99, 0x6f, 0xb9, 0x24, 0x27, 0xae, 0x41, 0xe4, 0x64, 0x9b, 0x93, 0x4c, 0xa4, 0x95, 0x99, 0x1b, 0x78, 0x52, 0xb8, 0x55
    };
    auto digest = Crypto::Hash::SHA256::hash("");
    EXPECT(memcmp(result, digest.data, Crypto::Hash::SHA256::digest_size()) == 0);
}

TEST_CASE(test_SHA384_name)
{
    Crypto::Hash::SHA384 sha;
    EXPECT_EQ(sha.class_name(), "SHA384");
}

TEST_CASE(test_SHA384_hash_string)
{
    u8 result[] {
        0x2f, 0x01, 0x8e, 0x9a, 0x4f, 0xd1, 0x36, 0xb9, 0x0f, 0xcc, 0x21, 0xde, 0x1a, 0xd4, 0x49, 0x51, 0x57, 0x82, 0x86, 0x84, 0x54, 0x09, 0x82, 0x7b, 0x54, 0x56, 0x93, 0xac, 0x2c, 0x46, 0x0c, 0x1f, 0x5e, 0xec, 0xe0, 0xf7, 0x8b, 0x0b, 0x84, 0x27, 0xc8, 0xb8, 0xbe, 0x49, 0xce, 0x8f, 0x1c, 0xff
    };
    auto digest = Crypto::Hash::SHA384::hash("Well hello friends");
    EXPECT(memcmp(result, digest.data, Crypto::Hash::SHA384::digest_size()) == 0);
}

TEST_CASE(test_SHA512_name)
{
    Crypto::Hash::SHA512 sha;
    EXPECT_EQ(sha.class_name(), "SHA512");
}

TEST_CASE(test_SHA512_hash_string)
{
    u8 result[] {
        0x00, 0xfe, 0x68, 0x09, 0x71, 0x0e, 0xcb, 0x2b, 0xe9, 0x58, 0x00, 0x13, 0x69, 0x6a, 0x9e, 0x9e, 0xbd, 0x09, 0x1b, 0xfe, 0x14, 0xc9, 0x13, 0x82, 0xc7, 0x40, 0x34, 0xfe, 0xca, 0xe6, 0x87, 0xcb, 0x26, 0x36, 0x92, 0xe6, 0x34, 0x94, 0x3a, 0x11, 0xe5, 0xbb, 0xb5, 0xeb, 0x8e, 0x70, 0xef, 0x64, 0xca, 0xf7, 0x21, 0xb1, 0xde, 0xf2, 0x34, 0x85, 0x6f, 0xa8, 0x56, 0xd8, 0x23, 0xa1, 0x3b, 0x29
    };
    auto digest = Crypto::Hash::SHA512::hash("Well hello friends");
    EXPECT(memcmp(result, digest.data, Crypto::Hash::SHA512::digest_size()) == 0);
}

TEST_CASE(test_SHA512_hash_empty_string)
{
    u8 result[] {
        0xcf, 0x83, 0xe1, 0x35, 0x7e, 0xef, 0xb8, 0xbd, 0xf1, 0x54, 0x28, 0x50, 0xd6, 0x6d, 0x80, 0x07, 0xd6, 0x20, 0xe4, 0x05, 0x0b, 0x57, 0x15, 0xdc, 0x83, 0xf4, 0xa9, 0x21, 0xd3, 0x6c, 0xe9, 0xce, 0x47, 0xd0, 0xd1, 0x3c, 0x5d, 0x85, 0xf2, 0xb0, 0xff, 0x83, 0x18, 0xd2, 0x87, 0x7e, 0xec, 0x2f, 0x63, 0xb9, 0x31, 0xbd, 0x47, 0x41, 0x7a, 0x81, 0xa5, 0x38, 0x32, 0x7a, 0xf9, 0x27, 0xda, 0x3e
    };
    auto digest = Crypto::Hash::SHA512::hash("");
    EXPECT(memcmp(result, digest.data, Crypto::Hash::SHA512::digest_size()) == 0);
}

TEST_CASE(test_ghash_test_name)
{
    Crypto::Authentication::GHash ghash("WellHelloFriends");
    EXPECT_EQ(ghash.class_name(), "GHash");
}

TEST_CASE(test_ghash_galois_field_multiply)
{
    u32 x[4] { 0x42831ec2, 0x21777424, 0x4b7221b7, 0x84d0d49c },
        y[4] { 0xb83b5337, 0x08bf535d, 0x0aa6e529, 0x80d53b78 }, z[4] { 0, 0, 0, 0 };
    static constexpr u32 result[4] { 0x59ed3f2b, 0xb1a0aaa0, 0x7c9f56c6, 0xa504647b };

    Crypto::Authentication::galois_multiply(z, x, y);
    EXPECT(memcmp(result, z, 4 * sizeof(u32)) == 0);
}

TEST_CASE(test_ghash_galois_field_multiply2)
{
    u32 x[4] { 59300558, 1622582162, 4079534777, 1907555960 },
        y[4] { 1726565332, 4018809915, 2286746201, 3392416558 }, z[4];
    constexpr static u32 result[4] { 1580123974, 2440061576, 746958952, 1398005431 };

    Crypto::Authentication::galois_multiply(z, x, y);
    EXPECT(memcmp(result, z, 4 * sizeof(u32)) == 0);
}
