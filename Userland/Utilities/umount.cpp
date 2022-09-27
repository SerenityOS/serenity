/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>

decltype(serenity_main) umount_main;
ErrorOr<int> umount_main(Main::Arguments arguments)
{
    StringView mount_point;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(mount_point, "Mount point", "mountpoint");
    args_parser.parse(arguments);

    TRY(Core::System::umount(mount_point));
    return 0;
}

#ifndef EXCLUDE_SERENITY_MAIN
ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    return umount_main(arguments);
}
#endif
