/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ScopeGuard.h>
#include <LibCore/Account.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/GetPassword.h>
#include <stdio.h>
#include <unistd.h>

extern "C" int main(int, char**);

int main(int argc, char** argv)
{
    if (pledge("stdio rpath tty exec id", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (!isatty(STDIN_FILENO)) {
        warnln("{}: standard in is not a terminal", argv[0]);
        return 1;
    }

    const char* user = nullptr;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(user, "User to switch to (defaults to user with UID 0)", "user", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    if (geteuid() != 0) {
        warnln("Not running as root :(");
        return 1;
    }

    auto account_or_error = (user)
        ? Core::Account::from_name(user)
        : Core::Account::from_uid(0);
    if (account_or_error.is_error()) {
        warnln("Core::Account::from_name: {}", account_or_error.error());
        return 1;
    }

    if (pledge("stdio tty exec id", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    const auto& account = account_or_error.value();

    if (getuid() != 0 && account.has_password()) {
        auto password = Core::get_password();
        if (password.is_error()) {
            warnln("{}", password.error());
            return 1;
        }

        if (!account.authenticate(password.value())) {
            warnln("Incorrect or disabled password.");
            return 1;
        }
    }

    if (pledge("stdio exec id", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (!account.login()) {
        perror("Core::Account::login");
        return 1;
    }

    if (pledge("stdio exec", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    execl(account.shell().characters(), account.shell().characters(), nullptr);
    perror("execl");
    return 1;
}
