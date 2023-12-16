/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Tim Ledbetter <timledbetter@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio cpath"));

    bool remove_parents = false;
    bool verbose = false;
    Vector<StringView> paths;

    Core::ArgsParser args_parser;
    args_parser.add_option(remove_parents, "Remove all directories in each given path", "parents", 'p');
    args_parser.add_option(verbose, "List each directory as it is removed", "verbose", 'v');
    args_parser.add_positional_argument(paths, "Directories to remove", "paths");
    args_parser.parse(arguments);

    int status = 0;

    auto remove_directory = [&](StringView path) {
        if (verbose)
            outln("rmdir: removing directory '{}'", path);

        auto maybe_error = Core::System::rmdir(path);
        if (maybe_error.is_error()) {
            warnln("Failed to remove '{}': {}", path, maybe_error.release_error());
            status = 1;
        }

        return !maybe_error.is_error();
    };

    for (auto path : paths) {
        if (!remove_directory(path) || !remove_parents)
            continue;

        LexicalPath lexical_path(path);
        auto const& path_parts = lexical_path.parts_view();
        if (path_parts.size() < 2)
            continue;

        for (size_t i = path_parts.size() - 1; i > 0; --i) {
            auto current_path_parts = path_parts.span().slice(0, i);
            LexicalPath current_path { ByteString::join('/', current_path_parts) };
            if (!remove_directory(current_path.string()))
                break;
        }
    }
    return status;
}
