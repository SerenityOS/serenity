/*
 * Copyright (c) 2021, Brandon Pruitt <brapru@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Types.h>
#include <LibCore/Account.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <pwd.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    if (geteuid() != 0) {
        warnln("Not running as root :^(");
        return 1;
    }

    if (setegid(0) < 0) {
        perror("setegid");
        return 1;
    }

    if (pledge("stdio wpath rpath cpath fattr tty", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (unveil("/etc", "rwc") < 0) {
        perror("unveil");
        return 1;
    }

    int uid = 0;
    int gid = 0;
    bool lock = false;
    bool unlock = false;
    const char* new_home_directory = nullptr;
    bool move_home = false;
    const char* shell = nullptr;
    const char* gecos = nullptr;
    const char* username = nullptr;

    auto args_parser = Core::ArgsParser();
    args_parser.set_general_help("Modify a user account");
    args_parser.add_option(uid, "The new numerical value of the user's ID", "uid", 'u', "uid");
    args_parser.add_option(gid, "The group number of the user's new initial login group", "gid", 'g', "gid");
    args_parser.add_option(lock, "Lock password", "lock", 'L');
    args_parser.add_option(unlock, "Unlock password", "unlock", 'U');
    args_parser.add_option(new_home_directory, "The user's new login directory", "home", 'd', "new-home");
    args_parser.add_option(move_home, "Move the content of the user's home directory to the new location", "move", 'm');
    args_parser.add_option(shell, "The name of the user's new login shell", "shell", 's', "path-to-shell");
    args_parser.add_option(gecos, "Change the GECOS field of the user", "gecos", 'n', "general-info");
    args_parser.add_positional_argument(username, "Username of the account to modify", "username");

    args_parser.parse(argc, argv);

    auto account_or_error = Core::Account::from_name(username);

    if (account_or_error.is_error()) {
        warnln("Core::Account::from_name: {}", account_or_error.error());
        return 1;
    }

    // target_account is the account we are modifying.
    auto& target_account = account_or_error.value();

    if (move_home) {
        if (unveil(target_account.home_directory().characters(), "c") < 0) {
            perror("unveil");
            return 1;
        }

        if (unveil(new_home_directory, "wc") < 0) {
            perror("unveil");
            return 1;
        }
    }

    unveil(nullptr, nullptr);

    if (uid) {
        if (uid < 0) {
            warnln("invalid uid {}", uid);
            return 1;
        }

        if (getpwuid(static_cast<uid_t>(uid))) {
            warnln("uid {} already exists", uid);
            return 1;
        }

        target_account.set_uid(uid);
    }

    if (gid) {
        if (gid < 0) {
            warnln("invalid gid {}", gid);
            return 1;
        }

        target_account.set_gid(gid);
    }

    if (lock) {
        target_account.set_password_enabled(false);
    }

    if (unlock) {
        target_account.set_password_enabled(true);
    }

    if (new_home_directory) {
        if (move_home) {
            int rc = rename(target_account.home_directory().characters(), new_home_directory);
            if (rc < 0) {
                if (errno == EXDEV) {
                    auto result = Core::File::copy_file_or_directory(
                        new_home_directory, target_account.home_directory().characters(),
                        Core::File::RecursionMode::Allowed,
                        Core::File::LinkMode::Disallowed,
                        Core::File::AddDuplicateFileMarker::No);

                    if (result.is_error()) {
                        warnln("usermod: could not move directory {} : {}", target_account.home_directory().characters(), result.error().error_code);
                        return 1;
                    }
                    rc = unlink(target_account.home_directory().characters());
                    if (rc < 0)
                        warnln("usermod: unlink {} : {}", target_account.home_directory().characters(), strerror(errno));
                } else {
                    warnln("usermod: could not move directory {} : {}", target_account.home_directory().characters(), strerror(errno));
                }
            }
        }

        target_account.set_home_directory(new_home_directory);
    }

    if (shell) {
        target_account.set_shell(shell);
    }

    if (gecos) {
        target_account.set_gecos(gecos);
    }

    if (pledge("stdio wpath rpath cpath fattr", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (!target_account.sync()) {
        perror("Core::Account::Sync");
        return 1;
    }

    return 0;
}
