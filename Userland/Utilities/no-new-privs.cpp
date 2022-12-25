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
    Vector<StringView> command;
    Core::ArgsParser args_parser;
    int no_new_privs_mode = 1;
    bool preserve_env = false;
    args_parser.set_stop_on_first_non_option(true);
    args_parser.add_option(preserve_env, "Preserve user environment when running command", "preserve-env", 'E');
    args_parser.add_option(no_new_privs_mode, "No New Privs Mode [0 = None, 1 = Enforced , 2 = Enforced quietly]", "mode", 'm', "no-new-privs-mode");
    args_parser.add_positional_argument(command, "Command to execute", "command");
    args_parser.parse(arguments);

    TRY(Core::System::pledge("stdio rpath exec id tty"));
    TRY(Core::System::prctl(PR_SET_NO_NEW_PRIVS, no_new_privs_mode, 0, 0));
    TRY(Core::System::exec_command(command, preserve_env));
    return 0;
}
