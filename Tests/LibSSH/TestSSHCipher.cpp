/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/MemoryStream.h>
#include <LibSSH/Cipher.h>
#include <LibTest/TestCase.h>

TEST_CASE(identity_cipher)
{
    auto input = "This shouldn't change!"sv;
    auto buffer = TRY_OR_FAIL(ByteBuffer::copy(input.bytes()));

    auto cipher = make<SSH::IdentityCipher>();

    TRY_OR_FAIL(cipher->encrypt(0, buffer));
    EXPECT_EQ(buffer, input.bytes());

    TRY_OR_FAIL(cipher->decrypt(0, buffer));
    EXPECT_EQ(buffer, input.bytes());
}

TEST_CASE(ChaCha20Poly1305)
{
    // This test case comes from a real packet send by openssh, I dumped the first
    // encrypted message the client send and the secrets our server had.

    auto shared_secret_raw = "\x53\x22\xc2\x28\x50\xfe\x85\xbd\xb3\x35\xcf\xb4\x67\x4a\x82\x0b\xd0\x54\xa3\xd9\x4a\xc8\x3c\x55\x91\xb4\xd5\xf7\x52\x28\xd2\x6c"sv;
    auto hash_raw = "\xc0\x73\x95\x08\xd8\xc0\xbf\x2a\x6d\x13\x1b\xfd\x67\x88\x32\xdf\x15\xeb\x5a\x2e\x5a\xf3\xdf\x98\xa1\x1c\x41\x2b\x2a\x26\x4d\x62"sv;

    auto shared_secret = TRY_OR_FAIL(ByteBuffer::copy(shared_secret_raw.bytes()));
    SSH::ChaCha20Poly1305Cipher::Digest hash;
    hash_raw.bytes().copy_to({ &hash.data, decltype(hash)::Size });

    auto encrypted_raw = "\x0c\x3b\x6d\x66\xb1\x72\xf3\x85\xa4\x88\x35\xf2\x0a\x6d\xa5\x9b\x29\xcf\xe6\xe8\x5f\xad\x05\x6b\x94\x89\xae\xab\x10\x37\x88\x7a\x6b\x58\x30\xb1\x9b\x6f\xc1\x8f\x6c\x89\x68\x24"sv;
    auto packet = TRY_OR_FAIL(ByteBuffer::copy(encrypted_raw.bytes()));

    auto cipher = SSH::ChaCha20Poly1305Cipher::create(shared_secret, hash, hash);

    TRY_OR_FAIL(cipher->decrypt(3, packet));

    auto expected = to_array<u8>({
        0, 0, 0, 24,                                               // Packet size
        6,                                                         // Padding size
        5,                                                         // Message ID: SERVICE_REQUEST
        0, 0, 0, 12,                                               // string size
        's', 's', 'h', '-', 'u', 's', 'e', 'r', 'a', 'u', 't', 'h' // service name
    });

    EXPECT_EQ(packet.bytes().trim(expected.size()), expected.span());
}
