/*
 * Copyright (c) 2019-2020, Jesse Buhagiar <jooster669@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
#include <AK/String.h>
#include <LibCore/ArgsParser.h>
#include <ctype.h>
#include <pwd.h>
#include <stdio.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <unistd.h>

constexpr uid_t BASE_UID = 1000;
constexpr gid_t USERS_GID = 100;
constexpr const char* DEFAULT_SHELL = "/bin/Shell";

int main(int argc, char** argv)
{
    const char* home_path = nullptr;
    int uid = 0;
    int gid = USERS_GID;
    bool create_home_dir = false;
    const char* shell = DEFAULT_SHELL;
    const char* gecos = "";
    const char* username = nullptr;

    Core::ArgsParser args_parser;
    args_parser.add_option(home_path, "Home directory for the new user", "home-dir", 'd', "path");
    args_parser.add_option(uid, "User ID (uid) for the new user", "uid", 'u', "uid");
    args_parser.add_option(gid, "Group ID (gid) for the new user", "gid", 'g', "gid");
    args_parser.add_option(create_home_dir, "Create home directory if it does not exist", "create-home", 'm');
    args_parser.add_option(shell, "Path to the default shell binary for the new user", "shell", 's', "path-to-shell");
    args_parser.add_option(gecos, "GECOS name of the new user", "gecos", 'n', "general-info");
    args_parser.add_positional_argument(username, "Login user identity (username)", "login");

    args_parser.parse(argc, argv);

    // Let's run a quick sanity check on username
    if (strpbrk(username, "\\/!@#$%^&*()~+=`:\n")) {
        fprintf(stderr, "invalid character in username, %s\n", username);
        return 1;
    }

    // Disallow names starting with _ and -
    if (username[0] == '_' || username[0] == '-' || !isalpha(username[0])) {
        fprintf(stderr, "invalid username, %s\n", username);
        return 1;
    }

    if (uid < 0) {
        fprintf(stderr, "invalid uid %d!\n", uid);
        return 3;
    }

    // First, let's sort out the uid for the user
    if (uid > 0) {
        if (getpwuid(static_cast<uid_t>(uid))) {
            fprintf(stderr, "uid %u already exists!\n", uid);
            return 4;
        }

    } else {
        for (uid = BASE_UID; getpwuid(static_cast<uid_t>(uid)); uid++) {
        }
    }

    if (gid < 0) {
        fprintf(stderr, "invalid gid %d\n", gid);
        return 3;
    }

    FILE* pwfile = fopen("/etc/passwd", "a");
    if (!pwfile) {
        perror("failed to open /etc/passwd");
        return 1;
    }

    String home;
    if (!home_path)
        home = String::format("/home/%s", username);
    else
        home = home_path;

    if (create_home_dir) {
        if (mkdir(home.characters(), 0700) < 0) {
            perror(String::format("failed to create directory %s", home.characters()).characters());
            return 12;
        }

        if (chown(home.characters(), static_cast<uid_t>(uid), static_cast<gid_t>(gid)) < 0) {
            perror(String::format("failed to chown %s to %u:%u", home.characters(), uid, gid).characters());

            if (rmdir(home.characters()) < 0) {
                perror(String::format("failed to rmdir %s", home.characters()).characters());
                return 12;
            }
            return 12;
        }
    }

    struct passwd p;
    p.pw_name = const_cast<char*>(username);
    p.pw_dir = const_cast<char*>(home.characters());
    p.pw_uid = static_cast<uid_t>(uid);
    p.pw_gid = static_cast<gid_t>(gid);
    p.pw_shell = const_cast<char*>(shell);
    p.pw_gecos = const_cast<char*>(gecos);

    if (putpwent(&p, pwfile) < 0) {
        perror("putpwent");
        return 1;
    }

    fclose(pwfile);

    return 0;
}
