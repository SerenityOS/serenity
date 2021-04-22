/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <LibCore/ArgsParser.h>
#include <stdio.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    if (pledge("stdio", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    const char* path = nullptr;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(path, "Path to get basename from", "path");
    args_parser.parse(argc, argv);

    printf("%s\n", LexicalPath(path).basename().characters());
    return 0;
}
