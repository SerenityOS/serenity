/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Undefine <undefine@undefine.pl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/Account.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/Environment.h>
#include <LibCore/GetPassword.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <unistd.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath tty exec id"));

    StringView first_positional;
    StringView second_positional;
    StringView command;
    bool simulate_login = false;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(first_positional, "See --login", "-", Core::ArgsParser::Required::No);
    args_parser.add_positional_argument(second_positional, "User to switch to (defaults to user with UID 0)", "user", Core::ArgsParser::Required::No);
    args_parser.add_option(command, "Command to execute", "command", 'c', "command");
    args_parser.add_option(simulate_login, "Simulate login", "login", 'l');
    args_parser.parse(arguments);

    StringView user = first_positional;

    if (first_positional == '-') {
        simulate_login = true;
        user = second_positional;
    }

    if (geteuid() != 0)
        return Error::from_string_literal("Not running as root :(");

    auto account = TRY(user.is_empty() ? Core::Account::from_uid(0) : Core::Account::from_name(user));

    TRY(Core::System::pledge("stdio rpath tty exec id"));

    if (getuid() != 0 && account.has_password()) {
        if (!TRY(Core::System::isatty(STDIN_FILENO)))
            return Error::from_string_literal("Standard input is not a terminal");

        auto password = TRY(Core::get_password());
        if (!account.authenticate(password))
            return Error::from_string_literal("Incorrect or disabled password.");
    }

    TRY(Core::System::pledge("stdio rpath exec id"));

    TRY(account.login());

    if (simulate_login)
        TRY(Core::System::chdir(account.home_directory()));

    TRY(Core::System::pledge("stdio exec"));

    TRY(Core::Environment::set("HOME"sv, account.home_directory(), Core::Environment::Overwrite::Yes));

    if (command.is_null()) {
        TRY(Core::System::exec(account.shell(), Array<StringView, 1> { account.shell().view() }, Core::System::SearchInPath::No));
    } else {
        TRY(Core::System::exec(account.shell(), Array<StringView, 3> { account.shell().view(), "-c"sv, command }, Core::System::SearchInPath::No));
    }

    return 1;
}
