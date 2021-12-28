/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/Account.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/GetPassword.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <stdio.h>
#include <unistd.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    Vector<char const*> command;
    Core::ArgsParser args_parser;
    uid_t as_user_uid = 0;
    args_parser.set_stop_on_first_non_option(true);
    args_parser.add_option(as_user_uid, "User to execute as", nullptr, 'u', "UID");
    args_parser.add_positional_argument(command, "Command to run at elevated privilege level", "command");
    args_parser.parse(arguments);

    TRY(Core::System::pledge("stdio rpath exec id tty"));

    TRY(Core::System::seteuid(0));

    auto as_user = TRY(Core::Account::from_uid(as_user_uid));

    // If the current user is not a superuser, make them authenticate themselves.
    if (auto uid = getuid()) {
        auto account = TRY(Core::Account::from_uid(uid));
        if (account.has_password()) {
            auto password = TRY(Core::get_password());
            if (!account.authenticate(password))
                return Error::from_string_literal("Incorrect or disabled password."sv);
        }
    }

    TRY(Core::System::pledge("stdio rpath exec id"));

    TRY(Core::System::setgid(0));
    TRY(Core::System::setuid(as_user_uid));

    TRY(Core::System::pledge("stdio rpath exec"));

    Vector<char const*> exec_arguments;
    for (auto const& arg : command)
        exec_arguments.append(arg);
    exec_arguments.append(nullptr);

    Vector<String> environment_strings;
    if (auto* term = getenv("TERM"))
        environment_strings.append(String::formatted("TERM={}", term));

    Vector<char const*> exec_environment;
    for (auto& item : environment_strings)
        exec_environment.append(item.characters());
    exec_environment.append(nullptr);

    if (execvpe(command.at(0), const_cast<char**>(exec_arguments.data()), const_cast<char**>(exec_environment.data())) < 0) {
        perror("execvpe");
        exit(1);
    }
    return 0;
}
