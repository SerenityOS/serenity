/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/DirIterator.h>
#include <LibCore/System.h>
#include <stdio.h>
#include <unistd.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath"));

    const char* filename = nullptr;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(filename, "Name of executable", "executable");
    args_parser.parse(arguments);

    auto fullpath = Core::find_executable_in_path(filename);
    if (fullpath.is_empty()) {
        warnln("no '{}' in path", filename);
        return 1;
    }

    outln("{}", fullpath);
    return 0;
}
