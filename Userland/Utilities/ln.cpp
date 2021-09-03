/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <YAK/LexicalPath.h>
#include <LibCore/ArgsParser.h>
#include <stdio.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    if (pledge("stdio cpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    bool symbolic = false;
    const char* target = nullptr;
    const char* path = nullptr;

    Core::ArgsParser args_parser;
    args_parser.add_option(symbolic, "Create a symlink", "symbolic", 's');
    args_parser.add_positional_argument(target, "Link target", "target");
    args_parser.add_positional_argument(path, "Link path", "path", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    String path_buffer;
    if (!path) {
        path_buffer = LexicalPath::basename(target);
        path = path_buffer.characters();
    }

    if (symbolic) {
        int rc = symlink(target, path);
        if (rc < 0) {
            perror("symlink");
            return 1;
        }
        return 0;
    }

    int rc = link(target, path);
    if (rc < 0) {
        perror("link");
        return 1;
    }
    return 0;
}
