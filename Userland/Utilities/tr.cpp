/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteString.h>
#include <AK/CharacterTypes.h>
#include <AK/GenericLexer.h>
#include <AK/Optional.h>
#include <LibCore/ArgsParser.h>
#include <LibMain/Main.h>
#include <ctype.h>
#include <stdio.h>

static bool is_octal(int c)
{
    return c == '0' || c == '1' || c == '2' || c == '3' || c == '4' || c == '5' || c == '6' || c == '7';
}

static ErrorOr<void> generate_character_class(Function<int(int)> oracle, StringBuilder& out)
{
    for (int i = 0; i < 128; i++) {
        if (oracle(i))
            TRY(out.try_append(static_cast<char>(i)));
    }

    return {};
}

static ErrorOr<ByteString> build_set(StringView specification)
{
    StringBuilder out;
    GenericLexer lexer(specification);

    while (!lexer.is_eof()) {
        if (lexer.consume_specific("[:alnum:]"sv))
            TRY(generate_character_class(is_ascii_alphanumeric, out));
        else if (lexer.consume_specific("[:blank:]"sv))
            TRY(generate_character_class(is_ascii_blank, out));
        else if (lexer.consume_specific("[:digit:]"sv))
            TRY(generate_character_class(is_ascii_digit, out));
        else if (lexer.consume_specific("[:lower:]"sv))
            TRY(generate_character_class(is_ascii_lower_alpha, out));
        else if (lexer.consume_specific("[:punct:]"sv))
            TRY(generate_character_class(is_ascii_punctuation, out));
        else if (lexer.consume_specific("[:upper:]"sv))
            TRY(generate_character_class(is_ascii_upper_alpha, out));
        else if (lexer.consume_specific("[:alpha:]"sv))
            TRY(generate_character_class(is_ascii_alpha, out));
        else if (lexer.consume_specific("[:cntrl:]"sv))
            TRY(generate_character_class(is_ascii_control, out));
        else if (lexer.consume_specific("[:graph:]"sv))
            TRY(generate_character_class(is_ascii_graphical, out));
        else if (lexer.consume_specific("[:print:]"sv))
            TRY(generate_character_class(is_ascii_printable, out));
        else if (lexer.consume_specific("[:space:]"sv))
            TRY(generate_character_class(is_ascii_space, out));
        else if (lexer.consume_specific("[:xdigit:]"sv))
            TRY(generate_character_class(is_ascii_hex_digit, out));
        else if (lexer.consume_specific("\\\\"sv))
            TRY(out.try_append('\\'));
        else if (lexer.consume_specific("\\a"sv))
            TRY(out.try_append('\a'));
        else if (lexer.consume_specific("\\b"sv))
            TRY(out.try_append('\b'));
        else if (lexer.consume_specific("\\f"sv))
            TRY(out.try_append('\f'));
        else if (lexer.consume_specific("\\n"sv))
            TRY(out.try_append('\n'));
        else if (lexer.consume_specific("\\r"sv))
            TRY(out.try_append('\r'));
        else if (lexer.consume_specific("\\t"sv))
            TRY(out.try_append('\t'));
        else if (lexer.consume_specific("\\v"sv))
            TRY(out.try_append('\v'));
        else if (lexer.next_is('\\') && is_octal(lexer.peek(1))) {
            lexer.consume_specific('\\');
            int max_left_over = 3;
            auto octal_digits = lexer.consume_while([&](char i) -> bool {
                return is_octal(i) && max_left_over--;
            });

            int value = 0;
            for (char ch : octal_digits)
                value = value * 8 + (ch - '0');
            TRY(out.try_append(static_cast<char>(value)));
        } else
            TRY(out.try_append(lexer.consume(1)));
    }

    return out.to_byte_string();
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    bool complement_flag = false;
    bool delete_flag = false;
    bool squeeze_flag = false;
    StringView from_chars;
    StringView to_chars;

    Core::ArgsParser args_parser;
    args_parser.add_option(complement_flag, "Take the complement of the first set", "complement", 'c');
    args_parser.add_option(delete_flag, "Delete characters instead of replacing", "delete", 'd');
    args_parser.add_option(squeeze_flag, "Omit repeated characters listed in the last given set from the output", "squeeze-repeats", 's');
    args_parser.add_positional_argument(from_chars, "Set of characters to translate from", "from");
    args_parser.add_positional_argument(to_chars, "Set of characters to translate to", "to", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    bool transform_flag = !to_chars.is_empty() && !delete_flag;

    if (!transform_flag && !delete_flag && !squeeze_flag) {
        warnln("tr: Missing operand");
        args_parser.print_usage(stderr, arguments.strings[0]);
        return 1;
    }

    if (delete_flag && squeeze_flag && to_chars.is_empty()) {
        warnln("tr: Combined delete and squeeze operations need two sets of characters");
        args_parser.print_usage(stderr, arguments.strings[0]);
        return 1;
    }

    if (delete_flag && !squeeze_flag && !to_chars.is_empty()) {
        warnln("tr: Only one set of characters may be given when deleting without squeezing");
        args_parser.print_usage(stderr, arguments.strings[0]);
        return 1;
    }

    auto from_str = TRY(build_set(from_chars));
    if (complement_flag) {
        StringBuilder complement_set;
        for (int ch = 0; ch < 256; ch++) {
            if (!from_str.contains(static_cast<char>(ch)))
                TRY(complement_set.try_append(static_cast<char>(ch)));
        }
        from_str = complement_set.to_byte_string();
    }

    auto to_str = TRY(build_set(to_chars));
    auto squeeze_string = TRY(build_set(!to_chars.is_empty() ? to_chars : from_chars));
    Optional<char> last_char;

    for (;;) {
        char ch = fgetc(stdin);
        if (feof(stdin))
            break;

        if (delete_flag) {
            if (from_str.contains(ch))
                continue;
        }

        if (transform_flag) {
            auto match = from_str.find_last(ch);
            if (match.has_value())
                ch = to_str[min(match.value(), to_str.length() - 1)];
        }

        if (squeeze_flag && last_char.has_value() && last_char.value() == ch && squeeze_string.contains(ch))
            continue;

        last_char = ch;
        putchar(ch);
    }

    return 0;
}
