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
    bool now = false;

    Core::ArgsParser args_parser;
    args_parser.add_option(now, "Shut down now", "now", 'n');
    args_parser.parse(argc, argv);

    if (now) {
        if (halt() < 0) {
            perror("shutdown");
            return 1;
        }
    } else {
        args_parser.print_usage(stderr, argv[0]);
        return 1;
    }
}
