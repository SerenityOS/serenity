/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/Account.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/GetPassword.h>
#include <stdio.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    Vector<char const*> command;
    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(command, "Command to run at elevated privilege level", "command");
    args_parser.parse(argc, argv);

    if (pledge("stdio rpath exec id tty", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (seteuid(0) < 0) {
        perror("seteuid");
        return 1;
    }

    // If the current user is not a superuser, make them authenticate themselves.
    if (auto uid = getuid()) {
        auto account_or_error = Core::Account::from_uid(uid);
        if (account_or_error.is_error()) {
            warnln("{}", account_or_error.error());
            return 1;
        }

        auto const& account = account_or_error.value();
        if (account.has_password()) {
            auto password_or_error = Core::get_password();
            if (password_or_error.is_error()) {
                warnln("{}", password_or_error.error());
                return 1;
            }

            auto const& password = password_or_error.value();
            if (!account.authenticate(password.characters())) {
                warnln("Incorrect or disabled password.");
                return 1;
            }
        }
    }

    if (pledge("stdio rpath exec id", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (setgid(0) < 0) {
        perror("setgid");
        return 1;
    }

    if (setuid(0) < 0) {
        perror("setuid");
        return 1;
    }

    if (pledge("stdio rpath exec", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    Vector<char const*> arguments;
    for (auto const& arg : command)
        arguments.append(arg);
    arguments.append(nullptr);

    Vector<String> environment_strings;
    if (auto* term = getenv("TERM"))
        environment_strings.append(String::formatted("TERM={}", term));

    Vector<char const*> environment;
    for (auto& item : environment_strings)
        environment.append(item.characters());
    environment.append(nullptr);

    if (execvpe(command.at(0), const_cast<char**>(arguments.data()), const_cast<char**>(environment.data())) < 0) {
        perror("execvpe");
        exit(1);
    }
    return 0;
}
