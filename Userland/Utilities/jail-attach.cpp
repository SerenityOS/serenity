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
    StringView new_jail_name;
    Vector<StringView> command;
    Optional<size_t> existing_jail_index;
    Core::ArgsParser args_parser;
    bool preserve_env = false;
    args_parser.set_stop_on_first_non_option(true);
    args_parser.add_option(preserve_env, "Preserve user environment when running command", "preserve-env", 'E');
    args_parser.add_option(new_jail_name, "Create a new jail with a name", "jail-name", 'n', "New jail name");
    args_parser.add_option(existing_jail_index, "Use an existing jail index instead of creating new jail", "jail-index", 'i', "Existing jail index");
    args_parser.add_positional_argument(command, "Command to execute", "command");
    args_parser.parse(arguments);

    TRY(Core::System::pledge("stdio rpath exec id jail tty"));

    if (existing_jail_index.has_value() && !new_jail_name.is_null())
        return Error::from_string_view("Can't launch process in a new jail with a name and use an existing jail index."sv);

    if (existing_jail_index.has_value()) {
        TRY(Core::System::join_jail(existing_jail_index.value()));
    } else {
        // NOTE: We create a jail with "default" isolation options (as we define them in this program)
        JailIsolationFlags default_flags = (JailIsolationFlags::PIDIsolation);
        u64 new_jail_index = TRY(Core::System::create_jail(new_jail_name.is_null() ? ""sv : new_jail_name, default_flags));
        TRY(Core::System::join_jail(new_jail_index));
    }
    TRY(Core::System::exec_command(command, preserve_env));
    return 0;
}
