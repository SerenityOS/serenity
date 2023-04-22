/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/API/Syscall.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <unistd.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    int jail_index;
    Core::ArgsParser args_parser;
    bool set_clean_on_last_detach = false;
    bool unset_clean_on_last_detach = false;
    args_parser.add_positional_argument(jail_index, "Configured jail index", "jail index");
    args_parser.add_option(set_clean_on_last_detach, "Set jail to clean itself on last detach (may immediately clean the jail if no process is attached)", "set-clean-on-last-detach", 'c');
    args_parser.add_option(unset_clean_on_last_detach, "Unset jail to clean itself on last detach", "unset-clean-on-last-detach", 's');
    args_parser.parse(arguments);

    TRY(Core::System::pledge("stdio jail"));

    if (set_clean_on_last_detach && unset_clean_on_last_detach)
        return Error::from_string_view("Can't unset and set clean on last detach at the same time."sv);

    if (set_clean_on_last_detach) {
        TRY(Core::System::configure_jail(jail_index, static_cast<u64>(JailConfigureRequest::SetCleanOnLastDetach), 0, 0));
    }
    if (unset_clean_on_last_detach) {
        TRY(Core::System::configure_jail(jail_index, static_cast<u64>(JailConfigureRequest::UnsetCleanOnLastDetach), 0, 0));
    }

    return 0;
}
