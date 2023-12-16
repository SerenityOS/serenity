/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <AK/GenericLexer.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <stdio.h>
#include <unistd.h>

static u8 parse_octal_number(GenericLexer& lexer)
{
    u32 value = 0;
    for (size_t count = 0; count < 3; ++count) {
        auto c = lexer.peek();
        if (!(c >= '0' && c <= '7'))
            break;
        value = value * 8 + (c - '0');
        lexer.consume();
    }
    clamp(value, 0, 255);
    return value;
}

static Optional<u8> parse_hex_number(GenericLexer& lexer)
{
    u8 value = 0;
    for (size_t count = 0; count < 2; ++count) {
        auto c = lexer.peek();
        if (!is_ascii_hex_digit(c))
            return {};
        value = value * 16 + parse_ascii_hex_digit(c);
        lexer.consume();
    }
    return value;
}

static ByteString interpret_backslash_escapes(StringView string, bool& no_trailing_newline)
{
    static constexpr auto escape_map = "a\ab\be\ef\fn\nr\rt\tv\v"sv;
    static constexpr auto unescaped_chars = "\a\b\e\f\n\r\t\v\\"sv;

    StringBuilder builder;
    GenericLexer lexer { string };

    while (!lexer.is_eof()) {
        auto this_index = lexer.tell();
        auto this_char = lexer.consume();
        if (this_char == '\\') {
            if (lexer.is_eof()) {
                builder.append('\\');
                break;
            }
            auto next_char = lexer.peek();
            if (next_char == 'c') {
                no_trailing_newline = true;
                break;
            }
            if (next_char == '0') {
                lexer.consume();
                auto octal_number = parse_octal_number(lexer);
                builder.append(octal_number);
            } else if (next_char == 'x') {
                lexer.consume();
                auto maybe_hex_number = parse_hex_number(lexer);
                if (!maybe_hex_number.has_value()) {
                    auto bad_substring = string.substring_view(this_index, lexer.tell() - this_index);
                    builder.append(bad_substring);
                } else {
                    builder.append(maybe_hex_number.release_value());
                }
            } else if (next_char == 'u') {
                lexer.retreat();
                auto maybe_code_point = lexer.consume_escaped_code_point();
                if (maybe_code_point.is_error()) {
                    auto bad_substring = string.substring_view(this_index, lexer.tell() - this_index);
                    builder.append(bad_substring);
                } else {
                    builder.append_code_point(maybe_code_point.release_value());
                }
            } else {
                lexer.retreat();
                auto consumed_char = lexer.consume_escaped_character('\\', escape_map);
                if (!unescaped_chars.contains(consumed_char))
                    builder.append('\\');
                builder.append(consumed_char);
            }
        } else {
            builder.append(this_char);
        }
    }

    return builder.to_byte_string();
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio"));

    Vector<ByteString> text;
    bool no_trailing_newline = false;
    bool should_interpret_backslash_escapes = false;

    Core::ArgsParser args_parser;
    args_parser.add_option(no_trailing_newline, "Do not output a trailing newline", nullptr, 'n');
    args_parser.add_option(should_interpret_backslash_escapes, "Interpret backslash escapes", nullptr, 'e');
    args_parser.add_positional_argument(text, "Text to print out", "text", Core::ArgsParser::Required::No);
    args_parser.set_stop_on_first_non_option(true);
    args_parser.parse(arguments);

    if (text.is_empty()) {
        if (!no_trailing_newline)
            outln();
        return 0;
    }

    auto output = ByteString::join(' ', text);
    if (should_interpret_backslash_escapes)
        output = interpret_backslash_escapes(output, no_trailing_newline);
    out("{}", output);
    if (!no_trailing_newline)
        outln();
    return 0;
}
