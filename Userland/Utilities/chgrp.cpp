/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <grp.h>
#include <string.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath chown"));

    char const* gid_arg = nullptr;
    char const* path = nullptr;
    bool dont_follow_symlinks = false;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Change the owning group for a file or directory.");
    args_parser.add_option(dont_follow_symlinks, "Don't follow symlinks", "no-dereference", 'h');
    args_parser.add_positional_argument(gid_arg, "Group ID", "gid");
    args_parser.add_positional_argument(path, "Path to file", "path");
    args_parser.parse(arguments);

    gid_t new_gid = -1;

    if (String(gid_arg).is_empty()) {
        warnln("Empty gid option");
        return 1;
    }

    auto number = String(gid_arg).to_uint();
    if (number.has_value()) {
        new_gid = number.value();
    } else {
        auto* group = getgrnam(gid_arg);
        if (!group) {
            warnln("Unknown group '{}'", gid_arg);
            return 1;
        }
        new_gid = group->gr_gid;
    }

    if (dont_follow_symlinks) {
        TRY(Core::System::lchown(path, -1, new_gid));
    } else {
        TRY(Core::System::chown(path, -1, new_gid));
    }

    return 0;
}
