/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <stdio.h>
#include <unistd.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath"));

    char const* filename = nullptr;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(filename, "Name of executable", "executable");
    args_parser.parse(arguments);

    auto fullpath = Core::File::resolve_executable_from_environment({ filename, strlen(filename) });
    if (!fullpath.has_value()) {
        warnln("no '{}' in path", filename);
        return 1;
    }

    outln("{}", fullpath.release_value());
    return 0;
}
