/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringBuilder.h>
#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <stdio.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    if (pledge("stdio rpath cpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    bool recursive = false;
    bool force = false;
    bool verbose = false;
    Vector<const char*> paths;

    Core::ArgsParser args_parser;
    args_parser.add_option(recursive, "Delete directories recursively", "recursive", 'r');
    args_parser.add_option(force, "Force", "force", 'f');
    args_parser.add_option(verbose, "Verbose", "verbose", 'v');
    args_parser.add_positional_argument(paths, "Path(s) to remove", "path", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    if (!force && paths.is_empty()) {
        args_parser.print_usage(stderr, argv[0]);
        return 1;
    }

    bool had_errors = false;
    for (auto& path : paths) {
        auto result = Core::File::remove(path, recursive ? Core::File::RecursionMode::Allowed : Core::File::RecursionMode::Disallowed, force);

        if (result.is_error()) {
            warnln("rm: cannot remove '{}': {}", path, static_cast<Error const&>(result.error()));
            had_errors = true;
        }

        if (verbose)
            outln("removed '{}'", path);
    }
    return had_errors ? 1 : 0;
}
