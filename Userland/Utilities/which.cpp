/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <stdio.h>
#include <unistd.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath"));

    StringView filename;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(filename, "Name of executable", "executable");
    args_parser.parse(arguments);

    auto fullpath = Core::System::resolve_executable_from_environment(filename);
    if (fullpath.is_error()) {
        warnln("no '{}' in path", filename);
        return 1;
    }

    outln("{}", fullpath.release_value());
    return 0;
}
