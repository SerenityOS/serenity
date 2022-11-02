/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <unistd.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    unsigned jail_index = 0;
    Vector<StringView> command;
    Core::ArgsParser args_parser;
    bool preserve_env = false;
    args_parser.set_stop_on_first_non_option(true);
    args_parser.add_option(preserve_env, "Preserve user environment when running command", "preserve-env", 'E');
    args_parser.add_positional_argument(jail_index, "Jail Index", "jail index");
    args_parser.add_positional_argument(command, "Command to execute", "command");
    args_parser.parse(arguments);

    TRY(Core::System::pledge("stdio rpath exec id jail tty"));
    TRY(Core::System::join_jail(jail_index));
    TRY(Core::System::exec_command(command, preserve_env));
    return 0;
}
