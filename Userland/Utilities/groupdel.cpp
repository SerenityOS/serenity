/*
 * Copyright (c) 2020, Fei Wu <f.eiwu@yahoo.com>
 * Copyright (c) 2021, Brandon Pruitt <brapru@pm.me>
 * Copyright (c) 2021, Maxime Friess <M4x1me@pm.me>
 * Copyright (c) 2022, Umut İnan Erdoğan <umutinanerdogan62@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <grp.h>
#include <pwd.h>
#include <unistd.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio wpath rpath cpath fattr proc exec"));
    TRY(Core::System::unveil("/etc/", "rwc"));
    TRY(Core::System::unveil("/bin/rm", "x"));
    TRY(Core::System::unveil(nullptr, nullptr));

    ByteString groupname;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(groupname, "Group name", "group");
    args_parser.parse(arguments);

    setgrent();
    auto* g = getgrnam(groupname.characters());

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
    StringView temp_group_view { temp_group, strlen(temp_group) };

    auto unlink_temp_files = [&] {
        if (Core::System::unlink(temp_group_view).is_error())
            perror("unlink");
    };

    ArmedScopeGuard unlink_temp_files_guard = [&] {
        unlink_temp_files();
    };

    int temp_group_fd = TRY(Core::System::mkstemp(temp_group));

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

    TRY(Core::System::chmod(temp_group_view, S_IRUSR | S_IWUSR | S_IRGRP | S_IROTH));
    TRY(Core::System::rename(temp_group_view, "/etc/group"sv));

    unlink_temp_files_guard.disarm();

    return 0;
}
