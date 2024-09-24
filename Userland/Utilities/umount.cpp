/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    StringView mount_point;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(mount_point, "Mount point", "mountpoint");
    args_parser.parse(arguments);

    TRY(Core::System::umount({}, mount_point));
    return 0;
}
