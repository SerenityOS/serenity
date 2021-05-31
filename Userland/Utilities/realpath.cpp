/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <stdio.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    if (pledge("stdio rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    const char* path;

    Core::ArgsParser args_parser;
    args_parser.set_general_help(
        "Show the 'real' path of a file, by resolving all symbolic links along the way.");
    args_parser.add_positional_argument(path, "Path to resolve", "path");
    args_parser.parse(argc, argv);

    char* value = realpath(path, nullptr);
    if (value == nullptr) {
        perror("realpath");
        return 1;
    }
    outln("{}", value);
    free(value);
    return 0;
}
