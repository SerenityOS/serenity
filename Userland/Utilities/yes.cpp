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
    if (pledge("stdio", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    const char* string = "yes";

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(string, "String to output (defaults to 'yes')", "string", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    for (;;)
        puts(string);
}
