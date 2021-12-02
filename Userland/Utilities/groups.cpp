/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Vector.h>
#include <LibCore/Account.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <grp.h>
#include <unistd.h>

static void print_account_gids(const Core::Account& account)
{
    auto* gr = getgrgid(account.gid());
    if (!gr) {
        outln();
        return;
    }

    out("{}", gr->gr_name);
    for (auto& gid : account.extra_gids()) {
        gr = getgrgid(gid);
        out(" {}", gr->gr_name);
    }
    outln();
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::unveil("/etc/passwd", "r"));
    TRY(Core::System::unveil("/etc/shadow", "r"));
    TRY(Core::System::unveil("/etc/group", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));
    TRY(Core::System::pledge("stdio rpath", nullptr));

    Vector<const char*> usernames;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Print group memberships for each username or, if no username is specified, for the current process.");
    args_parser.add_positional_argument(usernames, "Usernames to list group memberships for", "usernames", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    if (usernames.is_empty()) {
        auto account = TRY(Core::Account::from_uid(geteuid()));
        print_account_gids(account);
    }

    for (auto username : usernames) {
        auto result = Core::Account::from_name(username);
        if (result.is_error()) {
            warnln("{} '{}'", result.error(), username);
            continue;
        }
        out("{} : ", username);
        print_account_gids(result.value());
    }
    return 0;
}
