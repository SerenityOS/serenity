/*
 * Copyright (c) 2020, Peter Elliott <pelliott@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <stdio.h>
#include <sys/stat.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    if (pledge("stdio dpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    mode_t mode = 0666;
    Vector<const char*> paths;

    Core::ArgsParser args_parser;
    // FIXME: add -m for file modes
    args_parser.add_positional_argument(paths, "Paths of FIFOs to create", "paths");
    args_parser.parse(argc, argv);

    int exit_code = 0;

    for (auto path : paths) {
        if (mkfifo(path, mode) < 0) {
            perror("mkfifo");
            exit_code = 1;
        }
    }

    return exit_code;
}
