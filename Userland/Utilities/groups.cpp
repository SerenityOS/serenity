/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <YAK/Vector.h>
#include <LibCore/Account.h>
#include <LibCore/ArgsParser.h>
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

int main(int argc, char** argv)
{
    if (unveil("/etc/passwd", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/etc/shadow", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/etc/group", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil(nullptr, nullptr) < 0) {
        perror("unveil");
        return 1;
    }

    if (pledge("stdio rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    Vector<const char*> usernames;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Print group memberships for each username or, if no username is specified, for the current process.");
    args_parser.add_positional_argument(usernames, "Usernames to list group memberships for", "usernames", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    if (usernames.is_empty()) {
        auto result = Core::Account::from_uid(geteuid());
        if (result.is_error()) {
            warnln("{}", result.error());
            return 1;
        }
        print_account_gids(result.value());
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
