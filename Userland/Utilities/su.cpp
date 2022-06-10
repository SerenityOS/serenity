/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Undefine <undefine@undefine.pl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ScopeGuard.h>
#include <LibCore/Account.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/GetPassword.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <stdio.h>
#include <unistd.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath tty exec id"));

    if (!TRY(Core::System::isatty(STDIN_FILENO)))
        return Error::from_string_literal("Standard input is not a terminal");

    StringView user;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(user, "User to switch to (defaults to user with UID 0)", "user", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    if (geteuid() != 0)
        return Error::from_string_literal("Not running as root :(");

    auto account = TRY(user.is_empty() ? Core::Account::from_uid(0) : Core::Account::from_name(user));

    TRY(Core::System::pledge("stdio tty exec id"));

    if (getuid() != 0 && account.has_password()) {
        auto password = TRY(Core::get_password());
        if (!account.authenticate(password))
            return Error::from_string_literal("Incorrect or disabled password.");
    }

    TRY(Core::System::pledge("stdio exec id"));

    if (!account.login()) {
        perror("Core::Account::login");
        return 1;
    }

    TRY(Core::System::pledge("stdio exec"));

    TRY(Core::System::setenv("HOME"sv, account.home_directory(), true));

    TRY(Core::System::exec(account.shell(), Array<StringView, 1> { account.shell().view() }, Core::System::SearchInPath::No));
    return 1;
}
