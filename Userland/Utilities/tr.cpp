/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <LibCore/ArgsParser.h>
#include <stdio.h>

int main(int argc, char** argv)
{
    bool delete_flag = false;
    const char* from_chars = nullptr;
    const char* to_chars = nullptr;

    Core::ArgsParser args_parser;
    args_parser.add_option(delete_flag, "Delete characters instead of replacing", nullptr, 'd');
    args_parser.add_positional_argument(from_chars, "Set of characters to translate from", "from");
    args_parser.add_positional_argument(to_chars, "Set of characters to translate to", "to", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    if (!to_chars && !delete_flag) {
        args_parser.print_usage(stderr, argv[0]);
        return 1;
    }

    auto from_str = AK::StringView(from_chars);

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
