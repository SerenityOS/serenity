/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/Vector.h>
#include <alloca.h>
#include <grp.h>
#include <pwd.h>
#include <stdio.h>
#include <unistd.h>

extern "C" int main(int, char**);

 #include <termios.h>

static void set_echo(int fd) {
    struct termios term;
    tcgetattr(0, &term);
    term.c_lflag |= ECHO;
    tcsetattr(fd, TCSAFLUSH, &term);
}

static void unset_echo(int fd) {
    struct termios term;
    tcgetattr(0, &term);
    term.c_lflag &= ~ECHO;
    tcsetattr(fd, TCSAFLUSH, &term);
}

static bool check_user_password(const struct passwd* pwd) {
    if (strlen(pwd->pw_passwd) == 0) return true;

    fprintf(stdout, "Password: ");
    fflush(stdout);
    unset_echo(0);
    
    // + 1 to be enough space for the newline character
    char input_password[PWDB_STR_MAX_LEN + 1];
    fgets(input_password, PWDB_STR_MAX_LEN, stdin);

    set_echo(1);
    puts("");

    int length = strlen(input_password);
    if (input_password[length - 1] == '\n') {
        input_password[length - 1] = '\0';
    }

    return strcmp(input_password, pwd->pw_passwd) == 0;
}

int main(int argc, char** argv)
{
    uid_t uid = 0;
    gid_t gid = 0;
    struct passwd* pwd = nullptr;
    if (argc > 1) {
        pwd = getpwnam(argv[1]);
        if (!pwd) {
            fprintf(stderr, "No such user: %s\n", argv[1]);
            return 1;
        }
        uid = pwd->pw_uid;
        gid = pwd->pw_gid;
    }

    if (!pwd)
        pwd = getpwuid(0);

    if (!pwd) {
        fprintf(stderr, "No passwd entry.\n");
        return 1;
    }

    if (getuid() != 0 and getgid() != 0) {
        bool is_password_valid = check_user_password(pwd);
        if (not is_password_valid) {
            fprintf(stderr, "Incorrect password.\n");
            return 1;
        }
    }

    Vector<gid_t> extra_gids;
    for (auto* group = getgrent(); group; group = getgrent()) {
        for (size_t i = 0; group->gr_mem[i]; ++i) {
            if (!strcmp(pwd->pw_name, group->gr_mem[i]))
                extra_gids.append(group->gr_gid);
        }
    }
    endgrent();

    int rc = setgroups(extra_gids.size(), extra_gids.data());
    if (rc < 0) {
        perror("setgroups");
        return 1;
    }
    rc = setgid(uid);
    if (rc < 0) {
        perror("setgid");
        return 1;
    }
    rc = setuid(gid);
    if (rc < 0) {
        perror("setuid");
        return 1;
    }
    rc = execl("/bin/sh", "sh", nullptr);
    perror("execl");
    return 1;
}
