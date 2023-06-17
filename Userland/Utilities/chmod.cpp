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

    Function<bool(StringView)> update_path_permissions = [&](StringView const& path) {
        auto stat_or_error = Core::System::lstat(path);
        if (stat_or_error.is_error()) {
            warnln("Could not stat '{}': {}", path, stat_or_error.release_error());
            return false;
        }

        auto stat = stat_or_error.release_value();
        if (S_ISLNK(stat.st_mode)) {
            // Symlinks don't get processed unless they are explicitly listed on the command line.
            if (!paths.contains_slow(path))
                return false;

            // The chmod syscall changes the file that a link points to, so we will have to get
            // the correct mode to base our modifications on.
            stat_or_error = Core::System::stat(path);
            if (stat_or_error.is_error()) {
                warnln("Could not stat '{}': {}", path, stat_or_error.release_error());
                return false;
            }

            stat = stat_or_error.release_value();
        }

        auto success = true;
        auto maybe_error = Core::System::chmod(path, mask.apply(stat.st_mode));
        if (maybe_error.is_error()) {
            warnln("Failed to change permissions of '{}': {}", path, maybe_error.release_error());
            success = false;
        }

        if (recursive && S_ISDIR(stat.st_mode)) {
            Core::DirIterator it(path, Core::DirIterator::Flags::SkipParentAndBaseDir);

            while (it.has_next())
                success &= update_path_permissions(it.next_full_path());
        }

        return success;
    };

    auto success = true;
    for (auto const& path : paths) {
        success &= update_path_permissions(path);
    }

    return success ? 0 : 1;
}
