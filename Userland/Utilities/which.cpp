/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/DirIterator.h>
#include <stdio.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    if (pledge("stdio rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    const char* filename = nullptr;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(filename, "Name of executable", "executable");
    args_parser.parse(argc, argv);

    auto fullpath = Core::find_executable_in_path(filename);
    if (fullpath.is_null()) {
        warnln("no '{}' in path", filename);
        return 1;
    }

    outln("{}", fullpath);
    return 0;
}
