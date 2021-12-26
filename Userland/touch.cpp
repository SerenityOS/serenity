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

#include <LibCore/ArgsParser.h>
#include <errno.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <unistd.h>
#include <utime.h>

static bool file_exists(const char* path)
{
    struct stat st;
    int rc = stat(path, &st);
    if (rc < 0) {
        if (errno == ENOENT)
            return false;
    }
    if (rc == 0) {
        return true;
    }
    perror("stat");
    exit(1);
}

int main(int argc, char** argv)
{
    if (pledge("stdio rpath cpath fattr", nullptr)) {
        perror("pledge");
        return 1;
    }

    Vector<const char*> paths;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(paths, "Files to touch", "path", Core::ArgsParser::Required::Yes);
    args_parser.parse(argc, argv);

    for (auto path : paths) {
        if (file_exists(path)) {
            int rc = utime(path, nullptr);
            if (rc < 0)
                perror("utime");
        } else {
            int fd = open(path, O_CREAT, 0100644);
            if (fd < 0) {
                perror("open");
                return 1;
            }
            int rc = close(fd);
            if (rc < 0) {
                perror("close");
                return 1;
            }
        }
    }
    return 0;
}
