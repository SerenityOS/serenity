/*
 * Copyright (c) 2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibFileSystem/FileSystem.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath"));

    bool no_newline = false;
    Vector<StringView> paths;

    Core::ArgsParser args_parser;
    args_parser.add_option(no_newline, "Do not append a newline", "no-newline", 'n');
    args_parser.add_positional_argument(paths, "Symlink path", "path");
    args_parser.parse(arguments);

    for (auto path : paths) {
        auto destination = TRY(FileSystem::read_link(path));
        out("{}", destination);
        if (!no_newline)
            outln();
    }

    return 0;
}
