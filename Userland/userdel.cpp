/*
 * Copyright (c) 2019-2020, Fei Wu <f.eiwu@yahoo.com>
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
#include <AK/StringBuilder.h>
#include <LibCore/ArgsParser.h>
#include <ctype.h>
#include <dirent.h>
#include <errno.h>
#include <pwd.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <unistd.h>

static int delete_user(const char* username, bool remove_home);

int main(int argc, char** argv)
{
    const char* username = nullptr;
    bool remove_home = false;

    Core::ArgsParser args_parser;
    args_parser.add_option(remove_home, "Remove home directory", "remove", 'r');
    args_parser.add_positional_argument(username, "Login user identity (username)", "login");
    args_parser.parse(argc, argv);

    return delete_user(username, remove_home);
}

int delete_user(const char* username, bool remove_home)
{
    const char* temp_filename = "passwd.temp";
    FILE* temp_file = fopen(temp_filename, "w");
    if (!temp_file) {
        perror("failed to create temporary file");
        return 1;
    }

    int error_code = 0;
    bool user_exists{ false };

    setpwent();
    for (auto* pw = getpwent(); pw; pw = getpwent()) {
        if (strcmp(pw->pw_name, username)) {
            if (putpwent(pw, temp_file) != 0) {
                perror("failed to put a entry in the temporay passwd file");
                error_code = 1;
                break;
            }
        } else {
            user_exists = true;
            if (remove_home) {
                if (access(pw->pw_dir, F_OK) != -1 && system(String::format("rm -r %s", pw->pw_dir).characters()) == -1) {
                    perror("failed to remove the home directory");
                    error_code = 12;
                    break;
                }
            }
        }
    }
    endpwent();
    fclose(temp_file);

    if (!user_exists) {
        fprintf(stderr, "Specified user doesn't exist!\n");
        error_code = 6;
    }

    if (error_code == 0 && rename(temp_filename, "/etc/passwd") < 0) {
        perror("failed to rename the temporay passwd file");
        error_code = 1;
    }

    return error_code;
}