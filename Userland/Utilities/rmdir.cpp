/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <errno.h>
#include <stdio.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    if (pledge("stdio cpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    const char* path;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(path, "Directory to remove", "path");
    args_parser.parse(argc, argv);

    int rc = rmdir(path);
    if (rc < 0) {
        perror("rmdir");
        return 1;
    }
    return 0;
}
