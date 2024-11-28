/*
 * Copyright (c) 2020, Peter Elliott <pelliott@serenityos.org>
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ScopeGuard.h>
#include <LibCore/Account.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/GetPassword.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <string.h>
#include <unistd.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    if (geteuid() != 0) {
        warnln("Not running as root :^(");
        return 1;
    }

    TRY(Core::System::setegid(0));

    TRY(Core::System::pledge("stdio wpath rpath cpath fattr tty"));
    TRY(Core::System::unveil("/etc", "rwc"));
    TRY(Core::System::unveil(nullptr, nullptr));

    bool del = false;
    bool lock = false;
    bool unlock = false;
    StringView username {};

    auto args_parser = Core::ArgsParser();
    args_parser.set_general_help("Modify an account password.");
    args_parser.add_option(del, "Delete password", "delete", 'd');
    args_parser.add_option(lock, "Lock password", "lock", 'l');
    args_parser.add_option(unlock, "Unlock password", "unlock", 'u');
    args_parser.add_positional_argument(username, "Username", "username", Core::ArgsParser::Required::No);

    args_parser.parse(arguments);

    uid_t current_uid = getuid();

    // target_account is the account we are changing the password of.
    auto target_account = TRY(!username.is_empty()
            ? Core::Account::from_name(username)
            : Core::Account::from_uid(current_uid));

    setpwent();

    if (current_uid != 0 && current_uid != target_account.uid()) {
        warnln("You can't modify passwd for {}", username);
        return 1;
    }

    if (del) {
        target_account.delete_password();
    } else if (lock) {
        target_account.set_password_enabled(false);
    } else if (unlock) {
        target_account.set_password_enabled(true);
    } else {
        if (current_uid != 0) {
            auto current_password = TRY(Core::get_password("Current password: "sv));

            if (!target_account.authenticate(current_password)) {
                warnln("Incorrect or disabled password.");
                warnln("Password for user {} unchanged.", target_account.username());
                return 1;
            }
        }

        auto new_password = TRY(Core::get_password("New password: "sv));
        auto new_password_retype = TRY(Core::get_password("Retype new password: "sv));

        if (new_password.is_empty() && new_password_retype.is_empty()) {
            warnln("No password supplied.");
            warnln("Password for user {} unchanged.", target_account.username());
            return 1;
        }

        if (new_password.view() != new_password_retype.view()) {
            warnln("Sorry, passwords don't match.");
            warnln("Password for user {} unchanged.", target_account.username());
            return 1;
        }

        TRY(target_account.set_password(new_password));
    }

    TRY(Core::System::pledge("stdio wpath rpath cpath fattr"));

    TRY(target_account.sync());

    outln("Password for user {} successfully updated.", target_account.username());
    return 0;
}
