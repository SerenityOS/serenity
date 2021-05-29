/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <stdio.h>
#include <stdlib.h>

int main(int argc, char** argv)
{
    const char* from_chars = nullptr;
    const char* to_chars = nullptr;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(from_chars, "Character to translate from", "from");
    args_parser.add_positional_argument(to_chars, "Character to translate to", "to");
    args_parser.parse(argc, argv);

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

    return 0;
}
