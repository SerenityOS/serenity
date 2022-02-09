/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <stdio.h>
#include <unistd.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath"));

    const char* path;

    Core::ArgsParser args_parser;
    args_parser.set_general_help(
        "Show the 'real' path of a file, by resolving all symbolic links along the way.");
    args_parser.add_positional_argument(path, "Path to resolve", "path");
    args_parser.parse(arguments);

    char* value = realpath(path, nullptr);
    if (value == nullptr) {
        perror("realpath");
        return 1;
    }
    outln("{}", value);
    free(value);
    return 0;
}
