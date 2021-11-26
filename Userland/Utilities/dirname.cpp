/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <LibCore/ArgsParser.h>

int main(int argc, char** argv)
{
    const char* path = nullptr;
    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(path, "Path", "path");
    args_parser.parse(argc, argv);

    outln("{}", LexicalPath::dirname(path));
    return 0;
}
