/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Optional.h>
#include <AK/String.h>
#include <LibCore/ArgsParser.h>
#include <grp.h>
#include <pwd.h>
#include <stdio.h>
#include <string.h>
#include <sys/stat.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    if (pledge("stdio rpath chown", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    const char* gid_arg = nullptr;
    const char* path = nullptr;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Change the owning group for a file or directory.");
    args_parser.add_positional_argument(gid_arg, "Group ID", "gid");
    args_parser.add_positional_argument(path, "Path to file", "path");
    args_parser.parse(argc, argv);

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

    int rc = chown(path, -1, new_gid);
    if (rc < 0) {
        perror("chgrp");
        return 1;
    }

    return 0;
}
