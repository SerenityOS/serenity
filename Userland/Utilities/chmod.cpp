/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Kenneth Myhra <kennethmyhra@serenityos.org>
 * Copyright (c) 2021, Xavier Defrang <xavier.defrang@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/DirIterator.h>
#include <LibCore/FilePermissionsMask.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath fattr"));

    StringView mode;
    Vector<StringView> paths;
    bool recursive = false;

    Core::ArgsParser args_parser;
    args_parser.add_option(recursive, "Change file modes recursively", "recursive", 'R');
    args_parser.add_positional_argument(mode, "File mode in octal or symbolic notation", "mode");
    args_parser.add_positional_argument(paths, "Paths to file", "paths");
    args_parser.parse(arguments);

    auto mask = TRY(Core::FilePermissionsMask::parse(mode));

    Function<ErrorOr<void>(StringView const&)> update_path_permissions = [&](StringView const& path) -> ErrorOr<void> {
        auto stat = TRY(Core::System::lstat(path));

        if (S_ISLNK(stat.st_mode)) {
            // Symlinks don't get processed unless they are explicitly listed on the command line.
            if (!paths.contains_slow(path))
                return {};

            // The chmod syscall changes the file that a link points to, so we will have to get
            // the correct mode to base our modifications on.
            stat = TRY(Core::System::stat(path));
        }

        TRY(Core::System::chmod(path, mask.apply(stat.st_mode)));

        if (recursive && S_ISDIR(stat.st_mode)) {
            Core::DirIterator it(path, Core::DirIterator::Flags::SkipParentAndBaseDir);

            while (it.has_next())
                TRY(update_path_permissions(it.next_full_path()));
        }

        return {};
    };

    for (auto const& path : paths) {
        TRY(update_path_permissions(path));
    }

    return 0;
}
