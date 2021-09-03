/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <YAK/GenericLexer.h>
#include <YAK/Optional.h>
#include <YAK/String.h>
#include <LibCore/ArgsParser.h>
#include <ctype.h>
#include <stdio.h>

static bool is_octal(int c)
{
    return c == '0' || c == '1' || c == '2' || c == '3' || c == '4' || c == '5' || c == '6' || c == '7';
}

static void generate_character_class(Function<int(int)> oracle, StringBuilder& out)
{
    for (int i = 0; i < 128; i++) {
        if (oracle(i))
            out.append(static_cast<char>(i));
    }
}

static String build_set(StringView specification)
{
    StringBuilder out;
    GenericLexer lexer(specification);

    while (!lexer.is_eof()) {
        if (lexer.consume_specific("[:alnum:]"sv))
            generate_character_class(isalnum, out);
        else if (lexer.consume_specific("[:blank:]"sv))
            generate_character_class(isblank, out);
        else if (lexer.consume_specific("[:digit:]"sv))
            generate_character_class(isdigit, out);
        else if (lexer.consume_specific("[:lower:]"sv))
            generate_character_class(islower, out);
        else if (lexer.consume_specific("[:punct:]"sv))
            generate_character_class(ispunct, out);
        else if (lexer.consume_specific("[:upper:]"sv))
            generate_character_class(isupper, out);
        else if (lexer.consume_specific("[:alpha:]"sv))
            generate_character_class(isalpha, out);
        else if (lexer.consume_specific("[:cntrl:]"sv))
            generate_character_class(iscntrl, out);
        else if (lexer.consume_specific("[:graph:]"sv))
            generate_character_class(isgraph, out);
        else if (lexer.consume_specific("[:print:]"sv))
            generate_character_class(isprint, out);
        else if (lexer.consume_specific("[:space:]"sv))
            generate_character_class(isspace, out);
        else if (lexer.consume_specific("[:xdigit:]"sv))
            generate_character_class(isxdigit, out);
        else if (lexer.consume_specific("\\\\"sv))
            out.append('\\');
        else if (lexer.consume_specific("\\a"sv))
            out.append('\a');
        else if (lexer.consume_specific("\\b"sv))
            out.append('\b');
        else if (lexer.consume_specific("\\f"sv))
            out.append('\f');
        else if (lexer.consume_specific("\\n"sv))
            out.append('\n');
        else if (lexer.consume_specific("\\r"sv))
            out.append('\r');
        else if (lexer.consume_specific("\\t"sv))
            out.append('\t');
        else if (lexer.consume_specific("\\v"sv))
            out.append('\v');
        else if (lexer.next_is('\\') && is_octal(lexer.peek(1))) {
            lexer.consume_specific('\\');
            int max_left_over = 3;
            auto octal_digits = lexer.consume_while([&](char i) -> bool {
                return is_octal(i) && max_left_over--;
            });

            int value = 0;
            for (char ch : octal_digits)
                value = value * 8 + (ch - '0');
            out.append(static_cast<char>(value));
        } else
            out.append(lexer.consume(1));
    }

    return out.to_string();
}

int main(int argc, char** argv)
{
    bool complement_flag = false;
    bool delete_flag = false;
    const char* from_chars = nullptr;
    const char* to_chars = nullptr;

    Core::ArgsParser args_parser;
    args_parser.add_option(complement_flag, "Take the complement of the first set", "complement", 'c');
    args_parser.add_option(delete_flag, "Delete characters instead of replacing", "delete", 'd');
    args_parser.add_positional_argument(from_chars, "Set of characters to translate from", "from");
    args_parser.add_positional_argument(to_chars, "Set of characters to translate to", "to", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    if (!to_chars && !delete_flag) {
        args_parser.print_usage(stderr, argv[0]);
        return 1;
    }

    String from_str = build_set(from_chars);
    if (complement_flag) {
        StringBuilder complement_set;
        for (int ch = 0; ch < 256; ch++) {
            if (!from_str.contains(static_cast<char>(ch)))
                complement_set.append(static_cast<char>(ch));
        }
        from_str = complement_set.to_string();
    }

    if (delete_flag) {
        for (;;) {
            char ch = fgetc(stdin);
            if (feof(stdin))
                break;
            if (!from_str.contains(ch))
                putchar(ch);
        }
    } else {
        auto to_str = build_set(to_chars);

        for (;;) {
            char ch = fgetc(stdin);
            if (feof(stdin))
                break;
            auto match = from_str.find_last(ch);
            if (match.has_value())
                putchar(to_str[min(match.value(), to_str.length() - 1)]);
            else
                putchar(ch);
        }
    }

    return 0;
}
