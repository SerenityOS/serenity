/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio"));

    StringView string = "y"sv;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Repeatedly output a line with the specified string, or 'y'.");
    args_parser.add_positional_argument(string, "String to output (default 'y')", "string", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    for (;;)
        outln("{}", string);
}
