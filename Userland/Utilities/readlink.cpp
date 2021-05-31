/*
 * Copyright (c) 2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <stdio.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    if (pledge("stdio rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    bool no_newline = false;
    Vector<const char*> paths;

    Core::ArgsParser args_parser;
    args_parser.add_option(no_newline, "Do not append a newline", "no-newline", 'n');
    args_parser.add_positional_argument(paths, "Symlink path", "path");
    args_parser.parse(argc, argv);

    for (const char* path : paths) {
        auto destination = Core::File::read_link(path);
        if (destination.is_null()) {
            perror(path);
            return 1;
        }
        out("{}", destination);
        if (!no_newline)
            outln();
    }

    return 0;
}
