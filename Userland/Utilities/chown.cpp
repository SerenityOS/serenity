/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <AK/Vector.h>
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

    if (argc < 3) {
        printf("usage: chown <uid[:gid]> <path>\n");
        return 0;
    }

    uid_t new_uid = -1;
    gid_t new_gid = -1;

    auto parts = String(argv[1]).split(':', true);
    if (parts.is_empty()) {
        fprintf(stderr, "Empty uid/gid spec\n");
        return 1;
    }
    if (parts[0].is_empty() || (parts.size() == 2 && parts[1].is_empty()) || parts.size() > 2) {
        fprintf(stderr, "Invalid uid/gid spec\n");
        return 1;
    }

    auto number = parts[0].to_uint();
    if (number.has_value()) {
        new_uid = number.value();
    } else {
        auto* passwd = getpwnam(parts[0].characters());
        if (!passwd) {
            fprintf(stderr, "Unknown user '%s'\n", parts[0].characters());
            return 1;
        }
        new_uid = passwd->pw_uid;
    }

    if (parts.size() == 2) {
        auto number = parts[1].to_uint();
        if (number.has_value()) {
            new_gid = number.value();
        } else {
            auto* group = getgrnam(parts[1].characters());
            if (!group) {
                fprintf(stderr, "Unknown group '%s'\n", parts[1].characters());
                return 1;
            }
            new_gid = group->gr_gid;
        }
    }

    int rc = chown(argv[2], new_uid, new_gid);
    if (rc < 0) {
        perror("chown");
        return 1;
    }

    return 0;
}
