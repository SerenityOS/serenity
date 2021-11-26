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
#include <string.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    if (geteuid() != 0) {
        warnln("Not running as root :^(");
        return 1;
    }

    if (setegid(0) < 0) {
        perror("setegid");
        return 1;
    }

    if (pledge("stdio wpath rpath cpath fattr tty", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (unveil("/etc", "rwc") < 0) {
        perror("unveil");
        return 1;
    }

    unveil(nullptr, nullptr);

    bool del = false;
    bool lock = false;
    bool unlock = false;
    const char* username = nullptr;

    auto args_parser = Core::ArgsParser();
    args_parser.set_general_help("Modify an account password.");
    args_parser.add_option(del, "Delete password", "delete", 'd');
    args_parser.add_option(lock, "Lock password", "lock", 'l');
    args_parser.add_option(unlock, "Unlock password", "unlock", 'u');
    args_parser.add_positional_argument(username, "Username", "username", Core::ArgsParser::Required::No);

    args_parser.parse(argc, argv);

    uid_t current_uid = getuid();

    auto account_or_error = (username)
        ? Core::Account::from_name(username)
        : Core::Account::from_uid(current_uid);

    if (account_or_error.is_error()) {
        warnln("Core::Account::{}: {}", (username) ? "from_name" : "from_uid", account_or_error.error());
        return 1;
    }

    setpwent();

    if (pledge("stdio wpath rpath cpath fattr tty", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    // target_account is the account we are changing the password of.
    auto& target_account = account_or_error.value();

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
            auto current_password = Core::get_password("Current password: ");
            if (current_password.is_error()) {
                warnln("{}", current_password.error());
                return 1;
            }

            if (!target_account.authenticate(current_password.value())) {
                warnln("Incorrect or disabled password.");
                warnln("Password for user {} unchanged.", target_account.username());
                return 1;
            }
        }

        auto new_password = Core::get_password("New password: ");
        if (new_password.is_error()) {
            warnln("{}", new_password.error());
            return 1;
        }

        auto new_password_retype = Core::get_password("Retype new password: ");
        if (new_password_retype.is_error()) {
            warnln("{}", new_password_retype.error());
            return 1;
        }

        if (new_password.value().is_empty() && new_password_retype.value().is_empty()) {
            warnln("No password supplied.");
            warnln("Password for user {} unchanged.", target_account.username());
            return 1;
        }

        if (new_password.value().view() != new_password_retype.value().view()) {
            warnln("Sorry, passwords don't match.");
            warnln("Password for user {} unchanged.", target_account.username());
            return 1;
        }

        target_account.set_password(new_password.value());
    }

    if (pledge("stdio wpath rpath cpath fattr", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (!target_account.sync()) {
        perror("Core::Account::Sync");
    } else {
        outln("Password for user {} successfully updated.", target_account.username());
    }

    return 0;
}
