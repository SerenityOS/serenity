/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Optional.h>
#include <AK/String.h>
#include <LibCore/ArgsParser.h>
#include <stdio.h>

int main(int argc, char** argv)
{
    bool complement_flag = false;
    bool delete_flag = false;
    const char* from_chars = nullptr;
    const char* to_chars = nullptr;

    Core::ArgsParser args_parser;
    args_parser.add_option(complement_flag, "Take the complement of the first set", "complement", 'c');
    args_parser.add_option(delete_flag, "Delete characters instead of replacing", nullptr, 'd');
    args_parser.add_positional_argument(from_chars, "Set of characters to translate from", "from");
    args_parser.add_positional_argument(to_chars, "Set of characters to translate to", "to", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    if (!to_chars && !delete_flag) {
        args_parser.print_usage(stderr, argv[0]);
        return 1;
    }

    String from_complement;
    StringView from_str;
    if (complement_flag) {
        auto original_set = StringView(from_chars);
        StringBuilder complement_set;
        for (int i = 0; i < 256; i++) {
            if (!original_set.contains(i))
                complement_set.append(static_cast<int>(i));
        }
        from_complement = complement_set.to_string();
        from_str = from_complement;
    } else {
        from_str = from_chars;
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
        auto to_str = AK::StringView(to_chars);

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
