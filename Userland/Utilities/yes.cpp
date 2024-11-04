/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <stdio.h>
#include <unistd.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio"));

    Vector<StringView> strings;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Repeatedly output a line with all specified strings separated by spaces. If none are specified, output 'yes'.");
    args_parser.add_positional_argument(strings, "String to output (default 'yes')", "string", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    if (strings.is_empty())
        strings.append("yes"sv);

    for (;;) {
        for (size_t i = 0; i < strings.size(); i += 1) {
            out("{}", strings[i]);
            if (i < strings.size() - 1)
                putchar(' ');
        }
        outln();
    }
}
