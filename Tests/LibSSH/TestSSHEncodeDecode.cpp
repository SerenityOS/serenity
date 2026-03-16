/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibSSH/DataTypes.h>
#include <LibTest/TestCase.h>

TEST_CASE(string)
{
    auto test_cases = to_array({
        "Serenity"sv,
        "zlib"sv,
        "Including\0null"sv,
        "Including Space"sv,
        "Including Unicode ˢˢʰ"sv,
        "testing"sv,
    });

    for (auto test : test_cases) {
        AllocatingMemoryStream encoded_stream {};
        TRY_OR_FAIL(SSH::encode_string(encoded_stream, test));

        auto encoded = TRY_OR_FAIL(encoded_stream.read_until_eof());
        FixedMemoryStream decoded_stream { encoded.bytes() };

        auto decoded = TRY_OR_FAIL(SSH::decode_string(decoded_stream));
        EXPECT_EQ(test.bytes(), decoded);
    }
}

// Examples are from:
// https://datatracker.ietf.org/doc/html/rfc4251#section-5

TEST_CASE(string_spec)
{
    // "For example: the US-ASCII string "testing" is represented as 00 00 00 07 t e s t i n g"
    auto test_string = "testing"sv;
    auto encoded_string = to_array<u8>({ 0, 0, 0, 7, 't', 'e', 's', 't', 'i', 'n', 'g' });

    AllocatingMemoryStream encoded_stream {};
    TRY_OR_FAIL(SSH::encode_string(encoded_stream, test_string));

    auto encoded = TRY_OR_FAIL(encoded_stream.read_until_eof());
    EXPECT_EQ(encoded.span(), encoded_string);
}

TEST_CASE(name_list)
{
    // value                      representation (hex)
    // -----                      --------------------
    // (), the empty name-list    00 00 00 00
    // ("zlib")                   00 00 00 04 7a 6c 69 62
    // ("zlib,none")              00 00 00 09 7a 6c 69 62 2c 6e 6f 6e 65

    auto test = [](Span<StringView> list, ReadonlyBytes expected) -> ErrorOr<void> {
        AllocatingMemoryStream stream;
        TRY(SSH::encode_name_list(stream, list));
        auto encoded = TRY(stream.read_until_eof());
        EXPECT_EQ(encoded, expected);
        return {};
    };

    TRY_OR_FAIL(test({}, to_array<u8>({ 0, 0, 0, 0 })));
    TRY_OR_FAIL(test(to_array<StringView>({ "zlib"sv }), to_array<u8>({ 0, 0, 0, 4, 0x7a, 0x6c, 0x69, 0x62 })));
    TRY_OR_FAIL(test(to_array<StringView>({ "zlib"sv, "none"sv }), to_array<u8>({ 0, 0, 0, 9, 0x7a, 0x6c, 0x69, 0x62, 0x2c, 0x6e, 0x6f, 0x6e, 0x65 })));
}

TEST_CASE(mpint)
{
    // value (hex)        representation (hex)
    // -----------        --------------------
    // 0                  00 00 00 00
    // 9a378f9b2e332a7    00 00 00 08 09 a3 78 f9 b2 e3 32 a7
    // 80                 00 00 00 02 00 80
    // -1234              00 00 00 02 ed cc
    // -deadbeef          00 00 00 05 ff 21 52 41 11

    auto test = [](ReadonlyBytes value, ReadonlyBytes expected) -> ErrorOr<void> {
        auto encoded = TRY(SSH::as_mpint(value));
        EXPECT_EQ(encoded.bytes(), expected);
        return {};
    };

    TRY_OR_FAIL(test(to_array<u8>({ 0x09, 0xa3, 0x78, 0xf9, 0xb2, 0xe3, 0x32, 0xa7 }), to_array<u8>({ 0x00, 0x00, 0x00, 0x08, 0x09, 0xa3, 0x78, 0xf9, 0xb2, 0xe3, 0x32, 0xa7 })));
    TRY_OR_FAIL(test(to_array<u8>({ 0x80 }), to_array<u8>({ 0x00, 0x00, 0x00, 0x02, 0x00, 0x80 })));

    // FIXME: Whenever we add support for them add the corresponding tests:
    //        - for input with leading zeros
    //        - for negative input numbers
}

TEST_CASE(typed_blob)
{
    ByteBuffer a;
    a.resize(8);
    a.bytes().fill(0x42);
    auto input = SSH::TypedBlob { SSH::TypedBlob::Type::SSH_ED25519, a };

    AllocatingMemoryStream stream;
    TRY_OR_FAIL(input.encode(stream));
    auto result = TRY_OR_FAIL(stream.read_until_eof());

    auto expected = to_array<u8>({
        0x00, 0x00, 0x00, 27,                                  // Total size (string size + blob size + 8)
        0x00, 0x00, 0x00, 11,                                  // string size
        's', 's', 'h', '-', 'e', 'd', '2', '5', '5', '1', '9', // key type
        0x00, 0x00, 0x00, 8,                                   // blob size
        0x42, 0x42, 0x42, 0x42, 0x42, 0x42, 0x42, 0x42,        // blob
    });

    EXPECT_EQ(result.bytes(), expected.span());
}
