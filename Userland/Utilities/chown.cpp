/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
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

    String spec;
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

    auto number = parts[0].to_uint();
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
        auto number = parts[1].to_uint();
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

    Function<ErrorOr<void>(StringView)> update_path_owner = [&](StringView path) -> ErrorOr<void> {
        auto stat = TRY(Core::System::lstat(path));

        if (S_ISLNK(stat.st_mode) && !follow_symlinks && !paths.contains_slow(path))
            return {};

        if (no_dereference) {
            TRY(Core::System::lchown(path, new_uid, new_gid));
        } else {
            TRY(Core::System::chown(path, new_uid, new_gid));
        }

        if (recursive && S_ISDIR(stat.st_mode)) {
            Core::DirIterator it(path, Core::DirIterator::Flags::SkipParentAndBaseDir);

            while (it.has_next())
                TRY(update_path_owner(it.next_full_path()));
        }

        return {};
    };

    for (auto path : paths) {
        TRY(update_path_owner(path));
    }

    return 0;
}
