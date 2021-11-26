/*
 * Copyright (c) 2019-2020, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2021, Brandon Pruitt  <brapru@pm.me>
 * Copyright (c) 2021, Maxime Friess <M4x1me@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <ctype.h>
#include <grp.h>
#include <string.h>
#include <unistd.h>

constexpr gid_t GROUPS_GID = 100;

int main(int argc, char** argv)
{
    if (pledge("stdio wpath rpath cpath chown", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    int gid = 0;
    char const* groupname = nullptr;

    Core::ArgsParser args_parser;
    args_parser.add_option(gid, "Group ID (gid) for the new group", "gid", 'g', "gid");
    args_parser.add_positional_argument(groupname, "Name of the group (groupname)", "group");

    args_parser.parse(argc, argv);

    // Let's run a quick sanity check on groupname
    if (strpbrk(groupname, "\\/!@#$%^&*()~+=`:\n")) {
        warnln("invalid character in groupname, {}", groupname);
        return 1;
    }

    // Disallow names starting with _ and -
    if (groupname[0] == '_' || groupname[0] == '-' || !isalpha(groupname[0])) {
        warnln("invalid groupname, {}", groupname);
        return 1;
    }

    if (getgrnam(groupname)) {
        warnln("Group {} already exists!", groupname);
        return 1;
    }

    if (gid < 0) {
        warnln("invalid gid {}!", gid);
        return 3;
    }

    // First, let's sort out the gid for the group
    if (gid > 0) {
        if (getgrgid(static_cast<uid_t>(gid))) {
            warnln("gid {} already exists!", gid);
            return 4;
        }
    } else {
        for (gid = GROUPS_GID; getgrgid(static_cast<uid_t>(gid)); gid++) {
        }
    }

    if (gid < 0) {
        warnln("invalid gid {}", gid);
        return 3;
    }

    FILE* grfile = fopen("/etc/group", "a");
    if (!grfile) {
        perror("failed to open /etc/group");
        return 1;
    }

    struct group g;
    g.gr_name = const_cast<char*>(groupname);
    g.gr_passwd = const_cast<char*>("x");
    g.gr_gid = static_cast<gid_t>(gid);
    g.gr_mem = nullptr;

    if (putgrent(&g, grfile) < 0) {
        perror("putpwent");
        return 1;
    }

    fclose(grfile);

    return 0;
}
