/*
 * Copyright (c) 2026, Bastiaan van der Plaat <bastiaan.v.d.plaat@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/Group.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio wpath rpath cpath fattr"));
    TRY(Core::System::unveil("/etc/", "rwc"));
    TRY(Core::System::unveil(nullptr, nullptr));

    StringView new_name;
    StringView group_name;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Modify a group definition on the system");
    args_parser.add_option(new_name, "New name for the group", "new-name", 'n', "new-name");
    args_parser.add_positional_argument(group_name, "Name of the group to modify", "group");
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
        warnln("groupmod: group '{}' does not exist", group_name);
        return 6;
    }

    if (!new_name.is_empty()) {
        if (auto result = Core::Group::validate_name(new_name); result.is_error()) {
            warnln("groupmod: {}", result.error());
            return 1;
        }

        auto existing = TRY(Core::System::getgrnam(new_name));
        if (existing.has_value()) {
            warnln("groupmod: group '{}' already exists", new_name);
            return 9;
        }
        target->set_name(new_name);
    }

    TRY(target->sync());
    return 0;
}
