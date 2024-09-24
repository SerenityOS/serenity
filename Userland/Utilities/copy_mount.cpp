/*
 * Copyright (c) 2024, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    StringView original_mountpoint;
    StringView target_mountpoint;

    Core::ArgsParser args_parser;
    // FIXME: Possibly allow to pass VFS root context IDs and flags?
    args_parser.add_positional_argument(original_mountpoint, "Source path", "source", Core::ArgsParser::Required::Yes);
    args_parser.add_positional_argument(target_mountpoint, "Mount point", "mountpoint", Core::ArgsParser::Required::Yes);
    args_parser.parse(arguments);

    VERIFY(!(original_mountpoint.is_null() || original_mountpoint.is_empty()));
    VERIFY(!(target_mountpoint.is_null() || target_mountpoint.is_empty()));
    TRY(Core::System::copy_mount({}, {}, original_mountpoint, target_mountpoint, 0));
    return 0;
}
