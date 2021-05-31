/*
 * Copyright (c) 2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <LibCore/ArgsParser.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    int flags = -1;
    uid_t chroot_user = 0;
    gid_t chroot_group = 0;
    const char* path = nullptr;
    const char* program = "/bin/Shell";
    const char* userspec = "0:0";

    Core::ArgsParser args_parser;
    args_parser.set_general_help(
        "Run a program in a chroot sandbox. During execution, the program "
        "sees the given path as '/', and cannot access files outside of it.");
    args_parser.add_positional_argument(path, "New root directory", "path");
    args_parser.add_positional_argument(program, "Program to run", "program", Core::ArgsParser::Required::No);

    Core::ArgsParser::Option userspec_option {
        true,
        "The uid:gid to use",
        "userspec",
        'u',
        "userpec",
        [&userspec](const char* s) {
            Vector<StringView> parts = StringView(s).split_view(':', true);
            if (parts.size() != 2)
                return false;
            userspec = s;
            return true;
        }
    };
    args_parser.add_option(move(userspec_option));

    Core::ArgsParser::Option mount_options {
        true,
        "Mount options",
        "options",
        'o',
        "options",
        [&flags](const char* s) {
            flags = 0;
            Vector<StringView> parts = StringView(s).split_view(',');
            for (auto& part : parts) {
                if (part == "defaults")
                    continue;
                else if (part == "nodev")
                    flags |= MS_NODEV;
                else if (part == "noexec")
                    flags |= MS_NOEXEC;
                else if (part == "nosuid")
                    flags |= MS_NOSUID;
                else if (part == "ro")
                    flags |= MS_RDONLY;
                else if (part == "remount")
                    flags |= MS_REMOUNT;
                else if (part == "bind")
                    warnln("Ignoring -o bind, as it doesn't make sense for chroot");
                else
                    return false;
            }
            return true;
        }
    };
    args_parser.add_option(move(mount_options));
    args_parser.parse(argc, argv);

    if (chroot_with_mount_flags(path, flags) < 0) {
        perror("chroot");
        return 1;
    }

    if (chdir("/") < 0) {
        perror("chdir(/)");
        return 1;
    }

    // Failed parsing will silently fail open (uid=0; gid=0);
    // 0:0 is also the default when no --userspec argument is provided.
    auto parts = String(userspec).split(':', true);
    chroot_user = (uid_t)strtol(parts[0].characters(), nullptr, 10);
    chroot_group = (uid_t)strtol(parts[1].characters(), nullptr, 10);

    if (setresgid(chroot_group, chroot_group, chroot_group)) {
        perror("setgid");
        return 1;
    }

    if (setresuid(chroot_user, chroot_user, chroot_user)) {
        perror("setuid");
        return 1;
    }

    execl(program, program, nullptr);
    perror("execl");
    return 1;
}
