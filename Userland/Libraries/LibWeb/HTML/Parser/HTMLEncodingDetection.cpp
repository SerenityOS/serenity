/*
 * Copyright (c) 2021, Max Wipfli <mail@maxwipfli.ch>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <AK/GenericLexer.h>
#include <AK/StringView.h>
#include <AK/Utf8View.h>
#include <LibTextCodec/Decoder.h>
#include <LibWeb/DOM/Attribute.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/Parser/HTMLEncodingDetection.h>
#include <ctype.h>

namespace Web::HTML {

bool prescan_should_abort(const ByteBuffer& input, const size_t& position)
{
    return position >= input.size() || position >= 1024;
}

bool prescan_is_whitespace_or_slash(const u8& byte)
{
    return byte == '\t' || byte == '\n' || byte == '\f' || byte == '\r' || byte == ' ' || byte == '/';
}

bool prescan_skip_whitespace_and_slashes(const ByteBuffer& input, size_t& position)
{
    while (!prescan_should_abort(input, position) && (input[position] == '\t' || input[position] == '\n' || input[position] == '\f' || input[position] == '\r' || input[position] == ' ' || input[position] == '/'))
        ++position;
    return !prescan_should_abort(input, position);
}

// https://html.spec.whatwg.org/multipage/urls-and-fetching.html#algorithm-for-extracting-a-character-encoding-from-a-meta-element
Optional<String> extract_character_encoding_from_meta_element(String const& string)
{
    // Checking for "charset" is case insensitive, as is getting an encoding.
    // Therefore, stick to lowercase from the start for simplicity.
    auto lowercase_string = string.to_lowercase();
    GenericLexer lexer(lowercase_string);

    for (;;) {
        auto charset_index = lexer.remaining().find("charset");
        if (!charset_index.has_value())
            return {};

        // 7 is the length of "charset".
        lexer.ignore(charset_index.value() + 7);

        lexer.ignore_while([](char c) {
            // FIXME: Not the exact same ASCII whitespace. The spec does not include vertical tab (\v).
            return is_ascii_space(c);
        });

        if (lexer.peek() != '=')
            continue;

        break;
    }

    // Ignore the '='.
    lexer.ignore();

    lexer.ignore_while([](char c) {
        // FIXME: Not the exact same ASCII whitespace. The spec does not include vertical tab (\v).
        return is_ascii_space(c);
    });

    if (lexer.is_eof())
        return {};

    if (lexer.consume_specific('"')) {
        auto matching_double_quote = lexer.remaining().find("\"");
        if (!matching_double_quote.has_value())
            return {};

        auto encoding = lexer.remaining().substring_view(0, matching_double_quote.value());
        return TextCodec::get_standardized_encoding(encoding);
    }

    if (lexer.consume_specific('\'')) {
        auto matching_single_quote = lexer.remaining().find("'");
        if (!matching_single_quote.has_value())
            return {};

        auto encoding = lexer.remaining().substring_view(0, matching_single_quote.value());
        return TextCodec::get_standardized_encoding(encoding);
    }

    auto encoding = lexer.consume_until([](char c) {
        // FIXME: Not the exact same ASCII whitespace. The spec does not include vertical tab (\v).
        return is_ascii_space(c) || c == ';';
    });
    return TextCodec::get_standardized_encoding(encoding);
}

RefPtr<DOM::Attribute> prescan_get_attribute(DOM::Document& document, const ByteBuffer& input, size_t& position)
{
    if (!prescan_skip_whitespace_and_slashes(input, position))
        return {};
    if (input[position] == '>')
        return {};

    StringBuilder attribute_name;
    while (true) {
        if (input[position] == '=' && !attribute_name.is_empty()) {
            ++position;
            goto value;
        } else if (input[position] == '\t' || input[position] == '\n' || input[position] == '\f' || input[position] == '\r' || input[position] == ' ')
            goto spaces;
        else if (input[position] == '/' || input[position] == '>')
            return DOM::Attribute::create(document, attribute_name.to_string(), "");
        else
            attribute_name.append_as_lowercase(input[position]);
        ++position;
        if (prescan_should_abort(input, position))
            return {};
    }

spaces:
    if (!prescan_skip_whitespace_and_slashes(input, position))
        return {};
    if (input[position] != '=')
        return DOM::Attribute::create(document, attribute_name.to_string(), "");
    ++position;

value:
    if (!prescan_skip_whitespace_and_slashes(input, position))
        return {};

    StringBuilder attribute_value;
    if (input[position] == '"' || input[position] == '\'') {
        u8 quote_character = input[position];
        ++position;
        for (; !prescan_should_abort(input, position); ++position) {
            if (input[position] == quote_character)
                return DOM::Attribute::create(document, attribute_name.to_string(), attribute_value.to_string());
            else
                attribute_value.append_as_lowercase(input[position]);
        }
        return {};
    } else if (input[position] == '>')
        return DOM::Attribute::create(document, attribute_name.to_string(), "");
    else
        attribute_value.append_as_lowercase(input[position]);

    ++position;
    if (prescan_should_abort(input, position))
        return {};

    for (; !prescan_should_abort(input, position); ++position) {
        if (input[position] == '\t' || input[position] == '\n' || input[position] == '\f' || input[position] == '\r' || input[position] == ' ' || input[position] == '>')
            return DOM::Attribute::create(document, attribute_name.to_string(), attribute_value.to_string());
        else
            attribute_value.append_as_lowercase(input[position]);
    }
    return {};
}

// https://html.spec.whatwg.org/multipage/parsing.html#prescan-a-byte-stream-to-determine-its-encoding
Optional<String> run_prescan_byte_stream_algorithm(DOM::Document& document, const ByteBuffer& input)
{
    // https://html.spec.whatwg.org/multipage/parsing.html#prescan-a-byte-stream-to-determine-its-encoding

    // Detects '<?x'
    if (!prescan_should_abort(input, 6)) {
        if (input[0] == 0x3C && input[1] == 0x00 && input[2] == 0x3F && input[3] == 0x00 && input[4] == 0x78 && input[5] == 0x00)
            return "utf-16le";
        if (input[0] == 0x00 && input[1] == 0x3C && input[2] == 0x00 && input[4] == 0x3F && input[5] == 0x00 && input[6] == 0x78)
            return "utf-16be";
    }

    for (size_t position = 0; !prescan_should_abort(input, position); ++position) {
        if (!prescan_should_abort(input, position + 5) && input[position] == '<' && input[position + 1] == '!'
            && input[position + 2] == '-' && input[position + 3] == '-') {
            position += 2;
            for (; !prescan_should_abort(input, position + 3); ++position) {
                if (input[position] == '-' && input[position + 1] == '-' && input[position + 2] == '>') {
                    position += 2;
                    break;
                }
            }
        } else if (!prescan_should_abort(input, position + 6)
            && input[position] == '<'
            && (input[position + 1] == 'M' || input[position + 1] == 'm')
            && (input[position + 2] == 'E' || input[position + 2] == 'e')
            && (input[position + 3] == 'T' || input[position + 3] == 't')
            && (input[position + 4] == 'A' || input[position + 4] == 'a')
            && prescan_is_whitespace_or_slash(input[position + 5])) {
            position += 6;
            Vector<String> attribute_list {};
            bool got_pragma = false;
            Optional<bool> need_pragma {};
            Optional<String> charset {};

            while (true) {
                auto attribute = prescan_get_attribute(document, input, position);
                if (!attribute)
                    break;
                if (attribute_list.contains_slow(attribute->name()))
                    continue;
                auto& attribute_name = attribute->name();
                attribute_list.append(attribute->name());

                if (attribute_name == "http-equiv") {
                    got_pragma = attribute->value() == "content-type";
                } else if (attribute_name == "content") {
                    auto encoding = extract_character_encoding_from_meta_element(attribute->value());
                    if (encoding.has_value() && !charset.has_value()) {
                        charset = encoding.value();
                        need_pragma = true;
                    }
                } else if (attribute_name == "charset") {
                    auto maybe_charset = TextCodec::get_standardized_encoding(attribute->value());
                    if (maybe_charset.has_value()) {
                        charset = Optional<String> { maybe_charset };
                        need_pragma = { false };
                    }
                }
            }

            if (!need_pragma.has_value() || (need_pragma.value() && !got_pragma) || !charset.has_value())
                continue;
            if (charset.value() == "UTF-16BE/LE")
                return "UTF-8";
            else if (charset.value() == "x-user-defined")
                return "windows-1252";
            else
                return charset.value();
        } else if (!prescan_should_abort(input, position + 3) && input[position] == '<'
            && ((input[position + 1] == '/' && isalpha(input[position + 2])) || isalpha(input[position + 1]))) {
            position += 2;
            prescan_skip_whitespace_and_slashes(input, position);
            while (prescan_get_attribute(document, input, position)) { };
        } else if (!prescan_should_abort(input, position + 1) && input[position] == '<' && (input[position + 1] == '!' || input[position + 1] == '/' || input[position + 1] == '?')) {
            position += 2;
            while (input[position] != '>') {
                ++position;
                if (prescan_should_abort(input, position))
                    return {};
            }
        } else {
            // Do nothing.
        }
    }
    return {};
}

// https://html.spec.whatwg.org/multipage/parsing.html#determining-the-character-encoding
String run_encoding_sniffing_algorithm(DOM::Document& document, const ByteBuffer& input)
{
    if (input.size() >= 2) {
        if (input[0] == 0xFE && input[1] == 0xFF) {
            return "UTF-16BE";
        } else if (input[0] == 0xFF && input[1] == 0xFE) {
            return "UTF-16LE";
        } else if (input.size() >= 3 && input[0] == 0xEF && input[1] == 0xBB && input[2] == 0xBF) {
            return "UTF-8";
        }
    }

    // FIXME: If the user has explicitly instructed the user agent to override the document's character
    //        encoding with a specific encoding.
    // FIXME: The user agent may wait for more bytes of the resource to be available, either in this step or
    //        at any later step in this algorithm.
    // FIXME: If the transport layer specifies a character encoding, and it is supported.

    auto optional_encoding = run_prescan_byte_stream_algorithm(document, input);
    if (optional_encoding.has_value()) {
        return optional_encoding.value();
    }

    // FIXME: If the HTML parser for which this algorithm is being run is associated with a Document whose browsing context
    //        is non-null and a child browsing context.
    // FIXME: If the user agent has information on the likely encoding for this page, e.g. based on the encoding of the page
    //        when it was last visited.

    if (!Utf8View(StringView(input)).validate()) {
        // FIXME: As soon as Locale is supported, this should sometimes return a different encoding based on the locale.
        return "windows-1252";
    }

    // NOTE: This is the authoritative place to actually decide on using the default encoding as per the HTML specification.
    //       "Otherwise, return an implementation-defined or user-specified default character encoding, [...]."
    return "UTF-8";
}

}
