/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <YAK/LexicalPath.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <stdio.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    if (pledge("stdio rpath wpath cpath fattr chown", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    bool link = false;
    bool preserve = false;
    bool recursion_allowed = false;
    bool verbose = false;
    Vector<String> sources;
    String destination;

    Core::ArgsParser args_parser;
    args_parser.add_option(link, "Link files instead of copying", "link", 'l');
    args_parser.add_option(preserve, "Preserve time, UID/GID and file permissions", nullptr, 'p');
    args_parser.add_option(recursion_allowed, "Copy directories recursively", "recursive", 'R');
    args_parser.add_option(recursion_allowed, "Same as -R", nullptr, 'r');
    args_parser.add_option(verbose, "Verbose", "verbose", 'v');
    args_parser.add_positional_argument(sources, "Source file paths", "source");
    args_parser.add_positional_argument(destination, "Destination file path", "destination");
    args_parser.parse(argc, argv);

    if (preserve) {
        umask(0);
    } else {
        if (pledge("stdio rpath wpath cpath fattr", nullptr) < 0) {
            perror("pledge");
            return 1;
        }
    }

    bool destination_is_existing_dir = Core::File::is_directory(destination);

    for (auto& source : sources) {
        auto destination_path = destination_is_existing_dir
            ? String::formatted("{}/{}", destination, LexicalPath::basename(source))
            : destination;

        auto result = Core::File::copy_file_or_directory(
            destination_path, source,
            recursion_allowed ? Core::File::RecursionMode::Allowed : Core::File::RecursionMode::Disallowed,
            link ? Core::File::LinkMode::Allowed : Core::File::LinkMode::Disallowed,
            Core::File::AddDuplicateFileMarker::No,
            preserve ? Core::File::PreserveMode::PermissionsOwnershipTimestamps : Core::File::PreserveMode::Nothing);

        if (result.is_error()) {
            if (result.error().tried_recursing)
                warnln("cp: -R not specified; omitting directory '{}'", source);
            else
                warnln("cp: unable to copy '{}' to '{}': {}", source, destination_path, result.error().error_code);
            return 1;
        }

        if (verbose)
            outln("'{}' -> '{}'", source, destination_path);
    }
    return 0;
}
