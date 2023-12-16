/*
 * Copyright (c) 2023, Valtteri Koskivuori <vkoskiv@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "QuotedPrintable.h"
#include <AK/Base64.h>
#include <AK/GenericLexer.h>
#include <AK/StringBuilder.h>
#include <LibIMAP/MessageHeaderEncoding.h>
#include <LibTextCodec/Decoder.h>

namespace IMAP {

ErrorOr<ByteBuffer> decode_rfc2047_encoded_words(StringView input)
{
    GenericLexer lexer(input);
    StringBuilder output;

    while (!lexer.is_eof()) {
        auto ascii_view = lexer.consume_until("=?"sv);
        ByteString ascii = ascii_view.replace("\r"sv, " "sv, ReplaceMode::All);
        ascii = ascii.replace("\n"sv, " "sv, ReplaceMode::All);
        TRY(output.try_append(ascii));
        if (lexer.is_eof())
            break;
        lexer.consume_specific("=?"sv);
        auto charset = lexer.consume_until('?');
        lexer.consume();
        auto encoding = lexer.consume_until('?');
        lexer.consume();
        auto encoded_text = lexer.consume_until("?=");
        lexer.consume_specific("?="sv);

        // RFC 2047 Section 6.2, "...any 'linear-white-space' that separates a pair of adjacent 'encoded-word's is ignored."
        // https://datatracker.ietf.org/doc/html/rfc2047#section-6.2
        bool found_next_start = false;
        int spaces = 0;
        for (size_t i = 0; i < lexer.tell_remaining(); ++i) {
            if (lexer.peek(i) == ' ' || lexer.peek(i) == '\r' || lexer.peek(i) == '\n') {
                spaces++;
                if (lexer.peek(i + 1) == '=' && lexer.peek(i + 2) == '?') {
                    found_next_start = true;
                    break;
                }
            } else {
                break;
            }
        }
        if (found_next_start) {
            for (int i = 0; i < spaces; i++) {
                lexer.consume();
            }
        }

        ByteBuffer first_pass_decoded;
        if (encoding == 'Q' || encoding == 'q') {
            auto maybe_decoded_data = decode_quoted_printable(encoded_text);
            if (maybe_decoded_data.is_error()) {
                dbgln("Failed to decode quoted-printable rfc2047 text, skipping.");
                continue;
            }
            // RFC 2047 Section 4.2.2, https://datatracker.ietf.org/doc/html/rfc2047#section-4.2
            auto decoded_data = maybe_decoded_data.release_value();
            for (auto character : decoded_data.bytes()) {
                if (character == '_')
                    first_pass_decoded.append(' ');
                else
                    first_pass_decoded.append(character);
            }
        } else if (encoding == 'B' || encoding == 'b') {
            auto maybe_decoded_data = AK::decode_base64(encoded_text);
            if (maybe_decoded_data.is_error()) {
                dbgln("Failed to decode base64-encoded rfc2047 text, skipping.");
                continue;
            }
            first_pass_decoded = maybe_decoded_data.release_value();
        } else {
            dbgln("Unknown encoding \"{}\" found, skipping, original string: \"{}\"", encoding, input);
            continue;
        }
        if (first_pass_decoded.is_empty())
            continue;
        auto maybe_decoder = TextCodec::decoder_for(charset);
        if (!maybe_decoder.has_value()) {
            dbgln("No decoder found for charset \"{}\", skipping.", charset);
            continue;
        }
        auto decoded_text = TRY(maybe_decoder->to_utf8(first_pass_decoded));
        TRY(output.try_append(decoded_text));
    }

    return output.to_byte_buffer();
}

}
