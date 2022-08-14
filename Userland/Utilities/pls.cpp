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

    Vector<StringView> exec_environment;
    for (size_t i = 0; environ[i]; ++i) {
        StringView env_view { environ[i], strlen(environ[i]) };
        auto maybe_needle = env_view.find('=');

        if (!maybe_needle.has_value())
            continue;

        // FIXME: Allow a custom selection of variables once ArgsParser supports options with optional parameters.
        if (!preserve_env && env_view.substring_view(0, maybe_needle.value()) != "TERM"sv)
            continue;

        exec_environment.append(env_view);
    }

    Vector<String> exec_arguments;
    exec_arguments.ensure_capacity(command.size());
    for (auto const& it : command)
        exec_arguments.append(it.to_string());

    TRY(Core::System::exec(command.at(0), command, Core::System::SearchInPath::Yes, exec_environment));
    return 0;
}
