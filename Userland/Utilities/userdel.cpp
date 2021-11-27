/*
 * Copyright (c) 2020, Fei Wu <f.eiwu@yahoo.com>
 * Copyright (c) 2021, Brandon Pruitt <brapru@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ScopeGuard.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <LibCore/Account.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <ctype.h>
#include <dirent.h>
#include <errno.h>
#include <pwd.h>
#include <shadow.h>
#include <spawn.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <unistd.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio wpath rpath cpath fattr proc exec"));
    TRY(Core::System::unveil("/etc/", "rwc"));
    TRY(Core::System::unveil("/bin/rm", "x"));

    const char* username = nullptr;
    bool remove_home = false;

    Core::ArgsParser args_parser;
    args_parser.add_option(remove_home, "Remove home directory", "remove", 'r');
    args_parser.add_positional_argument(username, "Login user identity (username)", "login");
    args_parser.parse(arguments);

    auto account_or_error = Core::Account::from_name(username);

    if (account_or_error.is_error()) {
        warnln("Core::Account::from_name: {}", account_or_error.error());
        return 1;
    }

    auto& target_account = account_or_error.value();

    if (remove_home) {
        TRY(Core::System::unveil(target_account.home_directory().characters(), "c"));
    } else {
        TRY(Core::System::pledge("stdio wpath rpath cpath fattr"));
    }
    TRY(Core::System::unveil(nullptr, nullptr));

    char temp_passwd[] = "/etc/passwd.XXXXXX";
    char temp_shadow[] = "/etc/shadow.XXXXXX";

    auto unlink_temp_files = [&] {
        if (unlink(temp_passwd) < 0)
            perror("unlink");
        if (unlink(temp_shadow) < 0)
            perror("unlink");
    };

    ArmedScopeGuard unlink_temp_files_guard = [&] {
        unlink_temp_files();
    };

    auto temp_passwd_fd = mkstemp(temp_passwd);
    if (temp_passwd_fd == -1) {
        perror("failed to create temporary passwd file");
        return 1;
    }

    auto temp_shadow_fd = mkstemp(temp_shadow);
    if (temp_shadow_fd == -1) {
        perror("failed to create temporary shadow file");
        return 1;
    }

    FILE* temp_passwd_file = fdopen(temp_passwd_fd, "w");
    if (!temp_passwd_file) {
        perror("fdopen");
        return 1;
    }

    FILE* temp_shadow_file = fdopen(temp_shadow_fd, "w");
    if (!temp_shadow_file) {
        perror("fdopen");
        return 1;
    }

    setpwent();
    for (auto* pw = getpwent(); pw; pw = getpwent()) {
        if (strcmp(pw->pw_name, target_account.username().characters())) {
            if (putpwent(pw, temp_passwd_file) != 0) {
                perror("failed to put an entry in the temporary passwd file");
                return 1;
            }
        }
    }
    endpwent();

    setspent();
    for (auto* spwd = getspent(); spwd; spwd = getspent()) {
        if (strcmp(spwd->sp_namp, target_account.username().characters())) {
            if (putspent(spwd, temp_shadow_file) != 0) {
                perror("failed to put an entry in the temporary shadow file");
                return 1;
            }
        }
    }
    endspent();

    if (fclose(temp_passwd_file)) {
        perror("fclose");
        return 1;
    }

    if (fclose(temp_shadow_file)) {
        perror("fclose");
        return 1;
    }

    if (chmod(temp_passwd, S_IRUSR | S_IWUSR | S_IRGRP | S_IROTH)) {
        perror("chmod");
        return 1;
    }

    if (chmod(temp_shadow, S_IRUSR | S_IWUSR | S_IRGRP | S_IROTH)) {
        perror("chmod");
        return 1;
    }

    if (rename(temp_passwd, "/etc/passwd") < 0) {
        perror("failed to rename the temporary passwd file");
        return 1;
    }

    if (rename(temp_shadow, "/etc/shadow") < 0) {
        perror("failed to rename the temporary shadow file");
        return 1;
    }

    unlink_temp_files_guard.disarm();

    if (remove_home) {
        if (access(target_account.home_directory().characters(), F_OK) == -1)
            return 0;

        String real_path = Core::File::real_path_for(target_account.home_directory());

        if (real_path == "/") {
            warnln("home directory is /, not deleted!");
            return 12;
        }

        pid_t child;
        const char* argv[] = { "rm", "-r", target_account.home_directory().characters(), nullptr };
        if ((errno = posix_spawn(&child, "/bin/rm", nullptr, nullptr, const_cast<char**>(argv), environ))) {
            perror("posix_spawn");
            return 12;
        }
        int wstatus;
        if (waitpid(child, &wstatus, 0) < 0) {
            perror("waitpid");
            return 12;
        }
        if (WEXITSTATUS(wstatus)) {
            warnln("failed to remove the home directory");
            return 12;
        }
    }

    return 0;
}
