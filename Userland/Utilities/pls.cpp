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
#include <unistd.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    Vector<StringView> command;
    Core::ArgsParser args_parser;
    uid_t as_user_uid = 0;
    bool preserve_env = false;
    args_parser.set_stop_on_first_non_option(true);
    args_parser.add_option(as_user_uid, "User to execute as", nullptr, 'u', "UID");
    args_parser.add_option(preserve_env, "Preserve user environment when running command", "preserve-env", 'E');
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
                return Error::from_string_literal("Incorrect or disabled password.");
        }
    }

    TRY(Core::System::pledge("stdio rpath exec id"));

    TRY(as_user.login());

    TRY(Core::System::pledge("stdio rpath exec"));
    TRY(Core::System::exec_command(command, preserve_env));
    return 0;
}
