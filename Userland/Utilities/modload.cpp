/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <serenity.h>
#include <string.h>

int main(int argc, char** argv)
{
    char const* path = nullptr;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(path, "Path to the module to load", "path");
    args_parser.parse(argc, argv);

    int rc = module_load(path, strlen(path));
    if (rc < 0) {
        perror("module_load");
        return 1;
    }
    return 0;
}
