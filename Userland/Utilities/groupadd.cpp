/*
 * Copyright (c) 2019-2020, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2021, Brandon Pruitt  <brapru@pm.me>
 * Copyright (c) 2021, Maxime Friess <M4x1me@pm.me>
 * Copyright (c) 2022, Kenneth Myhra <kennethmyhra@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/Group.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio wpath rpath cpath chown"));

    gid_t gid = 0;
    StringView group_name;

    Core::ArgsParser args_parser;
    args_parser.add_option(gid, "Group ID (gid) for the new group", "gid", 'g', "gid");
    args_parser.add_positional_argument(group_name, "Name of the group (groupname)", "group");
    args_parser.parse(arguments);

    auto group = Core::Group { group_name, gid };
    TRY(Core::Group::add_group(group));

    return 0;
}
