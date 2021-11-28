/*
 * Copyright (c) 2020, Tom Lebreux <tomlebreux@hotmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/Base64.h>
#include <AK/String.h>
#include <string.h>

TEST_CASE(test_decode)
{
    auto decode_equal = [&](const char* input, const char* expected) {
        auto decoded_option = decode_base64(StringView(input));
        EXPECT(decoded_option.has_value());
        auto decoded = decoded_option.release_value();
        EXPECT(String::copy(decoded) == String(expected));
        EXPECT(StringView(expected).length() <= calculate_base64_decoded_length(StringView(input).bytes()));
    };

    decode_equal("", "");
    decode_equal("Zg==", "f");
    decode_equal("Zm8=", "fo");
    decode_equal("Zm9v", "foo");
    decode_equal("Zm9vYg==", "foob");
    decode_equal("Zm9vYmE=", "fooba");
    decode_equal("Zm9vYmFy", "foobar");
}

TEST_CASE(test_decode_invalid)
{
    EXPECT(!decode_base64(StringView("asdf\xffqwe")).has_value());
    EXPECT(!decode_base64(StringView("asdf\x80qwe")).has_value());
    EXPECT(!decode_base64(StringView("asdf:qwe")).has_value());
    EXPECT(!decode_base64(StringView("asdf=qwe")).has_value());
}

TEST_CASE(test_encode)
{
    auto encode_equal = [&](const char* input, const char* expected) {
        auto encoded = encode_base64({ input, strlen(input) });
        EXPECT(encoded == String(expected));
        EXPECT_EQ(StringView(expected).length(), calculate_base64_encoded_length(StringView(input).bytes()));
    };

    encode_equal("", "");
    encode_equal("f", "Zg==");
    encode_equal("fo", "Zm8=");
    encode_equal("foo", "Zm9v");
    encode_equal("foob", "Zm9vYg==");
    encode_equal("fooba", "Zm9vYmE=");
    encode_equal("foobar", "Zm9vYmFy");
}
