/*
 * Copyright (c) 2020, Fei Wu <f.eiwu@yahoo.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <ctype.h>
#include <dirent.h>
#include <errno.h>
#include <pwd.h>
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

    char temp_filename[] = "/etc/passwd.XXXXXX";
    auto fd = mkstemp(temp_filename);
    if (fd == -1) {
        perror("failed to create temporary file");
        return 1;
    }

    FILE* temp_file = fdopen(fd, "w");
    if (!temp_file) {
        perror("fdopen");
        if (unlink(temp_filename) < 0) {
            perror("unlink");
        }

        return 1;
    }

    bool user_exists = false;
    String home_directory;

    int rc = 0;
    setpwent();
    for (auto* pw = getpwent(); pw; pw = getpwent()) {
        if (strcmp(pw->pw_name, username)) {
            if (putpwent(pw, temp_file) != 0) {
                perror("failed to put an entry in the temporary passwd file");
                rc = 1;
                break;
            }
        } else {
            user_exists = true;
            if (remove_home)
                home_directory = pw->pw_dir;
        }
    }
    endpwent();

    if (fclose(temp_file)) {
        perror("fclose");
        if (!rc)
            rc = 1;
    }

    if (rc == 0 && !user_exists) {
        fprintf(stderr, "specified user doesn't exist\n");
        rc = 6;
    }

    if (rc == 0 && chmod(temp_filename, S_IRUSR | S_IWUSR | S_IRGRP | S_IROTH)) {
        perror("chmod");
        rc = 1;
    }

    if (rc == 0 && rename(temp_filename, "/etc/passwd") < 0) {
        perror("failed to rename the temporary passwd file");
        rc = 1;
    }

    if (rc) {
        if (unlink(temp_filename) < 0) {
            perror("unlink");
        }
        return rc;
    }

    if (remove_home) {
        if (access(home_directory.characters(), F_OK) == -1)
            return 0;

        String real_path = Core::File::real_path_for(home_directory);

        if (real_path == "/") {
            fprintf(stderr, "home directory is /, not deleted!\n");
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
            fprintf(stderr, "failed to remove the home directory\n");
            return 12;
        }
    }

    return 0;
}
