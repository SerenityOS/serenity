/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCrypto/Hash/SHA3.h>
#include <LibTest/TestCase.h>

// All examples are from the SHA-3 files at:
// https://csrc.nist.gov/projects/cryptographic-standards-and-guidelines/example-values

static ByteBuffer make_1600bits_message()
{
    ByteBuffer message;
    message.resize(200);
    message.span().fill('\xA3');
    return message;
}

TEST_CASE(SHA3_224)
{
    Crypto::Hash::SHA3_224 test;

    // 0-bit message
    // https://csrc.nist.gov/CSRC/media/Projects/Cryptographic-Standards-and-Guidelines/documents/examples/SHA3-224_Msg0.pdf
    auto result = test.digest();
    EXPECT_EQ(result.bytes(), "\x6b\x4e\x03\x42\x36\x67\xdb\xb7\x3b\x6e\x15\x45\x4f\x0e\xb1\xab\xd4\x59\x7f\x9a\x1b\x07\x8e\x3f\x5b\x5a\x6b\xc7"sv.bytes());

    // 1600-bits message
    // https://csrc.nist.gov/CSRC/media/Projects/Cryptographic-Standards-and-Guidelines/documents/examples/SHA3-224_1600.pdf
    auto message = make_1600bits_message();
    test.update(message);
    result = test.digest();
    EXPECT_EQ(result.bytes(), "\x93\x76\x81\x6a\xba\x50\x3f\x72\xf9\x6c\xe7\xeb\x65\xac\x09\x5d\xee\xe3\xbe\x4b\xf9\xbb\xc2\xa1\xcb\x7e\x11\xe0"sv.bytes());
}

TEST_CASE(SHA3_256)
{
    Crypto::Hash::SHA3_256 test;

    // 0-bit message
    // https://csrc.nist.gov/CSRC/media/Projects/Cryptographic-Standards-and-Guidelines/documents/examples/SHA3-256_Msg0.pdf
    auto result = test.digest();
    EXPECT_EQ(result.bytes(), "\xa7\xff\xc6\xf8\xbf\x1e\xd7\x66\x51\xc1\x47\x56\xa0\x61\xd6\x62\xf5\x80\xff\x4d\xe4\x3b\x49\xfa\x82\xd8\x0a\x4b\x80\xf8\x43\x4a"sv.bytes());

    // 1600-bits message
    // https://csrc.nist.gov/CSRC/media/Projects/Cryptographic-Standards-and-Guidelines/documents/examples/SHA3-256_1600.pdf
    auto message = make_1600bits_message();
    test.update(message);
    result = test.digest();
    EXPECT_EQ(result.bytes(), "\x79\xf3\x8a\xde\xc5\xc2\x03\x07\xa9\x8e\xf7\x6e\x83\x24\xaf\xbf\xd4\x6c\xfd\x81\xb2\x2e\x39\x73\xc6\x5f\xa1\xbd\x9d\xe3\x17\x87"sv.bytes());
}

TEST_CASE(SHA3_384)
{
    Crypto::Hash::SHA3_384 test;

    // 0-bit message
    // https://csrc.nist.gov/CSRC/media/Projects/Cryptographic-Standards-and-Guidelines/documents/examples/SHA3-384_Msg0.pdf
    auto result = test.digest();
    EXPECT_EQ(result.bytes(), "\x0c\x63\xa7\x5b\x84\x5e\x4f\x7d\x01\x10\x7d\x85\x2e\x4c\x24\x85\xc5\x1a\x50\xaa\xaa\x94\xfc\x61\x99\x5e\x71\xbb\xee\x98\x3a\x2a\xc3\x71\x38\x31\x26\x4a\xdb\x47\xfb\x6b\xd1\xe0\x58\xd5\xf0\x04"sv.bytes());

    // 1600-bits message
    // https://csrc.nist.gov/CSRC/media/Projects/Cryptographic-Standards-and-Guidelines/documents/examples/SHA3-384_1600.pdf
    auto message = make_1600bits_message();
    test.update(message);
    result = test.digest();
    EXPECT_EQ(result.bytes(), "\x18\x81\xde\x2c\xa7\xe4\x1e\xf9\x5d\xc4\x73\x2b\x8f\x5f\x00\x2b\x18\x9c\xc1\xe4\x2b\x74\x16\x8e\xd1\x73\x26\x49\xce\x1d\xbc\xdd\x76\x19\x7a\x31\xfd\x55\xee\x98\x9f\x2d\x70\x50\xdd\x47\x3e\x8f"sv.bytes());
}

TEST_CASE(SHA3_512)
{
    Crypto::Hash::SHA3_512 test;

    // 0-bit message
    // https://csrc.nist.gov/CSRC/media/Projects/Cryptographic-Standards-and-Guidelines/documents/examples/SHA3-512_Msg0.pdf
    auto result = test.digest();
    EXPECT_EQ(result.bytes(), "\xa6\x9f\x73\xcc\xa2\x3a\x9a\xc5\xc8\xb5\x67\xdc\x18\x5a\x75\x6e\x97\xc9\x82\x16\x4f\xe2\x58\x59\xe0\xd1\xdc\xc1\x47\x5c\x80\xa6\x15\xb2\x12\x3a\xf1\xf5\xf9\x4c\x11\xe3\xe9\x40\x2c\x3a\xc5\x58\xf5\x00\x19\x9d\x95\xb6\xd3\xe3\x01\x75\x85\x86\x28\x1d\xcd\x26"sv.bytes());

    // 1600-bits message
    // https://csrc.nist.gov/CSRC/media/Projects/Cryptographic-Standards-and-Guidelines/documents/examples/SHA3-512_1600.pdf
    auto message = make_1600bits_message();
    test.update(message);
    result = test.digest();
    EXPECT_EQ(result.bytes(), "\xe7\x6d\xfa\xd2\x20\x84\xa8\xb1\x46\x7f\xcf\x2f\xfa\x58\x36\x1b\xec\x76\x28\xed\xf5\xf3\xfd\xc0\xe4\x80\x5d\xc4\x8c\xae\xec\xa8\x1b\x7c\x13\xc3\x0a\xdf\x52\xa3\x65\x95\x84\x73\x9a\x2d\xf4\x6b\xe5\x89\xc5\x1c\xa1\xa4\xa8\x41\x6d\xf6\x54\x5a\x1c\xe8\xba\x00"sv.bytes());
}
