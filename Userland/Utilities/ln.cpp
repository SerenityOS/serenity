/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio cpath rpath"));

    bool force = false;
    bool symbolic = false;
    bool verbose = false;
    StringView target;
    StringView path;

    Core::ArgsParser args_parser;
    args_parser.add_option(force, "Force the creation", "force", 'f');
    args_parser.add_option(symbolic, "Create a symlink", "symbolic", 's');
    args_parser.add_option(verbose, "Verbose", "verbose", 'v');
    args_parser.add_positional_argument(target, "Link target", "target");
    args_parser.add_positional_argument(path, "Link path", "path", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    ByteString path_buffer;
    if (path.is_empty()) {
        path_buffer = LexicalPath::basename(target);
        path = path_buffer.view();
    }

    auto stat = Core::System::lstat(path);

    if (stat.is_error() && stat.error().code() != ENOENT)
        return stat.release_error();

    if (!stat.is_error() && S_ISDIR(stat.value().st_mode)) {
        // The target path is a directory, so we presumably want <path>/<filename> as the effective path.
        path_buffer = LexicalPath::join(path, LexicalPath::basename(target)).string();
        path = path_buffer.view();
        stat = Core::System::lstat(path);
    }

    if (force && !stat.is_error()) {
        TRY(Core::System::unlink(path));
    }

    if (symbolic) {
        TRY(Core::System::symlink(target, path));
    } else {
        TRY(Core::System::link(target, path));
    }

    if (verbose)
        outln("'{}' -> '{}'", path, target);

    return 0;
}
