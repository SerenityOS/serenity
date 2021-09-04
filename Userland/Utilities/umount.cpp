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
    char const* mount_point = nullptr;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(mount_point, "Mount point", "mountpoint");
    args_parser.parse(argc, argv);

    if (umount(mount_point) < 0) {
        perror("umount");
        return 1;
    }
    return 0;
}
