/*
 * Copyright (c) 2020, Fei Wu <f.eiwu@yahoo.com>
 * Copyright (c) 2021, Brandon Pruitt <brapru@pm.me>
 * Copyright (c) 2021, Maxime Friess <M4x1me@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <grp.h>
#include <pwd.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    if (pledge("stdio wpath rpath cpath fattr proc exec", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (unveil("/etc/", "rwc") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/bin/rm", "x") < 0) {
        perror("unveil");
        return 1;
    }

    char const* groupname = nullptr;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(groupname, "Group name", "group");
    args_parser.parse(argc, argv);

    setgrent();
    auto* g = getgrnam(groupname);

    // Check if the group exists
    if (!g) {
        warnln("group {} does not exist", groupname);
        return 6;
    }
    auto gid = g->gr_gid;
    endgrent();

    // Search if the group is the primary group of an user
    setpwent();
    struct passwd* pw;
    for (pw = getpwent(); pw; pw = getpwent()) {
        if (pw->pw_gid == gid)
            break;
    }

    // If pw is not NULL it means we ended prematurely, aka. the group was found as primary group of an user
    if (pw) {
        warnln("cannot remove the primary group of user '{}'", pw->pw_name);
        endpwent();
        return 8;
    }

    endpwent();
    // We can now safely delete the group

    // Create a temporary group file
    char temp_group[] = "/etc/group.XXXXXX";

    auto unlink_temp_files = [&] {
        if (unlink(temp_group) < 0)
            perror("unlink");
    };

    ArmedScopeGuard unlink_temp_files_guard = [&] {
        unlink_temp_files();
    };

    auto temp_group_fd = mkstemp(temp_group);
    if (temp_group_fd == -1) {
        perror("failed to create temporary group file");
        return 1;
    }

    FILE* temp_group_file = fdopen(temp_group_fd, "w");
    if (!temp_group_file) {
        perror("fdopen");
        return 1;
    }

    setgrent();
    for (auto* gr = getgrent(); gr; gr = getgrent()) {
        if (gr->gr_gid != gid) {
            if (putgrent(gr, temp_group_file) != 0) {
                perror("failed to put an entry in the temporary group file");
                return 1;
            }
        }
    }
    endgrent();

    if (fclose(temp_group_file)) {
        perror("fclose");
        return 1;
    }

    if (chmod(temp_group, S_IRUSR | S_IWUSR | S_IRGRP | S_IROTH)) {
        perror("chmod");
        return 1;
    }

    if (rename(temp_group, "/etc/group") < 0) {
        perror("failed to rename the temporary group file");
        return 1;
    }

    unlink_temp_files_guard.disarm();

    return 0;
}
