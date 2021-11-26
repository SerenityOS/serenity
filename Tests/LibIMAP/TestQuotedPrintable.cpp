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
    auto decode_equal = [](const char* input, const char* expected) {
        auto decoded = IMAP::decode_quoted_printable(StringView(input));
        EXPECT(String::copy(decoded) == String(expected));
    };

    auto decode_equal_byte_buffer = [](const char* input, const char* expected, size_t expected_length) {
        auto decoded = IMAP::decode_quoted_printable(StringView(input));
        EXPECT(decoded == *ByteBuffer::copy(expected, expected_length));
    };

    decode_equal("hello world", "hello world");
    decode_equal("=3D", "=");
    decode_equal("hello=\r\n world", "hello world");
    decode_equal("=68=65=6C=6C=6F=20=\r\n=77=6F=72=6C=64", "hello world");

    // Doesn't mistake hex sequences without a preceding '=' as an escape sequence.
    decode_equal("4A=4B=4C4D", "4AKL4D");

    // Allows lowercase escape sequences.
    decode_equal("=4a=4b=4c=4d=4e=4f", "JKLMNO");

    // Bytes for U+1F41E LADY BEETLE
    decode_equal_byte_buffer("=F0=9F=90=9E", "\xF0\x9F\x90\x9E", 4);

    // Illegal characters. If these aren't escaped, they are simply ignored.
    // Illegal characters are:
    // - ASCII control bytes that aren't tab, carriage return or new line
    // - Any byte above 0x7E
    StringBuilder illegal_character_builder;
    for (u16 byte = 0; byte <= 0xFF; ++byte) {
        if (byte > 0x7E || (is_ascii_control(byte) && byte != '\t' && byte != '\r' && byte != '\n'))
            illegal_character_builder.append(byte);
    }

    auto illegal_character_decode = IMAP::decode_quoted_printable(illegal_character_builder.to_string());
    EXPECT(illegal_character_decode.is_empty());
}
