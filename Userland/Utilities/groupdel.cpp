/*
 * Copyright (c) 2020, Fei Wu <f.eiwu@yahoo.com>
 * Copyright (c) 2021, Brandon Pruitt <brapru@pm.me>
 * Copyright (c) 2021, Maxime Friess <M4x1me@pm.me>
 * Copyright (c) 2022, Umut İnan Erdoğan <umutinanerdogan62@gmail.com>
 * Copyright (c) 2026, Bastiaan van der Plaat <bastiaan.v.d.plaat@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/Account.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/Group.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio wpath rpath cpath fattr"));
    TRY(Core::System::unveil("/etc/", "rwc"));
    TRY(Core::System::unveil(nullptr, nullptr));

    StringView group_name;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Delete a group from the system group file");
    args_parser.add_positional_argument(group_name, "Name of the group to delete", "group");
    args_parser.parse(arguments);

    auto groups = TRY(Core::Group::all());
    Optional<Core::Group> target;
    for (auto& group : groups) {
        if (group.name() == group_name) {
            target = move(group);
            break;
        }
    }
    if (!target.has_value()) {
        warnln("groupdel: group '{}' does not exist", group_name);
        return 6;
    }

    auto accounts = TRY(Core::Account::all(Core::Account::Read::PasswdOnly));
    for (auto const& account : accounts) {
        if (account.gid() == target->id()) {
            warnln("groupdel: cannot remove the primary group of user '{}'", account.username());
            return 8;
        }
    }

    TRY(Core::Group::delete_group(group_name));
    return 0;
}
