/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <errno.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/stat.h>
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
    args_parser.set_general_help("Create a file, or update its mtime (time of last modification).");
    args_parser.add_ignored(nullptr, 'f');
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
