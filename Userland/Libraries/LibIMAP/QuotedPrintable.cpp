/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <AK/GenericLexer.h>
#include <AK/StringBuilder.h>
#include <LibIMAP/QuotedPrintable.h>

namespace IMAP {

static constexpr bool is_illegal_character(char c)
{
    return (u8)c > 0x7E || (is_ascii_control(c) && c != '\t' && c != '\r' && c != '\n');
}

// RFC 2045 Section 6.7 "Quoted-Printable Content-Transfer-Encoding", https://datatracker.ietf.org/doc/html/rfc2045#section-6.7
ErrorOr<ByteBuffer> decode_quoted_printable(StringView input)
{
    GenericLexer lexer(input);
    StringBuilder output;

    // For any invalid escape sequence. RFC 2045 says a reasonable solution is just to append '=' followed by the unaltered escape characters.
    auto append_invalid_escape_sequence = [&](Optional<char> first = {}, Optional<char> second = {}) -> ErrorOr<void> {
        TRY(output.try_append('='));
        if (first.has_value() && !is_illegal_character(first.value()))
            TRY(output.try_append(first.value()));

        if (second.has_value() && !is_illegal_character(second.value()))
            TRY(output.try_append(second.value()));

        return {};
    };

    // NOTE: The RFC says that encoded lines must not be longer than 76 characters.
    //       However, the RFC says implementations can ignore this and parse as is,
    //       which is the approach we're taking.

    while (!lexer.is_eof()) {
        char potential_character = lexer.consume();

        if (is_illegal_character(potential_character))
            continue;

        if (potential_character == '=') {
            if (lexer.is_eof()) {
                TRY(append_invalid_escape_sequence());
                continue;
            }

            char first_escape_character = lexer.consume();

            // The RFC doesn't formally allow lowercase, but says implementations can treat lowercase the same as uppercase.
            // Thus we can use is_ascii_hex_digit.
            if (is_ascii_hex_digit(first_escape_character)) {
                if (lexer.is_eof()) {
                    TRY(append_invalid_escape_sequence(first_escape_character));
                    continue;
                }

                char second_escape_character = lexer.consume();

                if (is_ascii_hex_digit(second_escape_character)) {
                    u8 actual_character = (parse_ascii_hex_digit(first_escape_character) << 4) | parse_ascii_hex_digit(second_escape_character);
                    TRY(output.try_append(actual_character));
                } else {
                    TRY(append_invalid_escape_sequence(first_escape_character, second_escape_character));
                    continue;
                }
            } else if (first_escape_character == '\r') {
                if (lexer.is_eof()) {
                    TRY(append_invalid_escape_sequence(first_escape_character));
                    continue;
                }

                char second_escape_character = lexer.consume();

                if (second_escape_character == '\n') {
                    // This is a soft line break. Don't append anything to the output.
                } else {
                    TRY(append_invalid_escape_sequence(first_escape_character, second_escape_character));
                    continue;
                }
            } else {
                TRY(append_invalid_escape_sequence(first_escape_character));
                continue;
            }
        } else {
            TRY(output.try_append(potential_character));
        }
    }

    return output.to_byte_buffer();
}

}
