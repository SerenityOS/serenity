/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <limits.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    const char* hostname = nullptr;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(hostname, "Hostname to set", "hostname", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    if (!hostname) {
        char buffer[HOST_NAME_MAX];
        int rc = gethostname(buffer, sizeof(buffer));
        if (rc < 0) {
            perror("gethostname");
            return 1;
        }
        outln("{}", buffer);
    } else {
        if (strlen(hostname) >= HOST_NAME_MAX) {
            warnln("Hostname must be less than {} characters", HOST_NAME_MAX);
            return 1;
        }
        int rc = sethostname(hostname, strlen(hostname));
        if (rc < 0) {
            perror("sethostname");
            return 1;
        }
    }
    return 0;
}
