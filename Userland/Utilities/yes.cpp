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
    using enum Kernel::Pledge;
    TRY((Core::System::Promise<stdio>::pledge()));

    const char* string = "yes";

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(string, "String to output (defaults to 'yes')", "string", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    for (;;)
        puts(string);
}
