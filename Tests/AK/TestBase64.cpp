/*
 * Copyright (c) 2020, Tom Lebreux <tomlebreux@hotmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/Base64.h>
#include <AK/ByteString.h>
#include <string.h>

TEST_CASE(test_decode)
{
    auto decode_equal = [&](StringView input, StringView expected) {
        auto decoded = TRY_OR_FAIL(decode_base64(input));
        EXPECT(ByteString::copy(decoded) == expected);
        EXPECT(expected.length() <= calculate_base64_decoded_length(input.bytes()));
    };

    decode_equal(""sv, ""sv);
    decode_equal("Zg=="sv, "f"sv);
    decode_equal("Zm8="sv, "fo"sv);
    decode_equal("Zm9v"sv, "foo"sv);
    decode_equal("Zm9vYg=="sv, "foob"sv);
    decode_equal("Zm9vYmE="sv, "fooba"sv);
    decode_equal("Zm9vYmFy"sv, "foobar"sv);
    decode_equal(" Zm9vYmFy "sv, "foobar"sv);
    decode_equal("  \n\r \t Zm9vYmFy \n"sv, "foobar"sv);

    decode_equal("aGVsbG8/d29ybGQ="sv, "hello?world"sv);
}

TEST_CASE(test_decode_invalid)
{
    EXPECT(decode_base64(("asdf\xffqwe"sv)).is_error());
    EXPECT(decode_base64(("asdf\x80qwe"sv)).is_error());
    EXPECT(decode_base64(("asdf:qwe"sv)).is_error());
    EXPECT(decode_base64(("asdf=qwe"sv)).is_error());

    EXPECT(decode_base64("aGVsbG8_d29ybGQ="sv).is_error());
    EXPECT(decode_base64url("aGVsbG8/d29ybGQ="sv).is_error());

    EXPECT(decode_base64("Y"sv).is_error());
    EXPECT(decode_base64("YQ"sv).is_error());
    EXPECT(decode_base64("YQ="sv).is_error());
    EXPECT(decode_base64("PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMC42MDUiIGhlaWdodD0iMTUuNTU1Ij48cGF0aCBmaWxsPSIjODg5IiBkPSJtMi44MjggMTUuNTU1IDcuNzc3LTcuNzc5TDIuODI4IDAgMCAyLjgyOGw0Ljk0OSA0Ljk0OEwwIDEyLjcyN2wyLjgyOCAyLjgyOHoiLz48L3N2Zz4"sv).is_error());
}

TEST_CASE(test_decode_only_padding)
{
    // Only padding is not allowed
    EXPECT(decode_base64("="sv).is_error());
    EXPECT(decode_base64("=="sv).is_error());
    EXPECT(decode_base64("==="sv).is_error());
    EXPECT(decode_base64("===="sv).is_error());

    EXPECT(decode_base64url("="sv).is_error());
    EXPECT(decode_base64url("=="sv).is_error());
    EXPECT(decode_base64url("==="sv).is_error());
    EXPECT(decode_base64url("===="sv).is_error());
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

TEST_CASE(test_urldecode)
{
    auto decode_equal = [&](StringView input, StringView expected) {
        auto decoded = TRY_OR_FAIL(decode_base64url(input));
        EXPECT(ByteString::copy(decoded) == expected);
        EXPECT(expected.length() <= calculate_base64_decoded_length(input.bytes()));
    };

    decode_equal(""sv, ""sv);
    decode_equal("Zg=="sv, "f"sv);
    decode_equal("Zm8="sv, "fo"sv);
    decode_equal("Zm9v"sv, "foo"sv);
    decode_equal("Zm9vYg=="sv, "foob"sv);
    decode_equal("Zm9vYmE="sv, "fooba"sv);
    decode_equal("Zm9vYmFy"sv, "foobar"sv);
    decode_equal(" Zm9vYmFy "sv, "foobar"sv);
    decode_equal("  \n\r \t Zm9vYmFy \n"sv, "foobar"sv);

    decode_equal("TG9yZW0gaXBzdW0gZG9sb3Igc2l0IGFtZXQsIGNvbnNlY3RldHVyIGFkaXBpc2NpbmcgZWxpdCwgc2VkIGRvIGVpdXNtb2QgdGVtcG9yIGluY2lkaWR1bnQgdXQgbGFib3JlIGV0IGRvbG9yZSBtYWduYSBhbGlxdWEu"sv, "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua."sv);
    decode_equal("aGVsbG8_d29ybGQ="sv, "hello?world"sv);
}

TEST_CASE(test_urlencode)
{
    auto encode_equal = [&](StringView input, StringView expected) {
        auto encoded = MUST(encode_base64url(input.bytes()));
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

    encode_equal("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua."sv, "TG9yZW0gaXBzdW0gZG9sb3Igc2l0IGFtZXQsIGNvbnNlY3RldHVyIGFkaXBpc2NpbmcgZWxpdCwgc2VkIGRvIGVpdXNtb2QgdGVtcG9yIGluY2lkaWR1bnQgdXQgbGFib3JlIGV0IGRvbG9yZSBtYWduYSBhbGlxdWEu"sv);
    encode_equal("hello?world"sv, "aGVsbG8_d29ybGQ="sv);

    encode_equal("hello!!world"sv, "aGVsbG8hIXdvcmxk"sv);
}
