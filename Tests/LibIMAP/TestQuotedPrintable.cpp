/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <LibIMAP/QuotedPrintable.h>
#include <LibTest/TestCase.h>

TEST_CASE(test_decode)
{
    auto decode_equal = [](StringView input, StringView expected) {
        auto decoded = MUST(IMAP::decode_quoted_printable(input));
        EXPECT(decoded.bytes() == expected.bytes());
    };

    auto decode_equal_byte_buffer = [](StringView input, StringView expected) {
        auto decoded = MUST(IMAP::decode_quoted_printable(input));
        EXPECT(decoded.bytes() == expected.bytes());
    };

    decode_equal("hello world"sv, "hello world"sv);
    decode_equal("=3D"sv, "="sv);
    decode_equal("hello=\r\n world"sv, "hello world"sv);
    decode_equal("=68=65=6C=6C=6F=20=\r\n=77=6F=72=6C=64"sv, "hello world"sv);

    // Doesn't mistake hex sequences without a preceding '=' as an escape sequence.
    decode_equal("4A=4B=4C4D"sv, "4AKL4D"sv);

    // Allows lowercase escape sequences.
    decode_equal("=4a=4b=4c=4d=4e=4f"sv, "JKLMNO"sv);

    // Bytes for U+1F41E LADY BEETLE
    decode_equal_byte_buffer("=F0=9F=90=9E"sv, "\xF0\x9F\x90\x9E"sv);

    // Illegal characters. If these aren't escaped, they are simply ignored.
    // Illegal characters are:
    // - ASCII control bytes that aren't tab, carriage return or new line
    // - Any byte above 0x7E
    StringBuilder illegal_character_builder;
    for (u16 byte = 0; byte <= 0xFF; ++byte) {
        if (byte > 0x7E || (is_ascii_control(byte) && byte != '\t' && byte != '\r' && byte != '\n'))
            illegal_character_builder.append(byte);
    }

    auto illegal_character_decode = MUST(IMAP::decode_quoted_printable(illegal_character_builder.to_byte_string()));
    EXPECT(illegal_character_decode.is_empty());

    // If an escape sequence is invalid the characters are output unaltered. Illegal characters are ignored as usual.
    decode_equal("="sv, "="sv);
    decode_equal("=Z"sv, "=Z"sv);
    decode_equal("=\x7F"sv, "="sv);
    decode_equal("=\x7F\x7F"sv, "="sv);
    decode_equal("=A\x7F"sv, "=A"sv);
    decode_equal("=A"sv, "=A"sv);
    decode_equal("=AZ"sv, "=AZ"sv);
    decode_equal("=\r"sv, "=\r"sv);
    decode_equal("=\r\r"sv, "=\r\r"sv);
    decode_equal("=\n\r"sv, "=\n\r"sv);
    decode_equal("=\rA"sv, "=\rA"sv);
}
