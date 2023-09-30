/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Tim Ledbetter <timledbetter@gmail.com>
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

    bool quiet { false };
    Vector<StringView> paths;

    Core::ArgsParser args_parser;
    args_parser.set_general_help(
        "Show the 'real' path of a file, by resolving all symbolic links along the way.");
    args_parser.add_option(quiet, "Suppress error messages", "quiet", 'q');
    args_parser.add_positional_argument(paths, "Path to resolve", "paths");
    args_parser.parse(arguments);

    auto has_errors = false;
    for (auto path : paths) {
        auto resolved_path_or_error = FileSystem::real_path(path);
        if (resolved_path_or_error.is_error()) {
            if (!quiet)
                warnln("realpath: {}: {}", path, strerror(resolved_path_or_error.error().code()));

            has_errors = true;
            continue;
        }

        outln("{}", resolved_path_or_error.release_value());
    }

    return has_errors ? 1 : 0;
}
