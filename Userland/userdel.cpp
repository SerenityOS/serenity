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
 *
 * 
 * 
 * The code for function remove() was taken from rm.cpp in this directory.
 * The copyright notice is as follows:
 *
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

static void parseArg(int argc, char** argv, const char*& username, bool& removeHome);
static int deleteUser(const char* username, bool removeHome);
static int remove(bool recursive, String path);

int main(int argc, char** argv)
{
    const char* username = nullptr;
    bool removeHome = false;
    parseArg(argc, argv, username, removeHome);

    return deleteUser(username, removeHome);
}

void parseArg(int argc, char** argv, const char*& username, bool& removeHome)
{
    Core::ArgsParser args_parser;
    args_parser.add_option(removeHome, "Remove home directory", "remove", 'r');
    args_parser.add_positional_argument(username, "Login user identity (username)", "login");
    args_parser.parse(argc, argv);
}

int deleteUser(const char* username, bool removeHome)
{
    FILE* pwfile = fopen("/etc/passwd", "r+");
    if (!pwfile) {
        perror("failed to open /etc/passwd");
        return 1;
    }

    int errorCode = 0;

    void* contentBuffer = nullptr;
    size_t readSize = 0;
    while (true) {
        char buffer[1024];
        auto lastPosition = ftell(pwfile);
        if (fgets(buffer, sizeof(buffer), pwfile) == nullptr) {
            if (ferror(pwfile)) {
                perror("failed to read /etc/passwd");
                errorCode = 1;
            } else {
                fprintf(stderr, "No such user: %s\n", username);
                errorCode = 6;
            }
            break;
        }
        readSize += strlen(buffer);

        String line(buffer, Chomp);
        auto parts = line.split(':', true);
        if (parts[0] == username) {
            struct stat st;
            if (stat("/etc/passwd", &st) != 0) {
                perror("stat");
                errorCode = 1;
                break;
            }

            auto remainSize = st.st_size - readSize;
            if (remainSize > 0) {
                if ((contentBuffer = malloc(remainSize)) == nullptr) {
                    fprintf(stderr, "Failed to allocate memory\n");
                    errorCode = 1;
                    break;
                }

                if (fread(contentBuffer, 1, remainSize, pwfile) < remainSize) {
                    perror("failed to read /etc/passwd");
                    errorCode = 1;
                    break;
                }
                if (fseek(pwfile, lastPosition, SEEK_SET) != 0) {
                    perror("fseek");
                    errorCode = 1;
                    break;
                }
                if (fwrite(contentBuffer, 1, remainSize, pwfile) < remainSize) {
                    perror("failed to write /etc/passwd");
                    errorCode = 1;
                    break;
                }
            }

            auto newSize = st.st_size - strlen(buffer);
            if (ftruncate(fileno(pwfile), newSize) != 0) {
                perror("failed to truncate /etc/passwd");
                errorCode = 1;
                break;
            }

            if (removeHome) {
                if (parts.size() != 7) {
                    fprintf(stderr, "Malformed entry in /etc/passwd\n");
                    errorCode = 12;
                    break;
                }

                auto home = parts[5];

                if (access(home.characters(), F_OK) != -1 && remove(true, home) != 0) {
                    perror(String::format("failed to remove %s", home.characters()).characters());
                    errorCode = 12;
                    break;
                }
            }
            break;
        }
    }


    if (fclose(pwfile) != 0) {
        perror("failed to close /etc/passwd");
        errorCode = 1;
    }
    if (contentBuffer)
        free(contentBuffer);

    return errorCode;
}

// For licensing information pertaining to this function, see the comments at the top of this file.
int remove(bool recursive, String path)
{
    struct stat path_stat;
    if (lstat(path.characters(), &path_stat) < 0) {
        perror("lstat");
        return 1;
    }

    if (S_ISDIR(path_stat.st_mode) && recursive) {
        DIR* derp = opendir(path.characters());
        if (!derp) {
            return 1;
        }

        while (auto* de = readdir(derp)) {
            if (strcmp(de->d_name, ".") != 0 && strcmp(de->d_name, "..") != 0) {
                StringBuilder builder;
                builder.append(path);
                builder.append('/');
                builder.append(de->d_name);
                int s = remove(true, builder.to_string());
                if (s < 0)
                    return s;
            }
        }
        int s = rmdir(path.characters());
        if (s < 0) {
            perror("rmdir");
            return 1;
        }
    } else {
        int rc = unlink(path.characters());
        if (rc < 0) {
            perror("unlink");
            return 1;
        }
    }
    return 0;
}