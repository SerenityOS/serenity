/*
 * Copyright (c) 2019-2020, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2021, Brandon Pruitt  <brapru@pm.me>
 * Copyright (c) 2021, Maxime Friess <M4x1me@pm.me>
 * Copyright (c) 2022, Kenneth Myhra <kennethmyhra@serenityos.org>
 * Copyright (c) 2023, Tim Ledbetter <timledbetter@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/HashTable.h>
#include <LibCore/Account.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/Group.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio wpath rpath cpath chown"));

    gid_t gid = 0;
    StringView group_name;
    Vector<ByteString> group_members;

    Core::ArgsParser args_parser;
    args_parser.add_option(gid, "Group ID (gid) for the new group", "gid", 'g', "gid");
    args_parser.add_option(Core::ArgsParser::Option {
        .argument_mode = Core::ArgsParser::OptionArgumentMode::Required,
        .help_string = "A comma-separated list of usernames to add as members of the new group",
        .long_name = "users",
        .short_name = 'U',
        .value_name = "user-list",
        .accept_value = [&group_members](StringView comma_separated_users) {
            auto accounts_or_error = Core::Account::all(Core::Account::Read::PasswdOnly);
            if (accounts_or_error.is_error())
                return false;

            OrderedHashTable<ByteString> unique_group_members;
            auto accounts = accounts_or_error.release_value();
            for (auto const& username : comma_separated_users.split_view(',')) {
                auto matching_account = accounts.first_matching([&](auto const& account) { return username == account.username(); });
                if (!matching_account.has_value()) {
                    warnln("Invalid member username: '{}'", username);
                    return false;
                }

                unique_group_members.set(matching_account->username());
            }

            for (auto const& member : unique_group_members)
                group_members.append(member);

            return true;
        },
    });
    args_parser.add_positional_argument(group_name, "Name of the group (groupname)", "group");
    args_parser.parse(arguments);

    Core::Group group { group_name, gid, group_members };
    TRY(Core::Group::add_group(group));

    return 0;
}
