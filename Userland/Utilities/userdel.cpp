/*
 * Copyright (c) 2020, Fei Wu <f.eiwu@yahoo.com>
 * Copyright (c) 2021, Brandon Pruitt <brapru@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ScopeGuard.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
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

    unveil(nullptr, nullptr);

    const char* username = nullptr;
    bool remove_home = false;

    Core::ArgsParser args_parser;
    args_parser.add_option(remove_home, "Remove home directory", "remove", 'r');
    args_parser.add_positional_argument(username, "Login user identity (username)", "login");
    args_parser.parse(argc, argv);

    if (!remove_home) {
        if (pledge("stdio wpath rpath cpath fattr", nullptr) < 0) {
            perror("pledge");
            return 1;
        }
    }

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

    bool user_exists = false;
    String home_directory;

    setpwent();
    for (auto* pw = getpwent(); pw; pw = getpwent()) {
        if (strcmp(pw->pw_name, username)) {
            if (putpwent(pw, temp_passwd_file) != 0) {
                perror("failed to put an entry in the temporary passwd file");
                return 1;
            }
        } else {
            user_exists = true;
            if (remove_home)
                home_directory = pw->pw_dir;
        }
    }
    endpwent();

    setspent();
    for (auto* spwd = getspent(); spwd; spwd = getspent()) {
        if (strcmp(spwd->sp_namp, username)) {
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

    if (!user_exists) {
        warnln("specified user doesn't exist");
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
        if (access(home_directory.characters(), F_OK) == -1)
            return 0;

        String real_path = Core::File::real_path_for(home_directory);

        if (real_path == "/") {
            warnln("home directory is /, not deleted!");
            return 12;
        }

        pid_t child;
        const char* argv[] = { "rm", "-r", home_directory.characters(), nullptr };
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
