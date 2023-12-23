/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringUtils.h>
#include <LibCore/Account.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <alloca.h>
#include <grp.h>
#include <pwd.h>
#include <stdio.h>
#include <unistd.h>

static int print_id_objects(Core::Account const&);

static bool flag_print_uid = false;
static bool flag_print_gid = false;
static bool flag_print_name = false;
static bool flag_print_gid_all = false;
static ByteString user_str;

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::unveil("/etc/passwd", "r"));
    TRY(Core::System::unveil("/etc/group", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));
    TRY(Core::System::pledge("stdio rpath"));

    Core::ArgsParser args_parser;
    args_parser.add_option(flag_print_uid, "Print UID", nullptr, 'u');
    args_parser.add_option(flag_print_gid, "Print GID", nullptr, 'g');
    args_parser.add_option(flag_print_gid_all, "Print all GIDs", nullptr, 'G');
    args_parser.add_option(flag_print_name, "Print name", nullptr, 'n');
    args_parser.add_positional_argument(user_str, "User name/UID to query", "USER", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    if (flag_print_name && !(flag_print_uid || flag_print_gid || flag_print_gid_all)) {
        warnln("cannot print only names or real IDs in default format");
        return 1;
    }

    if (flag_print_uid + flag_print_gid + flag_print_gid_all > 1) {
        warnln("cannot print \"only\" of more than one choice");
        return 1;
    }

    Optional<Core::Account> account;
    if (!user_str.is_empty()) {
        if (auto user_id = user_str.to_number<uid_t>(); user_id.has_value())
            account = TRY(Core::Account::from_uid(user_id.value(), Core::Account::Read::PasswdOnly));
        else
            account = TRY(Core::Account::from_name(user_str, Core::Account::Read::PasswdOnly));
    } else {
        account = TRY(Core::Account::self(Core::Account::Read::PasswdOnly));
    }

    return print_id_objects(account.value());
}

static bool print_uid_object(Core::Account const& account)
{
    if (flag_print_name)
        out("{}", account.username());
    else
        out("{}", account.uid());

    return true;
}

static bool print_gid_object(Core::Account const& account)
{
    if (flag_print_name) {
        struct group* gr = getgrgid(account.gid());
        out("{}", gr ? gr->gr_name : "n/a");
    } else
        out("{}", account.gid());

    return true;
}

static bool print_gid_list(Core::Account const& account)
{
    auto& extra_gids = account.extra_gids();
    auto extra_gid_count = extra_gids.size();
    for (size_t g = 0; g < extra_gid_count; ++g) {
        auto gid = extra_gids[g];
        auto* gr = getgrgid(gid);
        if (flag_print_name && gr)
            out("{}", gr->gr_name);
        else
            out("{}", gid);
        if (g != extra_gid_count - 1)
            out(" ");
    }

    return true;
}

static bool print_full_id_list(Core::Account const& account)
{
    auto uid = account.uid();
    auto gid = account.gid();
    struct passwd* pw = getpwuid(uid);
    struct group* gr = getgrgid(gid);

    out("uid={}({}) gid={}({})", uid, pw ? pw->pw_name : "n/a", gid, gr ? gr->gr_name : "n/a");

    for (auto extra_gid : account.extra_gids()) {
        auto* gr = getgrgid(extra_gid);
        if (gr)
            out(" {}({})", extra_gid, gr->gr_name);
        else
            out(" {}", extra_gid);
    }

    return true;
}

static int print_id_objects(Core::Account const& account)
{
    if (flag_print_uid) {
        if (!print_uid_object(account))
            return 1;
    } else if (flag_print_gid) {
        if (!print_gid_object(account))
            return 1;
    } else if (flag_print_gid_all) {
        if (!print_gid_list(account))
            return 1;
    } else {
        if (!print_full_id_list(account))
            return 1;
    }

    outln();
    return 0;
}
