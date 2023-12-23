/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/DirIterator.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <grp.h>
#include <pwd.h>
#include <stdio.h>
#include <string.h>
#include <sys/stat.h>
#include <unistd.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath chown"));

    StringView spec;
    Vector<StringView> paths;
    bool no_dereference = false;
    bool recursive = false;
    bool follow_symlinks = false;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Change the ownership of a file or directory.");
    args_parser.add_option(no_dereference, "Don't follow symlinks", "no-dereference", 'h');
    args_parser.add_option(recursive, "Change file ownership recursively", "recursive", 'R');
    args_parser.add_option(follow_symlinks, "Follow symlinks while recursing into directories", nullptr, 'L');
    args_parser.add_positional_argument(spec, "User and group IDs", "USER[:GROUP]");
    args_parser.add_positional_argument(paths, "Paths to files", "PATH");
    args_parser.parse(arguments);

    uid_t new_uid = -1;
    gid_t new_gid = -1;

    auto parts = spec.split_view(':', SplitBehavior::KeepEmpty);
    if (parts[0].is_empty() || (parts.size() == 2 && parts[1].is_empty()) || parts.size() > 2) {
        warnln("Invalid uid/gid spec");
        return 1;
    }

    auto number = parts[0].to_number<uid_t>();
    if (number.has_value()) {
        new_uid = number.value();
    } else {
        auto passwd = TRY(Core::System::getpwnam(parts[0]));
        if (!passwd.has_value()) {
            warnln("Unknown user '{}'", parts[0]);
            return 1;
        }
        new_uid = passwd->pw_uid;
    }

    if (parts.size() == 2) {
        auto number = parts[1].to_number<gid_t>();
        if (number.has_value()) {
            new_gid = number.value();
        } else {
            auto group = TRY(Core::System::getgrnam(parts[1]));
            if (!group.has_value()) {
                warnln("Unknown group '{}'", parts[1]);
                return 1;
            }
            new_gid = group->gr_gid;
        }
    }

    Function<bool(StringView)> update_path_owner = [&](StringView path) {
        auto stat_or_error = Core::System::lstat(path);
        if (stat_or_error.is_error()) {
            warnln("Could not stat '{}': {}", path, stat_or_error.release_error());
            return false;
        }

        auto stat = stat_or_error.release_value();

        if (S_ISLNK(stat.st_mode) && !follow_symlinks && !paths.contains_slow(path))
            return false;

        auto success = true;
        auto maybe_error = no_dereference
            ? Core::System::lchown(path, new_uid, new_gid)
            : Core::System::chown(path, new_uid, new_gid);

        if (maybe_error.is_error()) {
            warnln("Failed to change owner of '{}': {}", path, maybe_error.release_error());
            success = false;
        }

        if (recursive && S_ISDIR(stat.st_mode)) {
            Core::DirIterator it(path, Core::DirIterator::Flags::SkipParentAndBaseDir);

            while (it.has_next())
                success &= update_path_owner(it.next_full_path());
        }

        return success;
    };

    auto success = true;
    for (auto const& path : paths) {
        success &= update_path_owner(path);
    }

    return success ? 0 : 1;
}
