/*
 * Copyright (c) 2020, Tom Lebreux <tomlebreux@hotmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/Base64.h>
#include <AK/DeprecatedString.h>
#include <string.h>

TEST_CASE(test_decode)
{
    auto decode_equal = [&](StringView input, StringView expected) {
        auto decoded_option = decode_base64(input);
        EXPECT(!decoded_option.is_error());
        auto decoded = decoded_option.release_value();
        EXPECT(DeprecatedString::copy(decoded) == expected);
        EXPECT(expected.length() <= calculate_base64_decoded_length(input.bytes()));
    };

    decode_equal(""sv, ""sv);
    decode_equal("Zg=="sv, "f"sv);
    decode_equal("Zm8="sv, "fo"sv);
    decode_equal("Zm9v"sv, "foo"sv);
    decode_equal("Zm9vYg=="sv, "foob"sv);
    decode_equal("Zm9vYmE="sv, "fooba"sv);
    decode_equal("Zm9vYmFy"sv, "foobar"sv);
    decode_equal("Z m\r9\n   v\v  Ym\tFy"sv, "foobar"sv);
    EXPECT_EQ(decode_base64(" ZD Qg\r\nPS An Zm91cic\r\n 7"sv).value(), decode_base64("ZDQgPSAnZm91cic7"sv).value());
}

TEST_CASE(test_decode_invalid)
{
    EXPECT(decode_base64(("asdf\xffqwe"sv)).is_error());
    EXPECT(decode_base64(("asdf\x80qwe"sv)).is_error());
    EXPECT(decode_base64(("asdf:qwe"sv)).is_error());
    EXPECT(decode_base64(("asdf=qwe"sv)).is_error());
}

TEST_CASE(test_encode)
{
    auto encode_equal = [&](StringView input, StringView expected) {
        auto encoded = MUST(encode_base64(input.bytes()));
        EXPECT(encoded == expected);
        EXPECT_EQ(expected.length(), calculate_base64_encoded_length(input.bytes()));
    };

    encode_equal(""sv, ""sv);
    encode_equal("f"sv, "Zg=="sv);
    encode_equal("fo"sv, "Zm8="sv);
    encode_equal("foo"sv, "Zm9v"sv);
    encode_equal("foob"sv, "Zm9vYg=="sv);
    encode_equal("fooba"sv, "Zm9vYmE="sv);
    encode_equal("foobar"sv, "Zm9vYmFy"sv);
}
