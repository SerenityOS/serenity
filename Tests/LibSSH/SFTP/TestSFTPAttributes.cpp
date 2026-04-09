/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibSSH/SFTP/Peer.h>
#include <LibTest/TestCase.h>

static void run_test(SSH::SFTP::Attributes attributes, ReadonlyBytes expected)
{
    AllocatingMemoryStream stream;
    TRY_OR_FAIL(attributes.encode(stream));
    auto encoded = TRY_OR_FAIL(stream.read_until_eof());

    EXPECT_EQ(encoded.bytes(), expected);

    FixedMemoryStream encoded_stream { encoded.bytes() };
    auto roundtrip = TRY_OR_FAIL(SSH::SFTP::Attributes::from_stream(encoded_stream));

    EXPECT_EQ(roundtrip, attributes);
}

TEST_CASE(attributes_empty)
{
    auto empty = SSH::SFTP::Attributes {};

    auto expected = to_array<u8>({
        0x00, 0x00, 0x00, 0x00, // flags (no fields present)
    });

    run_test(empty, expected);
}

TEST_CASE(attributes_size_only)
{
    auto input = SSH::SFTP::Attributes {
        .size = 1024,
    };

    auto expected = to_array<u8>({
        0x00, 0x00, 0x00, 0x01,                         // flags (SSH_FILEXFER_ATTR_SIZE)
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x04, 0x00, // size (1024)
    });

    run_test(input, expected);
}

TEST_CASE(attributes_uid_gid)
{
    auto input = SSH::SFTP::Attributes {
        .uid = 1000,
        .gid = 1000,
    };

    auto expected = to_array<u8>({
        0x00, 0x00, 0x00, 0x02, // flags (SSH_FILEXFER_ATTR_UIDGID)
        0x00, 0x00, 0x03, 0xe8, // uid (1000)
        0x00, 0x00, 0x03, 0xe8, // gid (1000)
    });

    run_test(input, expected);
}

TEST_CASE(attributes_permissions)
{
    auto input = SSH::SFTP::Attributes {
        .mode = 0100644,
    };

    auto expected = to_array<u8>({
        0x00, 0x00, 0x00, 0x04, // flags (SSH_FILEXFER_ATTR_PERMISSIONS)
        0x00, 0x00, 0x81, 0xa4, // mode (0100644)
    });

    run_test(input, expected);
}

TEST_CASE(attributes_timestamps)
{
    auto input = SSH::SFTP::Attributes {
        .atim = 1700000000,
        .mtim = 1700000001,
    };

    auto expected = to_array<u8>({
        0x00, 0x00, 0x00, 0x08, // flags (SSH_FILEXFER_ATTR_ACMODTIME)
        0x65, 0x53, 0xF1, 0x00, // atime (1700000000)
        0x65, 0x53, 0xF1, 0x01, // mtime (1700000001)
    });

    run_test(input, expected);
}

TEST_CASE(attributes_all_fields)
{
    auto input = SSH::SFTP::Attributes {
        .size = 4096,
        .uid = 501,
        .gid = 20,
        .mode = 0100644,
        .atim = 1700000000,
        .mtim = 1700000001,
    };

    auto expected = to_array<u8>({
        0x00, 0x00, 0x00, 0x0f,                         // flags (size | uidgid | permissions | acmodtime)
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x10, 0x00, // size (4096)
        0x00, 0x00, 0x01, 0xf5,                         // uid (501)
        0x00, 0x00, 0x00, 0x14,                         // gid (20)
        0x00, 0x00, 0x81, 0xa4,                         // mode (0100644)
        0x65, 0x53, 0xF1, 0x00,                         // atime (1700000000)
        0x65, 0x53, 0xF1, 0x01,                         // mtime (1700000001)
    });

    run_test(input, expected);
}
