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
    args_parser.add_positional_argument(from_chars, "Character to translate from", "from");
    args_parser.add_positional_argument(to_chars, "Character to translate to", "to", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    if (!to_chars && !delete_flag) {
        args_parser.print_usage(stderr, argv[0]);
        return 1;
    }

    if (delete_flag) {
        auto from_str = AK::StringView(from_chars);

        for (;;) {
            char ch = fgetc(stdin);
            if (feof(stdin))
                break;
            if (!from_str.contains(ch))
                putchar(ch);
        }
    } else {
        // TODO: Support multiple characters to translate from and to
        auto from = from_chars[0];
        auto to = to_chars[0];

        for (;;) {
            char ch = fgetc(stdin);
            if (feof(stdin))
                break;
            if (ch == from)
                putchar(to);
            else
                putchar(ch);
        }
    }

    return 0;
}
