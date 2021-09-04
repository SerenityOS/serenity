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
    char const* name = nullptr;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(name, "Name of the module to unload", "name");
    args_parser.parse(argc, argv);

    int rc = module_unload(name, strlen(name));
    if (rc < 0) {
        perror("module_unload");
        return 1;
    }
    return 0;
}
