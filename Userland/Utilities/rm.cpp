/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringBuilder.h>
#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibFileSystem/FileSystem.h>
#include <LibMain/Main.h>
#include <stdio.h>
#include <unistd.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath cpath"));

    bool recursive = false;
    bool force = false;
    bool verbose = false;
    bool no_preserve_root = false;
    Vector<StringView> paths;

    Core::ArgsParser args_parser;
    args_parser.add_option(recursive, "Delete directories recursively", "recursive", 'r');
    args_parser.add_option(force, "Ignore nonexistent files", "force", 'f');
    args_parser.add_option(verbose, "Verbose", "verbose", 'v');
    args_parser.add_option(no_preserve_root, "Do not consider '/' specially", "no-preserve-root");
    args_parser.add_positional_argument(paths, "Path(s) to remove", "path", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    if (!force && paths.is_empty()) {
        args_parser.print_usage(stderr, arguments.strings[0]);
        return 1;
    }

    bool had_errors = false;
    for (auto& path : paths) {
        if (!no_preserve_root && path == "/") {
            warnln("rm: '/' is protected, try with --no-preserve-root to override this behavior");
            continue;
        }

        auto result = FileSystem::remove(path, recursive ? FileSystem::RecursionMode::Allowed : FileSystem::RecursionMode::Disallowed);

        if (result.is_error()) {
            auto error = result.release_error();

            if (force && error.is_errno() && error.code() == ENOENT)
                continue;

            warnln("rm: cannot remove '{}': {}", path, error);
            had_errors = true;
        }

        if (verbose)
            outln("removed '{}'", path);
    }
    return had_errors ? 1 : 0;
}
