/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <stdio.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    if (pledge("stdio rpath wpath cpath fattr", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    bool link = false;
    bool recursion_allowed = false;
    bool verbose = false;
    Vector<const char*> sources;
    const char* destination = nullptr;

    Core::ArgsParser args_parser;
    args_parser.add_option(link, "Link files instead of copying", "link", 'l');
    args_parser.add_option(recursion_allowed, "Copy directories recursively", "recursive", 'R');
    args_parser.add_option(recursion_allowed, "Same as -R", nullptr, 'r');
    args_parser.add_option(verbose, "Verbose", "verbose", 'v');
    args_parser.add_positional_argument(sources, "Source file path", "source");
    args_parser.add_positional_argument(destination, "Destination file path", "destination");
    args_parser.parse(argc, argv);

    for (auto& source : sources) {
        auto result = Core::File::copy_file_or_directory(
            destination, source,
            recursion_allowed ? Core::File::RecursionMode::Allowed : Core::File::RecursionMode::Disallowed,
            link ? Core::File::LinkMode::Allowed : Core::File::LinkMode::Disallowed,
            Core::File::AddDuplicateFileMarker::No);

        if (result.is_error()) {
            if (result.error().tried_recursing)
                warnln("cp: -R not specified; omitting directory '{}'", source);
            else
                warnln("cp: unable to copy '{}': {}", source, result.error().error_code);
            return 1;
        }

        if (verbose)
            outln("'{}' -> '{}'", source, destination);
    }
    return 0;
}
