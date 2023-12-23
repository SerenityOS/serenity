/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <grp.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath chown"));

    StringView gid_arg;
    Vector<StringView> paths;
    bool dont_follow_symlinks = false;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Change the owning group for files or directories.");
    args_parser.add_option(dont_follow_symlinks, "Don't follow symlinks", "no-dereference", 'h');
    args_parser.add_positional_argument(gid_arg, "Group ID", "gid");
    args_parser.add_positional_argument(paths, "Paths to files", "paths");
    args_parser.parse(arguments);

    gid_t new_gid = -1;

    if (gid_arg.is_empty()) {
        warnln("Empty gid option");
        return 1;
    }

    auto number = gid_arg.to_number<unsigned>();
    if (number.has_value()) {
        new_gid = number.value();
    } else {
        auto group = TRY(Core::System::getgrnam(gid_arg));
        if (!group.has_value()) {
            warnln("Unknown group '{}'", gid_arg);
            return 1;
        }
        new_gid = group->gr_gid;
    }

    auto has_errors = false;
    for (auto path : paths) {
        ErrorOr<void> maybe_error;
        if (dont_follow_symlinks)
            maybe_error = Core::System::lchown(path, -1, new_gid);
        else
            maybe_error = Core::System::chown(path, -1, new_gid);

        if (maybe_error.is_error()) {
            has_errors = true;
            warnln("Changing group of '{}'. {}", path, maybe_error.release_error());
        }
    }

    return has_errors ? 1 : 0;
}
